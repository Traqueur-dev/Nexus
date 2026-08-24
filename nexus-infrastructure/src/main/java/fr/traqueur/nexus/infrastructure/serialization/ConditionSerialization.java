package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.RecordComponent;

/**
 * Polymorphic JSON for conditions, resolved through the registry.
 *
 * <p>Unlike a fixed {@code @JsonSubTypes} list, the type is looked up when the
 * value is read, so a condition registered after the mapper was built still
 * deserializes.
 */
public class ConditionSerialization {

    public static class Deserializer extends ValueDeserializer<Condition> {

        private final Registry<Condition, ConditionMetadata> registry;

        public Deserializer(Registry<Condition, ConditionMetadata> registry) {
            this.registry = registry;
        }

        @Override
        public Condition deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = ctxt.readTree(p);

            JsonNode typeNode = node.get("type");
            if (typeNode == null) {
                throw DatabindException.from(p, "Condition is missing its 'type' field");
            }
            String type = typeNode.asString();
            ((ObjectNode) node).remove("type");

            Class<? extends Condition> conditionClass = registry.getClassForType(type);
            if (conditionClass == null) {
                throw DatabindException.from(p, "Unknown condition type: " + type);
            }

            return ctxt.readTreeAsValue(node, conditionClass);
        }
    }

    public static class Serializer extends ValueSerializer<Condition> {

        private final Registry<Condition, ConditionMetadata> registry;

        public Serializer(Registry<Condition, ConditionMetadata> registry) {
            this.registry = registry;
        }

        @Override
        public void serialize(Condition value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            String type = registry.getTypeForClass(value.getClass());
            if (type == null) {
                throw DatabindException.from(gen, "Unknown condition class: " + value.getClass().getName());
            }

            gen.writeStartObject();
            gen.writeStringProperty("type", type);

            // Written component by component rather than delegating to the mapper:
            // delegating would re-enter this serializer and recurse forever.
            if (value instanceof Record record) {
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
            }

            gen.writeEndObject();
        }
    }
}
