package fr.traqueur.nexus.domain.workflow.actions;

import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;

@ActionMetadata(type = "send_email")
public record SendEmailAction(String subject, String content, String to) implements Action {
}
