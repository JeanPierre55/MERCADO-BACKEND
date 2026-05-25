package com.mercato.pos.service;

import com.mercato.pos.dto.LoginRequest;
import com.mercato.pos.dto.LoginResponse;
import com.mercato.pos.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // In-memory users
    private static class User {
        String id;
        String username;
        String hashedPassword;
        String role;
        String displayName;
        boolean enabled;

        User(String id, String username, String hashedPassword, String role, String displayName, boolean enabled) {
            this.id = id;
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.role = role;
            this.displayName = displayName;
            this.enabled = enabled;
        }
    }

    private final Map<String, User> users;

    public AuthService() {
        this.users = new HashMap<>();
        // Initialize hardcoded users with BCrypt hashed passwords
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        users.put("admin", new User("1", "admin", encoder.encode("admin123"), "ADMIN", "Administrador", true));
        users.put("cajero", new User("2", "cajero", encoder.encode("cajero123"), "CASHIER", "Cajero 1", true));
    }

    /**
     * Authenticates a user and returns a JWT token
     * @param request LoginRequest with username and password
     * @return LoginResponse with token, role and user info
     * @throws ResponseStatusException HTTP 401 for invalid credentials, HTTP 403 for disabled account
     */
    public LoginResponse login(LoginRequest request) {
        User user = users.get(request.username());

        // Check if user exists and password matches
        if (user == null || !passwordEncoder.matches(request.password(), user.hashedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        // Check if user is enabled
        if (!user.enabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }

        // Generate JWT token
        String token = generateToken(user);

        // Return response
        UserDto userDto = new UserDto(user.id, user.username, user.displayName);
        return new LoginResponse(token, user.role, userDto);
    }

    /**
     * Generates a JWT token for the given user
     */
    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(user.id)
                .claim("username", user.username)
                .claim("role", user.role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a JWT token
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the username from a JWT token
     * @param token JWT token
     * @return username claim
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * Extracts the role from a JWT token
     * @param token JWT token
     * @return role claim
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extracts all claims from a JWT token
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
