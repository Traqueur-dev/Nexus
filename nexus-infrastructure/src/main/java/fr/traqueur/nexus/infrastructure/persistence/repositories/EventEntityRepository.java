package fr.traqueur.nexus.infrastructure.persistence.repositories;

import fr.traqueur.nexus.infrastructure.persistence.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventEntityRepository extends JpaRepository<EventEntity, String> {

    Optional<EventEntity> findFirstBySourceOrderByTimestampDesc(String source);

}
