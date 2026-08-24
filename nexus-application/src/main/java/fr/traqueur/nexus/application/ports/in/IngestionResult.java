package fr.traqueur.nexus.application.ports.in;

import fr.traqueur.nexus.application.workflow.WorkflowRun;
import fr.traqueur.nexus.domain.events.Event;

import java.util.List;

/**
 * What happened when an event was ingested: the event that was stored, and what
 * the workflows reacting to it did.
 *
 * <p>The workflow runs are returned rather than swallowed so a caller can report
 * a workflow that failed. An event that is stored while its workflow silently
 * fails is exactly the kind of failure nobody notices.
 */
public record IngestionResult(Event event, List<WorkflowRun> workflowRuns) {

    public IngestionResult {
        workflowRuns = workflowRuns == null ? List.of() : List.copyOf(workflowRuns);
    }

    public List<WorkflowRun> failedRuns() {
        return workflowRuns.stream().filter(run -> !run.succeeded()).toList();
    }
}
