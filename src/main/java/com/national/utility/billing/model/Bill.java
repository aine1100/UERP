package com.national.utility.billing.model;

import com.national.utility.billing.model.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bill entity. INSERT operations fire PostgreSQL trigger {@code trg_bill_after_insert}
 * which writes a {@code BILL_GENERATED} notification (see {@code db/routines.sql}).
 */
@Entity
@Table(name = "bills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bill extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String billReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal consumption;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal penalty;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_id", unique = true, nullable = false)
    private Reading reading;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

}
