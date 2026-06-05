package com.national.utility.billing.repository;

import com.national.utility.billing.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByUserId(Long userId);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);
}
