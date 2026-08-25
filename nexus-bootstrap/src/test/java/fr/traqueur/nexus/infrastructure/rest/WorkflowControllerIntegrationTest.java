package fr.traqueur.nexus.infrastructure.rest;

import fr.traqueur.nexus.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.domain.workflow.conditions.CompositeCondition;
import fr.traqueur.nexus.domain.workflow.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole path, HTTP to PostgreSQL and back (#10).
 *
 * <p>{@code WorkflowControllerTest} asserts the contract against a mocked port;
 * this asserts that a workflow posted over HTTP is the workflow the engine will
 * later read out of the database — including its condition tree, which crosses
 * two encodings on the way.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("Workflows API")
class WorkflowControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-alpine");

    @Autowired
    private MockMvc mockMvc;

    /* Asserted through the outbound port: what matters is what the engine will read. */
    @Autowired
    private WorkflowRepository workflows;

    private static final String NESTED_BODY = """
            {
              "events": ["github.push_received", "discord.message_received"],
              "condition": {
                "type": "composite",
                "operator": "AND",
                "rules": [
                  {"type": "equals", "field": "branch", "value": "main"},
                  {"type": "group", "minRequirements": 1, "rules": [
                    {"type": "contains", "field": "content", "value": "urgent"},
                    {"type": "always"}
                  ]}
                ]
              },
              "actions": [
                {"type": "send_email", "subject": "Pushed", "content": "body", "to": "dev@example.com"}
              ]
            }
            """;

    private static String id(String name) {
        return name + "-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("should store a workflow put over HTTP, condition tree included")
    void shouldStoreAWorkflowPutOverHttp() throws Exception {
        String id = id("nested");

        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NESTED_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        Optional<Workflow> stored = workflows.findById(id);
        assertThat(stored).isPresent();
        assertThat(stored.get().events())
                .containsExactly(EventType.of("github.push_received"), EventType.of("discord.message_received"));

        Condition condition = stored.get().condition();
        assertThat(condition).isInstanceOf(CompositeCondition.class);
        assertThat(((CompositeCondition) condition).rules()).hasSize(2);

        // And the engine finds it by event type, which is the only reason it is stored.
        assertThat(workflows.findTriggeredBy(EventType.of("github.push_received")))
                .anyMatch(workflow -> workflow.id().equals(id));
    }

    @Test
    @DisplayName("should read back over HTTP exactly what was written")
    void shouldRoundTripOverHttp() throws Exception {
        String id = id("roundtrip");

        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NESTED_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workflows/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.condition.type").value("composite"))
                .andExpect(jsonPath("$.condition.rules[1].type").value("group"))
                .andExpect(jsonPath("$.condition.rules[1].rules[0].value").value("urgent"))
                .andExpect(jsonPath("$.actions[0].type").value("send_email"));
    }

    @Test
    @DisplayName("should replace a workflow put twice under the same id")
    void shouldReplaceOnSecondPut() throws Exception {
        String id = id("edited");
        String simpler = """
                {
                  "events": ["github.push_received"],
                  "condition": {"type": "always"},
                  "actions": [{"type": "send_email", "subject": "s", "content": "c", "to": "d@e.f"}]
                }
                """;

        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(NESTED_BODY));
        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(simpler))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workflows/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition.type").value("always"))
                .andExpect(jsonPath("$.events.length()").value(1));

        assertThat(workflows.findAll().stream().filter(w -> w.id().equals(id))).hasSize(1);
    }

    @Test
    @DisplayName("should list stored workflows")
    void shouldListWorkflows() throws Exception {
        String id = id("listed");

        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(NESTED_BODY));

        mockMvc.perform(get("/api/v1/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());
    }

    @Test
    @DisplayName("should delete a stored workflow and then answer 404")
    void shouldDeleteAWorkflow() throws Exception {
        String id = id("deleted");

        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(NESTED_BODY));

        mockMvc.perform(delete("/api/v1/workflows/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(workflows.findById(id)).isEmpty();

        mockMvc.perform(get("/api/v1/workflows/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/workflows/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should reject an invalid workflow without storing anything")
    void shouldRejectAnInvalidWorkflow() throws Exception {
        String id = id("invalid");
        String noActions = """
                {
                  "events": ["github.push_received"],
                  "condition": {"type": "always"},
                  "actions": []
                }
                """;

        mockMvc.perform(put("/api/v1/workflows/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noActions))
                .andExpect(status().isBadRequest());

        // The CHECK constraint would have caught this too; the point is that the
        // request never reached it.
        assertThat(workflows.findById(id)).isEmpty();
    }
}