package de.campusplatform.campus_platform_service.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;

@Component
@Order(1)
public class UserInitializer implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial.admin.email:}")
    private String initialAdminEmail;

    @Value("${app.initial.admin.password:}")
    private String initialAdminPassword;

    @Value("${app.initial.lecturer.email:}")
    private String initialLecturerEmail;

    @Value("${app.initial.lecturer.password:}")
    private String initialLecturerPassword;

    @Value("${app.initial.student.email:}")
    private String initialStudentEmail;

    @Value("${app.initial.student.password:}")
    private String initialStudentPassword;

    @Value("${app.defaults.theme}")
    private String defaultTheme;

    @Value("${app.defaults.brightness}")
    private String defaultBrightness;

    public UserInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {
        boolean created = false;
        if (!initialAdminEmail.isEmpty() && !initialAdminPassword.isEmpty() && 
                userRepository.findByEmail(initialAdminEmail).isEmpty()) {
            createAdminUser();
            created = true;
        }
        if (!initialLecturerEmail.isEmpty() && !initialLecturerPassword.isEmpty() && 
                userRepository.findByEmail(initialLecturerEmail).isEmpty()) {
            createLecturerUser();
            created = true;
        }
        if (!initialStudentEmail.isEmpty() && !initialStudentPassword.isEmpty() && 
                userRepository.findByEmail(initialStudentEmail).isEmpty()) {
            createStudentUser();
            created = true;
        }

        if (created) {
            System.out.println("=================================================================");
            System.out.println("   ✓ CORE SYSTEM USERS INITIALIZED");
            System.out.println("   Admin:    " + initialAdminEmail);
            System.out.println("   Lecturer: " + initialLecturerEmail);
            System.out.println("   Student:  " + initialStudentEmail);
            System.out.println("=================================================================");
        }
    }

    public void createAdminUser() {
        AppUser admin = new AppUser();
        admin.setEmail(initialAdminEmail);
        admin.setPassword(passwordEncoder.encode(initialAdminPassword));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setTheme(defaultTheme);
        admin.setBrightness(defaultBrightness);
        admin.setStartYear(2024);
        admin.setStartQuartal(4);
        userRepository.save(admin);
    }

    public void createLecturerUser() {
        AppUser lecturer = new AppUser();
        lecturer.setEmail(initialLecturerEmail);
        lecturer.setPassword(passwordEncoder.encode(initialLecturerPassword));
        lecturer.setFirstName("Lecturer");
        lecturer.setLastName("User");
        lecturer.setRole(Role.LECTURER);
        lecturer.setEnabled(true);
        lecturer.setTheme(defaultTheme);
        lecturer.setBrightness(defaultBrightness);
        lecturer.setStartYear(2024);
        lecturer.setStartQuartal(4);
        userRepository.save(lecturer);
    }

    public void createStudentUser() {
        AppUser student = new AppUser();
        student.setEmail(initialStudentEmail);
        student.setPassword(passwordEncoder.encode(initialStudentPassword));
        student.setFirstName("Student");
        student.setLastName("User");
        student.setRole(Role.STUDENT);
        student.setEnabled(true);
        student.setTheme(defaultTheme);
        student.setBrightness(defaultBrightness);
        student.setStartYear(2024);
        student.setStartQuartal(4);
        userRepository.save(student);
    }
}
