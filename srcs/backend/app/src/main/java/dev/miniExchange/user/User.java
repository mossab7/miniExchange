package dev.miniExchange.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    private Boolean locked = false;
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    protected User() {
        // Default constructor for JPA
    }
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    public boolean is_enabled() {
        return enabled;
    }
    public boolean is_locked() {
        return locked;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
