package fr.traqueur.nexus.infrastructure.persistence;

import fr.traqueur.nexus.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.infrastructure.persistence.entities.WorkflowEntity;
import fr.traqueur.nexus.infrastructure.persistence.repositories.WorkflowEntityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA adapter for {@link WorkflowRepository}, replacing the in-memory one that
 * lost every workflow on restart (#6).
 */
@Repository
public class JpaWorkflowRepository implements WorkflowRepository {

    private final WorkflowEntityRepository entities;
    private final WorkflowEntityMapper mapper;

    public JpaWorkflowRepository(WorkflowEntityRepository entities, WorkflowEntityMapper mapper) {
        this.entities = entities;
        this.mapper = mapper;
    }

    /**
     * Uses {@code save()}, which #27 established is an upsert — and that is the
     * behaviour wanted here.
     *
     * <p>Worth stating because the neighbouring {@link JpaEventRepository} goes out
     * of its way to avoid the same call. The difference is the data, not the
     * technology: an event is a fact, so an UPDATE is silent loss; a workflow is
     * configuration, so an UPDATE is the user editing it. Copying the {@code
     * persist()} pattern here would make a workflow impossible to modify.
     */
    @Override
    @Transactional
    public void save(Workflow workflow) {
        entities.save(mapper.toEntity(workflow));
    }

    /**
     * Transactional because {@code spring.jpa.open-in-view=false}: the entities
     * must be mapped to domain objects before the session closes.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findTriggeredBy(EventType type) {
        List<WorkflowEntity> matching = entities.findTriggeredBy(type.value());
        return matching.stream().map(mapper::toDomain).toList();
    }
}