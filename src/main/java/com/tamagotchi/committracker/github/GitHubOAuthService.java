package com.tamagotchi.committracker.github;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for GitHub OAuth 2.0 Device Flow authentication.
 * Handles the complete OAuth lifecycle including token acquisition,
 * refresh, and validation.
 * 
 * Requirements: 1.2, 1.3, 1.4
 */
public interface GitHubOAuthService {
    
    /**
     * Initiates the OAuth Device Flow authentication.
     * Returns a DeviceCodeResponse containing user_code and verification_uri.
     * The user must visit the verification_uri and enter the user_code.
     * 
     * @return CompletableFuture containing the device code response
     */
    CompletableFuture<DeviceCodeResponse> initiateDeviceFlow();
    
    /**
     * Polls for access token after user completes authorization.
     * Implements polling with the interval specified in DeviceCodeResponse.
     * 
     * @param deviceCode the device code from initiateDeviceFlow()
     * @return CompletableFuture containing the access token response
     */
    CompletableFuture<AccessTokenResponse> pollForAccessToken(String deviceCode);
    
    /**
     * Refreshes an expired access token using the refresh token.
     * 
     * @param refreshToken the refresh token from previous authentication
     * @return CompletableFuture containing the new access token response
     */
    CompletableFuture<AccessTokenResponse> refreshAccessToken(String refreshToken);
    
    /**
     * Revokes the current access token.
     * 
     * @param accessToken the token to revoke
     * @return CompletableFuture that completes when revocation is done
     */
    CompletableFuture<Void> revokeToken(String accessToken);
    
    /**
     * Checks if the current token is valid by making a test API call.
     * 
     * @param accessToken the token to validate
     * @return CompletableFuture containing true if valid, false otherwise
     */
    CompletableFuture<Boolean> validateToken(String accessToken);
}
