package fr.traqueur.nexus.core.infrastructure.actions;

import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Carries out a {@link SendEmailAction}.
 *
 * <p>This is the concrete half of ADR-002. {@code SendEmailAction} is a record
 * describing an intent and knows nothing about SMTP; everything technical lives
 * here, where depending on Spring's mail client is legitimate.
 *
 * <p>Registered only when {@code spring.mail.host} is configured — the same
 * condition Spring Boot uses to create the mail sender. Without it there is no
 * handler for the action, and the dispatcher says so explicitly rather than
 * pretending the email was sent.
 */
public class SendEmailActionHandler implements ActionHandler<SendEmailAction> {

    private final JavaMailSender mailSender;
    private final String from;

    public SendEmailActionHandler(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public Class<SendEmailAction> handles() {
        return SendEmailAction.class;
    }

    @Override
    public void execute(SendEmailAction action, Event event) throws ActionExecutionException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(action.to());
        message.setSubject(action.subject());
        message.setText(action.content());

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new ActionExecutionException(
                    "Failed to send email to %s for event %s".formatted(action.to(), event.id()), e);
        }
    }
}
