package com.commerce.api.service;

import com.commerce.api.domain.User;
import com.commerce.api.dto.UserRequest;
import com.commerce.api.exception.BusinessException;
import com.commerce.api.exception.ErrorCode;
import com.commerce.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User create(UserRequest req) {
        return userRepository.save(new User(req.name(), req.email()));
    }

    public User findById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, id));
    }
}
