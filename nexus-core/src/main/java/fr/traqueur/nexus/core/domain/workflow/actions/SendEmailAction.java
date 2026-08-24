package fr.traqueur.nexus.core.domain.workflow.actions;

import fr.traqueur.nexus.core.domain.workflow.Action;
import fr.traqueur.nexus.core.domain.workflow.ActionMetadata;

@ActionMetadata(type = "send_email")
public record SendEmailAction(String subject, String content, String to) implements Action {
}
