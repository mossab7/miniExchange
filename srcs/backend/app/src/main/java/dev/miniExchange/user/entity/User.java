package dev.miniExchange.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @Column(nullable = false, name = "locked")
    private Boolean locked;
    @Column(nullable = false, name = "enabled")
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

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
    @PrePersist
    protected void onPrePersist() {
        this.uuid = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.role = Role.USER; // Default role
        this.locked = false; // Default locked status
        this.enabled = true; // Default enabled status
    }
}
