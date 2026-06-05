package com.national.utility.billing.service;

import com.national.utility.billing.config.AppProperties;
import com.national.utility.billing.dto.response.NotificationResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.model.Payment;
import com.national.utility.billing.model.NotificationMessage;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.model.enums.NotificationType;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.repository.BillRepository;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.NotificationMessageRepository;
import com.national.utility.billing.security.SecurityUtils;
import com.national.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reads notification rows written by PostgreSQL triggers/procedures and delivers them by email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMessageRepository notificationRepository;
    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();

        if (principal.getRole() == UserRole.CUSTOMER) {
            UUID customerId = resolveCurrentCustomerId();
            return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                    .map(this::toResponse);
        }

        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        NotificationMessage notification = findAccessibleNotification(id);
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public void emailNotificationToCustomer(UUID notificationId) {
        NotificationMessage notification = findAccessibleNotification(notificationId);
        deliverByEmail(notification);
    }

    @Transactional(readOnly = true)
    public void emailNotificationsForBill(UUID billId, NotificationType... types) {
        List<NotificationMessage> notifications = notificationRepository
                .findByBillIdAndNotificationTypeIn(billId, List.of(types));

        for (NotificationMessage notification : notifications) {
            deliverByEmail(notification);
        }
    }

    /**
     * Called when finance approves a bill — creates the in-app notification and emails the customer.
     */
    @Transactional
    public void publishApprovedBill(Bill bill) {
        NotificationMessage notification = NotificationMessage.builder()
                .customer(bill.getCustomer())
                .bill(bill)
                .notificationType(NotificationType.BILL_GENERATED)
                .message(String.format(
                        "A new utility bill %s has been generated for %s/%s. Total: RWF %s. Outstanding: RWF %s.",
                        bill.getBillReference(),
                        bill.getMonth(),
                        bill.getYear(),
                        bill.getTotalAmount(),
                        bill.getOutstandingBalance()))
                .build();
        notificationRepository.save(notification);
        emailNewBillNotification(bill);
    }

    /**
     * Called when finance approves a customer payment — creates notifications and emails the customer.
     */
    @Transactional
    public void publishApprovedPayment(Payment payment, Bill bill) {
        NotificationMessage paymentReceived = NotificationMessage.builder()
                .customer(bill.getCustomer())
                .bill(bill)
                .notificationType(NotificationType.PAYMENT_RECEIVED)
                .message(String.format(
                        "Payment of RWF %s received for bill %s (%s/%s) via %s. Remaining balance: RWF %s.",
                        payment.getAmountPaid(),
                        bill.getBillReference(),
                        bill.getMonth(),
                        bill.getYear(),
                        payment.getPaymentMethod(),
                        bill.getOutstandingBalance()))
                .build();
        notificationRepository.save(paymentReceived);

        if (bill.getStatus() == BillStatus.PAID) {
            NotificationMessage billPaid = NotificationMessage.builder()
                    .customer(bill.getCustomer())
                    .bill(bill)
                    .notificationType(NotificationType.BILL_PAID)
                    .message(String.format(
                            "Bill %s (%s/%s) is fully paid. Amount paid: RWF %s. Thank you for your payment!",
                            bill.getBillReference(),
                            bill.getMonth(),
                            bill.getYear(),
                            payment.getAmountPaid()))
                    .build();
            notificationRepository.save(billPaid);
        }
    }

    /**
     * Emails the customer when a new bill is generated. If they already have other unpaid bills,
     * the email includes the full outstanding summary so they are not left unaware after a prior
     * overdue reminder was already sent.
     */
    public void emailNewBillNotification(Bill bill) {
        Customer customer = bill.getCustomer();
        List<Bill> unpaidBills = billRepository
                .findByCustomerIdAndStatusInAndOutstandingBalanceGreaterThanOrderByYearAscMonthAsc(
                        customer.getId(),
                        List.of(BillStatus.UNPAID, BillStatus.PARTIAL),
                        BigDecimal.ZERO);

        if (unpaidBills.size() <= 1) {
            emailNotificationsForBill(bill.getId(), NotificationType.BILL_GENERATED);
            return;
        }

        String newBillLine = formatBillLine(bill, true);
        emailService.sendNewBillWithOutstandingSummaryEmail(
                customer.getEmail(),
                customer.getFullNames(),
                newBillLine,
                formatOutstandingBillSummary(unpaidBills));
    }

    /**
     * Runs {@code sp_send_overdue_reminders(overdueDays)}, then emails each affected customer once
     * with a summary of all their overdue months (not one email per bill).
     */
    @Transactional
    public int sendOverdueReminders() {
        int overdueDays = appProperties.getNotifications().getOverdueDays();
        LocalDateTime marker = LocalDateTime.now();
        LocalDateTime overdueCutoff = marker.minusDays(overdueDays);

        // PostgreSQL uses native CALL syntax (not JDBC {call ...} escape)
        jdbcTemplate.execute("CALL sp_send_overdue_reminders(" + overdueDays + ")");

        List<NotificationMessage> reminders = notificationRepository
                .findByNotificationTypeAndCreatedAtAfter(NotificationType.OVERDUE_REMINDER, marker);

        Set<UUID> customerIds = new LinkedHashSet<>();
        for (NotificationMessage notification : reminders) {
            customerIds.add(notification.getCustomer().getId());
        }

        int emailsSent = 0;
        for (UUID customerId : customerIds) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
            List<Bill> overdueBills = billRepository
                    .findByCustomerIdAndStatusInAndOutstandingBalanceGreaterThanAndCreatedAtBeforeOrderByYearAscMonthAsc(
                            customerId,
                            List.of(BillStatus.UNPAID, BillStatus.PARTIAL),
                            BigDecimal.ZERO,
                            overdueCutoff);

            if (!overdueBills.isEmpty()) {
                deliverConsolidatedOverdueEmail(customer, overdueBills);
                emailsSent++;
            }
        }

        log.info(
                "Overdue reminders: {} in-app notification(s), {} consolidated email(s) (bills older than {} days)",
                reminders.size(), emailsSent, overdueDays);
        return reminders.size();
    }

    private void deliverByEmail(NotificationMessage notification) {
        Customer customer = notification.getCustomer();
        emailService.sendCustomerNotificationEmail(
                customer.getEmail(),
                customer.getFullNames(),
                notification.getNotificationType().name(),
                notification.getMessage());
    }

    private void deliverConsolidatedOverdueEmail(Customer customer, List<Bill> overdueBills) {
        emailService.sendConsolidatedOverdueReminderEmail(
                customer.getEmail(),
                customer.getFullNames(),
                formatOutstandingBillSummary(overdueBills));
    }

    private String formatOutstandingBillSummary(List<Bill> bills) {
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        StringBuilder lines = new StringBuilder();

        for (Bill bill : bills) {
            lines.append(formatBillLine(bill, false))
                    .append(System.lineSeparator());
            totalOutstanding = totalOutstanding.add(bill.getOutstandingBalance());
        }

        lines.append(System.lineSeparator())
                .append("Total outstanding: RWF ")
                .append(totalOutstanding);

        return lines.toString();
    }

    private String formatBillLine(Bill bill, boolean includeTotal) {
        String line = "- Bill "
                + bill.getBillReference()
                + " ("
                + bill.getMonth()
                + "/"
                + bill.getYear()
                + "): Outstanding RWF "
                + bill.getOutstandingBalance();

        if (includeTotal) {
            line += ", Total RWF " + bill.getTotalAmount();
        }

        return line;
    }

    private NotificationMessage findAccessibleNotification(UUID id) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();

        if (principal.getRole() == UserRole.CUSTOMER) {
            UUID customerId = resolveCurrentCustomerId();
            return notificationRepository.findByIdAndCustomerId(id, customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        }

        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    private UUID resolveCurrentCustomerId() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() != UserRole.CUSTOMER) {
            throw new BusinessException("Only customers have a linked customer profile");
        }
        return customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"))
                .getId();
    }

    private NotificationResponse toResponse(NotificationMessage notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomer().getId())
                .customerName(notification.getCustomer().getFullNames())
                .billId(notification.getBill() != null ? notification.getBill().getId() : null)
                .billReference(notification.getBill() != null ? notification.getBill().getBillReference() : null)
                .notificationType(notification.getNotificationType())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
