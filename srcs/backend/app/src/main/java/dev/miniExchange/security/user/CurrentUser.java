package dev.miniExchange.security.user;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@Component
public class CurrentUser {
    public SecurityUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found");
        }
        return (SecurityUser) authentication.getPrincipal();
    }

    public Long getId() {
        SecurityUser user = get();
        return  user.getId();
    }
    public UUID getUuid() {
        SecurityUser user = get();
        return user.getUuid();
    }
    
}
