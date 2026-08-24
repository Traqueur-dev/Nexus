package fr.traqueur.nexus.core.application.registry;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Bidirectional mapping between a stable string type identifier and the class
 * implementing it.
 *
 * <p>Types are registered explicitly. The previous implementation discovered them
 * by walking {@code getPermittedSubclasses()}, which required the hierarchy to be
 * {@code sealed} and therefore made any type declared outside this compilation
 * module impossible. Explicit registration is what allows a bundled or
 * third-party adapter to contribute its own types (ADR-001).
 *
 * <p>Registration is expected to happen at startup, but the maps are concurrent
 * so a plugin loaded later can register safely while the application reads.
 *
 * @param <T> the base type being registered (Event, Condition, …)
 * @param <A> the annotation carrying the type identifier
 */
public class Registry<T, A extends Annotation> {

    private final Class<T> baseType;
    private final Class<A> annotationType;
    private final Function<A, String> typeExtractor;

    private final Map<String, Class<? extends T>> typeToClass;
    private final Map<Class<? extends T>, String> classToType;

    public Registry(Class<T> baseType, Class<A> annotationType, Function<A, String> typeExtractor) {
        this.baseType = baseType;
        this.annotationType = annotationType;
        this.typeExtractor = typeExtractor;
        this.typeToClass = new ConcurrentHashMap<>();
        this.classToType = new ConcurrentHashMap<>();
    }

    /**
     * Registers a type, reading its identifier from the metadata annotation.
     *
     * <p>Registering the same class twice is a no-op, so an adapter reloaded at
     * runtime does not fail. Two different classes claiming the same identifier
     * is a conflict and is rejected: silently letting one win would make
     * deserialization depend on registration order.
     *
     * @throws IllegalArgumentException if the class carries no metadata annotation
     *                                  or declares a blank identifier
     * @throws IllegalStateException    if another class already claims the identifier
     */
    public Registry<T, A> register(Class<? extends T> type) {
        A metadata = type.getAnnotation(annotationType);
        if (metadata == null) {
            throw new IllegalArgumentException("%s must be annotated with @%s to be registered as a %s type"
                    .formatted(type.getName(), annotationType.getSimpleName(), baseType.getSimpleName()));
        }

        String identifier = typeExtractor.apply(metadata);
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("@%s on %s declares a blank type identifier"
                    .formatted(annotationType.getSimpleName(), type.getName()));
        }

        Class<? extends T> existing = typeToClass.get(identifier);
        if (existing != null && !existing.equals(type)) {
            throw new IllegalStateException("Type identifier '%s' is already registered to %s, cannot register %s"
                    .formatted(identifier, existing.getName(), type.getName()));
        }

        typeToClass.put(identifier, type);
        classToType.put(type, identifier);
        return this;
    }

    @SafeVarargs
    public final Registry<T, A> registerAll(Class<? extends T>... types) {
        for (Class<? extends T> type : types) {
            register(type);
        }
        return this;
    }

    public Registry<T, A> registerAll(Collection<Class<? extends T>> types) {
        types.forEach(this::register);
        return this;
    }

    /** Returns the class for an identifier, or {@code null} if none is registered. */
    public Class<? extends T> getClassForType(String type) {
        return typeToClass.get(type);
    }

    /**
     * Returns the class for an identifier, failing with a readable message when
     * none is registered.
     *
     * <p>Prefer this at every boundary that reads an identifier from outside —
     * database rows, queue messages, HTTP payloads — so an unknown type surfaces
     * as a clear error rather than a {@code NullPointerException} further down.
     */
    public Class<? extends T> requireClassForType(String type) {
        Class<? extends T> resolved = typeToClass.get(type);
        if (resolved == null) {
            throw new UnknownTypeException(type, baseType, typeToClass.keySet());
        }
        return resolved;
    }

    /** Returns the identifier for a class, or {@code null} if it is not registered. */
    public String getTypeForClass(Class<? extends T> clazz) {
        return classToType.get(clazz);
    }

    /** Returns the identifier for a class, failing when it is not registered. */
    public String requireTypeForClass(Class<? extends T> clazz) {
        String identifier = classToType.get(clazz);
        if (identifier == null) {
            throw new UnknownTypeException(clazz, baseType);
        }
        return identifier;
    }

    public boolean isRegistered(String type) {
        return typeToClass.containsKey(type);
    }

    /** The registered identifiers, for diagnostics and dynamic serialization setup. */
    public Set<String> registeredTypes() {
        return Set.copyOf(typeToClass.keySet());
    }

    /** The registered classes, for diagnostics and dynamic serialization setup. */
    public Set<Class<? extends T>> registeredClasses() {
        return Set.copyOf(classToType.keySet());
    }
}
