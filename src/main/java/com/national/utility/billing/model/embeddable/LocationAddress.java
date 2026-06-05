package com.national.utility.billing.model.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationAddress {

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String sector;

    @Column(nullable = false)
    private String cell;

    @Column(nullable = false)
    private String village;

    public String toFormattedAddress() {
        return String.join(", ", province, district, sector, cell, village);
    }
}
