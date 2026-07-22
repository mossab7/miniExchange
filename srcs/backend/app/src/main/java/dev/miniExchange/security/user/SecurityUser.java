package dev.miniExchange.security.user;

import dev.miniExchange.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityUser implements UserDetails {
    private final User user;

    public SecurityUser(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Implement custom logic if needed
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.is_locked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Implement custom logic if needed
    }

    @Override
    public boolean isEnabled() {
        return user.is_enabled(); 
    }
}