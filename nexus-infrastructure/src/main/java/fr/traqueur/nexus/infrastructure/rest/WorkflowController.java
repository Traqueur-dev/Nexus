package fr.traqueur.nexus.infrastructure.rest;

import fr.traqueur.nexus.application.ports.in.ManageWorkflows;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.infrastructure.rest.dto.WorkflowRequestDto;
import fr.traqueur.nexus.infrastructure.rest.dto.WorkflowResponseDto;
import fr.traqueur.nexus.infrastructure.rest.exceptions.WorkflowNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Managing workflows over HTTP (#10).
 *
 * <p><b>Why PUT and no POST.</b> A workflow id is chosen by the user and is
 * meaningful — {@code notify-on-main-push}, not a generated key — and the store
 * is an upsert. That makes writing a workflow idempotent and addressable, which
 * is what PUT means. A POST to the collection would have to invent an id the
 * caller did not ask for, and then "create" and "update" would be two operations
 * where the system only has one.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final ManageWorkflows workflows;
    private final WorkflowDtoMapper mapper;

    /**
     * Depends on the inbound port rather than on {@code WorkflowService}, for the
     * same reason {@link EventController} does: the adapter states what it needs
     * from the application and nothing more.
     */
    public WorkflowController(ManageWorkflows workflows, WorkflowDtoMapper mapper) {
        this.workflows = workflows;
        this.mapper = mapper;
    }

    @GetMapping
    public List<WorkflowResponseDto> listWorkflows() {
        return mapper.toDto(workflows.findAll());
    }

    @GetMapping("/{id}")
    public WorkflowResponseDto getWorkflow(@PathVariable String id) {
        return workflows.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new WorkflowNotFoundException(id));
    }

    /**
     * Creates or replaces the workflow at this id.
     *
     * <p>Answers 200 in both cases rather than distinguishing 201 on creation.
     * Telling them apart means reading before every write to learn something the
     * caller already knows — it chose the id — or widening the port's {@code save}
     * to report it, which would put an HTTP status concern in the application
     * layer. The stored representation comes back either way.
     */
    @PutMapping("/{id}")
    public WorkflowResponseDto putWorkflow(@PathVariable String id, @RequestBody WorkflowRequestDto body) {
        Workflow workflow = mapper.toDomain(id, body);
        workflows.save(workflow);
        return mapper.toDto(workflow);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkflow(@PathVariable String id) {
        if (!workflows.delete(id)) {
            throw new WorkflowNotFoundException(id);
        }
    }
}