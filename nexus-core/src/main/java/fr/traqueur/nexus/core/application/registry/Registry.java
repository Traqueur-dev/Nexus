package fr.traqueur.nexus.core.application.registry;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Registry<T, A extends Annotation> {

    private final Class<T> clazz;
    private final Class<A> annotationClazz;
    private final Function<A, String> typeExtractor;

    private final Map<String, Class<? extends T>> typeToClass;
    private final Map<Class<? extends T>, String> classToType;

    public Registry(Class<T> clazz, Class<A> annotationClazz, Function<A, String> typeExtractor) {
        this.clazz = clazz;
        this.annotationClazz = annotationClazz;
        this.typeExtractor = typeExtractor;
        this.typeToClass = new ConcurrentHashMap<>();
        this.classToType = new ConcurrentHashMap<>();
        this.registerAll();
    }

    public Class<? extends T> getClassForType(String type) {
        return typeToClass.get(type);
    }
    public String getTypeForClass(Class<? extends T> eventClass) {
        return classToType.get(eventClass);
    }

    private void registerAll() {
        List<Class<? extends T>> allElements = new ArrayList<>();
        collectSealedPermits(clazz, allElements);
        for (Class<? extends T> elementClass : allElements) {
            A metadata = elementClass.getAnnotation(annotationClazz);
            String type = this.typeExtractor.apply(metadata);
            typeToClass.put(type, elementClass);
            classToType.put(elementClass, type);
        }
    }

    private void collectSealedPermits(Class<?> sealedClass, List<Class<? extends T>> result) {
        if (!sealedClass.isSealed()) return;

        for (Class<?> permitted : sealedClass.getPermittedSubclasses()) {
            if (permitted.isAnnotationPresent(annotationClazz) && clazz.isAssignableFrom(permitted)) {
                result.add((Class<? extends T>) permitted);
            }
            collectSealedPermits(permitted, result);
        }
    }
}
