package fr.traqueur.nexus.infrastructure.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a path variable cannot be parsed into an {@code Event.Id}.
 *
 * <p>A malformed identifier is a bad request, not a missing resource and not a
 * server error. Without this, the {@code IllegalArgumentException} raised by the
 * domain would surface as a 500.
 *
 * <p>The expected format is deliberately <em>not</em> spelled out here. It used
 * to be — "expected &lt;source&gt;-&lt;6 alphanumeric chars&gt;" — and it went
 * stale the day ADR-008 made identifiers UUIDs, telling every client a format
 * that had not existed for two releases. A copy of a rule that lives in another
 * module rots without anything failing, which is the same argument ADR-011 makes
 * against restating domain invariants on a DTO.
 *
 * <p>So the reason comes from the exception the domain raised. It already knows
 * which part was wrong — the prefix, the UUID, the missing separator — and it
 * cannot disagree with itself.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEventIdException extends RuntimeException {

    public InvalidEventIdException(String id, Throwable cause) {
        super("Invalid event id '" + id + "': " + reason(cause), cause);
    }

    /** {@code Objects.requireNonNull} without an argument carries no message. */
    private static String reason(Throwable cause) {
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}