package fr.traqueur.nexus.domain.workflow.conditions;

import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.domain.workflow.exceptions.ConditionEvaluationException;

@ConditionMetadata(type = "always")
public record AlwaysCondition() implements Condition {
    @Override
    public boolean isMet(Event event) throws ConditionEvaluationException {
        return true;
    }
}
