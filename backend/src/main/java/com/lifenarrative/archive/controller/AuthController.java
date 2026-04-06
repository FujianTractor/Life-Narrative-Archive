package com.lifenarrative.archive.controller;

import com.lifenarrative.archive.dto.AuthDtos.AuthResponse;
import com.lifenarrative.archive.dto.AuthDtos.LoginRequest;
import com.lifenarrative.archive.dto.AuthDtos.MeResponse;
import com.lifenarrative.archive.dto.AuthDtos.RegisterRequest;
import com.lifenarrative.archive.security.ArchivePrincipal;
import com.lifenarrative.archive.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal ArchivePrincipal principal) {
        return authService.currentUser(principal);
    }
}
