package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;

public final class AlwaysCondition implements Condition {
    @Override
    public boolean isMet(Event event) throws ConditionEvaluationException {
        return true;
    }
}
