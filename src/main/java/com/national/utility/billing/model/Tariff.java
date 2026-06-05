package com.national.utility.billing.model;

import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.model.enums.TariffStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tariffs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType utilityType;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedServiceCharge;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal latePenaltyFee;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
