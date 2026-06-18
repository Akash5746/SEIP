package com.seip.notification.service;

import com.seip.notification.entity.NotificationLog;
import com.seip.notification.entity.NotificationType;
import com.seip.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationLogRepository notificationLogRepository;

    @Async
    public void sendHtmlEmail(
            String to,
            String toName,
            String subject,
            String templateName,
            Map<String, Object> variables,
            NotificationType type,
            String referenceId) {

        log.info("Sending {} email to {} for reference {}", type, to, referenceId);

        String htmlBody = null;
        String status = "SENT";
        String errorMessage = null;

        try {
            // Build Thymeleaf context
            Context context = new Context();
            context.setVariables(variables);

            // Process template to string
            htmlBody = templateEngine.process("email/" + templateName, context);

            // Build MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (toName != null && !toName.isBlank()) {
                helper.setTo(new jakarta.mail.internet.InternetAddress(to, toName));
            }

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to {} for type {}", to, type);

        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("Failed to send email to {} for type {}: {}", to, type, e.getMessage(), e);
        } finally {
            // Persist notification log regardless of success/failure
            NotificationLog logEntry = NotificationLog.builder()
                    .recipientEmail(to)
                    .recipientName(toName)
                    .subject(subject)
                    .body(htmlBody != null ? truncate(htmlBody, 5000) : "Template render failed")
                    .notificationType(type)
                    .referenceId(referenceId)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            try {
                notificationLogRepository.save(logEntry);
            } catch (Exception dbEx) {
                log.error("Failed to persist notification log: {}", dbEx.getMessage(), dbEx);
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
