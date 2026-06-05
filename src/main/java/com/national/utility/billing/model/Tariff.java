package com.national.utility.billing.model;

import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.model.enums.TariffStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tariffs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tariff extends AuditableEntity {

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

}
