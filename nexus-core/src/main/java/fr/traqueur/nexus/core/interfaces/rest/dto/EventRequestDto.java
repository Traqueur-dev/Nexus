package fr.traqueur.nexus.core.interfaces.rest.dto;

import java.time.Instant;
import java.util.Map;

public record EventRequestDto(
        String source,
        String type,
        Instant timestamp,
        String context,
        Map<String, Object> payload
) {
}
