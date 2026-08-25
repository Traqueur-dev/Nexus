package fr.traqueur.nexus.infrastructure.persistence;

import fr.traqueur.nexus.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.domain.workflow.conditions.AlwaysCondition;
import fr.traqueur.nexus.domain.workflow.conditions.CompositeCondition;
import fr.traqueur.nexus.domain.workflow.conditions.ContainsCondition;
import fr.traqueur.nexus.domain.workflow.conditions.EqualsCondition;
import fr.traqueur.nexus.domain.workflow.conditions.GroupCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Workflow persistence against a real PostgreSQL (#6).
 *
 * <p>The interesting column is not the id: it is {@code condition}, an open,
 * recursive hierarchy stored as JSONB. A round-trip through it is what proves the
 * storage decision — that a plugin's condition type needs no schema change, and
 * that nodes are named by their registered identifier rather than by a Java class
 * name that a package move would invalidate.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Workflow persistence")
class WorkflowRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-alpine");

    private static final EventType GITHUB_PUSH = EventType.of("github.push_received");
    private static final EventType DISCORD_MESSAGE = EventType.of("discord.message_received");

    @Autowired
    private WorkflowRepository workflows;

    @Test
    @DisplayName("should round-trip a workflow through the database")
    void shouldRoundTripAWorkflow() {
        Workflow workflow = new Workflow(
                id("simple"),
                List.of(GITHUB_PUSH),
                new EqualsCondition("branch", "main"),
                List.of(new SendEmailAction("Pushed", "Someone pushed to main", "dev@example.com")));

        workflows.save(workflow);

        assertThat(single(GITHUB_PUSH, workflow.id())).isEqualTo(workflow);
    }

    @Test
    @DisplayName("should preserve a deeply nested condition tree")
    void shouldPreserveNestedConditions() {
        // The shape a relational model would have needed an adjacency table for,
        // and the reason the column is JSONB.
        Condition condition = new CompositeCondition(
                Condition.Operator.OR,
                List.of(
                        new ContainsCondition("content", "urgent"),
                        new GroupCondition(2, List.of(
                                new EqualsCondition("authorId", "123"),
                                new EqualsCondition("authorId", "456"),
                                new CompositeCondition(Condition.Operator.AND, List.of(
                                        new ContainsCondition("content", "help"),
                                        new AlwaysCondition()))))));

        Workflow workflow = new Workflow(
                id("nested"), List.of(DISCORD_MESSAGE), condition,
                List.of(new SendEmailAction("Subject", "Body", "dev@example.com")));

        workflows.save(workflow);

        assertThat(single(DISCORD_MESSAGE, workflow.id()).condition()).isEqualTo(condition);
    }

    @Test
    @DisplayName("should preserve several actions in order")
    void shouldPreserveActions() {
        List<Action> actions = List.of(
                new SendEmailAction("First", "one", "a@example.com"),
                new SendEmailAction("Second", "two", "b@example.com"));

        Workflow workflow = new Workflow(
                id("actions"), List.of(GITHUB_PUSH), new AlwaysCondition(), actions);

        workflows.save(workflow);

        assertThat(single(GITHUB_PUSH, workflow.id()).actions()).containsExactlyElementsOf(actions);
    }

    @Test
    @DisplayName("should return only the workflows reacting to the event type")
    void shouldFilterByEventType() {
        // The containment query, which is the whole reason the port is shaped as
        // findTriggeredBy rather than findAll.
        Workflow githubOnly = workflow(id("github-only"), List.of(GITHUB_PUSH));
        Workflow discordOnly = workflow(id("discord-only"), List.of(DISCORD_MESSAGE));
        Workflow both = workflow(id("both"), List.of(GITHUB_PUSH, DISCORD_MESSAGE));

        workflows.save(githubOnly);
        workflows.save(discordOnly);
        workflows.save(both);

        assertThat(workflows.findTriggeredBy(GITHUB_PUSH))
                .contains(githubOnly, both)
                .doesNotContain(discordOnly);
    }

    @Test
    @DisplayName("should return nothing for an event type no workflow declares")
    void shouldReturnNothingForAnUnknownEventType() {
        assertThat(workflows.findTriggeredBy(EventType.of("minecraft.player_joined"))).isEmpty();
    }

    @Test
    @DisplayName("should replace a workflow saved again under the same id")
    void shouldReplaceOnSave() {
        // Upsert, unlike the event store. A workflow is configuration the user
        // edits; refusing the second write would make it uneditable (#27 is about
        // events, which are facts, and does not apply here).
        String id = id("edited");
        workflows.save(workflow(id, List.of(GITHUB_PUSH)));

        Workflow edited = new Workflow(
                id, List.of(GITHUB_PUSH), new EqualsCondition("branch", "develop"),
                List.of(new SendEmailAction("Edited", "body", "dev@example.com")));
        workflows.save(edited);

        assertThat(single(GITHUB_PUSH, id)).isEqualTo(edited);
        assertThat(workflows.findTriggeredBy(GITHUB_PUSH).stream().filter(w -> w.id().equals(id)))
                .hasSize(1);
    }

    /*
     * Each test writes into the same database, and none of them cares about the
     * others' rows. Unique ids keep them independent without a truncation hook,
     * and the assertions filter rather than assume an empty table.
     */
    private static String id(String name) {
        return name + "-" + java.util.UUID.randomUUID();
    }

    private static Workflow workflow(String id, List<EventType> events) {
        return new Workflow(id, events, new AlwaysCondition(),
                List.of(new SendEmailAction("Subject", "Body", "dev@example.com")));
    }

    private Workflow single(EventType type, String id) {
        return workflows.findTriggeredBy(type).stream()
                .filter(workflow -> workflow.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no workflow stored under " + id));
    }
}