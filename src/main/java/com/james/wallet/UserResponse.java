package com.james.wallet;

import java.util.List;

public record UserResponse(Long id, String email, String name, List<AccountResponse> accounts) {

    public static UserResponse from(User user, List<Account> accounts) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                accounts.stream().map(AccountResponse::from).toList()
        );
    }
}
