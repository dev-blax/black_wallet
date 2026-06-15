package com.james.wallet;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserWithAccounts createUser(String email, String name, String password, String currency) {
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }
        String passwordHash = passwordEncoder.encode(password);
        User user = users.save(new User(email, name, passwordHash));
        Account account = accounts.save(new Account(user, currency));
        return new UserWithAccounts(user, List.of(account));
    }

    @Transactional(readOnly = true)
    public UserWithAccounts findById(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        List<Account> userAccounts = accounts.findByUserId(id);
        return new UserWithAccounts(user, userAccounts);
    }

    public record UserWithAccounts(User user, List<Account> accounts) { }
}
