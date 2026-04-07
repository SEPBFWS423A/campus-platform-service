package de.campusplatform.campus_platform_service.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import de.campusplatform.campus_platform_service.model.Invitation;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.resend.from}")
    private String fromEmail;

    @Value("${app.resend.api.key}")
    private String resendApiKey;

    private final InstitutionRepository institutionRepository;

    public EmailService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    public void sendPasswordResetEmail(AppUser user, String token) {
        
        InstitutionInfo info = institutionRepository.getFirst().orElseThrow(() -> new IllegalStateException("Institution Info not initialized"));
        String lang = (user.getLanguage() != null ? user.getLanguage() : "de").toLowerCase();
        
        String subject;
        String bodyTemplate;

        if ("en".equals(lang)) {
            subject = info.getPasswordResetEmailSubjectEn();
            bodyTemplate = info.getPasswordResetEmailBodyEn();
        } else {
            subject = info.getPasswordResetEmailSubjectDe();
            bodyTemplate = info.getPasswordResetEmailBodyDe();
        }
        
        String url = normalizeUrl(frontendUrl) + "/reset-password?token=" + token;
        
        String body = bodyTemplate.replace("{url}", url);
        sendEmail(user.getEmail(), subject, body);
    }

    public void sendInvitationEmail(Invitation invitation, String language) {
        
        InstitutionInfo info = institutionRepository.getFirst().orElseThrow(() -> new IllegalStateException("Institution Info not initialized"));
        String lang = (language != null ? language : "de").toLowerCase();
        
        String subject;
        String bodyTemplate;

        if ("en".equals(lang)) {
            subject = info.getInvitationEmailSubjectEn();
            bodyTemplate = info.getInvitationEmailBodyEn();
        } else {
            subject = info.getInvitationEmailSubjectDe();
            bodyTemplate = info.getInvitationEmailBodyDe();
        }
        
        String url = normalizeUrl(frontendUrl) + "/complete-registration?token=" + invitation.getToken() + "&email=" + invitation.getEmail();
        
        String body = (bodyTemplate != null) ? bodyTemplate.replace("{url}", url) : url;
        sendEmail(invitation.getEmail(), subject, body);
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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
