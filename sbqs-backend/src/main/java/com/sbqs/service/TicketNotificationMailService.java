package com.sbqs.service;

import com.sbqs.config.PasswordResetProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class TicketNotificationMailService {
    private static final Logger log = LoggerFactory.getLogger(TicketNotificationMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties mailProperties;

    public TicketNotificationMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordResetProperties mailProperties) {

        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    public boolean sendTicketCalled(
            String customerEmail,
            String ticketNumber,
            String branchName,
            String serviceName,
            String queueMachineLocationNote,
            String counterName,
            String staffName) {

        if (customerEmail == null || customerEmail.isBlank()) {
            log.info("Skip ticket notification because customer email is empty");
            return false;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Skip ticket notification because SMTP is not configured");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailProperties.getFromEmail());
            helper.setTo(customerEmail);
            helper.setSubject("SBQS - Phiếu " + value(ticketNumber) + " đang được gọi");
            helper.setText(buildCalledEmail(
                    ticketNumber,
                    branchName,
                    serviceName,
                    queueMachineLocationNote,
                    counterName,
                    staffName), true);
            mailSender.send(message);
            log.info("Ticket called notification sent ticketNumber={}", ticketNumber);
            return true;
        } catch (MessagingException | RuntimeException ex) {
            log.error("Ticket called notification could not be sent ticketNumber={}", ticketNumber, ex);
            return false;
        }
    }

    private String buildCalledEmail(
            String ticketNumber,
            String branchName,
            String serviceName,
            String queueMachineLocationNote,
            String counterName,
            String staffName) {

        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:28px 0;background:#f4f7fb;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #d8e2ef;border-radius:8px;overflow:hidden;">
                        <tr><td style="padding:22px 28px;background:#005baa;color:#ffffff;">
                          <strong style="font-size:22px;">SBQS</strong>
                          <div style="margin-top:4px;font-size:13px;opacity:.9;">Smart Banking Queue System</div>
                        </td></tr>
                        <tr><td style="padding:28px;">
                          <p style="margin:0 0 8px;color:#64748b;font-size:14px;">Số thứ tự của bạn</p>
                          <div style="margin:0 0 22px;color:#005baa;font-size:42px;font-weight:800;line-height:1;">%s</div>
                          <p style="margin:0 0 18px;font-size:16px;line-height:1.6;">Phiếu của bạn đã được gọi. Vui lòng đến đúng quầy để được phục vụ.</p>
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="8" style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:6px;font-size:14px;">
                            <tr><td style="color:#64748b;">Chi nhánh</td><td style="font-weight:700;">%s</td></tr>
                            <tr><td style="color:#64748b;">Dịch vụ</td><td style="font-weight:700;">%s</td></tr>
                            <tr><td style="color:#64748b;">Ghi chú vị trí</td><td style="font-weight:700;">%s</td></tr>
                            <tr><td style="color:#64748b;">Quầy phục vụ</td><td style="font-weight:700;">%s</td></tr>
                            <tr><td style="color:#64748b;">Nhân viên</td><td style="font-weight:700;">%s</td></tr>
                          </table>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(value(ticketNumber)),
                escapeHtml(value(branchName)),
                escapeHtml(value(serviceName)),
                escapeHtml(value(queueMachineLocationNote)),
                escapeHtml(value(counterName)),
                escapeHtml(value(staffName)));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
