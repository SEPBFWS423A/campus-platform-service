package de.campusplatform.campus_platform_service.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import de.campusplatform.campus_platform_service.model.Invitation;
import de.campusplatform.campus_platform_service.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.resend.from}")
    private String fromEmail;

    @Value("${app.resend.api.key}")
    private String resendApiKey;

    public void sendPasswordResetEmail(User user, String token) {
        String url = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your Campus Platform Password";
        String body = "<h1>Reset your Password</h1><p>Click the link below to reset your password:</p><a href=\"" + url + "\">Reset Password</a>";
        sendEmail(user.getEmail(), subject, body);
    }

    public void sendInvitationEmail(Invitation invitation) {
        String url = frontendUrl + "/complete-registration?token=" + invitation.getToken();
        String subject = "You are invited to join the Campus Platform";
        String body = "<h1>Invitation to Campus Platform</h1><p>Click the link below to complete your registration:</p><a href=\"" + url + "\">Complete Registration</a>";
        sendEmail(invitation.getEmail(), subject, body);
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(body)
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("E-Mail erfolgreich versendet! ID: " + data.getId());
        } catch (ResendException e) {
            logger.error("Failed to send email to {}", to, e);
            throw new IllegalStateException("Failed to send email");
        }
    }
}
