package fr.traqueur.nexus.core.application.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.infrastructure.persistence.entities.EventEntity;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventRequestDto;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventResponseDto;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventMapper {

    private final Registry<Event, EventMetadata> registry;
    private final ObjectMapper mapper;

    public EventMapper(Registry<Event, EventMetadata> registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    public Event toDomain(EventEntity entity) {
        Class<? extends Event> eventClass = registry.getClassForType(entity.getType());
        Context context = deserialize(entity.getContext(), Context.class);
        Map<String, Object> payload = deserialize(entity.getPayload(), Map.class);
        Event.Id id = Event.Id.fromString(entity.getId());

       return createEventInstance(eventClass, id, context, entity.getTimestamp(), payload);
    }

    public EventEntity toEntity(Event event) {
        String id = event.id().toString();
        String type = registry.getTypeForClass(event.getClass());
        Instant timestamp = event.timestamp();
        String contextJson = serialize(event.context());
        RecordComponent[] components = event.getClass().getRecordComponents();
        Map<String, Object> payload = extractPayload(event, components);
        String payloadJson = serialize(payload);

        EventEntity entity = new EventEntity();
        entity.setId(id);
        entity.setSource(event.context().source());
        entity.setType(type);
        entity.setTimestamp(timestamp);
        entity.setContext(contextJson);
        entity.setPayload(payloadJson);
        return entity;
    }

    public Event toDomain(EventRequestDto eventRequestDto) {
        Class<? extends Event> eventClass = registry.getClassForType(eventRequestDto.type());
        Context context = deserialize(eventRequestDto.context(), Context.class);
        Map<String, Object> payload = eventRequestDto.payload();
        Event.Id id = Event.Id.generate(eventRequestDto.source());

        return createEventInstance(eventClass, id, context, eventRequestDto.timestamp(), payload);
    }

    private Event createEventInstance(Class<? extends Event> eventClass, Event.Id id, Context context, Instant timestamp, Map<String, Object> payload) {
        RecordComponent[] components = eventClass.getRecordComponents();
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent comp = components[i];
            args[i] = switch (comp.getName()) {
                case "id" -> id;
                case "context" -> context;
                case "timestamp" -> timestamp;
                default -> convertPayloadValue(payload.get(comp.getName()), comp.getType());
            };
        }

        try {
            Class<?>[] paramTypes = Arrays.stream(eventClass.getRecordComponents())
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);

            Constructor<? extends Event> constructor = eventClass.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct " + eventClass.getSimpleName(), e);
        }
    }

    private Object convertPayloadValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;
        if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        }
        return value;
    }


    private <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialize failed", e);
        }
    }

    private String serialize(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize failed", e);
        }
    }


    public EventResponseDto toDto(Event event) {
        RecordComponent[] components = event.getClass().getRecordComponents();
        Map<String, Object> payload = extractPayload(event, components);
        return new EventResponseDto(
                event.id().toString(),
                event.context().source(),
                registry.getTypeForClass(event.getClass()),
                event.timestamp(),
                serialize(event.context()),
                payload
        );
    }

    private Map<String, Object> extractPayload(Event event, RecordComponent[] components) {
        Map<String, Object> payload = new HashMap<>();
        for (RecordComponent comp : components) {
            String name = comp.getName();
            if (name.equals("id") || name.equals("context") || name.equals("timestamp")) {
                continue;
            }
            try {
                Object value = comp.getAccessor().invoke(event);
                payload.put(name, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get value for component " + name, e);
            }
        }
        return payload;
    }
}
