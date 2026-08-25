package fr.traqueur.nexus.application.services;

import fr.traqueur.nexus.application.ports.in.ManageWorkflows;
import fr.traqueur.nexus.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.domain.workflow.Workflow;

import java.util.List;
import java.util.Optional;

/**
 * The management use cases for workflows.
 *
 * <p>Thin on purpose. Everything a workflow must satisfy — at least one event
 * type, at least one action, a condition — is enforced by the {@link Workflow}
 * record itself, so a service re-checking it here would be a second place to keep
 * the rules, and the one that adapters happen to call rather than the one the
 * type guarantees.
 *
 * <p>It exists anyway, rather than letting the controller hold the repository:
 * that is what keeps the REST adapter talking to an inbound port instead of
 * reaching into storage, and what lets a second driving adapter — a CLI, the
 * dashboard's own path — get the same behaviour.
 */
public class WorkflowService implements ManageWorkflows {

    private final WorkflowRepository workflows;

    public WorkflowService(WorkflowRepository workflows) {
        this.workflows = workflows;
    }

    @Override
    public void save(Workflow workflow) {
        workflows.save(workflow);
    }

    @Override
    public Optional<Workflow> findById(String id) {
        return workflows.findById(id);
    }

    @Override
    public List<Workflow> findAll() {
        return workflows.findAll();
    }

    @Override
    public boolean delete(String id) {
        return workflows.deleteById(id);
    }
}