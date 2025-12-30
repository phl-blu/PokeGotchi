package com.tamagotchi.committracker.github;

/**
 * Response from GitHub's Device Flow initiation endpoint.
 * Contains the device code and user code needed for authentication.
 * 
 * Requirements: 1.2
 */
public record DeviceCodeResponse(
    String deviceCode,
    String userCode,
    String verificationUri,
    int expiresIn,
    int interval
) {
    /**
     * Creates a DeviceCodeResponse from GitHub API JSON field names.
     * GitHub uses snake_case in responses.
     */
    public static DeviceCodeResponse fromGitHubResponse(
            String device_code,
            String user_code,
            String verification_uri,
            int expires_in,
            int interval) {
        return new DeviceCodeResponse(device_code, user_code, verification_uri, expires_in, interval);
    }
}
