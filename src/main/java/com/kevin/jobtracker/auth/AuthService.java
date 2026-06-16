package com.kevin.jobtracker.auth;

import com.kevin.jobtracker.auth.dto.AuthResponse;
import com.kevin.jobtracker.auth.dto.LoginRequest;
import com.kevin.jobtracker.auth.dto.RegisterRequest;
import com.kevin.jobtracker.common.exception.DuplicateResourceException;
import com.kevin.jobtracker.common.exception.InvalidCredentialsException;
import com.kevin.jobtracker.user.User;
import com.kevin.jobtracker.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );

        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getFullName());
    }
}
