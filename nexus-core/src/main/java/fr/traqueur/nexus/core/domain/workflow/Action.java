package fr.traqueur.nexus.core.domain.workflow;

import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;

public sealed interface Action permits SendEmailAction {

}
