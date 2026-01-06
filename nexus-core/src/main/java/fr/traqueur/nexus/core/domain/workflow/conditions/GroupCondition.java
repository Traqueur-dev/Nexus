package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;

import java.util.List;

public record GroupCondition(Integer minRequirements, List<Condition> rules) implements Condition {
    @Override
    public boolean isMet(Event event) throws ConditionEvaluationException {
        long metConditions = 0L;
        for (Condition condition : rules) {
            if (condition.isMet(event)) {
                metConditions++;
            }
        }
        return minRequirements == null ? metConditions == rules.size() : metConditions >= minRequirements;
    }
}
