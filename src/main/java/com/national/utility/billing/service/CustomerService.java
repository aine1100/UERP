package com.national.utility.billing.service;

import com.national.utility.billing.dto.request.CustomerRequest;
import com.national.utility.billing.dto.response.CustomerResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.service.mapper.EntityMapper;
import com.national.utility.billing.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final LocationService locationService;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(EntityMapper::toCustomerResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return EntityMapper.toCustomerResponse(findCustomer(id));
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateUniqueFields(request, null);

        Customer customer = Customer.builder()
                .fullNames(request.getFullNames())
                .nationalId(request.getNationalId())
                .email(request.getEmail())
                .phoneNumber(PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber()))
                .address(locationService.resolveSelection(request.getLocation()))
                .status(request.getStatus())
                .build();

        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        validateUniqueFields(request, id);

        customer.setFullNames(request.getFullNames());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber()));
        customer.setAddress(locationService.resolveSelection(request.getLocation()));
        customer.setStatus(request.getStatus());

        return EntityMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = findCustomer(id);
        customerRepository.delete(customer);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private void validateUniqueFields(CustomerRequest request, Long excludeId) {
        customerRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException("Email is already in use");
            }
        });

        customerRepository.findByNationalId(request.getNationalId()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new BusinessException("National ID is already in use");
            }
        });
    }
}
