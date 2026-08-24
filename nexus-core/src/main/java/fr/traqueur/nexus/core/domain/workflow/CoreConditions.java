package fr.traqueur.nexus.core.domain.workflow;

import fr.traqueur.nexus.core.domain.workflow.conditions.AlwaysCondition;
import fr.traqueur.nexus.core.domain.workflow.conditions.CompositeCondition;
import fr.traqueur.nexus.core.domain.workflow.conditions.ContainsCondition;
import fr.traqueur.nexus.core.domain.workflow.conditions.EqualsCondition;
import fr.traqueur.nexus.core.domain.workflow.conditions.GroupCondition;

import java.util.List;

/**
 * The condition types shipped by the core itself.
 *
 * <p>Same rationale as {@link CoreEvents}: the hierarchy is open, so the set of
 * built-in conditions is declared rather than discovered.
 */
public final class CoreConditions {

    private CoreConditions() {
    }

    public static List<Class<? extends Condition>> types() {
        return List.of(
                AlwaysCondition.class,
                CompositeCondition.class,
                ContainsCondition.class,
                EqualsCondition.class,
                GroupCondition.class
        );
    }
}
