package fr.traqueur.nexus.infrastructure.rest;

import fr.traqueur.nexus.application.ports.in.ManageWorkflows;
import fr.traqueur.nexus.bootstrap.RegistriesConfig;
import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.domain.workflow.conditions.EqualsCondition;
import fr.traqueur.nexus.infrastructure.serialization.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The HTTP contract, without a database.
 *
 * <p>The real {@code WorkflowDtoMapper} is imported rather than mocked: the thing
 * worth asserting here is that a request body becomes a domain object and that
 * the domain's invariants come back as 400 rather than 500, which a mocked mapper
 * would assert nothing about.
 */
@WebMvcTest(WorkflowController.class)
@Import({JacksonConfig.class, RegistriesConfig.class, WorkflowDtoMapper.class})
@DisplayName("WorkflowController")
class WorkflowControllerTest {

    private static final String VALID_BODY = """
            {
              "events": ["github.push_received"],
              "condition": {"type": "equals", "field": "branch", "value": "main"},
              "actions": [
                {"type": "send_email", "subject": "Pushed", "content": "body", "to": "dev@example.com"}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManageWorkflows workflows;

    private static Workflow workflow(String id) {
        return new Workflow(
                id,
                List.of(EventType.of("github.push_received")),
                new EqualsCondition("branch", "main"),
                List.of(new SendEmailAction("Pushed", "body", "dev@example.com")));
    }

    @Nested
    @DisplayName("GET /api/v1/workflows")
    class ListWorkflows {

        @Test
        @DisplayName("should return 200 with every workflow")
        void shouldListWorkflows() throws Exception {
            when(workflows.findAll()).thenReturn(List.of(workflow("first"), workflow("second")));

            mockMvc.perform(get("/api/v1/workflows"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("first"))
                    .andExpect(jsonPath("$[1].id").value("second"));
        }

        @Test
        @DisplayName("should return 200 with an empty array when there is none")
        void shouldReturnEmptyArray() throws Exception {
            when(workflows.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/workflows"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/workflows/{id}")
    class GetWorkflow {

        @Test
        @DisplayName("should return 200 with the workflow when found")
        void shouldReturnWorkflow() throws Exception {
            when(workflows.findById("notify")).thenReturn(Optional.of(workflow("notify")));

            mockMvc.perform(get("/api/v1/workflows/{id}", "notify"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("notify"))
                    .andExpect(jsonPath("$.events[0]").value("github.push_received"))
                    // Written by the registry-backed serializer, so the client sees
                    // the registered identifier and never a Java class name.
                    .andExpect(jsonPath("$.condition.type").value("equals"))
                    .andExpect(jsonPath("$.condition.field").value("branch"))
                    .andExpect(jsonPath("$.actions[0].type").value("send_email"))
                    .andExpect(jsonPath("$.actions[0].to").value("dev@example.com"));
        }

        @Test
        @DisplayName("should return 404 when the workflow does not exist")
        void shouldReturn404() throws Exception {
            when(workflows.findById("missing")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/workflows/{id}", "missing"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/workflows/{id}")
    class PutWorkflow {

        @Test
        @DisplayName("should return 200 and store the workflow built from the body")
        void shouldStoreWorkflow() throws Exception {
            mockMvc.perform(put("/api/v1/workflows/{id}", "notify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("notify"))
                    .andExpect(jsonPath("$.condition.type").value("equals"));

            // The id comes from the path, not the body: the body carries none.
            verify(workflows).save(workflow("notify"));
        }

        @Test
        @DisplayName("should return 400 when the workflow declares no action")
        void shouldRejectAWorkflowWithoutActions() throws Exception {
            // The invariant lives in the Workflow record. Without the translation in
            // WorkflowDtoMapper this is a 500 — the server reporting that it broke,
            // when in fact it refused the request on purpose.
            String noActions = """
                    {
                      "events": ["github.push_received"],
                      "condition": {"type": "always"},
                      "actions": []
                    }
                    """;

            mockMvc.perform(put("/api/v1/workflows/{id}", "empty")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(noActions))
                    .andExpect(status().isBadRequest());

            verify(workflows, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when the workflow declares no event type")
        void shouldRejectAWorkflowWithoutEvents() throws Exception {
            String noEvents = """
                    {
                      "events": [],
                      "condition": {"type": "always"},
                      "actions": [{"type": "send_email", "subject": "s", "content": "c", "to": "d@e.f"}]
                    }
                    """;

            mockMvc.perform(put("/api/v1/workflows/{id}", "empty")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(noEvents))
                    .andExpect(status().isBadRequest());

            verify(workflows, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when a condition type is not registered")
        void shouldRejectAnUnknownConditionType() throws Exception {
            // Resolved at the edge by the registry-backed deserializer, so an unknown
            // type is a bad request here rather than a failure on the way to storage.
            String unknownCondition = """
                    {
                      "events": ["github.push_received"],
                      "condition": {"type": "sorcery", "field": "branch"},
                      "actions": [{"type": "send_email", "subject": "s", "content": "c", "to": "d@e.f"}]
                    }
                    """;

            mockMvc.perform(put("/api/v1/workflows/{id}", "unknown")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(unknownCondition))
                    .andExpect(status().isBadRequest());

            verify(workflows, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/workflows/{id}")
    class DeleteWorkflow {

        @Test
        @DisplayName("should return 204 when the workflow was removed")
        void shouldReturn204() throws Exception {
            when(workflows.delete("notify")).thenReturn(true);

            mockMvc.perform(delete("/api/v1/workflows/{id}", "notify"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when there was nothing to remove")
        void shouldReturn404() throws Exception {
            when(workflows.delete("missing")).thenReturn(false);

            mockMvc.perform(delete("/api/v1/workflows/{id}", "missing"))
                    .andExpect(status().isNotFound());
        }
    }
}