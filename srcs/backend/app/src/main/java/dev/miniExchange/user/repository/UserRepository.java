package dev.miniExchange.user.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.user.entity.User;

import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
