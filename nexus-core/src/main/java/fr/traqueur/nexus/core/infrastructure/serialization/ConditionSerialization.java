package fr.traqueur.nexus.core.infrastructure.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;

import java.io.IOException;
import java.lang.reflect.RecordComponent;

public class ConditionSerialization {

    public static class Deserializer extends JsonDeserializer<Condition> {

        private final Registry<Condition, ConditionMetadata> registry;

        public Deserializer(Registry<Condition, ConditionMetadata> registry) {
            this.registry = registry;
        }

        @Override
        public Condition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();

            String type = node.get("type").asText();
            ((ObjectNode) node).remove("type");

            Class<? extends Condition> conditionClass = this.registry.getClassForType(type);
            if (conditionClass == null) {
                throw new IOException("Unknown condition type: " + type);
            }

            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            return mapper.treeToValue(node, conditionClass);
        }
    }

    public static class Serializer extends JsonSerializer<Condition> {

        private final Registry<Condition, ConditionMetadata> registry;

        public Serializer(Registry<Condition, ConditionMetadata> registry) {
            this.registry = registry;
        }

        @Override
        public void serialize(Condition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            String type = this.registry.getTypeForClass(value.getClass());
            if (type == null) {
                throw new IOException("Unknown condition class: " + value.getClass().getName());
            }

            gen.writeStringField("type", type);

            // Sérialiser les champs du record manuellement pour éviter la récursion infinie
            if (value instanceof Record record) {
                RecordComponent[] components = record.getClass().getRecordComponents();
                for (RecordComponent component : components) {
                    try {
                        Object fieldValue = component.getAccessor().invoke(record);
                        gen.writeFieldName(component.getName());
                        serializers.defaultSerializeValue(fieldValue, gen);
                    } catch (Exception e) {
                        throw new IOException("Failed to serialize field: " + component.getName(), e);
                    }
                }
            }

            gen.writeEndObject();
        }
    }
}
