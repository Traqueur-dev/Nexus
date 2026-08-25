-- #6: workflows lived in a ConcurrentHashMap and did not survive a restart.
--
-- A workflow is a fixed four-field shape whose condition and actions are open,
-- recursive hierarchies. They are stored as JSONB rather than decomposed into
-- tables: a condition tree is arbitrarily nested and its node types are
-- contributed by plugins, so a relational model would need an adjacency table
-- keyed on types the core does not know. Encoding is the application's one
-- Jackson mapper, which writes each node under its registered identifier
-- (ADR-009) — never a Java class name, so moving a class breaks no row.

CREATE TABLE workflows (
                        id VARCHAR(255) PRIMARY KEY,
                        events TEXT[] NOT NULL,
                        condition JSONB NOT NULL,
                        actions JSONB NOT NULL
);

-- The hot query is "which workflows react to this event type", run on every
-- ingested event. A GIN index answers `events @> ARRAY[...]` from the index
-- instead of scanning every row, which is the reason WorkflowRepository states
-- the query that way rather than exposing findAll().
CREATE INDEX idx_workflows_events ON workflows USING GIN(events);

-- Both mirror an invariant the Workflow record already enforces. Duplicated here
-- on purpose: the domain protects what it constructs, the schema protects what a
-- migration, a fixture or a future adapter writes. A workflow triggered by
-- nothing, or doing nothing when it fires, is one the user believes is active.
ALTER TABLE workflows ADD CONSTRAINT check_workflow_has_events
    CHECK (cardinality(events) > 0);

ALTER TABLE workflows ADD CONSTRAINT check_workflow_has_actions
    CHECK (jsonb_array_length(actions) > 0);