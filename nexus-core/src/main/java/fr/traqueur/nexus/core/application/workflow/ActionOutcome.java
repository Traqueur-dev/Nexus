package fr.traqueur.nexus.core.application.workflow;

import fr.traqueur.nexus.core.domain.workflow.Action;

/**
 * What became of one action a workflow asked for.
 *
 * @param action  the intent that was carried out, or attempted
 * @param failure why it failed, or {@code null} when it succeeded
 */
public record ActionOutcome(Action action, String failure) {

    public static ActionOutcome succeeded(Action action) {
        return new ActionOutcome(action, null);
    }

    public static ActionOutcome failed(Action action, String reason) {
        return new ActionOutcome(action, reason == null ? "unknown failure" : reason);
    }

    public boolean succeeded() {
        return failure == null;
    }
}
