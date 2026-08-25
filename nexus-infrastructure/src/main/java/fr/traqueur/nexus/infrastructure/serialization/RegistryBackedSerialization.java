package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registry;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.RecordComponent;
import java.util.Locale;

/**
 * Polymorphic JSON for an open hierarchy, resolved through its {@link Registry}.
 *
 * <p>One mechanism for every hierarchy — contexts, conditions, actions — rather
 * than one per hierarchy. Adding a fourth is a single {@link #register} call.
 *
 * <p><b>Why not Jackson's own subtype resolution.</b> {@code @JsonTypeInfo} plus a
 * subtype list resolves the type when the mapper is <em>built</em>. Feeding that
 * list from the registry, as {@code ContextMixin} did, only moves the snapshot one
 * level up: a plugin registering {@code SlackContext} at T+5min is in the registry
 * and still fails to deserialize, because the mapper stopped listening at startup.
 * Rebuilding the mapper is not an option either — {@code EventEntityMapper},
 * {@code EventDtoMapper} and {@code EventConsumer} already hold a reference to it.
 *
 * <p>Here the registry is consulted inside {@code serialize} / {@code deserialize},
 * so a type registered after the mapper was built works on the next call. That is
 * the property the plugin loader needs, and it is why this shape won over the
 * mixin rather than the other way round.
 *
 * <p><b>Wire format.</b> The discriminator is written inline as a regular property
 * — {@code {"source":"discord"}}, {@code {"type":"equals","field":…}} — which is
 * what {@code @JsonTypeInfo(As.PROPERTY)} produced. Stored rows and in-flight
 * messages are unaffected.
 *
 * <p><b>Records only.</b> Serialization walks record components rather than
 * delegating to the mapper, because delegating re-enters this serializer and
 * recurses forever. A non-record type therefore cannot be written, and says so
 * instead of silently emitting nothing but its discriminator. ArchUnit enforces
 * the same constraint at build time for types in this repository; a plugin's
 * types are only reachable at runtime, which is what this check is for.
 */
public final class RegistryBackedSerialization {

    private RegistryBackedSerialization() {
    }

    /**
     * Registers both directions for one hierarchy.
     *
     * @param baseType      the hierarchy's interface — Jackson resolves the
     *                      serializer through it for every implementation
     * @param discriminator the property naming the type. It is a persisted
     *                      contract: changing it breaks stored rows.
     */
    public static <T> void register(SimpleModule module,
                                    Class<T> baseType,
                                    Registry<T, ?> registry,
                                    String discriminator) {
        module.addSerializer(baseType, new Serializer<>(baseType, registry, discriminator));
        module.addDeserializer(baseType, new Deserializer<>(baseType, registry, discriminator));
    }

    /** {@code Condition} -> "condition", for messages a reader can act on. */
    private static String label(Class<?> baseType) {
        return baseType.getSimpleName().toLowerCase(Locale.ROOT);
    }

    static final class Deserializer<T> extends ValueDeserializer<T> {

        private final Class<T> baseType;
        private final Registry<T, ?> registry;
        private final String discriminator;

        Deserializer(Class<T> baseType, Registry<T, ?> registry, String discriminator) {
            this.baseType = baseType;
            this.registry = registry;
            this.discriminator = discriminator;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(p);
            if (!node.isObject()) {
                throw DatabindException.from(p, "A %s must be a JSON object".formatted(label(baseType)));
            }

            JsonNode typeNode = node.get(discriminator);
            if (typeNode == null) {
                throw DatabindException.from(p, "%s is missing its '%s' field"
                        .formatted(baseType.getSimpleName(), discriminator));
            }
            String type = typeNode.asString();

            // Removed before the value is read: the discriminator is not a record
            // component, and Jackson rejects unknown properties.
            ((ObjectNode) node).remove(discriminator);

            Class<? extends T> resolved = registry.getClassForType(type);
            if (resolved == null) {
                throw DatabindException.from(p, "Unknown %s type: %s".formatted(label(baseType), type));
            }

            return ctxt.readTreeAsValue(node, resolved);
        }
    }

    static final class Serializer<T> extends ValueSerializer<T> {

        private final Class<T> baseType;
        private final Registry<T, ?> registry;
        private final String discriminator;

        Serializer(Class<T> baseType, Registry<T, ?> registry, String discriminator) {
            this.baseType = baseType;
            this.registry = registry;
            this.discriminator = discriminator;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            String type = registry.getTypeForClass((Class<? extends T>) value.getClass());
            if (type == null) {
                throw DatabindException.from(gen, "Unknown %s class: %s"
                        .formatted(label(baseType), value.getClass().getName()));
            }
            if (!(value instanceof Record record)) {
                throw DatabindException.from(gen, "%s must be a record to be serialized as a %s"
                        .formatted(value.getClass().getName(), label(baseType)));
            }

            gen.writeStartObject();
            gen.writeStringProperty(discriminator, type);

            for (RecordComponent component : record.getClass().getRecordComponents()) {
                Object fieldValue;
                try {
                    fieldValue = component.getAccessor().invoke(record);
                } catch (ReflectiveOperationException e) {
                    throw DatabindException.from(gen, "Failed to read field: " + component.getName(), e);
                }
                gen.writeName(component.getName());
                ctxt.writeValue(gen, fieldValue);
            }

            gen.writeEndObject();
        }
    }
}