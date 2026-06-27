package org.rrc.pollution_response_system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.rrc.pollution_response_system.entity.User;
import java.util.List;

/**
 * Runs on every application startup and migrates legacy role names to their
 * current equivalents.  Safe to re-run: the UPDATE is a no-op when no
 * matching rows exist.
 *
 * Migrations performed:
 *   ENVIRONMENTAL_OFFICER  → ENVIRONMENTAL_AUTHORITY
 *   DISASTER_OFFICER       → ENVIRONMENTAL_AUTHORITY
 */
@Component
public class RoleMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleMigrationRunner.class);

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;

    public RoleMigrationRunner(JdbcTemplate jdbc, UserRepository userRepository) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Widen the role column to accommodate the longer 'ENVIRONMENTAL_AUTHORITY' string
        try {
            jdbc.execute("ALTER TABLE user_roles ALTER COLUMN role VARCHAR(50)");
            log.info("[RoleMigration] Widened user_roles.role column to VARCHAR(50) (H2 Dialect)");
        } catch (Exception e) {
            try {
                jdbc.execute("ALTER TABLE user_roles MODIFY COLUMN role VARCHAR(50)");
                log.info("[RoleMigration] Widened user_roles.role column to VARCHAR(50) (MySQL Dialect)");
            } catch (Exception ex) {
                log.warn("[RoleMigration] Could not widen role column: {}", ex.getMessage());
            }
        }

        try {
            int updated1 = jdbc.update(
                "UPDATE user_roles SET role = 'ENVIRONMENTAL_AUTHORITY' WHERE role = 'ENVIRONMENTAL_OFFICER'"
            );
            if (updated1 > 0) {
                log.info("[RoleMigration] Migrated {} ENVIRONMENTAL_OFFICER → ENVIRONMENTAL_AUTHORITY", updated1);
            }

            int updated2 = jdbc.update(
                "UPDATE user_roles SET role = 'ENVIRONMENTAL_AUTHORITY' WHERE role = 'DISASTER_OFFICER'"
            );
            if (updated2 > 0) {
                log.info("[RoleMigration] Migrated {} DISASTER_OFFICER → ENVIRONMENTAL_AUTHORITY", updated2);
            }

            int updated3 = jdbc.update(
                "UPDATE user_roles SET role = 'ENVIRONMENTAL_AUTHORITY' WHERE role NOT IN ('ADMIN', 'ENVIRONMENTAL_AUTHORITY', 'CITIZEN')"
            );
            if (updated3 > 0) {
                log.info("[RoleMigration] Fixed {} invalid or truncated roles to ENVIRONMENTAL_AUTHORITY", updated3);
            }

            log.info("[RoleMigration] Role migration complete.");
        } catch (Exception e) {
            log.warn("[RoleMigration] Could not run role migration (table may not exist yet): {}", e.getMessage());
        }

        // Migrate stale investigation_status values from the old 5-status system to the new 3-status system
        try {
            int closedToResolved = jdbc.update(
                "UPDATE pollution_cases SET investigation_status = 'RESOLVED' WHERE investigation_status = 'CLOSED'"
            );
            if (closedToResolved > 0) {
                log.info("[StatusMigration] Migrated {} CLOSED → RESOLVED", closedToResolved);
            }

            int approvedToResolved = jdbc.update(
                "UPDATE pollution_cases SET investigation_status = 'RESOLVED' WHERE investigation_status = 'APPROVED'"
            );
            if (approvedToResolved > 0) {
                log.info("[StatusMigration] Migrated {} APPROVED → RESOLVED", approvedToResolved);
            }

            int rejectedToPending = jdbc.update(
                "UPDATE pollution_cases SET investigation_status = 'PENDING' WHERE investigation_status = 'REJECTED'"
            );
            if (rejectedToPending > 0) {
                log.info("[StatusMigration] Migrated {} REJECTED → PENDING", rejectedToPending);
            }

            // Catch any other invalid statuses
            int otherToResolved = jdbc.update(
                "UPDATE pollution_cases SET investigation_status = 'RESOLVED' WHERE investigation_status NOT IN ('PENDING', 'IN_PROGRESS', 'RESOLVED')"
            );
            if (otherToResolved > 0) {
                log.info("[StatusMigration] Fixed {} invalid statuses → RESOLVED", otherToResolved);
            }

            log.info("[StatusMigration] Investigation status migration complete.");
        } catch (Exception e) {
            log.warn("[StatusMigration] Could not run status migration: {}", e.getMessage());
        }

        try {
            List<User> users = userRepository.findAll();
            int updatedPhones = 0;
            for (User user : users) {
                try {
                    if (user.getPhone() != null && !user.getPhone().matches("^07[2389]\\d{7}$")) {
                        user.setPhone("0780000000");
                        userRepository.save(user);
                        updatedPhones++;
                    }
                } catch (Exception e) {
                    log.warn("[RoleMigration] Failed to fix phone for user {}: {}", user.getUsername(), e.getMessage());
                }
            }
            if (updatedPhones > 0) {
                log.info("[RoleMigration] Fixed {} invalid phone numbers in the database using Java regex", updatedPhones);
            }
        } catch (Exception e) {
            log.warn("[RoleMigration] Could not fetch users for phone migration: {}", e.getMessage());
        }

        // Region cleanup: Keep only Gasabo
        try {
            Long gasaboId = jdbc.queryForObject("SELECT id FROM region WHERE name = 'Gasabo' LIMIT 1", Long.class);
            if (gasaboId != null) {
                int reportsUpdated = jdbc.update("UPDATE pollution_cases SET region_id = ?", gasaboId);
                if (reportsUpdated > 0) {
                    log.info("[RegionCleanup] Reassigned {} reports to Gasabo", reportsUpdated);
                }
                
                int regionsDeleted = jdbc.update("DELETE FROM region WHERE id != ?", gasaboId);
                if (regionsDeleted > 0) {
                    log.info("[RegionCleanup] Deleted {} non-Gasabo regions", regionsDeleted);
                }
            }
        } catch (Exception e) {
            log.warn("[RegionCleanup] Could not clean up regions: {}", e.getMessage());
        }
    }
}
