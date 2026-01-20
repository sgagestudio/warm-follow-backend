package com.sgagestudio.warm_follow_backend.provider;

import com.sgagestudio.warm_follow_backend.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailProvider implements EmailProvider {
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final MailProperties mailProperties;

    public SmtpEmailProvider(JavaMailSender mailSender, EmailProperties emailProperties, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        this.mailProperties = mailProperties;
    }

    @Override
    public String sendEmail(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            applyFrom(helper, request);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(request.body(), true);
            mailSender.send(message);
            String messageId = message.getMessageID();
            return StringUtils.hasText(messageId) ? messageId : UUID.randomUUID().toString();
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("Failed to send email via SMTP: " + ex.getMessage(), ex);
        }
    }

    private void applyFrom(MimeMessageHelper helper, EmailRequest request)
            throws MessagingException, UnsupportedEncodingException {
        String fromAddress = request.fromAddress();
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = emailProperties.getFromAddress();
        }
        if (!StringUtils.hasText(fromAddress)) {
            fromAddress = mailProperties.getUsername();
        }
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("Missing email from address. Set app.email.from-address or spring.mail.username.");
        }
        String fromName = request.fromName();
        if (!StringUtils.hasText(fromName)) {
            fromName = emailProperties.getFromName();
        }
        if (StringUtils.hasText(fromName)) {
            helper.setFrom(fromAddress, fromName);
        } else {
            helper.setFrom(fromAddress);
        }
    }
}
