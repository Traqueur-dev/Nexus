package fr.traqueur.nexus.core.infrastructure.actions;

import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class ActionHandlersConfig {

    /**
     * Gated on the same property Spring Boot uses to create the mail sender, so
     * the handler exists exactly when it can actually work.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    public ActionHandler<SendEmailAction> sendEmailActionHandler(
            JavaMailSender mailSender,
            @Value("${nexus.mail.from:nexus@localhost}") String from) {
        return new SendEmailActionHandler(mailSender, from);
    }
}
