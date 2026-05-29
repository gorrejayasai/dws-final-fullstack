package com.wallet.notification.sender;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.notification.entity.Notification;
import com.wallet.notification.enums.NotificationChannel;
import com.wallet.notification.template.TemplateRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final TemplateRegistry templateRegistry;
    private final ObjectMapper objectMapper;

    @Value("${notification.email.from:noreply@digitalwallet.com}")
    private String fromAddress;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) throws Exception {
        if (notification.getRecipient() == null || notification.getRecipient().isBlank()) {
            throw new IllegalArgumentException(
                    "No recipient email for notification " + notification.getId());
        }

        Map<String, Object> vars = Map.of();
        if (notification.getPayload() != null && !notification.getPayload().isBlank()) {
            vars = objectMapper.readValue(
                    notification.getPayload(),
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        Context ctx = new Context();
        ctx.setVariables(vars);
        ctx.setVariable("userId", notification.getUserId());

        String tplName = "email/" +
                templateRegistry.getTemplateName(notification.getTemplateCode());

        String htmlBody;
        try {
            htmlBody = templateEngine.process(tplName, ctx);
        } catch (Exception e) {
            log.warn("Template '{}' not found, falling back to generic", tplName);
            ctx.setVariable("message",
                    "Notification: " + notification.getTemplateCode());
            htmlBody = templateEngine.process("email/generic", ctx);
        }

        var msg = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(notification.getRecipient());
        helper.setSubject(templateRegistry.getSubject(notification.getTemplateCode()));
        helper.setText(htmlBody, true);

        mailSender.send(msg);
        log.info("Email sent to {} [{}]",
                notification.getRecipient(), notification.getTemplateCode());
    }
}