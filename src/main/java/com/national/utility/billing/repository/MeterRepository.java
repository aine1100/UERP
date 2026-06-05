package com.national.utility.billing.repository;

import com.national.utility.billing.model.Meter;
import com.national.utility.billing.model.enums.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {

    Optional<Meter> findByMeterNumber(String meterNumber);

    boolean existsByMeterNumber(String meterNumber);

    Page<Meter> findByCustomerId(Long customerId, Pageable pageable);

    Page<Meter> findByMeterType(MeterType meterType, Pageable pageable);
}
