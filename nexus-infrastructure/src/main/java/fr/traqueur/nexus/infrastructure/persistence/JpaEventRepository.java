package fr.traqueur.nexus.infrastructure.persistence;

import fr.traqueur.nexus.application.ports.out.EventAlreadyStoredException;
import fr.traqueur.nexus.application.ports.out.EventRepository;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.infrastructure.persistence.entities.EventEntity;
import fr.traqueur.nexus.infrastructure.persistence.repositories.EventEntityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    private final EntityManager entityManager;

    public JpaEventRepository(EventEntityRepository entities,
                              EventEntityMapper mapper,
                              EntityManager entityManager) {
        this.entities = entities;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    /**
     * Inserts, never updates.
     *
     * <p>{@code JpaRepository.save()} is an upsert: handed an id that already
     * exists it issues an UPDATE, so a colliding event silently replaced the one
     * already stored (#27). {@code persist()} only ever inserts, which turns that
     * into a constraint violation — a loud failure instead of lost data.
     *
     * <p>It is also one query cheaper: {@code save()} has to SELECT first to
     * decide between insert and update.
     *
     * <p>The flush is deliberate. Without it the violation surfaces when the
     * transaction commits, outside this method, and reaches the caller as a
     * generic infrastructure error rather than as the contract this port states.
     */
    @Override
    @Transactional
    public void save(Event event) {
        EventEntity entity = mapper.toEntity(event);
        try {
            entityManager.persist(entity);
            entityManager.flush();
        } catch (PersistenceException | DataIntegrityViolationException e) {
            throw new EventAlreadyStoredException(event.id(), e);
        }
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