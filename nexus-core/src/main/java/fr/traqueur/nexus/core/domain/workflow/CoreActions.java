package fr.traqueur.nexus.core.domain.workflow;

import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;

import java.util.List;

/**
 * The action types shipped by the core itself.
 *
 * <p>Same rationale as {@link CoreConditions}: the hierarchy is open, so the set
 * of built-in actions is declared rather than discovered.
 */
public final class CoreActions {

    private CoreActions() {
    }

    public static List<Class<? extends Action>> types() {
        return List.of(
                SendEmailAction.class
        );
    }
}
