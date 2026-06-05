package com.national.utility.billing.repository;

import com.national.utility.billing.model.Payment;
import com.national.utility.billing.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByBillCustomerId(UUID customerId, Pageable pageable);

    Page<Payment> findByBillId(UUID billId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.status = com.national.utility.billing.model.enums.PaymentStatus.APPROVED")
    BigDecimal sumApprovedAmountPaid();
}
