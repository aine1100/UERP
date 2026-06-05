package com.national.utility.billing.repository;

import com.national.utility.billing.model.User;
import com.national.utility.billing.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByInviteToken(String inviteToken);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    long countByRole(UserRole role);
}
