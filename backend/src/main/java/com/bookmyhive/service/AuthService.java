package com.bookmyhive.service;

import com.bookmyhive.dto.AuthDto;
import com.bookmyhive.entity.Role;
import com.bookmyhive.entity.User;
import com.bookmyhive.exception.ConflictException;
import com.bookmyhive.exception.ResourceNotFoundException;
import com.bookmyhive.repository.UserRepository;
import com.bookmyhive.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** Public sign-up always creates a normal USER; admins are created by seeding or directly in the DB. */
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        return toResponse(userRepository.save(user));
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    private AuthDto.AuthResponse toResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthDto.AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
