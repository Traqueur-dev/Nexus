package fr.traqueur.nexus.application.workflow;

import java.util.List;
import java.util.Objects;

/**
 * The record of one workflow reacting to one event.
 *
 * <p>Returned rather than logged: the engine is application code, and choosing
 * how to report belongs to whoever called it. It is also the natural shape for
 * the execution history a workflow UI will need.
 *
 * @param workflowId      which workflow this is about
 * @param evaluationError why its condition could not be evaluated, or {@code null}
 * @param outcomes        what became of each of its actions
 */
public record WorkflowRun(String workflowId, String evaluationError, List<ActionOutcome> outcomes) {

    public WorkflowRun {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }

    public static WorkflowRun fired(String workflowId, List<ActionOutcome> outcomes) {
        return new WorkflowRun(workflowId, null, outcomes);
    }

    public static WorkflowRun evaluationFailed(String workflowId, String reason) {
        return new WorkflowRun(workflowId, Objects.requireNonNullElse(reason, ActionOutcome.UNKNOWN_FAILURE), List.of());
    }

    /** Whether the workflow actually ran, as opposed to failing to evaluate. */
    public boolean fired() {
        return evaluationError == null;
    }

    public boolean succeeded() {
        return fired() && outcomes.stream().allMatch(ActionOutcome::succeeded);
    }

    public List<ActionOutcome> failures() {
        return outcomes.stream().filter(outcome -> !outcome.succeeded()).toList();
    }
}
