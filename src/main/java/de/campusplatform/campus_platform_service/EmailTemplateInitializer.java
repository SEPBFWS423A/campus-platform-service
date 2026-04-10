package de.campusplatform.campus_platform_service;

import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class EmailTemplateInitializer implements CommandLineRunner {

    private final InstitutionRepository institutionRepository;

    @Override
    public void run(String... args) {
        InstitutionInfo info = institutionRepository.getFirst().orElseGet(() -> {
            InstitutionInfo newInfo = new InstitutionInfo();
            newInfo.setUniversityName("Campus Platform");
            return newInfo;
        });

        boolean updated = false;

        // Invitation DE
        if (isEmpty(info.getInvitationEmailSubjectDe())) {
            info.setInvitationEmailSubjectDe("Einladung zur Campus Platform");
            updated = true;
        }
        if (isEmpty(info.getInvitationEmailBodyDe())) {
            info.setInvitationEmailBodyDe("<h1>Willkommen!</h1><p>Klicken Sie auf den folgenden Link, um Ihre Registrierung abzuschließen:</p><a href=\"{url}\">Registrierung abschließen</a><br><br>Oder kopieren Sie diesen Link in Ihren Browser:<br>{url}");
            updated = true;
        }

        // Invitation EN
        if (isEmpty(info.getInvitationEmailSubjectEn())) {
            info.setInvitationEmailSubjectEn("Invitation to Campus Platform");
            updated = true;
        }
        if (isEmpty(info.getInvitationEmailBodyEn())) {
            info.setInvitationEmailBodyEn("<h1>Welcome!</h1><p>Click the link below to complete your registration:</p><a href=\"{url}\">Complete Registration</a><br><br>Or copy this link into your browser:<br>{url}");
            updated = true;
        }

        // Password Reset DE
        if (isEmpty(info.getPasswordResetEmailSubjectDe())) {
            info.setPasswordResetEmailSubjectDe("Passwort zurücksetzen - Campus Platform");
            updated = true;
        }
        if (isEmpty(info.getPasswordResetEmailBodyDe())) {
            info.setPasswordResetEmailBodyDe("<h1>Passwort vergessen?</h1><p>Klicken Sie auf den folgenden Link, um Ihr Passwort zurückzusetzen:</p><a href=\"{url}\">Passwort zurücksetzen</a><br><br>Oder kopieren Sie diesen Link in Ihren Browser:<br>{url}");
            updated = true;
        }

        // Password Reset EN
        if (isEmpty(info.getPasswordResetEmailSubjectEn())) {
            info.setPasswordResetEmailSubjectEn("Reset your Campus Platform Password");
            updated = true;
        }
        if (isEmpty(info.getPasswordResetEmailBodyEn())) {
            info.setPasswordResetEmailBodyEn("<h1>Forgot your password?</h1><p>Click the link below to reset your password:</p><a href=\"{url}\">Reset Password</a><br><br>Or copy this link into your browser:<br>{url}");
            updated = true;
        }

        if (updated) {
            institutionRepository.save(info);
            System.out.println("   ✓ Email templates initialized with default texts");
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
