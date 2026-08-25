package fr.traqueur.nexus.application.ports.out;

import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Workflow;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for workflow storage.
 *
 * <p>{@link #findTriggeredBy} is expressed as "which workflows react to this
 * event type" rather than "give me everything so I can filter": it states the
 * intent, and lets an adapter answer it with an index instead of a full scan.
 * {@link #findAll} exists for the management API, which genuinely wants every
 * workflow — it is not a substitute for the query above.
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

    Optional<Workflow> findById(String id);

    List<Workflow> findAll();

    /**
     * Removes a workflow.
     *
     * @return whether a workflow was actually removed, so a caller can answer 404
     *         without a read before every delete
     */
    boolean deleteById(String id);

    List<Workflow> findTriggeredBy(EventType type);
}