package fr.traqueur.nexus.core.infrastructure.registry;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventRegistry {

    private Map<String, Class<? extends Event>> typeToClass;
    private Map<Class<? extends Event>, String> classToType;

    public EventRegistry() {
        this.typeToClass = new ConcurrentHashMap<>();
        this.classToType = new ConcurrentHashMap<>();
        this.registerAllEvents();
    }

    public Class<? extends Event> getClassForType(String type) {
        return typeToClass.get(type);
    }
    public String getTypeForClass(Class<? extends Event> eventClass) {
        return classToType.get(eventClass);
    }

    private void registerAllEvents() {
        List<Class<? extends Event>> allEvents = new ArrayList<>();
        collectSealedPermits(Event.class, allEvents);
        for (Class<? extends Event> eventClass : allEvents) {
            EventMetadata metadata = eventClass.getAnnotation(EventMetadata.class);
            String type = metadata.type();
            typeToClass.put(type, eventClass);
            classToType.put(eventClass, type);
        }
    }

    private void collectSealedPermits(Class<?> sealedClass, List<Class<? extends Event>> result) {
        if (!sealedClass.isSealed()) return;

        for (Class<?> permitted : sealedClass.getPermittedSubclasses()) {
            if (permitted.isAnnotationPresent(EventMetadata.class)) {
                result.add((Class<? extends Event>) permitted);
            }
            collectSealedPermits(permitted, result);
        }
    }
}
