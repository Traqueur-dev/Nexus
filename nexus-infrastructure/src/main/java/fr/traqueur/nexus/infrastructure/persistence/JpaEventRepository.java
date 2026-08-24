package fr.traqueur.nexus.infrastructure.persistence;

import fr.traqueur.nexus.application.ports.out.EventRepository;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.infrastructure.persistence.entities.EventEntity;
import fr.traqueur.nexus.infrastructure.persistence.repositories.EventEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter for {@link EventRepository}.
 *
 * <p>This is where the entity/domain translation belongs. Keeping it here is what
 * lets the application layer deal in {@link Event} alone and never see an
 * {@code @Entity}.
 */
@Repository
public class JpaEventRepository implements EventRepository {

    private final EventEntityRepository entities;
    private final EventEntityMapper mapper;

    public JpaEventRepository(EventEntityRepository entities, EventEntityMapper mapper) {
        this.entities = entities;
        this.mapper = mapper;
    }

    @Override
    public void save(Event event) {
        EventEntity entity = mapper.toEntity(event);
        entities.save(entity);
    }

    @Override
    public Optional<Event> findById(Event.Id id) {
        return entities.findById(id.toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<Event> findLatestBySource(String source) {
        return entities.findFirstBySourceOrderByTimestampDesc(source).map(mapper::toDomain);
    }
}
