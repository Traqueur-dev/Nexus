package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupConditionTest {

    @Nested
    @DisplayName("With minRequirements")
    class WithMinRequirements {

        @Test
        @DisplayName("should return true when exactly minRequirements conditions are met")
        void shouldReturnTrueWhenExactlyMinRequirementsAreMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 999L);
            GroupCondition condition = new GroupCondition(
                    2,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new ContainsCondition("content", "message"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true when more than minRequirements conditions are met")
        void shouldReturnTrueWhenMoreThanMinRequirementsAreMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 123456L);
            GroupCondition condition = new GroupCondition(
                    2,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new ContainsCondition("content", "message"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when less than minRequirements conditions are met")
        void shouldReturnFalseWhenLessThanMinRequirementsAreMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("normal text", 999L);
            GroupCondition condition = new GroupCondition(
                    2,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new ContainsCondition("content", "message"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when minRequirements is 1 and one condition is met")
        void shouldReturnTrueWhenMinRequirementsIsOneAndOneConditionIsMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent", 999L);
            GroupCondition condition = new GroupCondition(
                    1,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("With null minRequirements (all must match)")
    class WithNullMinRequirements {

        @Test
        @DisplayName("should return true when all conditions are met")
        void shouldReturnTrueWhenAllConditionsAreMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 123456L);
            GroupCondition condition = new GroupCondition(
                    null,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when one condition is not met")
        void shouldReturnFalseWhenOneConditionIsNotMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 999L);
            GroupCondition condition = new GroupCondition(
                    null,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new EqualsCondition("authorId", "123456")
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("should propagate exception from child condition")
        void shouldPropagateExceptionFromChildCondition() {
            // Given
            Event event = createDiscordEvent("message", 123456L);
            GroupCondition condition = new GroupCondition(
                    1,
                    List.of(
                            new EqualsCondition("unknownField", "value")
                    )
            );

            // When & Then
            assertThatThrownBy(() -> condition.isMet(event))
                    .isInstanceOf(ConditionEvaluationException.class);
        }
    }

    private Event createDiscordEvent(String content, long authorId) {
        return new DiscordMessageReceived(
                Event.Id.generate("disc"),
                new DiscordContext(),
                Instant.now(),
                content,
                authorId
        );
    }
}