package fr.traqueur.nexus.core.infrastructure.persistence.repositories;

import fr.traqueur.nexus.core.infrastructure.persistence.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, String> {
}
