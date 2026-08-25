package fr.traqueur.nexus.infrastructure.rest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a request body cannot be built into a {@code Workflow}.
 *
 * <p>The invariants live in the domain record — at least one event type, at least
 * one action, a condition — and it defends them with
 * {@code IllegalArgumentException}. That is the right exception for a domain to
 * throw and the wrong status for a client to receive: without this translation, a
 * user posting a workflow with no actions gets a 500, which says the server broke
 * when in fact the request was rejected on purpose.
 *
 * <p>Deliberately not Jakarta Bean Validation on the DTO. Doing so would restate
 * every invariant in annotations next to the ones the record already enforces,
 * and the two would drift — the domain would stay correct while the API grew its
 * own idea of a valid workflow.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidWorkflowException extends RuntimeException {
    public InvalidWorkflowException(String id, Throwable cause) {
        super("Invalid workflow '" + id + "': " + cause.getMessage(), cause);
    }
}