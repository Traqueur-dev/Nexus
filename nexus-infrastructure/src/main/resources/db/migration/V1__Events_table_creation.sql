CREATE TABLE events (
                        id VARCHAR(50) PRIMARY KEY,
                        source VARCHAR(255) NOT NULL,
                        type VARCHAR(255) NOT NULL,
                        context JSONB NOT NULL,
                        payload JSONB NOT NULL,
                        timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE events ADD CONSTRAINT check_event_id_format
    CHECK (id ~ '^[a-z]+-[a-z0-9]{6}$');

CREATE INDEX idx_events_source ON events(source);
CREATE INDEX idx_events_type ON events(type);
CREATE INDEX idx_events_timestamp ON events(timestamp DESC);
CREATE INDEX idx_events_source_type ON events(source, type);

CREATE INDEX idx_events_context_gin ON events USING GIN(context);
CREATE INDEX idx_events_payload_gin ON events USING GIN(payload);