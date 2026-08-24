package fr.traqueur.nexus.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises V2 against rows written under the old id format.
 *
 * <p>Every other test starts from an empty database, where V2's {@code UPDATE}
 * touches nothing and its correctness is never established. That statement
 * rewrites primary keys: getting it wrong loses events, which is the failure
 * #27 exists to remove, not to relocate.
 *
 * <p>Runs Flyway directly rather than through Spring: stopping at version 1,
 * seeding, then migrating is the whole point, and an application context offers
 * no way to pause halfway.
 */
@Testcontainers
@DisplayName("Event id migration")
class EventIdMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    /**
     * One container, but each test starts from nothing.
     *
     * <p>These tests migrate to different points, so a shared schema would leave
     * one test's constraint in place for the next — which is how the first run of
     * this class failed, on state rather than on the migration.
     */
    @BeforeEach
    void resetSchema() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .cleanDisabled(false)
                .load()
                .clean();
    }

    @Test
    @DisplayName("should rewrite pre-#27 ids without losing an event")
    void shouldRewriteLegacyIds() throws Exception {
        migrateTo("1");
        seedLegacyEvent("discord-abc123", "discord");
        seedLegacyEvent("github-xyz789", "github");

        migrateTo(MigrationVersion.LATEST.getVersion());

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            ResultSet rows = statement.executeQuery(
                    "SELECT id, source, payload FROM events ORDER BY source");

            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("source")).isEqualTo("discord");
            assertThat(rows.getString("id")).matches(
                    "discord-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            // The row is rewritten, not replaced: what the event says is untouched.
            assertThat(rows.getString("payload")).contains("kept");

            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("source")).isEqualTo("github");
            assertThat(rows.getString("id")).startsWith("github-");

            assertThat(rows.next()).isFalse();
        }
    }

    @Test
    @DisplayName("should reject an id in the old format once migrated")
    void shouldRejectLegacyIdAfterMigration() throws Exception {
        migrateTo(MigrationVersion.LATEST.getVersion());

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {

            // The constraint is what stops the old format coming back through a
            // path that bypasses the application.
            assertThat(insertFails(statement, "discord-abc123")).isTrue();
        }
    }

    private boolean insertFails(Statement statement, String id) {
        try {
            statement.execute(insertStatement(id, "discord"));
            return false;
        } catch (Exception expected) {
            return true;
        }
    }

    private void seedLegacyEvent(String id, String source) throws Exception {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute(insertStatement(id, source));
        }
    }

    private String insertStatement(String id, String source) {
        return """
                INSERT INTO events (id, source, type, context, payload, timestamp)
                VALUES ('%s', '%s', '%s.something', '{"source":"%s"}', '{"note":"kept"}', '%s')
                """.formatted(id, source, source, source, Instant.parse("2026-01-04T10:00:00Z"));
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target(MigrationVersion.fromVersion(version))
                .load()
                .migrate();
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}