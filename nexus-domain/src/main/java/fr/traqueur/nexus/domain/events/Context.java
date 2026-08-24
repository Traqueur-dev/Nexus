package fr.traqueur.nexus.domain.events;

/**
 * Source-specific metadata attached to an {@link Event}.
 *
 * <p>Open hierarchy: each adapter provides its own context type, annotated with
 * {@link ContextMetadata}.
 */
public interface Context {

    /**
     * The source identifier, read from {@link ContextMetadata}.
     *
     * <p>Deriving it from the annotation rather than restating it in each
     * implementation keeps a single source of truth: the value used to serialize
     * a context and the value it reports cannot drift apart.
     */
    default String source() {
        ContextMetadata metadata = getClass().getAnnotation(ContextMetadata.class);
        if (metadata == null) {
            throw new IllegalStateException(
                    "%s must be annotated with @ContextMetadata, or override source()"
                            .formatted(getClass().getName()));
        }
        return metadata.type();
    }

}
