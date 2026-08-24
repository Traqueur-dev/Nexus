package fr.traqueur.nexus.domain.workflow;

import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.exceptions.ConditionEvaluationException;

import java.util.List;
import java.util.Objects;

/**
 * What to run, and when: a set of event types, a condition, and the actions to
 * perform when both are satisfied.
 *
 * <p>The decision is made here and it is pure — no I/O, no clock, no randomness.
 * {@link #matches} answers whether the workflow should fire; performing its
 * {@link Action}s is an adapter's job, behind the action handler port. See
 * ADR-002 in docs/ARCHITECTURE.md.
 *
 * @param id        identifies the workflow
 * @param events    the event types this workflow reacts to
 * @param condition the predicate applied to a matching event
 * @param actions   what to do when the workflow fires
 */
public record Workflow(
        String id,
        List<EventType> events,
        Condition condition,
        List<Action> actions
) {

    public Workflow {
        Objects.requireNonNull(id, "workflow id is required");
        if (id.isBlank()) {
            throw new IllegalArgumentException("workflow id must not be blank");
        }

        Objects.requireNonNull(events, "workflow events are required");
        if (events.isEmpty()) {
            // A workflow triggered by nothing can never fire; accepting one would
            // silently store a workflow the user believes is active.
            throw new IllegalArgumentException("workflow '" + id + "' must declare at least one event type");
        }

        Objects.requireNonNull(condition, "workflow condition is required");

        Objects.requireNonNull(actions, "workflow actions are required");
        if (actions.isEmpty()) {
            // Likewise: a workflow that does nothing when it fires is a mistake,
            // not a valid configuration.
            throw new IllegalArgumentException("workflow '" + id + "' must declare at least one action");
        }

        events = List.copyOf(events);
        actions = List.copyOf(actions);
    }

    /** Whether this workflow reacts to the given event type at all. */
    public boolean triggersOn(EventType type) {
        return events.contains(type);
    }

    /**
     * Whether this workflow should fire for the given event.
     *
     * <p>The type is supplied by the caller rather than derived from the event:
     * resolving a class to its registered identifier is the registry's job, and
     * the registry is not something the domain should reach for.
     *
     * @throws ConditionEvaluationException if the condition cannot be evaluated,
     *         for instance because it references a field the event does not have
     */
    public boolean matches(EventType type, Event event) throws ConditionEvaluationException {
        Objects.requireNonNull(type, "event type is required");
        Objects.requireNonNull(event, "event is required");
        return triggersOn(type) && condition.isMet(event);
    }
}
