package com.james.wallet;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class WalletUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public WalletUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email)
                .map(WalletUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
    }
}
