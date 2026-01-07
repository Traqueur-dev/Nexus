package fr.traqueur.nexus.core.infrastructure.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.core.domain.workflow.conditions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ConditionSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Registry<Condition, ConditionMetadata> registry =
                new Registry<>(Condition.class, ConditionMetadata.class, ConditionMetadata::type);

        JacksonConfig config = new JacksonConfig(registry);
        objectMapper = config.objectMapper();
    }

    @Nested
    @DisplayName("Deserialization - Simple conditions")
    class DeserializationSimple {

        @Test
        @DisplayName("should deserialize ContainsCondition")
        void shouldDeserializeContainsCondition() throws Exception {
            String json = """
                {
                  "type": "contains",
                  "field": "content",
                  "value": "urgent"
                }
                """;

            Condition condition = objectMapper.readValue(json, Condition.class);

            assertThat(condition).isInstanceOf(ContainsCondition.class);
            ContainsCondition contains = (ContainsCondition) condition;
            assertThat(contains.field()).isEqualTo("content");
            assertThat(contains.value()).isEqualTo("urgent");
        }

        @Test
        @DisplayName("should deserialize EqualsCondition")
        void shouldDeserializeEqualsCondition() throws Exception {
            String json = """
                {
                  "type": "equals",
                  "field": "authorId",
                  "value": "123456"
                }
                """;

            Condition condition = objectMapper.readValue(json, Condition.class);

            assertThat(condition).isInstanceOf(EqualsCondition.class);
            EqualsCondition equals = (EqualsCondition) condition;
            assertThat(equals.field()).isEqualTo("authorId");
            assertThat(equals.value()).isEqualTo("123456");
        }

        @Test
        @DisplayName("should deserialize AlwaysCondition")
        void shouldDeserializeAlwaysCondition() throws Exception {
            String json = """
                {
                  "type": "always"
                }
                """;

            Condition condition = objectMapper.readValue(json, Condition.class);

            assertThat(condition).isInstanceOf(AlwaysCondition.class);
        }
    }

    @Nested
    @DisplayName("Deserialization - Composite conditions")
    class DeserializationComposite {

        @Test
        @DisplayName("should deserialize CompositeCondition with nested rules")
        void shouldDeserializeCompositeCondition() throws Exception {
            String json = """
                {
                  "type": "composite",
                  "operator": "AND",
                  "rules": [
                    {"type": "contains", "field": "content", "value": "urgent"},
                    {"type": "equals", "field": "authorId", "value": "123456"}
                  ]
                }
                """;

            Condition condition = objectMapper.readValue(json, Condition.class);

            assertThat(condition).isInstanceOf(CompositeCondition.class);
            CompositeCondition composite = (CompositeCondition) condition;
            assertThat(composite.operator()).isEqualTo(Condition.Operator.AND);
            assertThat(composite.rules()).hasSize(2);
            assertThat(composite.rules().get(0)).isInstanceOf(ContainsCondition.class);
            assertThat(composite.rules().get(1)).isInstanceOf(EqualsCondition.class);
        }

        @Test
        @DisplayName("should deserialize GroupCondition")
        void shouldDeserializeGroupCondition() throws Exception {
            String json = """
                {
                  "type": "group",
                  "minRequirements": 2,
                  "rules": [
                    {"type": "contains", "field": "content", "value": "urgent"},
                    {"type": "contains", "field": "content", "value": "help"},
                    {"type": "equals", "field": "authorId", "value": "123456"}
                  ]
                }
                """;

            Condition condition = objectMapper.readValue(json, Condition.class);

            assertThat(condition).isInstanceOf(GroupCondition.class);
            GroupCondition group = (GroupCondition) condition;
            assertThat(group.minRequirements()).isEqualTo(2);
            assertThat(group.rules()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Serialization")
    class Serialization {

        @Test
        @DisplayName("should serialize ContainsCondition with type field")
        void shouldSerializeContainsCondition() throws Exception {
            // Given
            Condition condition = new ContainsCondition("content", "urgent");

            // When
            String json = objectMapper.writeValueAsString(condition);

            // Then
            assertThat(json).contains("\"type\":\"contains\"");
            assertThat(json).contains("\"field\":\"content\"");
            assertThat(json).contains("\"value\":\"urgent\"");
        }

        @Test
        @DisplayName("should serialize EqualsCondition with type field")
        void shouldSerializeEqualsCondition() throws Exception {
            // Given
            Condition condition = new EqualsCondition("authorId", "123456");

            // When
            String json = objectMapper.writeValueAsString(condition);

            // Then
            assertThat(json).contains("\"type\":\"equals\"");
            assertThat(json).contains("\"field\":\"authorId\"");
            assertThat(json).contains("\"value\":\"123456\"");
        }

        @Test
        @DisplayName("should serialize AlwaysCondition with type field")
        void shouldSerializeAlwaysCondition() throws Exception {
            // Given
            Condition condition = new AlwaysCondition();

            // When
            String json = objectMapper.writeValueAsString(condition);

            // Then
            assertThat(json).contains("\"type\":\"always\"");
        }

        @Test
        @DisplayName("should serialize CompositeCondition with nested conditions")
        void shouldSerializeCompositeCondition() throws Exception {
            // Given
            Condition condition = new CompositeCondition(
                    Condition.Operator.AND,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            String json = objectMapper.writeValueAsString(condition);

            // Then
            assertThat(json).contains("\"type\":\"composite\"");
            assertThat(json).contains("\"operator\":\"AND\"");
            // Les nested conditions doivent aussi avoir leur "type"
            assertThat(json).contains("\"type\":\"contains\"");
            assertThat(json).contains("\"type\":\"equals\"");
        }

        @Test
        @DisplayName("should serialize GroupCondition with nested conditions")
        void shouldSerializeGroupCondition() throws Exception {
            // Given
            Condition condition = new GroupCondition(
                    2,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new ContainsCondition("content", "help"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            String json = objectMapper.writeValueAsString(condition);

            // Then
            assertThat(json).contains("\"type\":\"group\"");
            assertThat(json).contains("\"minRequirements\":2");
            assertThat(json).contains("\"type\":\"contains\"");
            assertThat(json).contains("\"type\":\"equals\"");
        }
    }

    @Nested
    @DisplayName("Roundtrip")
    class Roundtrip {

        @Test
        @DisplayName("should preserve simple condition through serialize/deserialize")
        void shouldPreserveSimpleCondition() throws Exception {
            // Given
            Condition original = new ContainsCondition("content", "urgent");

            // When
            String json = objectMapper.writeValueAsString(original);
            Condition restored = objectMapper.readValue(json, Condition.class);

            // Then
            assertThat(restored).isInstanceOf(ContainsCondition.class);
            ContainsCondition restoredContains = (ContainsCondition) restored;
            assertThat(restoredContains.field()).isEqualTo("content");
            assertThat(restoredContains.value()).isEqualTo("urgent");
        }

        @Test
        @DisplayName("should preserve composite condition through serialize/deserialize")
        void shouldPreserveCompositeCondition() throws Exception {
            // Given
            Condition original = new CompositeCondition(
                    Condition.Operator.OR,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            String json = objectMapper.writeValueAsString(original);
            Condition restored = objectMapper.readValue(json, Condition.class);

            // Then
            assertThat(restored).isInstanceOf(CompositeCondition.class);
            CompositeCondition restoredComposite = (CompositeCondition) restored;
            assertThat(restoredComposite.operator()).isEqualTo(Condition.Operator.OR);
            assertThat(restoredComposite.rules()).hasSize(2);
            assertThat(restoredComposite.rules().get(0)).isInstanceOf(ContainsCondition.class);
            assertThat(restoredComposite.rules().get(1)).isInstanceOf(EqualsCondition.class);
        }

        @Test
        @DisplayName("should preserve deeply nested conditions through serialize/deserialize")
        void shouldPreserveDeeplyNestedConditions() throws Exception {
            // Given
            Condition original = new CompositeCondition(
                    Condition.Operator.OR,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new GroupCondition(2, List.of(
                                    new EqualsCondition("authorId", "123"),
                                    new EqualsCondition("authorId", "456"),
                                    new EqualsCondition("authorId", "789")
                            ))
                    )
            );

            // When
            String json = objectMapper.writeValueAsString(original);
            Condition restored = objectMapper.readValue(json, Condition.class);

            // Then
            assertThat(restored).isInstanceOf(CompositeCondition.class);
            CompositeCondition restoredComposite = (CompositeCondition) restored;
            assertThat(restoredComposite.operator()).isEqualTo(Condition.Operator.OR);
            assertThat(restoredComposite.rules()).hasSize(2);

            // Vérifier la nested GroupCondition
            assertThat(restoredComposite.rules().get(1)).isInstanceOf(GroupCondition.class);
            GroupCondition restoredGroup = (GroupCondition) restoredComposite.rules().get(1);
            assertThat(restoredGroup.minRequirements()).isEqualTo(2);
            assertThat(restoredGroup.rules()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw exception for unknown type on deserialization")
        void shouldThrowExceptionForUnknownType() {
            String json = """
                {
                  "type": "unknown_type",
                  "field": "test"
                }
                """;

            assertThatThrownBy(() -> objectMapper.readValue(json, Condition.class))
                    .hasMessageContaining("Unknown condition type: unknown_type");
        }
    }
}