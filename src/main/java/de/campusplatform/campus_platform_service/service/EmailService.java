package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.model.Invitation;
import de.campusplatform.campus_platform_service.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}", to, e);
            throw new IllegalStateException("Failed to send email");
        }
    }
}
