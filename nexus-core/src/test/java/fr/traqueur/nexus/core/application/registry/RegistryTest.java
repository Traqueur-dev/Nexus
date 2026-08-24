package fr.traqueur.nexus.core.application.registry;

import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Registry")
class RegistryTest {

    // Types declared outside the domain package, standing in for what a plugin
    // would contribute. Before the hierarchies were opened these could not
    // compile at all: a sealed interface only permits subtypes it names.

    record OutsideContext() implements Context {
        @Override
        public String source() {
            return "outside";
        }
    }

    @EventMetadata(type = "outside.something_happened")
    record OutsideEvent(Id id, Context context, Instant timestamp, String payload) implements Event {
    }

    @EventMetadata(type = "outside.something_happened")
    record ConflictingEvent(Id id, Context context, Instant timestamp) implements Event {
    }

    @EventMetadata(type = "  ")
    record BlankTypeEvent(Id id, Context context, Instant timestamp) implements Event {
    }

    record UnannotatedEvent(Id id, Context context, Instant timestamp) implements Event {
    }

    @ConditionMetadata(type = "outside.custom")
    record OutsideCondition(String field) implements Condition {
        @Override
        public boolean isMet(Event event) throws ConditionEvaluationException {
            return true;
        }
    }

    private static Registry<Event, EventMetadata> eventRegistry() {
        return new Registry<>(Event.class, EventMetadata.class, EventMetadata::type);
    }

    @Nested
    @DisplayName("external registration")
    class ExternalRegistration {

        @Test
        @DisplayName("should register a type declared outside the domain (ADR-001)")
        void shouldRegisterTypeDeclaredOutsideTheDomain() {
            Registry<Event, EventMetadata> registry = eventRegistry().register(OutsideEvent.class);

            assertThat(registry.getClassForType("outside.something_happened")).isEqualTo(OutsideEvent.class);
            assertThat(registry.getTypeForClass(OutsideEvent.class)).isEqualTo("outside.something_happened");
        }

        @Test
        @DisplayName("should register condition types the same way")
        void shouldRegisterConditionTypes() {
            Registry<Condition, ConditionMetadata> registry =
                    new Registry<>(Condition.class, ConditionMetadata.class, ConditionMetadata::type)
                            .register(OutsideCondition.class);

            assertThat(registry.getClassForType("outside.custom")).isEqualTo(OutsideCondition.class);
        }

        @Test
        @DisplayName("should be idempotent when registering the same class twice")
        void shouldBeIdempotent() {
            Registry<Event, EventMetadata> registry = eventRegistry()
                    .register(OutsideEvent.class)
                    .register(OutsideEvent.class);

            assertThat(registry.registeredClasses()).containsExactly(OutsideEvent.class);
        }
    }

    @Nested
    @DisplayName("registration errors")
    class RegistrationErrors {

        @Test
        @DisplayName("should reject a class without metadata annotation")
        void shouldRejectUnannotatedClass() {
            assertThatThrownBy(() -> eventRegistry().register(UnannotatedEvent.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EventMetadata");
        }

        @Test
        @DisplayName("should reject a blank type identifier")
        void shouldRejectBlankIdentifier() {
            assertThatThrownBy(() -> eventRegistry().register(BlankTypeEvent.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank type identifier");
        }

        @Test
        @DisplayName("should reject two classes claiming the same identifier")
        void shouldRejectDuplicateIdentifier() {
            Registry<Event, EventMetadata> registry = eventRegistry().register(OutsideEvent.class);

            assertThatThrownBy(() -> registry.register(ConflictingEvent.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("outside.something_happened")
                    .hasMessageContaining(OutsideEvent.class.getName());
        }
    }

    @Nested
    @DisplayName("lookup")
    class Lookup {

        @Test
        @DisplayName("should return null for an unknown type")
        void shouldReturnNullForUnknownType() {
            assertThat(eventRegistry().getClassForType("nope")).isNull();
        }

        @Test
        @DisplayName("requireClassForType should fail listing the registered types")
        void requireClassShouldFailWithKnownTypes() {
            Registry<Event, EventMetadata> registry = eventRegistry().register(OutsideEvent.class);

            assertThatThrownBy(() -> registry.requireClassForType("nope"))
                    .isInstanceOf(UnknownTypeException.class)
                    .hasMessageContaining("nope")
                    .hasMessageContaining("outside.something_happened");
        }

        @Test
        @DisplayName("requireClassForType should report when nothing is registered")
        void requireClassShouldReportEmptyRegistry() {
            assertThatThrownBy(() -> eventRegistry().requireClassForType("nope"))
                    .isInstanceOf(UnknownTypeException.class)
                    .hasMessageContaining("<none>");
        }

        @Test
        @DisplayName("requireTypeForClass should fail for an unregistered class")
        void requireTypeShouldFailForUnregisteredClass() {
            assertThatThrownBy(() -> eventRegistry().requireTypeForClass(OutsideEvent.class))
                    .isInstanceOf(UnknownTypeException.class)
                    .hasMessageContaining(OutsideEvent.class.getName());
        }
    }
}
