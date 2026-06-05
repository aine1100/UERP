package com.national.utility.billing.repository;

import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillReference(String billReference);

    Page<Bill> findByCustomerId(Long customerId, Pageable pageable);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    Page<Bill> findByCustomerIdAndStatus(Long customerId, BillStatus status, Pageable pageable);
}
