package fr.traqueur.nexus.core.application.registry;

import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.core.domain.workflow.conditions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConditionRegistryTest {

    private Registry<Condition, ConditionMetadata> registry;

    @BeforeEach
    void setUp() {
        registry = new Registry<>(Condition.class, ConditionMetadata.class, ConditionMetadata::type);
    }

    @Nested
    @DisplayName("getClassForType")
    class GetClassForType {

        @Test
        @DisplayName("should return AlwaysCondition for always")
        void shouldReturnAlwaysCondition() {
            Class<? extends Condition> conditionClass = registry.getClassForType("always");

            assertThat(conditionClass).isEqualTo(AlwaysCondition.class);
        }

        @Test
        @DisplayName("should return ContainsCondition for contains")
        void shouldReturnContainsCondition() {
            Class<? extends Condition> conditionClass = registry.getClassForType("contains");

            assertThat(conditionClass).isEqualTo(ContainsCondition.class);
        }

        @Test
        @DisplayName("should return EqualsCondition for equals")
        void shouldReturnEqualsCondition() {
            Class<? extends Condition> conditionClass = registry.getClassForType("equals");

            assertThat(conditionClass).isEqualTo(EqualsCondition.class);
        }

        @Test
        @DisplayName("should return CompositeCondition for composite")
        void shouldReturnCompositeCondition() {
            Class<? extends Condition> conditionClass = registry.getClassForType("composite");

            assertThat(conditionClass).isEqualTo(CompositeCondition.class);
        }

        @Test
        @DisplayName("should return GroupCondition for group")
        void shouldReturnGroupCondition() {
            Class<? extends Condition> conditionClass = registry.getClassForType("group");

            assertThat(conditionClass).isEqualTo(GroupCondition.class);
        }

        @Test
        @DisplayName("should return null for unknown type")
        void shouldReturnNullForUnknownType() {
            Class<? extends Condition> conditionClass = registry.getClassForType("unknown");

            assertThat(conditionClass).isNull();
        }
    }

    @Nested
    @DisplayName("getTypeForClass")
    class GetTypeForClass {

        @Test
        @DisplayName("should return always for AlwaysCondition")
        void shouldReturnAlwaysType() {
            String type = registry.getTypeForClass(AlwaysCondition.class);

            assertThat(type).isEqualTo("always");
        }

        @Test
        @DisplayName("should return contains for ContainsCondition")
        void shouldReturnContainsType() {
            String type = registry.getTypeForClass(ContainsCondition.class);

            assertThat(type).isEqualTo("contains");
        }

        @Test
        @DisplayName("should return equals for EqualsCondition")
        void shouldReturnEqualsType() {
            String type = registry.getTypeForClass(EqualsCondition.class);

            assertThat(type).isEqualTo("equals");
        }

        @Test
        @DisplayName("should return composite for CompositeCondition")
        void shouldReturnCompositeType() {
            String type = registry.getTypeForClass(CompositeCondition.class);

            assertThat(type).isEqualTo("composite");
        }

        @Test
        @DisplayName("should return group for GroupCondition")
        void shouldReturnGroupType() {
            String type = registry.getTypeForClass(GroupCondition.class);

            assertThat(type).isEqualTo("group");
        }
    }

    @Nested
    @DisplayName("discovery")
    class Discovery {

        @Test
        @DisplayName("should discover all annotated conditions")
        void shouldDiscoverAllConditions() {
            // Verify bidirectional mapping works for all discovered conditions
            assertThat(registry.getClassForType("always")).isNotNull();
            assertThat(registry.getClassForType("contains")).isNotNull();
            assertThat(registry.getClassForType("equals")).isNotNull();
            assertThat(registry.getClassForType("composite")).isNotNull();
            assertThat(registry.getClassForType("group")).isNotNull();
        }
    }
}