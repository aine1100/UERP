package com.national.utility.billing.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
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

    /**
     * Sends the same text stored in {@code notification_messages} (from DB triggers) to the customer inbox.
     */
    @Async
    public void sendNewBillWithOutstandingSummaryEmail(String to, String customerName, String newBillLine,
                                                       String outstandingSummary) {
        String subject = "New Utility Bill - Outstanding Balance Update";
        String body = """
                Dear %s,

                A new utility bill has been generated:

                %s

                Your current outstanding bills are:

                %s

                Please sign in to view details and make payment.

                Regards,
                Utility Billing Team
                """.formatted(customerName, newBillLine, outstandingSummary);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendConsolidatedOverdueReminderEmail(String to, String customerName, String overdueSummary) {
        String subject = "Overdue Utility Bills Reminder";
        String body = """
                Dear %s,

                You have the following overdue utility bills:

                %s

                Please sign in to view details and make payment.

                Regards,
                Utility Billing Team
                """.formatted(customerName, overdueSummary);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendCustomerNotificationEmail(String to, String customerName,
                                              String notificationType, String message) {
        String subject = "Utility Billing Notification - " + notificationType.replace('_', ' ');
        String body = """
                Dear %s,

                %s

                Sign in to view all your notifications.

                Regards,
                Utility Billing Team
                """.formatted(customerName, message);

        sendPlainEmail(to, subject, body);
    }

    @Async
    public void sendPaymentReceipt(String to, String customerName, String billReference,
                                   int month, int year, BigDecimal amountPaid, boolean fullyPaid,
                                   byte[] pdfBytes, String filename) {
        String subject = "Payment Receipt - " + billReference + " (" + month + "/" + year + ")";
        String paymentStatus = fullyPaid
                ? "Your bill for " + month + "/" + year + " is now fully paid."
                : "This is a partial payment towards your " + month + "/" + year + " bill.";
        String body = """
                Dear %s,

                Thank you for your payment.

                Bill Reference: %s
                Billing Period: %s/%s
                Amount Paid: RWF %s

                %s

                Your receipt is attached.

                Regards,
                Utility Billing Team
                """.formatted(customerName, billReference, month, year, amountPaid, paymentStatus);

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
