package com.tamagotchi.committracker.github;

/**
 * Response from GitHub's OAuth token endpoint.
 * Contains the access token and optional refresh token.
 * 
 * Requirements: 1.2, 1.3, 1.4
 */
public record AccessTokenResponse(
    String accessToken,
    String tokenType,
    String scope,
    String refreshToken,
    int expiresIn
) {
    /**
     * Creates an AccessTokenResponse from GitHub API JSON field names.
     * GitHub uses snake_case in responses.
     */
    public static AccessTokenResponse fromGitHubResponse(
            String access_token,
            String token_type,
            String scope,
            String refresh_token,
            int expires_in) {
        return new AccessTokenResponse(access_token, token_type, scope, refresh_token, expires_in);
    }
    
    /**
     * Checks if this response contains a valid access token.
     */
    public boolean isValid() {
        return accessToken != null && !accessToken.isBlank();
    }
    
    /**
     * Checks if this response contains a refresh token.
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }
}
