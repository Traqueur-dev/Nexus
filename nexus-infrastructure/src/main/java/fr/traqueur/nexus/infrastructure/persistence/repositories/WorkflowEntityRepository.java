package fr.traqueur.nexus.infrastructure.persistence.repositories;

import fr.traqueur.nexus.infrastructure.persistence.entities.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowEntityRepository extends JpaRepository<WorkflowEntity, String> {

    /**
     * A native query because the containment operator has no JPQL equivalent.
     *
     * <p>{@code @>} is what the GIN index on {@code events} answers, so this reads
     * only the matching rows. Derived queries over a collection column would fetch
     * every workflow and filter in Java, which is exactly what the port's shape
     * exists to avoid.
     */
    @Query(value = "SELECT * FROM workflows WHERE events @> ARRAY[:type]::text[]", nativeQuery = true)
    List<WorkflowEntity> findTriggeredBy(@Param("type") String type);

    /**
     * Deletes and reports whether anything was there.
     *
     * <p>{@code CrudRepository.deleteById} returns void and is silent on a missing
     * id, so answering 404 through it needs an {@code existsById} first — two round
     * trips to learn what one statement already knows. A modifying query returns
     * the row count.
     */
    @Modifying
    @Query("DELETE FROM WorkflowEntity w WHERE w.id = :id")
    int deleteWorkflowById(@Param("id") String id);
}