package fr.traqueur.nexus.core.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.logging.NexusLogger;
import fr.traqueur.nexus.core.application.mapper.EventMapper;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventRequestDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private final NexusLogger logger;
    private final EventService eventService;
    private final ObjectMapper mapper;
    private final EventMapper eventMapper;

    public EventConsumer(NexusLogger logger, EventService service, ObjectMapper mapper, EventMapper eventMapper) {
        this.eventService = service;
        this.logger = logger;
        this.mapper = mapper;
        this.eventMapper = eventMapper;
    }

    @RabbitListener(queues = {"nexus.discord", "nexus.github", "nexus.internal"})
    public void consumeEvent(String event) {
        try {
            var eventDto = mapper.readValue(event, EventRequestDto.class);
            logger.eventReceived(eventDto.source(), eventDto.type());

            var domainEvent = eventMapper.toDomain(eventDto);
            eventService.save(domainEvent);
            logger.eventSaved(domainEvent.id().toString());
        } catch (Exception e) {
            logger.error("Error while processing event", e);
        }
    }
}
