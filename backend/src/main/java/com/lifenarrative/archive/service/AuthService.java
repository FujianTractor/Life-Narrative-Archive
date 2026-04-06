package com.lifenarrative.archive.service;

import com.lifenarrative.archive.dto.AuthDtos.AuthResponse;
import com.lifenarrative.archive.dto.AuthDtos.LoginRequest;
import com.lifenarrative.archive.dto.AuthDtos.MeResponse;
import com.lifenarrative.archive.dto.AuthDtos.RegisterRequest;
import com.lifenarrative.archive.dto.AuthDtos.UserProfile;
import com.lifenarrative.archive.entity.UserEntity;
import com.lifenarrative.archive.exception.ConflictException;
import com.lifenarrative.archive.exception.UnauthorizedException;
import com.lifenarrative.archive.repository.UserRepository;
import com.lifenarrative.archive.security.ArchivePrincipal;
import com.lifenarrative.archive.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return new AuthResponse(jwtTokenService.createToken(user), toUserProfile(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? request.username()
                : request.displayName().trim();

        UserEntity user = new UserEntity();
        user.setUsername(request.username().trim());
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return new AuthResponse(jwtTokenService.createToken(user), toUserProfile(user));
    }

    public MeResponse currentUser(ArchivePrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized or invalid token");
        }
        return new MeResponse(new UserProfile(principal.userId(), principal.username(), principal.displayName()));
    }

    private UserProfile toUserProfile(UserEntity user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getDisplayName());
    }
}