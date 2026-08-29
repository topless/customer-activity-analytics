package com.swissquote.caa.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.swissquote.caa.config.AppProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "test-signing-secret";

    @Mock
    private OperatorRepository operators;

    private AuthService service() throws Exception {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(SECRET.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
        AppProperties properties = new AppProperties(
            new AppProperties.Jwt(SECRET, Duration.ofHours(8)), null, null, null);
        return new AuthService(operators, new BCryptPasswordEncoder(),
            new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties,
            new LoginAttemptService());
    }

    @Test
    void loginIssuesDecodableTokenWithOperatorClaims() throws Exception {
        UUID id = UUID.randomUUID();
        Operator alice = new Operator(id, "alice", "Alice Meier",
            new BCryptPasswordEncoder().encode("operator123"), "OPERATOR");
        when(operators.findByUsername("alice")).thenReturn(Optional.of(alice));

        AuthService.LoginResponse response = service().login("alice", "operator123");

        assertThat(response.operator().username()).isEqualTo("alice");
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(SECRET.getBytes(StandardCharsets.UTF_8));
        var jwt = NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build()
            .decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo(id.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("OPERATOR");
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        Operator alice = new Operator(UUID.randomUUID(), "alice", "Alice Meier",
            new BCryptPasswordEncoder().encode("operator123"), "OPERATOR");
        when(operators.findByUsername("alice")).thenReturn(Optional.of(alice));

        AuthService service = service();
        assertThatThrownBy(() -> service.login("alice", "nope"))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unknownUsernameIsRejected() throws Exception {
        when(operators.findByUsername("mallory")).thenReturn(Optional.empty());

        AuthService service = service();
        assertThatThrownBy(() -> service.login("mallory", "operator123"))
            .isInstanceOf(BadCredentialsException.class);
    }
}
