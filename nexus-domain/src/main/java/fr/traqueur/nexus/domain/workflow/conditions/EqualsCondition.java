package fr.traqueur.nexus.domain.workflow.conditions;

import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.domain.workflow.exceptions.ConditionEvaluationException;

import java.util.Optional;

@ConditionMetadata(type = "equals")
public record EqualsCondition(String field, String value) implements Condition {


    @Override
    public boolean isMet(Event event) throws ConditionEvaluationException {
        Optional<Object> fieldValue = this.getFieldValue(event, field);
        if (fieldValue.isEmpty()) {
            throw new ConditionEvaluationException("field " + field + " not found");
        }
        return fieldValue.get().toString().equals(value);
    }
}
