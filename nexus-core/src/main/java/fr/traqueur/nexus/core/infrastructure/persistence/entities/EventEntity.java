package fr.traqueur.nexus.core.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name="events")
public class EventEntity {

        @Id
        private String id;
        private String source;
        private String type;
        @Column(columnDefinition = "jsonb")
        private String context;
        @Column(columnDefinition = "jsonb")
        private String payload;
        private Instant timestamp;

        public EventEntity() {}

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public String getSource() {
                return source;
        }

        public void setSource(String source) {
                this.source = source;
        }

        public String getType() {
                return type;
        }

        public void setType(String type) {
                this.type = type;
        }

        public String getContext() {
                return context;
        }

        public void setContext(String context) {
                this.context = context;
        }

        public String getPayload() {
                return payload;
        }

        public void setPayload(String payload) {
                this.payload = payload;
        }

        public Instant getTimestamp() { return timestamp; }

        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
