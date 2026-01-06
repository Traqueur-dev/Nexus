package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;

import java.util.List;

public record CompositeCondition(Operator operator, List<Condition> rules) implements Condition {
    @Override
    public boolean isMet(Event event) throws ConditionEvaluationException {
        return switch (operator) {
            case AND -> {
                boolean b = true;
                for (Condition condition : rules) {
                    if (!condition.isMet(event)) {
                        b = false;
                        break;
                    }
                }
                yield b;
            }
            case OR -> {
                boolean b = false;
                for (Condition condition : rules) {
                    if (condition.isMet(event)) {
                        b = true;
                        break;
                    }
                }
                yield b;
            }
        };
    }
}
