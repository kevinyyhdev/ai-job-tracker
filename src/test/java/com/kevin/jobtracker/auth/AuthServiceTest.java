package com.kevin.jobtracker.auth;

import com.kevin.jobtracker.auth.dto.RegisterRequest;
import com.kevin.jobtracker.user.User;
import com.kevin.jobtracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthService authService = new AuthService(userRepository, passwordEncoder);

    @Test
    void passwordIsHashedNotPlaintext() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("test@example.com", "password123", "Test"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
    }
}
