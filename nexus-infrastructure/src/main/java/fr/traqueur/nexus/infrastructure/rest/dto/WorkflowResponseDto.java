package fr.traqueur.nexus.infrastructure.rest.dto;

import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.Condition;

import java.util.List;

/** The REST representation of a workflow: the request body plus its id. */
public record WorkflowResponseDto(
        String id,
        List<String> events,
        Condition condition,
        List<Action> actions
) {
}