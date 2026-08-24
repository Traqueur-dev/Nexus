package fr.traqueur.nexus.core.domain.workflow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the stable identifier of an {@link Action} implementation.
 *
 * <p>Workflows are persisted, so their actions are written to and read from JSON
 * by this identifier. It is a persisted contract: renaming it breaks stored
 * workflows, and the class name is deliberately not used for the same reason —
 * moving a package would silently break them.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionMetadata {
    String type();
}
