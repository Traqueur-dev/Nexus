package fr.traqueur.nexus.core.domain.workflow;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventType;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.core.domain.workflow.conditions.AlwaysCondition;
import fr.traqueur.nexus.core.domain.workflow.conditions.EqualsCondition;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test: no Spring, no database, no mocks. That is the point of keeping
 * the decision in the domain and the execution behind a port.
 */
@DisplayName("Workflow")
class WorkflowTest {

    private static final EventType DISCORD_MESSAGE = EventType.of("discord.message_received");
    private static final EventType GITHUB_PUSH = EventType.of("github.push_received");

    private static final Action AN_ACTION = new SendEmailAction("subject", "body", "me@example.com");

    private static Event discordMessage(String content) {
        return new DiscordMessageReceived(
                new Event.Id("discord", "abc123"), new DiscordContext(),
                Instant.parse("2026-01-04T10:00:00Z"), content, 42L);
    }

    private static Workflow workflow(Condition condition) {
        return new Workflow("wf-1", List.of(DISCORD_MESSAGE), condition, List.of(AN_ACTION));
    }

    @Nested
    @DisplayName("invariants")
    class Invariants {

        @Test
        @DisplayName("should reject a blank id")
        void shouldRejectBlankId() {
            assertThatThrownBy(() -> new Workflow("  ", List.of(DISCORD_MESSAGE), new AlwaysCondition(), List.of(AN_ACTION)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should reject a workflow triggered by nothing")
        void shouldRejectEmptyEvents() {
            assertThatThrownBy(() -> new Workflow("wf-1", List.of(), new AlwaysCondition(), List.of(AN_ACTION)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one event type");
        }

        @Test
        @DisplayName("should reject a workflow that does nothing")
        void shouldRejectEmptyActions() {
            assertThatThrownBy(() -> new Workflow("wf-1", List.of(DISCORD_MESSAGE), new AlwaysCondition(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one action");
        }

        @Test
        @DisplayName("should require a condition")
        void shouldRequireCondition() {
            assertThatThrownBy(() -> new Workflow("wf-1", List.of(DISCORD_MESSAGE), null, List.of(AN_ACTION)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should not be affected by later changes to the lists passed in")
        void shouldCopyLists() {
            List<EventType> events = new ArrayList<>(List.of(DISCORD_MESSAGE));
            List<Action> actions = new ArrayList<>(List.of(AN_ACTION));

            Workflow workflow = new Workflow("wf-1", events, new AlwaysCondition(), actions);
            events.add(GITHUB_PUSH);
            actions.clear();

            assertThat(workflow.events()).containsExactly(DISCORD_MESSAGE);
            assertThat(workflow.actions()).containsExactly(AN_ACTION);
        }
    }

    @Nested
    @DisplayName("matches")
    class Matches {

        @Test
        @DisplayName("should fire when the type is declared and the condition holds")
        void shouldFire() throws ConditionEvaluationException {
            assertThat(workflow(new AlwaysCondition()).matches(DISCORD_MESSAGE, discordMessage("hello"))).isTrue();
        }

        @Test
        @DisplayName("should not fire for an undeclared event type")
        void shouldNotFireForOtherType() throws ConditionEvaluationException {
            assertThat(workflow(new AlwaysCondition()).matches(GITHUB_PUSH, discordMessage("hello"))).isFalse();
        }

        @Test
        @DisplayName("should not fire when the condition does not hold")
        void shouldNotFireWhenConditionFails() throws ConditionEvaluationException {
            Workflow workflow = workflow(new EqualsCondition("content", "expected"));

            assertThat(workflow.matches(DISCORD_MESSAGE, discordMessage("something else"))).isFalse();
        }

        @Test
        @DisplayName("should not evaluate the condition for an undeclared type")
        void shouldShortCircuitBeforeEvaluatingCondition() throws ConditionEvaluationException {
            // The condition references a field this event does not have, so it would
            // throw if it were evaluated.
            Workflow workflow = workflow(new EqualsCondition("no_such_field", "x"));

            assertThat(workflow.matches(GITHUB_PUSH, discordMessage("hello"))).isFalse();
        }

        @Test
        @DisplayName("should propagate a condition that cannot be evaluated")
        void shouldPropagateEvaluationFailure() {
            Workflow workflow = workflow(new EqualsCondition("no_such_field", "x"));

            assertThatThrownBy(() -> workflow.matches(DISCORD_MESSAGE, discordMessage("hello")))
                    .isInstanceOf(ConditionEvaluationException.class);
        }
    }

    @Test
    @DisplayName("triggersOn should report declared types")
    void triggersOnShouldReportDeclaredTypes() {
        Workflow workflow = workflow(new AlwaysCondition());

        assertThat(workflow.triggersOn(DISCORD_MESSAGE)).isTrue();
        assertThat(workflow.triggersOn(GITHUB_PUSH)).isFalse();
    }
}
