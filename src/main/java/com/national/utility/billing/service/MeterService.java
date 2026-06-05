package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.CustomerMeterRequest;
import com.national.utility.billing.dto.request.MeterRequest;
import com.national.utility.billing.dto.response.MeterResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.model.Meter;
import com.national.utility.billing.model.enums.CustomerStatus;
import com.national.utility.billing.model.enums.MeterStatus;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.MeterRepository;
import com.national.utility.billing.security.SecurityUtils;
import com.national.utility.billing.security.UserPrincipal;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    public Page<MeterResponse> getMetersForCurrentCustomer(Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.ADMIN) {
            return getAllMeters(pageable);
        }
        return getMetersByCustomer(resolveCurrentCustomerId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<MeterResponse> getMetersByCustomer(UUID customerId, Pageable pageable) {
        validateCustomerExists(customerId);
        return meterRepository.findByCustomerId(customerId, pageable).map(EntityMapper::toMeterResponse);
    }

    @Transactional(readOnly = true)
    public MeterResponse getMeterById(UUID id) {
        Meter meter = findMeter(id);
        validateMeterAccess(meter);
        return EntityMapper.toMeterResponse(meter);
    }

    @Transactional
    public MeterResponse createMeter(MeterRequest request) {
        return saveMeter(
                request.getMeterNumber(),
                request.getMeterType(),
                request.getInstallationDate(),
                request.getStatus(),
                resolveCustomerForStaff(request.getCustomerId()));
    }

    /** Customer registers a meter on their own account (status defaults to ACTIVE). */
    @Transactional
    public MeterResponse createMeterForCurrentCustomer(CustomerMeterRequest request) {
        return saveMeter(
                request.getMeterNumber(),
                request.getMeterType(),
                request.getInstallationDate(),
                MeterStatus.ACTIVE,
                resolveCurrentCustomer());
    }

    @Transactional
    public MeterResponse updateMeter(UUID id, MeterRequest request) {
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
    public void deleteMeter(UUID id) {
        meterRepository.delete(findMeter(id));
    }

    @Transactional
    public void deactivateAllForCustomer(UUID customerId) {
        setStatusForAllCustomerMeters(customerId, MeterStatus.INACTIVE);
    }

    @Transactional
    public void activateAllForCustomer(UUID customerId) {
        setStatusForAllCustomerMeters(customerId, MeterStatus.ACTIVE);
    }

    private void setStatusForAllCustomerMeters(UUID customerId, MeterStatus status) {
        List<Meter> meters = meterRepository.findByCustomerId(customerId);
        for (Meter meter : meters) {
            meter.setStatus(status);
        }
        meterRepository.saveAll(meters);
    }

    private MeterResponse saveMeter(String meterNumber, com.national.utility.billing.model.enums.MeterType meterType,
                                    LocalDate installationDate, MeterStatus status, Customer customer) {
        validateInstallationDate(installationDate);
        validateCustomerActive(customer);

        if (meterRepository.existsByMeterNumber(meterNumber)) {
            throw new BusinessException("Meter number already exists");
        }

        Meter meter = Meter.builder()
                .meterNumber(meterNumber)
                .meterType(meterType)
                .installationDate(installationDate)
                .status(status)
                .customer(customer)
                .build();

        return EntityMapper.toMeterResponse(meterRepository.save(meter));
    }

    private Customer resolveCustomerForStaff(UUID customerId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.CUSTOMER) {
            Customer own = resolveCurrentCustomer();
            if (!own.getId().equals(customerId)) {
                throw new BusinessException("You can only register meters on your own account");
            }
            return own;
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private Customer resolveCurrentCustomer() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return customerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for current user"));
    }

    private UUID resolveCurrentCustomerId() {
        return resolveCurrentCustomer().getId();
    }

    private void validateMeterAccess(Meter meter) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal.getRole() == UserRole.CUSTOMER) {
            UUID customerId = resolveCurrentCustomerId();
            if (!meter.getCustomer().getId().equals(customerId)) {
                throw new BusinessException("You can only view your own meters");
            }
        }
    }

    private Meter findMeter(UUID id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }

    private void validateCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
    }

    private void validateInstallationDate(LocalDate installationDate) {
        if (installationDate != null && installationDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Installation date cannot be in the future");
        }
    }

    private void validateCustomerActive(Customer customer) {
        if (customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BusinessException("Cannot register meters for an inactive customer");
        }
    }
}
