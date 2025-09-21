package lk.ijse.edu.golankacourier;

import lk.ijse.edu.golankacourier.config.AppProperties;
import lk.ijse.edu.golankacourier.entity.Role;
import lk.ijse.edu.golankacourier.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.Locale;

/**
 * Application entry point for GoLanka Courier backend.
 *
 * Responsibilities provided here:
 * - Bootstraps Spring Boot application.
 * - Enables scheduling (for cleanup jobs).
 * - Enables binding of AppProperties (app.*).
 * - Enables JPA auditing (created/updated timestamps if you annotate entities).
 * - Seeds default roles (ROLE_CUSTOMER, ROLE_DRIVER, ROLE_STAFF, ROLE_ADMIN) on startup if they do not exist.
 *
 * Adjust or remove role-seeding if you prefer Flyway-managed seeding.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing
public class GoLankaCourierApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoLankaCourierApplication.class, args);
    }

    /**
     * Seeds initial roles into the database on startup.
     * This is safe to keep in development; for production you may prefer to use Flyway SQL migrations.
     */
    @Bean
    public CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            List<String> defaultRoles = List.of("ROLE_CUSTOMER", "ROLE_DRIVER", "ROLE_STAFF", "ROLE_ADMIN");
            for (String r : defaultRoles) {
                String name = r.trim().toUpperCase(Locale.ROOT);
                roleRepository.findByName(name).ifPresentOrElse(
                        existing -> {
                            // role exists — nothing to do
                        },
                        () -> {
                            Role role = Role.builder().name(name).build();
                            roleRepository.save(role);
                            System.out.printf("Seeded role: %s%n", name);
                        }
                );
            }
        };
    }
}
