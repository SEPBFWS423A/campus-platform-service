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
public class UserInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
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

    public UserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String @NonNull ... args) {
        if(userRepository.count() == 0) {
            if (!initialAdminEmail.isEmpty() && !initialAdminPassword.isEmpty()) {
                createAdminUser();
            }
            if (!initialLecturerEmail.isEmpty() && !initialLecturerPassword.isEmpty()) {
                createLecturerUser();
            }
            if (!initialStudentEmail.isEmpty() && !initialStudentPassword.isEmpty()) {
                createStudentUser();
            }
        }

    }

    public void createAdminUser() {
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

    public void createLecturerUser() {
        User lecturer = new User();
        lecturer.setEmail(initialLecturerEmail);
        lecturer.setPassword(passwordEncoder.encode(initialLecturerPassword));
        lecturer.setFirstname("Lecturer");
        lecturer.setLastname("User");
        lecturer.setRole(Role.LECTURER);
        lecturer.setEnabled(true);
        lecturer.setTheme(defaultTheme);
        lecturer.setBrightness(defaultBrightness);
        userRepository.save(lecturer);

        System.out.println("=================================================================");
        System.out.println("INITIAL Lecturer USER CREATED");
        System.out.println("Email: " + initialLecturerEmail);
        System.out.println("Password: [PROVIDED EXTERNALLY]");
        System.out.println("=================================================================");
    }

    public void createStudentUser() {
        User student = new User();
        student.setEmail(initialStudentEmail);
        student.setPassword(passwordEncoder.encode(initialStudentPassword));
        student.setFirstname("Student");
        student.setLastname("User");
        student.setRole(Role.STUDENT);
        student.setEnabled(true);
        student.setTheme(defaultTheme);
        student.setBrightness(defaultBrightness);
        userRepository.save(student);

        System.out.println("=================================================================");
        System.out.println("INITIAL Student USER CREATED");
        System.out.println("Email: " + initialStudentEmail);
        System.out.println("Password: [PROVIDED EXTERNALLY]");
        System.out.println("=================================================================");
    }
}
