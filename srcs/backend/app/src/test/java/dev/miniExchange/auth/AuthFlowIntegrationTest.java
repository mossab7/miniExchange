package dev.miniExchange.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.auth.dto.AuthResponse;
import dev.miniExchange.auth.dto.LoginRequest;
import dev.miniExchange.auth.service.AuthService;
import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.user.dto.CreateUserRequest;
import dev.miniExchange.user.entity.User;
import dev.miniExchange.user.repository.UserRepository;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testSignupAndLoginFlow() {
        CreateUserRequest signupReq = new CreateUserRequest(
                "trader1",
                "trader1@example.com",
                "securePassword123"
        );

        // 1. Signup
        AuthResponse signupRes = authService.signup(signupReq);
        assertNotNull(signupRes);
        assertNotNull(signupRes.token());
        assertTrue(signupRes.expiresAt() > System.currentTimeMillis());

        // 2. Verify User saved & password hashed
        User user = userRepository.findByUsername("trader1").orElse(null);
        assertNotNull(user);
        assertNotEquals("securePassword123", user.getPassword());
        assertTrue(passwordEncoder.matches("securePassword123", user.getPassword()));

        // 3. Verify Default Portfolio created
        Portfolio portfolio = portfolioRepository.findByUser_IdAndName(user.getId(), "default portfolio").orElse(null);
        assertNotNull(portfolio);
        assertEquals("default portfolio", portfolio.getName());
        assertEquals(BigInteger.ZERO, portfolio.getAvailableBalance());
        assertEquals(BigInteger.ZERO, portfolio.getLockedBalance());

        // 4. Test Login
        LoginRequest loginReq = new LoginRequest("trader1", "securePassword123");
        AuthResponse loginRes = authService.login(loginReq);
        assertNotNull(loginRes);
        assertNotNull(loginRes.token());
    }

    @Test
    void testMultipleUsersCanSignupWithDefaultPortfolioName() {
        CreateUserRequest user1 = new CreateUserRequest("user1", "user1@test.com", "pass12345");
        CreateUserRequest user2 = new CreateUserRequest("user2", "user2@test.com", "pass12345");

        assertDoesNotThrow(() -> authService.signup(user1));
        assertDoesNotThrow(() -> authService.signup(user2));
    }
}
