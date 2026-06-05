package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.TariffRequest;
import com.national.utility.billing.dto.response.TariffResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Tariff;
import com.national.utility.billing.model.enums.TariffStatus;
import com.national.utility.billing.repository.TariffRepository;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;

    @Transactional(readOnly = true)
    public Page<TariffResponse> getAllTariffs(Pageable pageable) {
        return tariffRepository.findAll(pageable).map(EntityMapper::toTariffResponse);
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariffById(Long id) {
        return EntityMapper.toTariffResponse(findTariff(id));
    }

    @Transactional
    public TariffResponse createTariff(TariffRequest request) {
        if (request.getStatus() == TariffStatus.ACTIVE
                && tariffRepository.existsByUtilityTypeAndStatus(request.getUtilityType(), TariffStatus.ACTIVE)) {
            deactivateExistingActiveTariff(request.getUtilityType());
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
    public TariffResponse updateTariff(Long id, TariffRequest request) {
        Tariff tariff = findTariff(id);

        if (request.getStatus() == TariffStatus.ACTIVE
                && tariff.getStatus() != TariffStatus.ACTIVE
                && tariffRepository.existsByUtilityTypeAndStatus(request.getUtilityType(), TariffStatus.ACTIVE)) {
            deactivateExistingActiveTariff(request.getUtilityType());
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
    public Tariff getActiveTariffForUtility(com.national.utility.billing.model.enums.MeterType utilityType) {
        return tariffRepository.findByUtilityTypeAndStatus(utilityType, TariffStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "No active tariff found for utility type: " + utilityType));
    }

    private void deactivateExistingActiveTariff(com.national.utility.billing.model.enums.MeterType utilityType) {
        tariffRepository.findByUtilityTypeAndStatus(utilityType, TariffStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(TariffStatus.INACTIVE);
                    tariffRepository.save(existing);
                });
    }

    private Tariff findTariff(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found with id: " + id));
    }
}
