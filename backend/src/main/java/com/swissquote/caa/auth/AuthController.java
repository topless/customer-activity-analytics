package com.swissquote.caa.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public AuthService.LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public OperatorDto me(@AuthenticationPrincipal Jwt jwt) {
        return OperatorDto.of(authService.requireOperator(jwt.getSubject()));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
