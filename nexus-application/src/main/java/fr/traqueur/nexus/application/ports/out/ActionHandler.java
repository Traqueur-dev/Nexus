package fr.traqueur.nexus.application.ports.out;

import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.exceptions.ActionExecutionException;

/**
 * Outbound port: performs one kind of {@link Action}.
 *
 * <p>An action describes an intent and nothing more — it cannot send an email or
 * call a webhook, because doing so needs a mail client or an HTTP client, and
 * putting either in the domain would drag infrastructure into it. This port is
 * the seam: the domain decides what should happen, an implementation of this
 * interface makes it happen. See ADR-002 in docs/ARCHITECTURE.md.
 *
 * <p>One implementation per action type, living in an adapter.
 *
 * @param <A> the action type this handler performs
 */
public interface ActionHandler<A extends Action> {

    /** The action type this handler is registered for. */
    Class<A> handles();

    /**
     * Performs the action.
     *
     * @param action the intent to carry out
     * @param event  the event that triggered the workflow, for context
     * @throws ActionExecutionException when the action could not be performed
     */
    void execute(A action, Event event) throws ActionExecutionException;
}
