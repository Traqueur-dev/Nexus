package fr.traqueur.nexus.core.domain.workflow;

import java.util.List;

public record Workflow(
        String id,
        List<String> events,
        Condition condition,
        List<Action> actions
) {
}
