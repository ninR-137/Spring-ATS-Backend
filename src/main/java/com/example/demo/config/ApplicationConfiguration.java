package com.example.demo.config;


import com.example.demo.model.Role;
import com.example.demo.model.AdminRequestStatus;
import com.example.demo.model.RoleName;
import com.example.demo.repository.AdminRequestRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ApplicationConfiguration {
    private final UserRepository userRepository;
    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    CommandLineRunner seedRolesAndSyncAdminAccess(
            RoleRepository roleRepository,
            UserRepository userRepository,
            AdminRequestRepository adminRequestRepository
    ) {
        return args -> {
            for (RoleName roleName : RoleName.values()) {
                roleRepository.findByName(roleName)
                        .orElseGet(() -> roleRepository.save(new Role(roleName)));
            }

            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

            adminRequestRepository.findAllByStatusOrderByCreatedAtAsc(AdminRequestStatus.APPROVED)
                    .forEach(adminRequest -> userRepository.findById(adminRequest.getUser().getId())
                            .ifPresent(user -> {
                                boolean isAdmin = user.getRoles().stream()
                                        .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
                                if (!isAdmin) {
                                    user.getRoles().add(adminRole);
                                    userRepository.save(user);
                                }
                            }));
        };
    }
}
