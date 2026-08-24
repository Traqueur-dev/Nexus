package fr.traqueur.nexus.core.infrastructure.persistence;

import fr.traqueur.nexus.core.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.core.domain.events.EventType;
import fr.traqueur.nexus.core.domain.workflow.Workflow;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provisional in-memory adapter for {@link WorkflowRepository}.
 *
 * <p>Workflows have no table yet — that is issue #6. This exists so the engine
 * can be wired and exercised end to end in the meantime, and so the port's shape
 * is settled before a schema is written against it.
 *
 * <p>It is deliberately obvious that this is not durable: nothing survives a
 * restart. Replacing it with a JPA adapter should require no change outside this
 * package.
 */
@Repository
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();

    @Override
    public List<Workflow> findTriggeredBy(EventType type) {
        return workflows.values().stream()
                .filter(workflow -> workflow.triggersOn(type))
                .toList();
    }

    public void save(Workflow workflow) {
        workflows.put(workflow.id(), workflow);
    }

    public void deleteAll() {
        workflows.clear();
    }
}
