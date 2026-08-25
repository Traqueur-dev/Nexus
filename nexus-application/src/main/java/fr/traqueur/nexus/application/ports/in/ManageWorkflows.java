package fr.traqueur.nexus.application.ports.in;

import fr.traqueur.nexus.domain.workflow.Workflow;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: create, read and remove the workflows Nexus runs.
 *
 * <p>One port rather than the read/write split {@link QueryEvents} and
 * {@link IngestEvent} have. That split earned its keep because the REST adapter
 * reads events and must not be able to ingest them; here the same adapter
 * performs all four operations, so splitting would produce two interfaces for one
 * caller. If a read-only consumer ever appears — the dashboard, a plugin
 * inspecting configuration — that is the moment to split, with something to
 * justify it.
 *
 * <p>Writing is a plain replace, mirroring the store: the caller chooses the id,
 * so "create" and "update" are the same operation seen from two moments.
 */
public interface ManageWorkflows {

    void save(Workflow workflow);

    Optional<Workflow> findById(String id);

    List<Workflow> findAll();

    /** @return whether a workflow was actually removed */
    boolean delete(String id);
}