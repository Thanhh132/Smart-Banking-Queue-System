package com.sbqs.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationMailService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public AuthenticationMailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Async("notificationExecutor")
    /**
     * Gửi email trên thread pool riêng để đăng ký, quên mật khẩu và xác nhận tài khoản
     * không bị chậm theo thời gian kết nối SMTP.
     */
    public void sendHtml(String from, String to, String subject, String html, String mailType) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP is not configured; {} email was not sent to email={}", mailType, to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("{} email sent to email={}", mailType, to);
        } catch (Exception ex) {
            log.error("{} email could not be sent to email={}", mailType, to, ex);
        }
    }
}
