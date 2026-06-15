package com.james.wallet;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accountSecurity")
public class AccountSecurity {

    private final AccountRepository accounts;

    public AccountSecurity(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public boolean isOwner(Long accountId, Authentication authentication) {
        if (accountId == null || authentication == null) return false;
        if (!(authentication.getPrincipal() instanceof WalletUserDetails principal)) return false;
        // Projection query: fetch only the owner id, not the whole Account + lazy User.
        return accounts.findOwnerId(accountId)
                .map(ownerId -> ownerId.equals(principal.userId()))
                .orElse(false);
    }
}
