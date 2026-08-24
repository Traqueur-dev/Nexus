package fr.traqueur.nexus.application.registry;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Raised when a type identifier read from storage, a message or a request does
 * not match anything registered.
 *
 * <p>With an open type hierarchy the compiler can no longer guarantee that every
 * identifier resolves, so the failure has to be explicit and informative at
 * runtime. The message lists the registered identifiers because the usual cause
 * is an adapter that was not loaded, or a type identifier that was renamed after
 * rows had already been persisted with the old one.
 */
public class UnknownTypeException extends RuntimeException {

    public UnknownTypeException(String type, Class<?> baseType, Collection<String> knownTypes) {
        super("Unknown %s type '%s'. Registered types: %s".formatted(
                baseType.getSimpleName(),
                type,
                knownTypes.isEmpty() ? "<none>" : knownTypes.stream().sorted().collect(Collectors.joining(", "))));
    }

    public UnknownTypeException(Class<?> unregistered, Class<?> baseType) {
        super("%s is not registered as a %s type. Register it before use.".formatted(
                unregistered.getName(), baseType.getSimpleName()));
    }
}
