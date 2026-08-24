package fr.traqueur.nexus.core.domain.workflow;

/**
 * A described intent, never its execution.
 *
 * <p>An action carries data only — what should happen, not how. Performing it is
 * the job of an adapter behind the {@code ActionHandler} port, which is what
 * keeps the domain free of infrastructure. See ADR-002 in docs/ARCHITECTURE.md.
 *
 * <p>Open hierarchy: adapters contribute their own action types.
 */
public interface Action {

}
