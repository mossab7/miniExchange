package dev.miniExchange.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import dev.miniExchange.user.Role;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(
        name = "user_seq",
        sequenceName = "user_sequence",
        allocationSize = 50
    )
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt = Instant.now();
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt = Instant.now();

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
    public boolean isEnabled() {
        return enabled;
    }
    public boolean isLocked() {
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
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    public UUID getUuid() {
        return uuid;
    }
    public Long getId() {
        return id;
    }
}
