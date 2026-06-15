package com.james.wallet;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long ttlSeconds;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          @Value("${wallet.jwt.ttl-seconds}") long ttlSeconds) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.ttlSeconds = ttlSeconds;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            var principal = (WalletUserDetails) auth.getPrincipal();
            String token = jwtService.issue(principal.userId(), principal.email());
            return ResponseEntity.ok(LoginResponse.bearer(token, ttlSeconds));
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            // IMPORTANT: same error for "no such email" and "wrong password" — prevents user enumeration.
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
