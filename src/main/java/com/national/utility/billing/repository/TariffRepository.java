package com.national.utility.billing.repository;

import com.national.utility.billing.model.Tariff;
import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.model.enums.TariffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, UUID> {

    Optional<Tariff> findFirstByUtilityTypeAndStatusOrderByVersionDesc(
            MeterType utilityType, TariffStatus status);

    List<Tariff> findAllByUtilityTypeAndStatus(MeterType utilityType, TariffStatus status);

    boolean existsByUtilityTypeAndStatus(MeterType utilityType, TariffStatus status);
}
