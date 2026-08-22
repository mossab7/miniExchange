package dev.miniExchange.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.user.entity.User;
import dev.miniExchange.user.repository.UserRepository;
import dev.miniExchange.user.dto.CreateUserRequest;
import dev.miniExchange.user.exceptions.UserAlreadyExistException;
import dev.miniExchange.portfolio.service.PortfolioService;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PortfolioService portfolioService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PortfolioService portfolioService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.portfolioService = portfolioService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistException(request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistException(request.email());
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.username(), request.email(), encodedPassword);
        User savedUser = userRepository.save(user);

        portfolioService.createDefaultPortfolio(savedUser);
        return savedUser;
    }
}
