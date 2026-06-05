package com.national.utility.billing.repository;

import com.national.utility.billing.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByBillCustomerId(Long customerId, Pageable pageable);

    Page<Payment> findByBillId(Long billId, Pageable pageable);
}
