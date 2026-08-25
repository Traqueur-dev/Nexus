package fr.traqueur.nexus.infrastructure.rest;

import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.infrastructure.rest.dto.WorkflowRequestDto;
import fr.traqueur.nexus.infrastructure.rest.dto.WorkflowResponseDto;
import fr.traqueur.nexus.infrastructure.rest.exceptions.InvalidWorkflowException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates between the REST representation of a workflow and the domain one.
 *
 * <p>Needs no {@code ObjectMapper}: the conditions and actions are already domain
 * objects by the time they get here, decoded by the message converter. An earlier
 * version of the event mapper encoded JSON by hand and the converter encoded the
 * result again (#32); typing the DTO fields as domain types is what avoids that.
 */
@Component
public class WorkflowDtoMapper {

    /**
     * Builds the domain object, turning its invariants into a 400.
     *
     * <p>The construction is what validates: {@link Workflow} refuses an empty
     * event list, an empty action list or a missing condition. Catching here is
     * what turns "the domain rejected this" into "your request was wrong", and
     * keeps the malformed-input concern in the adapter that received it — the same
     * placement as {@code EventController.parseId}.
     */
    public Workflow toDomain(String id, WorkflowRequestDto dto) {
        try {
            return new Workflow(
                    id,
                    dto.events().stream().map(EventType::of).toList(),
                    dto.condition(),
                    dto.actions());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidWorkflowException(id, e);
        }
    }

    public WorkflowResponseDto toDto(Workflow workflow) {
        return new WorkflowResponseDto(
                workflow.id(),
                workflow.events().stream().map(EventType::value).toList(),
                workflow.condition(),
                workflow.actions());
    }

    public List<WorkflowResponseDto> toDto(List<Workflow> workflows) {
        return workflows.stream().map(this::toDto).toList();
    }
}