package dev.miniExchange.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import dev.miniExchange.auth.dto.SignupRequest;
import dev.miniExchange.auth.dto.AuthResponse;
import dev.miniExchange.auth.dto.LoginRequest;

import dev.miniExchange.user.entity.User;
import dev.miniExchange.user.repository.UserRepository;
import dev.miniExchange.security.jwt.JwtService;

import dev.miniExchange.auth.mapper.AuthMapper;


@Service
public class AuthService {
        
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest signupRequest) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(signupRequest.username())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(signupRequest.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        
        // Create a new user entity
        User user = new User(signupRequest.username(), signupRequest.email(), passwordEncoder.encode(signupRequest.password()));
        
        // Save the user to the database
        userRepository.save(user);
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return AuthMapper.toResponse(token, jwtService.extractExpiration(token).getTime());
    }
    
    public AuthResponse login(LoginRequest loginRequest) {
        // Authenticate the user (this is a simplified example, you should use Spring Security for real authentication)
        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return AuthMapper.toResponse(token, jwtService.extractExpiration(token).getTime());
    }
    
}
