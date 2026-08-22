package dev.miniExchange.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import dev.miniExchange.auth.dto.AuthResponse;
import dev.miniExchange.auth.dto.LoginRequest;
import dev.miniExchange.user.dto.CreateUserRequest;
import dev.miniExchange.user.entity.User;
import dev.miniExchange.security.jwt.JwtService;
import dev.miniExchange.auth.mapper.AuthMapper;
import dev.miniExchange.user.service.UserService;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public AuthResponse signup(CreateUserRequest request) {
        User user = userService.createUser(request);
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return AuthMapper.toResponse(token, jwtService.extractExpiration(token).getTime());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .findFirst()
                .orElse("USER");

        String token = jwtService.generateToken(authentication.getName(), role);
        return AuthMapper.toResponse(token, jwtService.extractExpiration(token).getTime());
    }
}
