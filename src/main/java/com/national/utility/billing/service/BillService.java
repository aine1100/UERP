package com.national.utility.billing.service;

import com.national.utility.billing.dto.response.BillResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.*;
import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.model.enums.UserRole;
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
import java.util.List;
import java.util.UUID;

/**
 * Application-layer bill generation. New bills start as {@link BillStatus#PENDING_APPROVAL}
 * until finance approves them; only then is the customer notified.
 */
@Service
@RequiredArgsConstructor
public class BillService {

    private static final List<BillStatus> CUSTOMER_VISIBLE_STATUSES =
            List.of(BillStatus.UNPAID, BillStatus.PARTIAL, BillStatus.PAID);

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final TariffService tariffService;
    private final NotificationService notificationService;

    @Transactional
    public Bill generateBillFromReading(Reading reading) {
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();
        Tariff tariff = tariffService.getActiveTariffForUtility(meter.getMeterType());
        if (tariff.getUtilityType() != meter.getMeterType()) {
            throw new BusinessException(
                    "Active tariff utility type (" + tariff.getUtilityType()
                            + ") does not match meter type (" + meter.getMeterType() + ")");
        }

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
                .status(BillStatus.PENDING_APPROVAL)
                .month(reading.getMonth())
                .year(reading.getYear())
                .reading(reading)
                .customer(customer)
                .build();

        return billRepository.saveAndFlush(bill);
    }

    @Transactional
    public BillResponse approveBill(UUID id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only bills pending finance approval can be approved");
        }

        bill.setStatus(BillStatus.UNPAID);
        bill = billRepository.save(bill);
        notificationService.publishApprovedBill(bill);
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional
    public BillResponse rejectBill(UUID id) {
        Bill bill = findBill(id);
        if (bill.getStatus() != BillStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only bills pending finance approval can be rejected");
        }

        bill.setStatus(BillStatus.REJECTED);
        return EntityMapper.toBillResponse(billRepository.save(bill));
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> getAllBills(Pageable pageable) {
        return billRepository.findAll(pageable).map(EntityMapper::toBillResponse);
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> getPendingApprovalBills(Pageable pageable) {
        return billRepository.findByStatus(BillStatus.PENDING_APPROVAL, pageable)
                .map(EntityMapper::toBillResponse);
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> getBillsForCurrentCustomer(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.ADMIN) {
            return getAllBills(pageable);
        }
        UUID customerId = getCurrentCustomerId();
        return billRepository.findByCustomerIdAndStatusIn(customerId, CUSTOMER_VISIBLE_STATUSES, pageable)
                .map(EntityMapper::toBillResponse);
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(UUID id) {
        Bill bill = findBill(id);
        validateBillAccess(bill);
        return EntityMapper.toBillResponse(bill);
    }

    @Transactional(readOnly = true)
    public Bill findBillEntity(UUID id) {
        return findBill(id);
    }

    private Bill findBill(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    private void validateBillAccess(Bill bill) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.CUSTOMER) {
            if (!CUSTOMER_VISIBLE_STATUSES.contains(bill.getStatus())) {
                throw new ResourceNotFoundException("Bill not found with id: " + bill.getId());
            }
            UUID customerId = getCurrentCustomerId();
            if (!bill.getCustomer().getId().equals(customerId)) {
                throw new BusinessException("You can only view your own bills");
            }
        }
    }

    private UUID getCurrentCustomerId() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return customerRepository.findByUserId(principal.getId())
                .map(Customer::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for current user"));
    }

    private String generateBillReference() {
        return "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
