package fr.traqueur.nexus.application.registry;

import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.events.CoreContexts;
import fr.traqueur.nexus.domain.events.CoreEvents;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventMetadata;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.domain.workflow.CoreActions;
import fr.traqueur.nexus.domain.workflow.CoreConditions;

/**
 * Builds the registries populated with the types the core ships.
 *
 * <p>Composing a registry is not a framework concern, so it lives here rather
 * than inside a Spring {@code @Configuration}. Bootstrap turns these into beans;
 * a test, a CLI or an embedded use gets the same registries without a container.
 *
 * <p>Each call returns a fresh registry. Registries are mutable by design —
 * adapters add their types as they load — so handing out a shared instance would
 * let one caller's registrations leak into another's.
 */
public final class Registries {

    private Registries() {
    }

    public static Registry<Event, EventMetadata> events() {
        return new Registry<>(Event.class, EventMetadata.class, EventMetadata::type)
                .registerAll(CoreEvents.types());
    }

    public static Registry<Context, ContextMetadata> contexts() {
        return new Registry<>(Context.class, ContextMetadata.class, ContextMetadata::type)
                .registerAll(CoreContexts.types());
    }

    public static Registry<Condition, ConditionMetadata> conditions() {
        return new Registry<>(Condition.class, ConditionMetadata.class, ConditionMetadata::type)
                .registerAll(CoreConditions.types());
    }

    public static Registry<Action, ActionMetadata> actions() {
        return new Registry<>(Action.class, ActionMetadata.class, ActionMetadata::type)
                .registerAll(CoreActions.types());
    }
}
