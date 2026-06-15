package com.james.wallet;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accountSecurity.isOwner(#id, authentication)")
    public ResponseEntity<AccountResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountView(id));
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("@accountSecurity.isOwner(#id, authentication)")
    public ResponseEntity<AccountResponse> deposit(@PathVariable Long id,
                                                   @Valid @RequestBody DepositRequest request) {
        Account account = accountService.deposit(id, request.amount());
        return ResponseEntity.ok(AccountResponse.from(account));
    }
}
