package fr.traqueur.nexus.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventMetadata;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADR-006: an open hierarchy without a metadata annotation is incomplete.
 *
 * <p>Since ADR-001 dropped {@code sealed}, nothing in the compiler knows that a
 * new {@code Event} needs a stable identifier. The failure shows up at runtime,
 * far from the cause: the registry cannot name the type, so the event cannot be
 * stored, dispatched or read back. These rules move that failure to build time.
 *
 * <p>They are also the reason {@code Event.Id} is a nested record rather than a
 * top-level one — anything implementing the interface is a type crossing a
 * boundary, and needs a name that is not its class name.
 */
@DisplayName("Open hierarchies")
class OpenHierarchyTest {

    @Test
    @DisplayName("every event type should declare its identifier")
    void eventsShouldBeAnnotated() {
        metadataRule(Event.class, EventMetadata.class).check(NexusClasses.get());
    }

    @Test
    @DisplayName("every context type should declare its identifier")
    void contextsShouldBeAnnotated() {
        metadataRule(Context.class, ContextMetadata.class).check(NexusClasses.get());
    }

    @Test
    @DisplayName("every condition type should declare its identifier")
    void conditionsShouldBeAnnotated() {
        metadataRule(Condition.class, ConditionMetadata.class).check(NexusClasses.get());
    }

    @Test
    @DisplayName("every action type should declare its identifier")
    void actionsShouldBeAnnotated() {
        metadataRule(Action.class, ActionMetadata.class).check(NexusClasses.get());
    }

    /** A context type as a forgetful contributor would write it. */
    private record UndeclaredContext(String value) implements Context {
    }

    @Test
    @DisplayName("should reject a type that declares no identifier")
    void shouldRejectATypeWithoutIdentifier() {
        // The rules above pass, which proves nothing on its own — a rule selecting
        // no class at all would look identical from here. Run one against a type
        // that violates it, and check the report names the offender.
        JavaClasses offender = new ClassFileImporter().importClasses(UndeclaredContext.class);

        assertThatThrownBy(() -> metadataRule(Context.class, ContextMetadata.class).check(offender))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("UndeclaredContext")
                .hasMessageContaining("persisted contract");
    }

    /**
     * Concrete implementations only: the interface itself carries no identifier,
     * and neither does an abstract intermediate type, which is not a type anything
     * can be resolved to.
     */
    private ArchRule metadataRule(Class<?> hierarchy, Class<? extends Annotation> metadata) {
        return classes().that().implement(hierarchy)
                .and().areNotInterfaces()
                .and().doNotHaveModifier(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                .should().beAnnotatedWith(metadata)
                .as("every " + hierarchy.getSimpleName() + " declares @" + metadata.getSimpleName())
                .because("the type identifier is a persisted contract: it names the type in "
                        + "stored rows, in in-flight messages and in workflow definitions. "
                        + "Without the annotation the registry cannot resolve the class, and "
                        + "nothing says so until an event is ingested at runtime. The class "
                        + "name is not an option — renaming or moving the class would break "
                        + "every row already written");
    }
}