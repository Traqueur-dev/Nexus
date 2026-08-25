package fr.traqueur.nexus.infrastructure.rest.exceptions;

import fr.traqueur.nexus.domain.events.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins that the client is told why its id was rejected, in the domain's own words.
 *
 * <p>The message used to restate the expected format, and said "6 alphanumeric
 * chars" for two releases after ADR-008 made identifiers UUIDs. Deriving the
 * reason from the raised exception is what makes that impossible; this asserts
 * the derivation rather than any particular wording.
 */
@DisplayName("InvalidEventIdException")
class InvalidEventIdExceptionTest {

    @Test
    @DisplayName("should report the reason the domain gave")
    void shouldReportTheDomainReason() {
        Throwable cause = catchFromDomain("github-not-a-uuid");

        assertThat(new InvalidEventIdException("github-not-a-uuid", cause).getMessage())
                .contains("github-not-a-uuid")
                .contains(cause.getMessage());
    }

    @Test
    @DisplayName("should not claim a format of its own")
    void shouldNotRestateTheFormat() {
        // The specific rot: a hardcoded description of a format defined elsewhere.
        Throwable cause = catchFromDomain("nohyphen");

        assertThat(new InvalidEventIdException("nohyphen", cause).getMessage())
                .doesNotContain("alphanumeric");
    }

    @Test
    @DisplayName("should stay readable when the cause carries no message")
    void shouldHandleAMessagelessCause() {
        assertThat(new InvalidEventIdException("x", new NullPointerException()).getMessage())
                .contains("NullPointerException");
    }

    private static Throwable catchFromDomain(String raw) {
        return org.assertj.core.api.Assertions.catchThrowable(() -> Event.Id.fromString(raw));
    }

    @Test
    @DisplayName("should be raised for an id the domain refuses")
    void shouldCoverTheCasesTheControllerParses() {
        // Guards the assumption the message derivation rests on: these really do
        // fail in the domain, with something to say.
        assertThatThrownBy(() -> Event.Id.fromString("nohyphen")).hasMessageContaining("nohyphen");
        assertThatThrownBy(() -> Event.Id.fromString("github-not-a-uuid")).hasMessageContaining("UUID");
    }
}