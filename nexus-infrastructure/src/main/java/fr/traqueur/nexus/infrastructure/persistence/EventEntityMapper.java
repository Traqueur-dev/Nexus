package fr.traqueur.nexus.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.application.events.EventFactory;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.infrastructure.persistence.entities.EventEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Translates between the domain event and its stored row.
 *
 * <p>Half of the former {@code EventMapper}. It lives in infrastructure because
 * that is where the technology it maps to lives — JPA entities and the JSON
 * encoding of the jsonb columns.
 */
@Component
public class EventEntityMapper {

    private final EventFactory factory;
    private final ObjectMapper json;

    public EventEntityMapper(EventFactory factory, ObjectMapper json) {
        this.factory = factory;
        this.json = json;
    }

    public EventEntity toEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setId(event.id().toString());
        entity.setSource(event.context().source());
        entity.setType(factory.typeOf(event));
        entity.setTimestamp(event.timestamp());
        entity.setContext(serialize(event.context()));
        entity.setPayload(serialize(factory.extractPayload(event)));
        return entity;
    }

    @SuppressWarnings("unchecked")
    public Event toDomain(EventEntity entity) {
        Context context = deserialize(entity.getContext(), Context.class);
        Map<String, Object> payload = deserialize(entity.getPayload(), Map.class);
        return factory.create(
                entity.getType(),
                Event.Id.fromString(entity.getId()),
                context,
                entity.getTimestamp(),
                payload);
    }

    private <T> T deserialize(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read stored JSON as " + type.getSimpleName(), e);
        }
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write JSON for persistence", e);
        }
    }
}
