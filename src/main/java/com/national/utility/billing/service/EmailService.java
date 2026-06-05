package com.national.utility.billing.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendInvitationEmail(String to, String fullNames, String inviteToken, int expirationHours) {
        String subject = "Invitation to National Utility Billing System";
        String body = """
                Dear %s,

                You have been invited to join the National Utility Billing System (WASAC & REG).

                Your invitation token:
                %s

                Activate your account by calling:
                POST /api/auth/setup-password

                Request body:
                {
                  "inviteToken": "%s",
                  "password": "YourNewSecurePassword"
                }

                This token expires in %d hours.

                Regards,
                Utility Billing Team
                """.formatted(fullNames, inviteToken, inviteToken, expirationHours);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String fullNames, String resetToken, int expirationHours) {
        String subject = "Password Reset - National Utility Billing System";
        String body = """
                Dear %s,

                We received a request to reset your password for the National Utility Billing System.

                Your password reset token:
                %s

                Reset your password by calling:
                POST /api/auth/reset-password

                Request body:
                {
                  "resetToken": "%s",
                  "password": "YourNewSecurePassword"
                }

                This token expires in %d hours. If you did not request this, ignore this email.

                Regards,
                Utility Billing Team
                """.formatted(fullNames, resetToken, resetToken, expirationHours);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendBillNotification(String to, String customerName, String billReference, String totalAmount) {
        String subject = "New Utility Bill - " + billReference;
        String body = """
                Dear %s,

                A new utility bill has been generated for your account.

                Bill Reference: %s
                Total Amount: RWF %s

                Please log in to view details and make payment.

                Regards,
                Utility Billing Team
                """.formatted(customerName, billReference, totalAmount);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendPaymentReceipt(String to, String customerName, String billReference,
                                   byte[] pdfBytes, String filename) {
        String subject = "Payment Receipt - " + billReference;
        String body = """
                Dear %s,

                Thank you for your payment on bill %s.

                Please find your payment receipt attached.

                Regards,
                Utility Billing Team
                """.formatted(customerName, billReference);

        sendEmailWithAttachment(to, subject, body, pdfBytes, filename);
    }

    private void sendPlainEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    private void sendEmailWithAttachment(String to, String subject, String body,
                                         byte[] attachment, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(filename, new ByteArrayResource(attachment));
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to send email with attachment to {}: {}", to, ex.getMessage());
        }
    }
}
