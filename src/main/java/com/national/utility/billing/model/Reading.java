package com.national.utility.billing.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "readings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"meter_id", "month", "year"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reading extends AuditableEntity {

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal previousReading;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentReading;

    @Column(nullable = false)
    private LocalDate readingDate;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @OneToOne(mappedBy = "reading", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Bill bill;

}
