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

class EqualsConditionTest {

    @Test
    @DisplayName("should return true when field equals value")
    void shouldReturnTrueWhenFieldEqualsValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("Hello World", 123456L);
        EqualsCondition condition = new EqualsCondition("content", "Hello World");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when field does not equal value")
    void shouldReturnFalseWhenFieldDoesNotEqualValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("Hello World", 123456L);
        EqualsCondition condition = new EqualsCondition("content", "Goodbye World");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true when authorId equals value")
    void shouldReturnTrueWhenAuthorIdEqualsValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("Hello", 123456L);
        EqualsCondition condition = new EqualsCondition("authorId", "123456");

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when source equals value")
    void shouldReturnTrueWhenSourceEqualsValue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("Hello", 123456L);
        EqualsCondition condition = new EqualsCondition("source", "discord");

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
        EqualsCondition condition = new EqualsCondition("unknownField", "value");

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