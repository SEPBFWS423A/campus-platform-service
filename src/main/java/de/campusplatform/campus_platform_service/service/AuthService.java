package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.Invitation;
import de.campusplatform.campus_platform_service.model.InvitationStatus;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Role;
import de.campusplatform.campus_platform_service.model.VerificationToken;
import de.campusplatform.campus_platform_service.repository.InvitationRepository;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.VerificationTokenRepository;
import de.campusplatform.campus_platform_service.security.CustomUserDetails;
import de.campusplatform.campus_platform_service.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.defaults.theme}")
    private String defaultTheme;

    @Value("${app.defaults.brightness}")
    private String defaultBrightness;

    public AuthService(AppUserRepository userRepository, VerificationTokenRepository tokenRepository, InvitationRepository invitationRepository, PasswordEncoder passwordEncoder, EmailService emailService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public void inviteUser(InvitationRequest request) {
        Invitation invitation = new Invitation(request.getEmail(), request.getRole());
        invitationRepository.save(invitation);
        emailService.sendInvitationEmail(invitation);
    }

    public void completeRegistration(CompleteRegistrationRequest request) {
        Invitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new AppException("error.invitation.invalidToken"));

        if (invitation.getStatus() == InvitationStatus.COMPLETED) {
            throw new AppException("error.invitation.alreadyCompleted");
        }

        AppUser user = new AppUser();
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(invitation.getEmail());
        user.setRole(invitation.getRole());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setTheme(defaultTheme);
        user.setBrightness(defaultBrightness);
        userRepository.save(user);

        invitation.setStatus(InvitationStatus.COMPLETED);
        invitationRepository.save(invitation);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) Objects.requireNonNull(authentication.getPrincipal());
        AppUser user = userDetails.appUser();

        String token = jwtService.generateToken(user);
        return new LoginResponse(token);
    }

    public UserProfileResponse getUserProfile(String username) {
        AppUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException("error.user.notFound"));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFirstname(), user.getLastname(), user.getRole(), user.getTheme(), user.getBrightness());
    }

    public void updatePersonalDetails(String username, PersonalDetailsRequest request) {
        AppUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        if (StringUtils.hasText(request.getFirstname())) {
            user.setFirstname(request.getFirstname());
        }
        if (StringUtils.hasText(request.getLastname())) {
            user.setLastname(request.getLastname());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);
    }

    public void updateUser(Long id, AdminUserUpdateRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        if (StringUtils.hasText(request.firstname())) {
            user.setFirstname(request.firstname());
        }
        if (StringUtils.hasText(request.lastname())) {
            user.setLastname(request.lastname());
        }
        if (StringUtils.hasText(request.email())) {
            user.setEmail(request.email());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }

        userRepository.save(user);
    }

    public void updateUserPreferences(String username, UserPreferencesRequest request) {
        AppUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        if (StringUtils.hasText(request.getTheme())) {
            user.setTheme(request.getTheme());
        }
        if (StringUtils.hasText(request.getBrightness())) {
            user.setBrightness(request.getBrightness());
        }

        userRepository.save(user);
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        AppUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException("error.user.notFound"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException("error.password.invalidOld");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void sendPasswordResetToken(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            createPasswordResetToken(user, token);
            emailService.sendPasswordResetEmail(user, token);
        });
    }

    private void createPasswordResetToken(AppUser user, String token) {
        VerificationToken passwordResetToken = new VerificationToken(token, user);
        tokenRepository.save(passwordResetToken);
    }

    public void resetPassword(String token, String newPassword) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException("error.verification.invalidToken"));
        AppUser user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail(),
                        user.getRole(),
                        user.isEnabled()
                ))
                .collect(Collectors.toList());
    }

    public void deleteUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("error.user.notFound"));
        userRepository.delete(user);
    }

    public UserStatsResponse getUserStats() {
        long total = userRepository.count();
        long staff = userRepository.countByRoleIn(List.of(Role.ADMIN, Role.LECTURER));
        long students = userRepository.countByRoleIn(List.of(Role.STUDENT));
        return new UserStatsResponse(total, staff, students);
    }
}
