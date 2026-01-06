package fr.traqueur.nexus.core.domain.workflow.conditions;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ConditionEvaluationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlwaysConditionTest {

    @Test
    @DisplayName("should always return true")
    void shouldAlwaysReturnTrue() throws ConditionEvaluationException {
        // Given
        Event event = createDiscordEvent("any content", 123456L);
        AlwaysCondition condition = new AlwaysCondition();

        // When
        boolean result = condition.isMet(event);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true regardless of event content")
    void shouldReturnTrueRegardlessOfEventContent() throws ConditionEvaluationException {
        // Given
        Event event1 = createDiscordEvent("", 0L);
        Event event2 = createDiscordEvent("some message", 999999L);
        AlwaysCondition condition = new AlwaysCondition();

        // When & Then
        assertThat(condition.isMet(event1)).isTrue();
        assertThat(condition.isMet(event2)).isTrue();
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