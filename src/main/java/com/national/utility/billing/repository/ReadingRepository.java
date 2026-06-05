package com.national.utility.billing.repository;

import com.national.utility.billing.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, UUID> {

    boolean existsByMeterIdAndMonthAndYear(UUID meterId, Integer month, Integer year);

    Optional<Reading> findTopByMeterIdOrderByYearDescMonthDesc(UUID meterId);
}
