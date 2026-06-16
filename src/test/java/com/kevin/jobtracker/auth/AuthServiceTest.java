package com.kevin.jobtracker.auth;

import com.kevin.jobtracker.auth.dto.AuthResponse;
import com.kevin.jobtracker.auth.dto.RegisterRequest;
import com.kevin.jobtracker.user.User;
import com.kevin.jobtracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = createJwtService();
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

    @Test
    void passwordIsHashedNotPlaintext() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        authService.register(new RegisterRequest("test@example.com", "password123", "Test"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registerReturnsTokenWithCorrectEmail() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        AuthResponse response = authService.register(new RegisterRequest("test@example.com", "password123", "Test"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(jwtService.isTokenValid(response.getToken())).isTrue();
    }

    private JwtService createJwtService() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "test-secret-key-do-not-use-in-production-min-32-chars");
        ReflectionTestUtils.setField(service, "expirationMs", 86400000L);
        return service;
    }
}
