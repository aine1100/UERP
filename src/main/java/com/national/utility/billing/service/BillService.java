package com.national.utility.billing.service;

import com.national.utility.billing.dto.response.BillResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.*;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.repository.BillRepository;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.security.SecurityUtils;
import com.national.utility.billing.security.UserPrincipal;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final TariffService tariffService;
    private final EmailService emailService;

    @Transactional
    public Bill generateBillFromReading(Reading reading) {
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();
        Tariff tariff = tariffService.getActiveTariffForUtility(meter.getMeterType());

        BigDecimal consumption = reading.getCurrentReading()
                .subtract(reading.getPreviousReading())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal amount = consumption.multiply(tariff.getRatePerUnit())
                .add(tariff.getFixedServiceCharge())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal tax = amount.multiply(tariff.getVatPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal penalty = tariff.getLatePenaltyFee();

        BigDecimal totalAmount = amount.add(tax).add(penalty).setScale(2, RoundingMode.HALF_UP);

        Bill bill = Bill.builder()
                .billReference(generateBillReference())
                .consumption(consumption)
                .amount(amount)
                .tax(tax)
                .penalty(penalty)
                .totalAmount(totalAmount)
                .outstandingBalance(totalAmount)
                .status(BillStatus.UNPAID)
                .month(reading.getMonth())
                .year(reading.getYear())
                .reading(reading)
                .customer(customer)
                .build();

        bill = billRepository.save(bill);

        emailService.sendBillNotification(
                customer.getEmail(),
                customer.getFullNames(),
                bill.getBillReference(),
                bill.getTotalAmount().toPlainString());

        return bill;
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> getAllBills(Pageable pageable) {
        return billRepository.findAll(pageable).map(EntityMapper::toBillResponse);
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> getBillsForCurrentCustomer(Pageable pageable) {
        Long customerId = getCurrentCustomerId();
        return billRepository.findByCustomerId(customerId, pageable).map(EntityMapper::toBillResponse);
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(Long id) {
        Bill bill = findBill(id);
        validateBillAccess(bill);
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional(readOnly = true)
    public Bill findBillEntity(Long id) {
        return findBill(id);
    }

    @Transactional
    public void applyPayment(Bill bill, BigDecimal amountPaid) {
        if (amountPaid.compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessException("Payment amount exceeds outstanding balance");
        }

        BigDecimal newBalance = bill.getOutstandingBalance().subtract(amountPaid)
                .setScale(2, RoundingMode.HALF_UP);
        bill.setOutstandingBalance(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else {
            bill.setStatus(BillStatus.PARTIAL);
        }

        billRepository.save(bill);
    }

    private Bill findBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    private void validateBillAccess(Bill bill) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == com.national.utility.billing.model.enums.UserRole.CUSTOMER) {
            Long customerId = getCurrentCustomerId();
            if (!bill.getCustomer().getId().equals(customerId)) {
                throw new BusinessException("You can only view your own bills");
            }
        }
    }

    private Long getCurrentCustomerId() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return customerRepository.findByUserId(principal.getId())
                .map(Customer::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for current user"));
    }

    private String generateBillReference() {
        return "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
