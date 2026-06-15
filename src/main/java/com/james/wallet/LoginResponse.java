package com.james.wallet;

public record LoginResponse(String tokenType, String accessToken, long expiresIn) {

    public static LoginResponse bearer(String token, long ttlSeconds) {
        return new LoginResponse("Bearer", token, ttlSeconds);
    }
}
