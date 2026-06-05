package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.PaymentRequest;
import com.national.utility.billing.dto.response.PaymentResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Payment;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.model.enums.NotificationType;
import com.national.utility.billing.model.enums.PaymentStatus;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.repository.BillRepository;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.PaymentRepository;
import com.national.utility.billing.security.SecurityUtils;
import com.national.utility.billing.security.UserPrincipal;
import com.national.utility.billing.service.mapper.EntityMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Customer payments require finance approval before the bill is updated and emails are sent.
 * Finance-recorded payments are approved immediately.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final BillService billService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final EntityManager entityManager;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Bill bill = billService.findBillEntity(request.getBillId());
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        validatePaymentAccess(bill);
        validateBillPayable(bill);

        if (request.getAmountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessException("Payment amount exceeds outstanding balance");
        }

        boolean financeSubmission = principal.getRole() == UserRole.FINANCE
                || principal.getRole() == UserRole.ADMIN;
        PaymentStatus initialStatus = financeSubmission
                ? PaymentStatus.APPROVED
                : PaymentStatus.PENDING_APPROVAL;

        Payment payment = Payment.builder()
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .status(initialStatus)
                .bill(bill)
                .build();

        payment = paymentRepository.save(payment);
        entityManager.flush();
        entityManager.refresh(bill);

        if (initialStatus == PaymentStatus.APPROVED) {
            deliverPaymentEmails(payment, bill);
        }

        return EntityMapper.toPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse approvePayment(UUID id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only payments pending finance approval can be approved");
        }

        Bill bill = payment.getBill();
        validateBillPayable(bill);
        if (payment.getAmountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessException("Payment amount exceeds outstanding balance");
        }

        payment.setStatus(PaymentStatus.APPROVED);
        payment = paymentRepository.save(payment);
        applyPaymentToBill(payment);
        entityManager.refresh(bill);
        deliverPaymentEmails(payment, bill);
        return EntityMapper.toPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse rejectPayment(UUID id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only payments pending finance approval can be rejected");
        }

        payment.setStatus(PaymentStatus.REJECTED);
        return EntityMapper.toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(EntityMapper::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPendingApprovalPayments(Pageable pageable) {
        return paymentRepository.findByStatus(PaymentStatus.PENDING_APPROVAL, pageable)
                .map(EntityMapper::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsForCurrentCustomer(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.ADMIN) {
            return getAllPayments(pageable);
        }
        UUID customerId = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"))
                .getId();
        return paymentRepository.findByBillCustomerId(customerId, pageable)
                .map(EntityMapper::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = findPayment(id);
        validatePaymentAccess(payment.getBill());
        return EntityMapper.toPaymentResponse(payment);
    }

    private void applyPaymentToBill(Payment payment) {
        Bill bill = payment.getBill();
        BigDecimal newOutstanding = bill.getOutstandingBalance().subtract(payment.getAmountPaid());

        bill.setOutstandingBalance(newOutstanding);
        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else if (newOutstanding.compareTo(bill.getTotalAmount()) < 0) {
            bill.setStatus(BillStatus.PARTIAL);
        } else {
            bill.setStatus(BillStatus.UNPAID);
        }

        billRepository.save(bill);
        notificationService.publishApprovedPayment(payment, bill);
    }

    private void deliverPaymentEmails(Payment payment, Bill bill) {
        notificationService.emailNotificationsForBill(
                bill.getId(), NotificationType.PAYMENT_RECEIVED, NotificationType.BILL_PAID);

        byte[] pdfBytes = pdfService.generatePaymentReceipt(bill, payment);
        String filename = "receipt-" + bill.getBillReference() + ".pdf";
        emailService.sendPaymentReceipt(
                bill.getCustomer().getEmail(),
                bill.getCustomer().getFullNames(),
                bill.getBillReference(),
                bill.getMonth(),
                bill.getYear(),
                payment.getAmountPaid(),
                bill.getStatus() == BillStatus.PAID,
                pdfBytes,
                filename);
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private void validateBillPayable(Bill bill) {
        if (bill.getStatus() != BillStatus.UNPAID && bill.getStatus() != BillStatus.PARTIAL) {
            throw new BusinessException("This bill is not open for payment");
        }
    }

    private void validatePaymentAccess(Bill bill) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.CUSTOMER) {
            UUID customerId = customerRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"))
                    .getId();
            if (!bill.getCustomer().getId().equals(customerId)) {
                throw new BusinessException("You can only pay your own bills");
            }
        }
    }
}
