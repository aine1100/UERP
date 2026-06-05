package com.national.utility.billing.repository;

import com.national.utility.billing.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {

    boolean existsByMeterIdAndMonthAndYear(Long meterId, Integer month, Integer year);

    Optional<Reading> findTopByMeterIdOrderByReadingDateDesc(Long meterId);
}
