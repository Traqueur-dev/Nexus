package fr.traqueur.nexus.core.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMQConfig {

    private static final List<String> SOURCES = List.of("discord", "github", "internal");

    @Bean
    public Declarables declarables() {
        TopicExchange topicExchange = new TopicExchange("nexus.events");
        List<Declarable> declarables = new ArrayList<>();
        declarables.add(topicExchange);

        for (String source : SOURCES) {
            Queue queue = new Queue("nexus." + source);
            Binding binding = BindingBuilder
                    .bind(queue)
                    .to(topicExchange)
                    .with(source + ".#");

            declarables.add(queue);
            declarables.add(binding);
        }

        return new Declarables(declarables);
    }

}
