package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainsConditionTest {

    @Test
    @DisplayName("should return true when field contains value")
    void shouldReturnTrueWhenFieldContainsValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("This is an urgent message", 123456L);
        ContainsCondition condition = new ContainsCondition("content", "urgent");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when field does not contain value")
    void shouldReturnFalseWhenFieldDoesNotContainValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("This is a normal message", 123456L);
        ContainsCondition condition = new ContainsCondition("content", "urgent");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true when field contains value at start")
    void shouldReturnTrueWhenFieldContainsValueAtStart() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("urgent: please respond", 123456L);
        ContainsCondition condition = new ContainsCondition("content", "urgent");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when field contains value at end")
    void shouldReturnTrueWhenFieldContainsValueAtEnd() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("this is urgent", 123456L);
        ContainsCondition condition = new ContainsCondition("content", "urgent");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("This is URGENT", 123456L);
        ContainsCondition condition = new ContainsCondition("content", "urgent");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true when checking source contains value")
    void shouldReturnTrueWhenSourceContainsValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("Hello", 123456L);
        ContainsCondition condition = new ContainsCondition("source", "disc");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should throw exception when field does not exist")
    void shouldThrowExceptionWhenFieldDoesNotExist() {
        // Given
        Event event = createDiscordEvent("Hello", 123456L);
        ContainsCondition condition = new ContainsCondition("unknownField", "value");

        // When & Then
        assertThatThrownBy(() -> condition.isMet(event))
                .isInstanceOf(ConditionEvaluationException.class)
                .hasMessageContaining("unknownField");
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