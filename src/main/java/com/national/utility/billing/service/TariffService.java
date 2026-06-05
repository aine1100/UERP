package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.TariffRequest;
import com.national.utility.billing.dto.response.TariffResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Tariff;
import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.model.enums.TariffStatus;
import com.national.utility.billing.repository.TariffRepository;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;

    @Transactional(readOnly = true)
    public Page<TariffResponse> getAllTariffs(Pageable pageable) {
        return tariffRepository.findAll(pageable).map(EntityMapper::toTariffResponse);
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariffById(UUID id) {
        return EntityMapper.toTariffResponse(findTariff(id));
    }

    @Transactional
    public TariffResponse createTariff(TariffRequest request) {
        if (request.getStatus() == TariffStatus.ACTIVE) {
            deactivateOtherActiveTariffs(request.getUtilityType(), null);
        }

        int nextVersion = tariffRepository.findAll().stream()
                .filter(t -> t.getUtilityType() == request.getUtilityType())
                .mapToInt(Tariff::getVersion)
                .max()
                .orElse(0) + 1;

        Tariff tariff = Tariff.builder()
                .utilityType(request.getUtilityType())
                .ratePerUnit(request.getRatePerUnit())
                .fixedServiceCharge(request.getFixedServiceCharge())
                .vatPercentage(request.getVatPercentage())
                .latePenaltyFee(request.getLatePenaltyFee())
                .version(nextVersion)
                .effectiveFrom(request.getEffectiveFrom())
                .status(request.getStatus())
                .build();

        return EntityMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffResponse updateTariff(UUID id, TariffRequest request) {
        Tariff tariff = findTariff(id);

        if (tariff.getUtilityType() != request.getUtilityType()) {
            throw new BusinessException(
                    "Utility type cannot be changed after tariff creation. Create a new tariff instead.");
        }

        if (request.getStatus() == TariffStatus.ACTIVE) {
            deactivateOtherActiveTariffs(request.getUtilityType(), id);
        }

        tariff.setUtilityType(request.getUtilityType());
        tariff.setRatePerUnit(request.getRatePerUnit());
        tariff.setFixedServiceCharge(request.getFixedServiceCharge());
        tariff.setVatPercentage(request.getVatPercentage());
        tariff.setLatePenaltyFee(request.getLatePenaltyFee());
        tariff.setEffectiveFrom(request.getEffectiveFrom());
        tariff.setStatus(request.getStatus());

        return EntityMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional(readOnly = true)
    public Tariff getActiveTariffForUtility(MeterType utilityType) {
        return tariffRepository.findFirstByUtilityTypeAndStatusOrderByVersionDesc(
                        utilityType, TariffStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "No active tariff found for utility type: " + utilityType));
    }

    private void deactivateOtherActiveTariffs(MeterType utilityType, UUID excludeId) {
        tariffRepository.findAllByUtilityTypeAndStatus(utilityType, TariffStatus.ACTIVE)
                .forEach(existing -> {
                    if (excludeId == null || !existing.getId().equals(excludeId)) {
                        existing.setStatus(TariffStatus.INACTIVE);
                        tariffRepository.save(existing);
                    }
                });
    }

    private Tariff findTariff(UUID id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found with id: " + id));
    }
}
