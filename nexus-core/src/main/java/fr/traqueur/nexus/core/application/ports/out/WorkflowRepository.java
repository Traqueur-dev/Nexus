package fr.traqueur.nexus.core.application.ports.out;

import fr.traqueur.nexus.core.domain.events.EventType;
import fr.traqueur.nexus.core.domain.workflow.Workflow;

import java.util.List;

/**
 * Outbound port for workflow storage.
 *
 * <p>The query is expressed as "which workflows react to this event type" rather
 * than "give me everything so I can filter": it states the intent, and lets an
 * adapter answer it with an index instead of a full scan.
 */
public interface WorkflowRepository {

    List<Workflow> findTriggeredBy(EventType type);
}
