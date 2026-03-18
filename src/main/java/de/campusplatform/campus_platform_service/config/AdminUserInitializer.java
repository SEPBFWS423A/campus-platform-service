package de.campusplatform.campus_platform_service.config;

import de.campusplatform.campus_platform_service.model.Role;
import de.campusplatform.campus_platform_service.model.User;
import de.campusplatform.campus_platform_service.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial.admin.email:}")
    private String initialAdminEmail;

    @Value("${app.initial.admin.password:}")
    private String initialAdminPassword;

    @Value("${app.defaults.theme}")
    private String defaultTheme;

    @Value("${app.defaults.brightness}")
    private String defaultBrightness;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (userRepository.count() == 0 && !initialAdminEmail.isEmpty() && !initialAdminPassword.isEmpty()) {
            User admin = new User();
            admin.setEmail(initialAdminEmail);
            admin.setPassword(passwordEncoder.encode(initialAdminPassword));
            admin.setFirstname("Admin");
            admin.setLastname("User");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setTheme(defaultTheme);
            admin.setBrightness(defaultBrightness);
            userRepository.save(admin);
            System.out.println("=================================================================");
            System.out.println("INITIAL ADMIN USER CREATED");
            System.out.println("Email: " + initialAdminEmail);
            System.out.println("Password: [PROVIDED EXTERNALLY]");
            System.out.println("=================================================================");
        }
    }
}
