package com.national.utility.billing.model;

import com.national.utility.billing.model.enums.MeterStatus;
import com.national.utility.billing.model.enums.MeterType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meter extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String meterNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterType meterType;

    @Column(nullable = false)
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Reading> readings = new ArrayList<>();

}
