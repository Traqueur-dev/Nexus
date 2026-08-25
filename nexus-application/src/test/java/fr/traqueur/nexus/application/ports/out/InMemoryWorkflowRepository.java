package fr.traqueur.nexus.application.ports.out;

import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link WorkflowRepository}, for tests that need workflows but not a
 * database.
 *
 * <p>This was production code until #6 — a provisional adapter in
 * {@code infrastructure.persistence}, holding the place until workflows had a
 * table. Now that they do, what it actually was all along is a test double, so it
 * lives with the tests.
 *
 * <p>It matches the JPA adapter where the contract says it must: {@code save} is
 * an upsert, keyed on the workflow id.
 */
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();

    public InMemoryWorkflowRepository(Workflow... initial) {
        for (Workflow workflow : initial) {
            save(workflow);
        }
    }

    @Override
    public void save(Workflow workflow) {
        workflows.put(workflow.id(), workflow);
    }

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(workflows.get(id));
    }

    @Override
    public List<Workflow> findAll() {
        return List.copyOf(workflows.values());
    }

    @Override
    public boolean deleteById(String id) {
        return workflows.remove(id) != null;
    }

    @Override
    public List<Workflow> findTriggeredBy(EventType type) {
        return workflows.values().stream()
                .filter(workflow -> workflow.triggersOn(type))
                .toList();
    }
}