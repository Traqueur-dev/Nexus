package fr.traqueur.nexus.core.application.workflow;

import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.application.registry.UnknownTypeException;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.Action;
import fr.traqueur.nexus.core.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes an action to the handler registered for its type.
 *
 * <p>Handlers are keyed by the registered identifier rather than by the Java
 * class. The identifier is the same one a persisted workflow carries, so a
 * stored action and a live one resolve through one source of truth instead of
 * the class name doubling as an implicit contract.
 *
 * <p>Handlers are collected at startup. An action with no handler is an error,
 * not a no-op: silently skipping it would let a workflow report success while
 * doing nothing.
 */
public class ActionDispatcher {

    private final Registry<Action, ActionMetadata> registry;
    private final Map<String, ActionHandler<? extends Action>> handlers = new HashMap<>();

    public ActionDispatcher(Registry<Action, ActionMetadata> registry,
                            List<ActionHandler<? extends Action>> handlers) {
        this.registry = registry;
        for (ActionHandler<? extends Action> handler : handlers) {
            // Fails at startup rather than at the first event: a handler for an
            // unregistered action type could never be reached anyway.
            String type = registry.requireTypeForClass(handler.handles());
            ActionHandler<? extends Action> existing = this.handlers.putIfAbsent(type, handler);
            if (existing != null) {
                throw new IllegalStateException(
                        "Two handlers registered for action '%s': %s and %s".formatted(
                                type, existing.getClass().getName(), handler.getClass().getName()));
            }
        }
    }

    /**
     * The action's registered identifier, falling back to the class name when it
     * is not registered.
     *
     * <p>Used for reporting, which must never fail because the thing it is
     * reporting on is already broken.
     */
    public String describe(Action action) {
        String type = registry.getTypeForClass(action.getClass());
        return type != null ? type : action.getClass().getName();
    }

    @SuppressWarnings("unchecked")
    public void dispatch(Action action, Event event) throws ActionExecutionException {
        String type;
        try {
            type = registry.requireTypeForClass(action.getClass());
        } catch (UnknownTypeException e) {
            throw new ActionExecutionException(
                    "Action type is not registered: " + action.getClass().getName(), e);
        }

        ActionHandler<Action> handler = (ActionHandler<Action>) handlers.get(type);
        if (handler == null) {
            throw new ActionExecutionException(
                    "No handler registered for action '%s'. Registered: %s".formatted(
                            type,
                            handlers.isEmpty() ? "<none>" : handlers.keySet().stream().sorted().toList()));
        }
        handler.execute(action, event);
    }
}
