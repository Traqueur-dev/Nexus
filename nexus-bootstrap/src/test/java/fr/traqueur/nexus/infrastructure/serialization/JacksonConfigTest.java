package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.bootstrap.RegistriesConfig;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.github.GitHubContext;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.conditions.EqualsCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Nexus' serialization reaches the mapper the application injects.
 *
 * <p>This covers the half {@code ContextSerializationTest} cannot: that test builds
 * the mapper itself, so it passes whether or not the configuration is wired into
 * Spring. When the serialization was declared as a competing {@code ObjectMapper}
 * bean, it passed while every context stored to PostgreSQL was written by Boot's
 * unconfigured mapper and could not be read back.
 *
 * <p>No Docker, no broker: it asserts the wiring, which is where that bug lived.
 */
@DisplayName("Jackson wiring")
class JacksonConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            // The real registry configuration rather than stubs: it is what decides
            // which context types the mapper can resolve, so stubbing it would test
            // a mapper the application never builds.
            .withUserConfiguration(RegistriesConfig.class, JacksonConfig.class);

    @Test
    @DisplayName("should leave exactly one mapper in the context")
    void shouldExposeASingleMapper() {
        // Two mappers is the failure mode itself: injection picks Boot's @Primary
        // one and the configured one is silently unused.
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeanNamesForType(ObjectMapper.class)).hasSize(1);
        });
    }

    @Test
    @DisplayName("should round-trip a context through the injected mapper")
    void shouldRoundTripContext() {
        runner.run(context -> {
            ObjectMapper json = context.getBean(ObjectMapper.class);

            String encoded = json.writeValueAsString(new GitHubContext());

            assertThat(encoded).isEqualTo("{\"source\":\"github\"}");
            assertThat(json.readValue(encoded, Context.class)).isInstanceOf(GitHubContext.class);
        });
    }

    @Test
    @DisplayName("should round-trip a condition through the injected mapper")
    void shouldRoundTripCondition() {
        runner.run(context -> {
            ObjectMapper json = context.getBean(ObjectMapper.class);

            String encoded = json.writeValueAsString(new EqualsCondition("content", "hello"));

            assertThat(encoded).contains("\"type\":\"equals\"");
            assertThat(json.readValue(encoded, Condition.class))
                    .isEqualTo(new EqualsCondition("content", "hello"));
        });
    }
}