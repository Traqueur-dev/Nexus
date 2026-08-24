package fr.traqueur.nexus.core.application.workflow;

import fr.traqueur.nexus.core.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventType;
import fr.traqueur.nexus.core.domain.workflow.Action;
import fr.traqueur.nexus.core.domain.workflow.Workflow;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the workflows that react to an event.
 *
 * <p>The engine orchestrates; it decides nothing. Whether a workflow applies is
 * {@link Workflow#matches}, which is pure domain logic. Carrying out an action is
 * a handler behind {@code ActionHandler}. What is left here is the sequencing —
 * which is exactly what an application service is for.
 *
 * <p>Failures are collected rather than thrown. One workflow with a broken
 * condition must not stop the others, and one failing action must not cancel the
 * remaining actions of the same workflow: the actions of a workflow are separate
 * intents, not a transaction. Everything that went wrong comes back in the
 * {@link WorkflowRun} list so the caller can report it.
 */
@Component
public class WorkflowEngine {

    private final WorkflowRepository workflows;
    private final ActionDispatcher dispatcher;

    public WorkflowEngine(WorkflowRepository workflows, ActionDispatcher dispatcher) {
        this.workflows = workflows;
        this.dispatcher = dispatcher;
    }

    public List<WorkflowRun> run(EventType type, Event event) {
        List<WorkflowRun> runs = new ArrayList<>();

        for (Workflow workflow : workflows.findTriggeredBy(type)) {
            boolean applies;
            try {
                applies = workflow.matches(type, event);
            } catch (ConditionEvaluationException e) {
                runs.add(WorkflowRun.evaluationFailed(workflow.id(), e.getMessage()));
                continue;
            }
            if (!applies) {
                continue;
            }
            runs.add(WorkflowRun.fired(workflow.id(), execute(workflow, event)));
        }

        return List.copyOf(runs);
    }

    private List<ActionOutcome> execute(Workflow workflow, Event event) {
        List<ActionOutcome> outcomes = new ArrayList<>();
        for (Action action : workflow.actions()) {
            try {
                dispatcher.dispatch(action, event);
                outcomes.add(ActionOutcome.succeeded(action));
            } catch (ActionExecutionException e) {
                outcomes.add(ActionOutcome.failed(action, e.getMessage()));
            } catch (RuntimeException e) {
                // A handler that throws unexpectedly is a bug in that handler, not
                // a reason to abandon the rest of the workflow.
                outcomes.add(ActionOutcome.failed(action, e.toString()));
            }
        }
        return outcomes;
    }
}
