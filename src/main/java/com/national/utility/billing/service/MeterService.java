package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.MeterRequest;
import com.national.utility.billing.dto.response.MeterResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.model.Meter;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.MeterRepository;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<MeterResponse> getAllMeters(Pageable pageable) {
        return meterRepository.findAll(pageable).map(EntityMapper::toMeterResponse);
    }

    @Transactional(readOnly = true)
    public Page<MeterResponse> getMetersByCustomer(Long customerId, Pageable pageable) {
        validateCustomerExists(customerId);
        return meterRepository.findByCustomerId(customerId, pageable).map(EntityMapper::toMeterResponse);
    }

    @Transactional(readOnly = true)
    public MeterResponse getMeterById(Long id) {
        return EntityMapper.toMeterResponse(findMeter(id));
    }

    @Transactional
    public MeterResponse createMeter(MeterRequest request) {
        validateInstallationDate(request.getInstallationDate());

        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new BusinessException("Meter number already exists");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus())
                .customer(customer)
                .build();

        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    @Transactional
    public MeterResponse updateMeter(Long id, MeterRequest request) {
        validateInstallationDate(request.getInstallationDate());

        Meter meter = findMeter(id);

        meterRepository.findByMeterNumber(request.getMeterNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Meter number already exists");
            }
        });

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        meter.setMeterNumber(request.getMeterNumber());
        meter.setMeterType(request.getMeterType());
        meter.setInstallationDate(request.getInstallationDate());
        meter.setStatus(request.getStatus());
        meter.setCustomer(customer);

        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    @Transactional
    public void deleteMeter(Long id) {
        meterRepository.delete(findMeter(id));
    }

    private Meter findMeter(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }

    private void validateCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
    }

    private void validateInstallationDate(LocalDate installationDate) {
        if (installationDate != null && installationDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Installation date cannot be in the future");
        }
    }
}
