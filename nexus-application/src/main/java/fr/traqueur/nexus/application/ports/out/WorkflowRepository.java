package fr.traqueur.nexus.application.ports.out;

import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;

import java.util.List;

/**
 * Outbound port for workflow storage.
 *
 * <p>The query is expressed as "which workflows react to this event type" rather
 * than "give me everything so I can filter": it states the intent, and lets an
 * adapter answer it with an index instead of a full scan.
 */
public interface WorkflowRepository {

    /**
     * Stores a workflow, replacing the one already carrying its id.
     *
     * <p>Deliberately an upsert, which is the opposite of what
     * {@link EventRepository#save} owes its caller. The difference is not
     * inconsistency: an event is a fact that happened, so overwriting one is data
     * loss (#27), while a workflow is configuration the user edits, so refusing to
     * overwrite it would make it uneditable.
     */
    void save(Workflow workflow);

    List<Workflow> findTriggeredBy(EventType type);
}