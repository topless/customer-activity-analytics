package com.swissquote.caa.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import com.swissquote.caa.common.NotFoundException;
import com.swissquote.caa.common.TooManyLoginAttemptsException;
import com.swissquote.caa.config.AppProperties;

@Service
public class AuthService {

    /**
     * Hash of a random throwaway password, matched against when the username is unknown so
     * both failure paths cost one BCrypt comparison (no username-existence timing oracle).
     */
    private static final String DUMMY_HASH =
        "$2a$10$eaCDN5ezMhHPU5Lj/DJ3eeoC/2IQvAnTC9EveuPA2q.A2GDwP/9da";

    private final OperatorRepository operators;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;
    private final LoginAttemptService loginAttempts;

    public AuthService(OperatorRepository operators, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, AppProperties properties,
                       LoginAttemptService loginAttempts) {
        this.operators = operators;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.loginAttempts = loginAttempts;
    }

    public LoginResponse login(String username, String password) {
        if (loginAttempts.isBlocked(username)) {
            throw new TooManyLoginAttemptsException("Login blocked for " + username);
        }
        Operator operator = operators.findByUsername(username).orElse(null);
        String hash = operator == null ? DUMMY_HASH : operator.getPasswordHash();
        boolean matches = passwordEncoder.matches(password, hash);
        if (operator == null || !matches) {
            loginAttempts.recordFailure(username);
            throw new BadCredentialsException("Invalid credentials");
        }
        loginAttempts.recordSuccess(username);

        Instant expiresAt = Instant.now().plus(properties.jwt().ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(operator.getId().toString())
            .issuedAt(Instant.now())
            .expiresAt(expiresAt)
            .claim("username", operator.getUsername())
            .claim("role", operator.getRole())
            .build();
        String token = jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

        return new LoginResponse(token, expiresAt, OperatorDto.of(operator));
    }

    /** Loads the operator behind an authenticated JWT subject. */
    public Operator requireOperator(String subject) {
        return operators.findById(UUID.fromString(subject))
            .orElseThrow(() -> new NotFoundException("Operator " + subject + " not found"));
    }

    public record LoginResponse(String token, Instant expiresAt, OperatorDto operator) {
    }
}
