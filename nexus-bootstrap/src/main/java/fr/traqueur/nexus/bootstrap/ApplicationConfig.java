package fr.traqueur.nexus.bootstrap;

import fr.traqueur.nexus.application.events.EventFactory;
import fr.traqueur.nexus.application.ports.out.ActionHandler;
import fr.traqueur.nexus.application.ports.out.EventRepository;
import fr.traqueur.nexus.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.application.services.EventService;
import fr.traqueur.nexus.application.services.WorkflowService;
import fr.traqueur.nexus.application.workflow.ActionDispatcher;
import fr.traqueur.nexus.application.workflow.WorkflowEngine;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventMetadata;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application layer.
 *
 * <p>Application classes carry no framework annotation, so they are declared
 * here instead of being discovered. The cost is this file; what it buys is a
 * layer that can be driven by something other than Spring — a plugin embedding
 * the workflow engine, a CLI, a different framework — without touching it.
 *
 * <p>It also makes the dependency graph of the application readable in one
 * place, which component scanning hides.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public EventFactory eventFactory(Registry<Event, EventMetadata> eventRegistry) {
        return new EventFactory(eventRegistry);
    }

    /**
     * Handlers are collected through an {@link ObjectProvider} rather than an
     * injected {@code List}: Spring fails to start when a {@code List<T>} has no
     * candidate, and having no action handler at all is a legitimate state —
     * {@code SendEmailActionHandler} only exists when SMTP is configured. An
     * empty dispatcher is fine; it reports a missing handler when asked to
     * dispatch.
     */
    @Bean
    public ActionDispatcher actionDispatcher(Registry<Action, ActionMetadata> actionRegistry,
                                             ObjectProvider<ActionHandler<? extends Action>> handlers) {
        return new ActionDispatcher(actionRegistry, handlers.stream().toList());
    }

    @Bean
    public WorkflowEngine workflowEngine(WorkflowRepository workflows, ActionDispatcher dispatcher) {
        return new WorkflowEngine(workflows, dispatcher);
    }

    @Bean
    public EventService eventService(EventRepository events, EventFactory factory, WorkflowEngine workflows) {
        return new EventService(events, factory, workflows);
    }

    @Bean
    public WorkflowService workflowService(WorkflowRepository workflows) {
        return new WorkflowService(workflows);
    }
}
