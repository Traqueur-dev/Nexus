package fr.traqueur.nexus.application.workflow;

import fr.traqueur.nexus.domain.workflow.Action;

import java.util.Objects;

/**
 * What became of one action a workflow asked for.
 *
 * <p>Carries the registered identifier alongside the action so a report — a log
 * line today, an execution history row later — names it by its stable contract
 * rather than by a Java class name that is free to move.
 *
 * @param actionType the registered identifier, e.g. {@code send_email}
 * @param action     the intent that was carried out, or attempted
 * @param failure    why it failed, or {@code null} when it succeeded
 */
public record ActionOutcome(String actionType, Action action, String failure) {

    static final String UNKNOWN_FAILURE = "unknown failure";

    public static ActionOutcome succeeded(String actionType, Action action) {
        return new ActionOutcome(actionType, action, null);
    }

    public static ActionOutcome failed(String actionType, Action action, String reason) {
        return new ActionOutcome(actionType, action, Objects.requireNonNullElse(reason, UNKNOWN_FAILURE));
    }

    public boolean succeeded() {
        return failure == null;
    }
}
