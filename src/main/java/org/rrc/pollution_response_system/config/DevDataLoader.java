package org.rrc.pollution_response_system.config;

import org.rrc.pollution_response_system.entity.User;
import java.util.Set;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataLoader {

    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setFullName("Dev Admin");
                admin.setEmail("admin@example.com");
                admin.setPhone("0780000000");
                admin.addRole(User.Role.ADMIN);
                users.save(admin);
            }
            if (users.findByUsername("authority").isEmpty()) {
                User authority = new User();
                authority.setUsername("authority");
                authority.setPassword(encoder.encode("authority123"));
                authority.setFullName("Dev Authority");
                authority.setEmail("authority@example.com");
                authority.setPhone("0781111111");
                authority.addRole(User.Role.ENVIRONMENTAL_AUTHORITY);
                users.save(authority);
            }
            if (users.findByUsername("Authority1").isEmpty()) {
                User authority1 = new User();
                authority1.setUsername("Authority1");
                authority1.setPassword(encoder.encode("Authority123"));
                authority1.setFullName("Environmental Authority");
                authority1.setEmail("authority1@pollution-reporting.io");
                authority1.setPhone("0782222222");
                authority1.addRole(User.Role.ENVIRONMENTAL_AUTHORITY);
                users.save(authority1);
            }

            if (users.findByUsername("citizen").isEmpty()) {
                User citizen = new User();
                citizen.setUsername("citizen");
                citizen.setPassword(encoder.encode("citizen123"));
                citizen.setFullName("Dev Citizen");
                citizen.setEmail("citizen@example.com");
                citizen.setPhone("0783333333");
                citizen.addRole(User.Role.CITIZEN);
                users.save(citizen);
            }
        };
    }
}
