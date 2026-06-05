package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.PaymentRequest;
import com.national.utility.billing.dto.response.PaymentResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.Payment;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.PaymentRepository;
import com.national.utility.billing.security.SecurityUtils;
import com.national.utility.billing.security.UserPrincipal;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillService billService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Bill bill = billService.findBillEntity(request.getBillId());
        validatePaymentAccess(bill);

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("Bill is already fully paid");
        }

        Payment payment = Payment.builder()
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .bill(bill)
                .build();

        billService.applyPayment(bill, request.getAmountPaid());
        payment = paymentRepository.save(payment);

        byte[] pdfBytes = pdfService.generatePaymentReceipt(bill, payment);
        String filename = "receipt-" + bill.getBillReference() + ".pdf";

        emailService.sendPaymentReceipt(
                bill.getCustomer().getEmail(),
                bill.getCustomer().getFullNames(),
                bill.getBillReference(),
                pdfBytes,
                filename);

        return EntityMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(EntityMapper::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsForCurrentCustomer(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        Long customerId = customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"))
                .getId();
        return paymentRepository.findByBillCustomerId(customerId, pageable)
                .map(EntityMapper::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        validatePaymentAccess(payment.getBill());
        return EntityMapper.toPaymentResponse(payment);
    }

    private void validatePaymentAccess(Bill bill) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.CUSTOMER) {
            Long customerId = customerRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"))
                    .getId();
            if (!bill.getCustomer().getId().equals(customerId)) {
                throw new BusinessException("You can only pay your own bills");
            }
        }
    }
}
