package fr.traqueur.nexus.infrastructure.rest.dto;

import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.Condition;

import java.util.List;

/**
 * The body of a workflow write. Carries no id — the id is the path.
 *
 * <p>{@code condition} and {@code actions} are typed as the domain types rather
 * than as raw JSON, for the same reason {@link EventResponseDto} carries a
 * {@code Context}: the one configured mapper reads them, so a client posts
 * {@code {"type":"equals",…}} and the registry resolves it. An unknown type
 * fails here, at the edge, as a 400 — not three layers down on the way to the
 * database.
 */
public record WorkflowRequestDto(
        List<String> events,
        Condition condition,
        List<Action> actions
) {
}