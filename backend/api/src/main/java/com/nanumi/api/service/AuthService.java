package com.nanumi.api.service;

import org.springframework.stereotype.Service;

import com.nanumi.api.repository.UserEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserEntityRepository userEntityRepository;

    public void signup(SignupRequest request) {

    }

    public TokenResponse login(LoginRequest request) {
        
    }
}
