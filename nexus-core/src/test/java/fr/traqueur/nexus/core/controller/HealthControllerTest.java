package fr.traqueur.nexus.core.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class HealthControllerTest {

    @Autowired
    private RestTestClient client;

    @Test
    public void health() {
        client.get().uri("/health").exchangeSuccessfully()
                .expectBody(HealthController.Status.class)
                .isEqualTo(new HealthController.Status("ok"));
    }
}