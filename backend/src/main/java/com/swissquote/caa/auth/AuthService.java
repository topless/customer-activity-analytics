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
import com.swissquote.caa.config.AppProperties;

@Service
public class AuthService {

    private final OperatorRepository operators;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;

    public AuthService(OperatorRepository operators, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, AppProperties properties) {
        this.operators = operators;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public LoginResponse login(String username, String password) {
        Operator operator = operators.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("Unknown username"));
        if (!passwordEncoder.matches(password, operator.getPasswordHash())) {
            throw new BadCredentialsException("Wrong password");
        }

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
