package fr.traqueur.nexus.core.domain.events;

/**
 * Source-specific metadata attached to an {@link Event}.
 *
 * <p>Open hierarchy: each adapter provides its own context type. The value
 * returned by {@link #source()} is the discriminator used when the context is
 * serialized, so it is a persisted contract.
 */
public interface Context {

    String source();

}
