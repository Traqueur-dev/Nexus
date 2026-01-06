package fr.traqueur.nexus.core.domain.workflow.actions;

import fr.traqueur.nexus.core.domain.workflow.Action;

public record SendEmailAction(String subject, String content, String to) implements Action {
}
