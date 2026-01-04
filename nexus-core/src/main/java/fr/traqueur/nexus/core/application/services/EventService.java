package fr.traqueur.nexus.core.application.services;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.application.mapper.EventMapper;
import fr.traqueur.nexus.core.infrastructure.persistence.repositories.EventEntityRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventService {

    private final EventEntityRepository eventEntityRepository;
    private final EventMapper eventMapper;

    public EventService(EventEntityRepository eventEntityRepository, EventMapper eventMapper) {
        this.eventEntityRepository = eventEntityRepository;
        this.eventMapper = eventMapper;
    }

    public void save(Event event) {
        var eventEntity = eventMapper.toEntity(event);
        eventEntityRepository.save(eventEntity);
    }

    public Optional<Event> findById(String id) {
        return eventEntityRepository.findById(id)
                .map(eventMapper::toDomain);
    }

}
