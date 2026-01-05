package fr.traqueur.nexus.core.application.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NexusLogger {

    private static final Logger log = LoggerFactory.getLogger("Nexus");

    // === Generic methods ===

    public void info(String message) {
        log.info(message);
    }

    public void info(String message, Object... args) {
        log.info(message, args);
    }

    public void warn(String message) {
        log.warn(message);
    }

    public void warn(String message, Object... args) {
        log.warn(message, args);
    }

    public void error(String message) {
        log.error(message);
    }

    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public void error(String message, Object... args) {
        log.error(message, args);
    }

    public void debug(String message) {
        log.debug(message);
    }

    public void debug(String message, Object... args) {
        log.debug(message, args);
    }

    // === Business methods - Events ===

    public void eventReceived(String source, String type) {
        log.info("[EVENT] Received: {}.{}", source, type);
    }

    public void eventSaved(String eventId) {
        log.info("[EVENT] Saved: {}", eventId);
    }

    public void eventFailed(String source, String type, Throwable throwable) {
        log.error("[EVENT] Failed to process: {}.{} - {}", source, type, throwable.getMessage(), throwable);
    }

    public void queueConsuming(String queueName) {
        log.info("[RABBITMQ] Consuming from queue: {}", queueName);
    }

    public void messageReceived(String queue) {
        log.debug("[RABBITMQ] Message received on: {}", queue);
    }

    // === Business methods - Application lifecycle ===

    public void applicationStarted() {
        log.info("[NEXUS] Application started");
    }

    public void applicationStopped() {
        log.info("[NEXUS] Application stopped");
    }

}