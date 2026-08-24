package fr.traqueur.nexus.core.domain.workflow.exceptions;

public class ConditionEvaluationException extends Exception {

    public ConditionEvaluationException(String message) {
        super(message);
    }

    public ConditionEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
