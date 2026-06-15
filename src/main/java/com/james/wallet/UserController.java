package com.james.wallet;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               UriComponentsBuilder uriBuilder) {
        var result = userService.createUser(request.email(), request.name(), request.password(), request.currency());
        URI location = uriBuilder.path("/users/{id}").buildAndExpand(result.user().getId()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(result.user(), result.accounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        var result = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(result.user(), result.accounts()));
    }
}
