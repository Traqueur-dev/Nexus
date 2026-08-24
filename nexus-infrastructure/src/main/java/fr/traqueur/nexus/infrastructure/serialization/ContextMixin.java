package fr.traqueur.nexus.infrastructure.serialization;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Makes {@code Context} polymorphic on its {@code source} discriminator.
 *
 * <p>The subtypes are deliberately not listed here. They used to be, as a fixed
 * {@code @JsonSubTypes} of three entries, which meant a context type contributed
 * by an adapter could never be deserialized — the hierarchy was open in the
 * domain but closed again at the serialization boundary. They are now registered
 * from the context registry in {@link JacksonConfig}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "source")
public abstract class ContextMixin {
}
