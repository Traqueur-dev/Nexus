package fr.traqueur.nexus.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.application.ports.in.IngestEvent;
import fr.traqueur.nexus.application.ports.in.IngestEventCommand;
import fr.traqueur.nexus.application.ports.in.IngestionResult;
import fr.traqueur.nexus.application.workflow.WorkflowRun;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.infrastructure.logging.NexusLogger;
import fr.traqueur.nexus.infrastructure.messaging.dto.EventMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private final NexusLogger logger;
    private final IngestEvent ingestEvent;
    private final ObjectMapper json;

    public EventConsumer(NexusLogger logger, IngestEvent ingestEvent, ObjectMapper json) {
        this.logger = logger;
        this.ingestEvent = ingestEvent;
        this.json = json;
    }

    @RabbitListener(queues = {"nexus.discord", "nexus.github", "nexus.internal"})
    public void consumeEvent(String message) {
        try {
            EventMessage received = json.readValue(message, EventMessage.class);
            logger.eventReceived(received.source(), received.type());

            IngestionResult result = ingestEvent.ingest(toCommand(received));
            logger.eventSaved(result.event().id().toString());
            reportFailedWorkflows(result);
        } catch (Exception e) {
            logger.error("Error while processing event", e);
        }
    }

    /**
     * A workflow that failed must not fail the ingestion — the event is already
     * stored — but it must not disappear either.
     */
    private void reportFailedWorkflows(IngestionResult result) {
        for (WorkflowRun run : result.failedRuns()) {
            if (!run.fired()) {
                logger.warn("[WORKFLOW] {} could not be evaluated: {}", run.workflowId(), run.evaluationError());
                continue;
            }
            run.failures().forEach(outcome -> logger.warn(
                    "[WORKFLOW] {} action {} failed: {}",
                    run.workflowId(), outcome.actionType(), outcome.failure()));
        }
    }

    /**
     * Turns the wire format into the application's ingestion contract.
     *
     * <p>Decoding the context is done here rather than in the application: reading
     * a transport's encoding is exactly what an adapter is for.
     */
    private IngestEventCommand toCommand(EventMessage message) throws Exception {
        Context context = json.readValue(message.context(), Context.class);
        return new IngestEventCommand(
                message.source(),
                message.type(),
                message.timestamp(),
                context,
                message.payload());
    }
}
