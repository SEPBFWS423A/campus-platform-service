package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.Invitation;
import de.campusplatform.campus_platform_service.model.InvitationStatus;
import de.campusplatform.campus_platform_service.model.User;
import de.campusplatform.campus_platform_service.model.VerificationToken;
import de.campusplatform.campus_platform_service.repository.InvitationRepository;
import de.campusplatform.campus_platform_service.repository.UserRepository;
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

import java.util.Objects;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
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

    public AuthService(UserRepository userRepository, VerificationTokenRepository tokenRepository, InvitationRepository invitationRepository, PasswordEncoder passwordEncoder, EmailService emailService, AuthenticationManager authenticationManager, JwtService jwtService) {
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

        User user = new User();
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
        User user = userDetails.user();

        String token = jwtService.generateToken(user);
        return new LoginResponse(token);
    }

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException("error.user.notFound"));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFirstname(), user.getLastname(), user.getRole(), user.getTheme(), user.getBrightness());
    }

    public void updatePersonalDetails(String username, PersonalDetailsRequest request) {
        User user = userRepository.findByEmail(username)
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

    public void updateUserPreferences(String username, UserPreferencesRequest request) {
        User user = userRepository.findByEmail(username)
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
        User user = userRepository.findByEmail(username)
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

    private void createPasswordResetToken(User user, String token) {
        VerificationToken passwordResetToken = new VerificationToken(token, user);
        tokenRepository.save(passwordResetToken);
    }

    public void resetPassword(String token, String newPassword) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException("error.verification.invalidToken"));
        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
    }
}
