package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeConditionTest {

    @Nested
    @DisplayName("AND operator")
    class AndOperator {

        @Test
        @DisplayName("should return true when all conditions are met")
        void shouldReturnTrueWhenAllConditionsAreMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 123456L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
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
            Event event = createDiscordEvent("normal message", 123456L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
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

        @Test
        @DisplayName("should return false when all conditions are not met")
        void shouldReturnFalseWhenAllConditionsAreNotMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("normal message", 999999L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
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

        @Test
        @DisplayName("should return true with single condition met")
        void shouldReturnTrueWithSingleConditionMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent", 123456L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
                    List.of(new ContainsCondition("content", "urgent"))
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("OR operator")
    class OrOperator {

        @Test
        @DisplayName("should return true when all conditions are met")
        void shouldReturnTrueWhenAllConditionsAreMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 123456L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.OR,
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
        @DisplayName("should return true when one condition is met")
        void shouldReturnTrueWhenOneConditionIsMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("normal message", 123456L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.OR,
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
        @DisplayName("should return false when no condition is met")
        void shouldReturnFalseWhenNoConditionIsMet() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("normal message", 999999L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.OR,
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
    @DisplayName("Nested conditions")
    class NestedConditions {

        @Test
        @DisplayName("should handle nested composite conditions")
        void shouldHandleNestedCompositeConditions() throws ConditionEvaluationException {
            // Given
            // "content contains 'urgent' AND (authorId = 123456 OR authorId = 789)"
            Event event = createDiscordEvent("urgent message", 789L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new CompositeCondition(
                                    Condition.Operator.OR,
                                    List.of(
                                            new EqualsCondition("authorId", "123456"),
                                            new EqualsCondition("authorId", "789")
                                    )
                            )
                    )
            );

            // When
            boolean result = condition.isMet(event);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when nested condition fails")
        void shouldReturnFalseWhenNestedConditionFails() throws ConditionEvaluationException {
            // Given
            Event event = createDiscordEvent("urgent message", 999L);
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
                    List.of(
                            new ContainsCondition("content", "urgent"),
                            new CompositeCondition(
                                    Condition.Operator.OR,
                                    List.of(
                                            new EqualsCondition("authorId", "123456"),
                                            new EqualsCondition("authorId", "789")
                                    )
                            )
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
            CompositeCondition condition = new CompositeCondition(
                    Condition.Operator.AND,
                    List.of(
                            new ContainsCondition("content", "message"),
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