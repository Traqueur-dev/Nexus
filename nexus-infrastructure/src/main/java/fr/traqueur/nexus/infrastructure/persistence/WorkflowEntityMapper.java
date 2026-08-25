package fr.traqueur.nexus.infrastructure.persistence;

import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.infrastructure.persistence.entities.WorkflowEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Translates between the domain workflow and its stored row.
 *
 * <p>The condition and action trees go through the application's one Jackson
 * mapper, so each node is written under its registered identifier rather than its
 * class name (ADR-009). That is what makes a stored workflow survive a package
 * move, and what lets a plugin's condition type be read back at all.
 */
@Component
public class WorkflowEntityMapper {

    private static final TypeReference<List<Action>> ACTIONS = new TypeReference<>() {
    };

    private final ObjectMapper json;

    public WorkflowEntityMapper(ObjectMapper json) {
        this.json = json;
    }

    public WorkflowEntity toEntity(Workflow workflow) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.id());
        entity.setEvents(workflow.events().stream().map(EventType::value).toList());
        entity.setCondition(serialize(workflow.condition()));
        entity.setActions(serialize(workflow.actions()));
        return entity;
    }

    public Workflow toDomain(WorkflowEntity entity) {
        return new Workflow(
                entity.getId(),
                entity.getEvents().stream().map(EventType::of).toList(),
                deserialize(entity.getCondition(), Condition.class),
                deserializeActions(entity.getActions()));
    }

    private <T> T deserialize(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read stored JSON as " + type.getSimpleName(), e);
        }
    }

    private List<Action> deserializeActions(String value) {
        try {
            return json.readValue(value, ACTIONS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read stored JSON as a list of actions", e);
        }
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write JSON for persistence", e);
        }
    }
}