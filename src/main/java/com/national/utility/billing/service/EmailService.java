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
    public void sendVerificationOtpEmail(String to, String fullNames, String otp, int expirationMinutes) {
        String subject = "Verify Your Account - Utility Billing";
        String body = """
                Dear %s,

                Welcome to the National Utility Billing System.

                Your verification code is: %s

                Please verify your account, set your password, then sign in.

                This code expires in %d minutes.
                If you did not receive it, you may request a new code using your email address.

                Regards,
                Utility Billing Team
                """.formatted(fullNames, otp, expirationMinutes);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetOtpEmail(String to, String fullNames, String otp, int expirationMinutes) {
        String subject = "Password Reset Code - Utility Billing";
        String body = """
                Dear %s,

                Your password reset code is: %s

                Use this code to set a new password, then sign in with your new credentials.

                This code expires in %d minutes.
                If you did not request this, you may ignore this email.

                Regards,
                Utility Billing Team
                """.formatted(fullNames, otp, expirationMinutes);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendBillNotification(String to, String customerName, String billReference, String totalAmount) {
        String subject = "New Utility Bill - " + billReference;
        String body = """
                Dear %s,

                A new bill has been generated for your account.

                Bill Reference: %s
                Total Amount: RWF %s

                Please sign in to view details and make payment.

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

                Thank you for your payment on bill %s. Your receipt is attached.

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
