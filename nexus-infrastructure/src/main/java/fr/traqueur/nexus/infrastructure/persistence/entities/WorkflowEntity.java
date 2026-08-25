package fr.traqueur.nexus.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "workflows")
public class WorkflowEntity {

        @Id
        private String id;

        /*
         * A native text[] rather than a jsonb array or a join table. The whole
         * point of the column is `events @> ARRAY[?]` served by a GIN index, which
         * is what WorkflowRepository.findTriggeredBy promises.
         */
        @JdbcTypeCode(SqlTypes.ARRAY)
        @Column(name = "events", columnDefinition = "text[]")
        private List<String> events;

        @Column(name = "condition", columnDefinition = "jsonb")
        private String condition;

        @Column(name = "actions", columnDefinition = "jsonb")
        private String actions;

        public WorkflowEntity() {}

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public List<String> getEvents() {
                return events;
        }

        public void setEvents(List<String> events) {
                this.events = events;
        }

        public String getCondition() {
                return condition;
        }

        public void setCondition(String condition) {
                this.condition = condition;
        }

        public String getActions() {
                return actions;
        }

        public void setActions(String actions) {
                this.actions = actions;
        }
}