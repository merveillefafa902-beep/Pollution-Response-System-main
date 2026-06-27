package org.rrc.pollution_response_system.config;

import org.rrc.pollution_response_system.entity.User;
import org.rrc.pollution_response_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

@Configuration
@EnableMethodSecurity  // ✅ Enables @PreAuthorize and @Secured
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    @Bean
        public UserDetailsService userDetailsService() {
                return username -> {
                        User user = userRepository.findByUsername(username)
                                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

                        String[] roles = user.getRoles().stream().map(Enum::name).toArray(String[]::new);
                        boolean disabled = !"ACTIVE".equalsIgnoreCase(user.getStatus());
                        
                        return org.springframework.security.core.userdetails.User.builder()
                                        .username(user.getUsername())
                                        .password(user.getPassword())
                                        .roles(roles)
                                        .disabled(disabled)
                                        .build();
                };
        }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

        // Role hierarchy: higher roles inherit lower role privileges
        @Bean
        public RoleHierarchy roleHierarchy() {
                RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
                hierarchy.setHierarchy("ROLE_ADMIN > ROLE_ENVIRONMENTAL_AUTHORITY \n ROLE_ENVIRONMENTAL_AUTHORITY > ROLE_CITIZEN");
                return hierarchy;
        }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/videos/**", "/static/**", "/uploads/**", "/ws/**").permitAll()
                        .requestMatchers("/api/reports/export/**").hasAnyRole("ADMIN", "ENVIRONMENTAL_AUTHORITY")

                        .requestMatchers("/api/users/current/**").authenticated()
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "ENVIRONMENTAL_AUTHORITY")
                        // Pollution report endpoints are refined with @PreAuthorize; allow authenticated then method-level rules apply
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/regions/**").hasAnyRole("ADMIN", "ENVIRONMENTAL_AUTHORITY")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}
