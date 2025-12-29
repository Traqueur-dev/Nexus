package fr.traqueur.nexus.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Status health() {
        return new Status("ok");
    }

    public record Status(String status) {}

}