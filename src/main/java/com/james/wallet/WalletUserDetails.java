package com.james.wallet;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record WalletUserDetails(Long userId, String email, String passwordHash) implements UserDetails {

    public static WalletUserDetails from(User user) {
        return new WalletUserDetails(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
}
