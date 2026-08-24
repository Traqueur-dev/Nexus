package fr.traqueur.nexus.core.application.workflow;

import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.Action;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes an action to the handler registered for its type.
 *
 * <p>Handlers are collected at startup. An action with no handler is an error,
 * not a no-op: silently skipping it would let a workflow report success while
 * doing nothing.
 */
public class ActionDispatcher {

    private final Map<Class<? extends Action>, ActionHandler<? extends Action>> handlers = new HashMap<>();

    public ActionDispatcher(List<ActionHandler<? extends Action>> handlers) {
        for (ActionHandler<? extends Action> handler : handlers) {
            ActionHandler<? extends Action> existing = this.handlers.putIfAbsent(handler.handles(), handler);
            if (existing != null) {
                throw new IllegalStateException(
                        "Two handlers registered for %s: %s and %s".formatted(
                                handler.handles().getName(),
                                existing.getClass().getName(),
                                handler.getClass().getName()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void dispatch(Action action, Event event) throws ActionExecutionException {
        ActionHandler<Action> handler = (ActionHandler<Action>) handlers.get(action.getClass());
        if (handler == null) {
            throw new ActionExecutionException(
                    "No handler registered for action %s. Registered: %s".formatted(
                            action.getClass().getName(),
                            handlers.isEmpty() ? "<none>" : handlers.keySet().stream()
                                    .map(Class::getSimpleName).sorted().toList()));
        }
        handler.execute(action, event);
    }
}
