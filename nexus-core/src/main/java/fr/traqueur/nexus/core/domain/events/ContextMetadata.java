package fr.traqueur.nexus.core.domain.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the source identifier of a {@link Context} implementation.
 *
 * <p>The value is the discriminator written when a context is serialized, so it
 * is a persisted contract: renaming it breaks stored rows and in-flight messages.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextMetadata {
    String type();
}
