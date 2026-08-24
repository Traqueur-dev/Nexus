package fr.traqueur.nexus.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static fr.traqueur.nexus.architecture.NexusClasses.APPLICATION;
import static fr.traqueur.nexus.architecture.NexusClasses.APPLICATION_SERVICES;
import static fr.traqueur.nexus.architecture.NexusClasses.BOOTSTRAP;
import static fr.traqueur.nexus.architecture.NexusClasses.DOMAIN;
import static fr.traqueur.nexus.architecture.NexusClasses.INFRASTRUCTURE;
import static fr.traqueur.nexus.architecture.NexusClasses.JDK;
import static fr.traqueur.nexus.architecture.NexusClasses.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rules 1 to 5 of CLAUDE.md: which layer may see which.
 *
 * <p>The module graph already fails the build on most of these. They are restated
 * here because a module boundary stops enforcing anything the moment two layers
 * share a module — which is how the codebase started, and what a future merge
 * could quietly reintroduce.
 */
@DisplayName("Layer dependencies")
class LayerDependencyTest {

    @Test
    @DisplayName("should have imported the whole application")
    void shouldHaveImportedTheApplication() {
        // Without this, an import that silently resolves to nothing would make
        // every rule below pass while checking absolutely nothing.
        assertThat(NexusClasses.get()).hasSizeGreaterThan(50);
        assertThat(NexusClasses.get().stream().map(JavaClass::getPackageName))
                .anyMatch(p -> p.startsWith("fr.traqueur.nexus.domain"))
                .anyMatch(p -> p.startsWith("fr.traqueur.nexus.application"))
                .anyMatch(p -> p.startsWith("fr.traqueur.nexus.infrastructure"))
                .anyMatch(p -> p.startsWith("fr.traqueur.nexus.bootstrap"));
    }

    @Test
    @DisplayName("domain should depend on nothing but the JDK")
    void domainShouldDependOnNothingButTheJdk() {
        classes().that().resideInAPackage(DOMAIN)
                .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, JDK)
                .as("nexus-domain has zero third-party dependencies")
                .because("the domain ships to third-party plugin authors as the SDK: "
                        + "every dependency added here is imposed on every plugin, forever. "
                        + "A Jackson annotation in the domain means every plugin author "
                        + "inherits Jackson, and the version Nexus happens to be on")
                .check(NexusClasses.get());
    }

    @Test
    @DisplayName("application should depend on the domain and nothing else")
    void applicationShouldDependOnDomainOnly() {
        classes().that().resideInAPackage(APPLICATION)
                .should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, JDK)
                .as("nexus-application carries no framework")
                .because("the application layer must stay drivable by something other than "
                        + "Spring — a plugin embedding the workflow engine, a CLI, another "
                        + "framework (ADR-005). It is also how the layer stays testable in "
                        + "milliseconds: no context to start, no container to wait for")
                .check(NexusClasses.get());
    }

    @Test
    @DisplayName("adapters should reach the application through its ports")
    void adaptersShouldGoThroughPorts() {
        noClasses().that().resideInAPackage(INFRASTRUCTURE)
                .should().dependOnClassesThat().resideInAPackage(APPLICATION_SERVICES)
                .as("adapters depend on ports, not on application services")
                .because("a port states what the adapter needs and nothing more, while the "
                        + "service is everything the application can do: the REST controller "
                        + "reading events had the ingestion path in reach for no reason. It "
                        + "also keeps the service free to change — splitting it, or renaming "
                        + "a method no adapter should have been calling — without touching "
                        + "an adapter (CLAUDE.md rule 5)")
                .check(NexusClasses.get());
    }

    @Test
    @DisplayName("adapters should not depend on the assembly module")
    void adaptersShouldNotDependOnBootstrap() {
        noClasses().that().resideInAnyPackage(DOMAIN, APPLICATION, INFRASTRUCTURE)
                .should().dependOnClassesThat().resideInAPackage(BOOTSTRAP)
                .as("nothing depends on nexus-bootstrap")
                .because("bootstrap decides which implementations are wired and which types "
                        + "are registered. A layer reaching back into it would make those "
                        + "assembly choices impossible to change without touching the layer, "
                        + "and would close the dependency graph into a cycle")
                .check(NexusClasses.get());
    }

    @Test
    @DisplayName("should be free of package cycles")
    void shouldBeFreeOfCycles() {
        SlicesRuleDefinition.slices()
                .matching(ROOT + ".(*)..")
                .should().beFreeOfCycles()
                .as("no cycles between layers")
                .because("a cycle means neither layer can be understood, tested or replaced "
                        + "without the other, whatever the arrows on the diagram say")
                .check(NexusClasses.get());
    }
}