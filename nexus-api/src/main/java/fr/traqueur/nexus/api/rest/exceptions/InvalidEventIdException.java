package fr.traqueur.nexus.api.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a path variable cannot be parsed into an {@code Event.Id}.
 *
 * <p>A malformed identifier is a bad request, not a missing resource and not a
 * server error. Without this, the {@code IllegalArgumentException} raised by the
 * domain would surface as a 500.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEventIdException extends RuntimeException {
    public InvalidEventIdException(String id, Throwable cause) {
        super("Invalid event id format: '" + id + "' (expected <source>-<6 alphanumeric chars>)", cause);
    }
}
