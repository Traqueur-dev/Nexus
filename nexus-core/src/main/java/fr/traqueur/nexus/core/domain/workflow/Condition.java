package fr.traqueur.nexus.core.domain.workflow;

import fr.traqueur.nexus.core.application.logging.NexusLogger;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.conditions.*;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Optional;

public sealed interface Condition permits AlwaysCondition, CompositeCondition, ContainsCondition, EqualsCondition, GroupCondition {

    enum Operator {
        AND,
        OR
    }

    boolean isMet(Event event) throws ConditionEvaluationException;

    default Optional<Object> getFieldValue(Event event, String field) {
        Object value =  switch (field) {
            case "id" -> event.id();
            case "source" -> event.context().source();
            case "timestamp" -> event.timestamp();
            default -> {
                RecordComponent[] components = event.getClass().getRecordComponents();
                for (RecordComponent component : components) {
                    if (component.getName().equals(field)) {
                        try {
                            yield component.getAccessor().invoke(event);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            yield null;
                        }
                    }
                }
                components = event.context().getClass().getRecordComponents();
                for (RecordComponent component : components) {
                    if (component.getName().equals(field)) {
                        try {
                            yield component.getAccessor().invoke(event.context());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            yield null;
                        }
                    }
                }
                yield null;
            }
        };
        return Optional.ofNullable(value);
    }

}
