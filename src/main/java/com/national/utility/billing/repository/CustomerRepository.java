package com.national.utility.billing.repository;

import com.national.utility.billing.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByUserId(UUID userId);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);
}
