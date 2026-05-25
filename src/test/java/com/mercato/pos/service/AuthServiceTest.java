package com.mercato.pos.service;

import com.mercato.pos.dto.LoginRequest;
import com.mercato.pos.dto.LoginResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=pos-mercato-secret-key-256-bits-minimum",
        "jwt.expiration-ms=86400000"
})
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    private String jwtSecret;

    @BeforeEach
    void setUp() {
        jwtSecret = "pos-mercato-secret-key-256-bits-minimum";
    }

    @Test
    void testLoginSuccessfulWithAdmin() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "admin123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("ADMIN", response.role());
        assertEquals("admin", response.user().username());
        assertEquals("Administrador", response.user().displayName());
        assertEquals("1", response.user().id());
    }

    @Test
    void testLoginSuccessfulWithCajero() {
        // Arrange
        LoginRequest request = new LoginRequest("cajero", "cajero123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("CASHIER", response.role());
        assertEquals("cajero", response.user().username());
        assertEquals("Cajero 1", response.user().displayName());
        assertEquals("2", response.user().id());
    }

    @Test
    void testLoginWithIncorrectCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(401, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Credenciales incorrectas"));
    }

    @Test
    void testLoginWithNonExistentUser() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(401, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Credenciales incorrectas"));
    }

    @Test
    void testJwtContainsRequiredClaims() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "admin123");

        // Act
        LoginResponse response = authService.login(request);
        String token = response.token();

        // Decode JWT and verify claims
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Assert
        assertNotNull(claims.getSubject()); // sub
        assertEquals("admin", claims.get("username")); // username
        assertEquals("ADMIN", claims.get("role")); // role
        assertNotNull(claims.getExpiration()); // exp
        assertTrue(claims.getExpiration().after(new Date())); // exp is in the future
    }

    @Test
    void testValidateTokenSuccessful() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "admin123");
        LoginResponse response = authService.login(request);
        String token = response.token();

        // Act
        boolean isValid = authService.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenInvalid() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = authService.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testExtractUsernameFromToken() {
        // Arrange
        LoginRequest request = new LoginRequest("cajero", "cajero123");
        LoginResponse response = authService.login(request);
        String token = response.token();

        // Act
        String username = authService.extractUsername(token);

        // Assert
        assertEquals("cajero", username);
    }

    @Test
    void testExtractRoleFromToken() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "admin123");
        LoginResponse response = authService.login(request);
        String token = response.token();

        // Act
        String role = authService.extractRole(token);

        // Assert
        assertEquals("ADMIN", role);
    }

    @Test
    void testJwtExpirationTime() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "admin123");

        // Act
        LoginResponse response = authService.login(request);
        String token = response.token();

        // Decode JWT and verify expiration
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date expiration = claims.getExpiration();
        Date now = new Date();

        // Assert - expiration should be approximately 24 hours from now
        long expirationTime = expiration.getTime() - now.getTime();
        assertTrue(expirationTime > 86400000 - 5000); // Allow 5 second margin
        assertTrue(expirationTime <= 86400000);
    }
}
