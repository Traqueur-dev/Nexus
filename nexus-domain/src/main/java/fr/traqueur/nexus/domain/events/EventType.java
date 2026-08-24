package fr.traqueur.nexus.domain.events;

import java.util.Objects;

/**
 * The stable identifier of an event type, e.g. {@code github.push_received}.
 *
 * <p>A workflow declares the event types it reacts to. Holding them as a value
 * object rather than bare strings means a workflow cannot be built from an
 * arbitrary string that happens to be lying around, and makes the intent visible
 * in every signature that carries one.
 *
 * <p>The convention is {@code <source>.<name>}, but it is not enforced: adapters
 * choose their own identifiers, and rejecting a plugin's naming would be an
 * arbitrary restriction. What is enforced is that the value can serve as a key —
 * present, non-blank, no surrounding or embedded whitespace.
 *
 * <p>This value is a persisted contract: it is stored on events and referenced by
 * workflows, so renaming one breaks existing rows.
 */
public record EventType(String value) {

    public EventType {
        Objects.requireNonNull(value, "event type is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("event type must not be blank");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("event type must not contain whitespace: '" + value + "'");
        }
    }

    public static EventType of(String value) {
        return new EventType(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
