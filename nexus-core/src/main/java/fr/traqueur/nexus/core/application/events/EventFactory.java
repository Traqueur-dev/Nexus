package fr.traqueur.nexus.core.application.events;

import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds event instances from their constituent parts, and takes them apart again.
 *
 * <p>This was the shared core of the old {@code EventMapper}: both the persistence
 * mapper and the REST mapper needed it, which is why that class ended up importing
 * an entity and two DTOs at once. Extracted here it depends on nothing but the
 * {@link Registry} — no Jackson, no JPA, no HTTP.
 *
 * <p>Instances are built reflectively from their record components. That is a
 * deliberate trade-off for an open type system: it lets an adapter contribute an
 * event type without this class knowing about it, at the cost of the mapping not
 * being checked at compile time. Renaming a record component is a silent
 * breaking change.
 */
public class EventFactory {

    private static final String ID = "id";
    private static final String CONTEXT = "context";
    private static final String TIMESTAMP = "timestamp";

    private final Registry<Event, EventMetadata> registry;

    public EventFactory(Registry<Event, EventMetadata> registry) {
        this.registry = registry;
    }

    /** Resolves the registered identifier of an event, failing if it is unknown. */
    public String typeOf(Event event) {
        return registry.requireTypeForClass(event.getClass());
    }

    /**
     * Builds an event of the given registered type.
     *
     * @throws fr.traqueur.nexus.core.application.registry.UnknownTypeException
     *         if no class is registered for {@code type}
     */
    public Event create(String type, Event.Id id, Context context, Instant timestamp, Map<String, Object> payload) {
        Class<? extends Event> eventClass = registry.requireClassForType(type);
        RecordComponent[] components = eventClass.getRecordComponents();
        if (components == null) {
            throw new IllegalStateException("Event type '%s' (%s) must be a record".formatted(type, eventClass.getName()));
        }

        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            args[i] = switch (component.getName()) {
                case ID -> id;
                case CONTEXT -> context;
                case TIMESTAMP -> timestamp;
                default -> convertPayloadValue(
                        payload == null ? null : payload.get(component.getName()), component.getType());
            };
        }

        try {
            Class<?>[] paramTypes = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);
            Constructor<? extends Event> constructor = eventClass.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to construct " + eventClass.getSimpleName(), e);
        }
    }

    /** The event's own components, excluding the ones every event carries. */
    public Map<String, Object> extractPayload(Event event) {
        Map<String, Object> payload = new HashMap<>();
        RecordComponent[] components = event.getClass().getRecordComponents();
        if (components == null) {
            return payload;
        }
        for (RecordComponent component : components) {
            String name = component.getName();
            if (name.equals(ID) || name.equals(CONTEXT) || name.equals(TIMESTAMP)) {
                continue;
            }
            try {
                payload.put(name, component.getAccessor().invoke(event));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to read component " + name, e);
            }
        }
        return payload;
    }

    private Object convertPayloadValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        }
        return value;
    }
}
