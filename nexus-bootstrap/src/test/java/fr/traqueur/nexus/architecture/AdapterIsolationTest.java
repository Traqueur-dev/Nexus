package fr.traqueur.nexus.architecture;

import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Rule 6 of CLAUDE.md, and the placement table of ARCHITECTURE.md section 4.
 *
 * <p>This is the one rule the module graph no longer enforces. Until ADR-007, an
 * adapter could not see another because they were separate Gradle modules;
 * merging them into packages of {@code nexus-infrastructure} traded that compile
 * error away, and these tests are what bought it back.
 */
@DisplayName("Adapter isolation")
class AdapterIsolationTest {

    /**
     * Packages that speak a protocol. Each is an adapter in its own right and must
     * not know the others exist.
     */
    private static final String[] PROTOCOLS = {"rest", "messaging", "persistence", "actions"};

    /**
     * Packages every adapter may use. They speak no protocol: {@code logging} is a
     * facade over the logger, {@code serialization} configures the JSON mapper for
     * whoever writes JSON. Sharing them couples adapters to a decision, not to
     * each other's wire format.
     */
    private static final String[] SHARED = {"logging", "serialization"};

    @Test
    @DisplayName("adapters should not depend on each other")
    void adaptersShouldNotDependOnEachOther() {
        for (String protocol : PROTOCOLS) {
            ruleFor(protocol).check(NexusClasses.get());
        }
    }

    private ArchRule ruleFor(String protocol) {
        String[] others = Arrays.stream(PROTOCOLS)
                .filter(candidate -> !candidate.equals(protocol))
                .map(candidate -> "fr.traqueur.nexus.infrastructure." + candidate + "..")
                .toArray(String[]::new);

        return noClasses().that().resideInAPackage("fr.traqueur.nexus.infrastructure." + protocol + "..")
                .should().dependOnClassesThat().resideInAnyPackage(others)
                .as(protocol + " should not depend on another adapter")
                .because("adapters translate between the outside and the application, and "
                        + "each speaks a different protocol. One reaching into another's "
                        + "types — the REST controller reading a RabbitMQ DTO, say — makes "
                        + "the wire format of one a hidden constraint on the other. When two "
                        + "adapters genuinely need the same payload, it is not a DTO: it is "
                        + "an application-level command. Shared support lives in "
                        + String.join(" or ", SHARED));
    }

    @Test
    @DisplayName("REST controllers should live in the rest package")
    void restControllersShouldLiveInRest() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("fr.traqueur.nexus.infrastructure.rest..")
                .as("@RestController belongs to infrastructure.rest")
                .because("a class annotated with a protocol's annotation is an adapter for "
                        + "that protocol, wherever it was put. Keeping HTTP in one package "
                        + "is what makes the answer to \"what does Nexus expose over HTTP?\" "
                        + "a directory listing")
                .check(NexusClasses.get());
    }

    @Test
    @DisplayName("JPA entities should live in the persistence package")
    void entitiesShouldLiveInPersistence() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("fr.traqueur.nexus.infrastructure.persistence..")
                .as("@Entity belongs to infrastructure.persistence")
                .because("an entity is a row, not a business concept. Letting one out of the "
                        + "persistence package is how a database schema starts dictating the "
                        + "domain model instead of storing it")
                .check(NexusClasses.get());
    }

    @Test
    @DisplayName("Rabbit listeners should live in the messaging package")
    void rabbitListenersShouldLiveInMessaging() {
        methods().that().areAnnotatedWith(RabbitListener.class)
                .should().beDeclaredInClassesThat()
                .resideInAPackage("fr.traqueur.nexus.infrastructure.messaging..")
                .as("@RabbitListener belongs to infrastructure.messaging")
                .because("consuming from a broker is an adapter's job. A listener elsewhere "
                        + "means business code that only runs when RabbitMQ is up")
                .check(NexusClasses.get());
    }
}