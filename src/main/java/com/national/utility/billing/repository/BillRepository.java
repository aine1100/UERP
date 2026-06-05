package com.national.utility.billing.repository;

import com.national.utility.billing.model.Bill;
import com.national.utility.billing.model.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    Optional<Bill> findByBillReference(String billReference);

    Page<Bill> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Bill> findByCustomerIdAndStatusIn(UUID customerId, List<BillStatus> statuses, Pageable pageable);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    Page<Bill> findByCustomerIdAndStatus(UUID customerId, BillStatus status, Pageable pageable);

    List<Bill> findByCustomerIdAndStatusInAndOutstandingBalanceGreaterThanAndCreatedAtBeforeOrderByYearAscMonthAsc(
            UUID customerId,
            List<BillStatus> statuses,
            BigDecimal minOutstanding,
            LocalDateTime createdBefore);

    List<Bill> findByCustomerIdAndStatusInAndOutstandingBalanceGreaterThanOrderByYearAscMonthAsc(
            UUID customerId,
            List<BillStatus> statuses,
            BigDecimal minOutstanding);

    long countByStatus(BillStatus status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(b.outstandingBalance), 0) FROM Bill b")
    BigDecimal sumOutstandingBalance();
}
