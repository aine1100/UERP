package com.national.utility.billing.service;

import com.national.utility.billing.dto.response.UserResponse;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.User;
import com.national.utility.billing.repository.UserRepository;
import com.national.utility.billing.service.mapper.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(EntityMapper::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return EntityMapper.toUserResponse(findUser(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

   private String deleteUser()
}
