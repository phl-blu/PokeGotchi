package com.tamagotchi.committracker.github;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of GitHubOAuthService using OkHttp and Gson.
 * Handles GitHub OAuth 2.0 Device Flow authentication.
 * 
 * Requirements: 1.2, 1.3, 1.4
 */
public class GitHubOAuthServiceImpl implements GitHubOAuthService {
    
    private static final Logger LOGGER = Logger.getLogger(GitHubOAuthServiceImpl.class.getName());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String clientId;
    private final ScheduledExecutorService scheduler;
    
    // Polling state
    private static final int MAX_POLL_ATTEMPTS = 180; // 15 minutes at 5 second intervals
    
    public GitHubOAuthServiceImpl() {
        this(GitHubConfig.getClientId());
    }
    
    public GitHubOAuthServiceImpl(String clientId) {
        this.clientId = clientId;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "github-oauth-poller");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<DeviceCodeResponse> initiateDeviceFlow() {
        return CompletableFuture.supplyAsync(() -> {
            if (clientId == null || clientId.isBlank()) {
                throw new IllegalStateException("GitHub Client ID not configured. Set " + 
                    GitHubConfig.CLIENT_ID_ENV_VAR + " environment variable.");
            }
            
            String requestBody = "client_id=" + clientId + "&scope=" + 
                GitHubConfig.OAUTH_SCOPES.replace(" ", "%20");
            
            Request request = new Request.Builder()
                .url(GitHubConfig.DEVICE_CODE_URL)
                .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
                .post(RequestBody.create(requestBody, FORM))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    throw new OAuthException("Failed to initiate device flow: " + response.code() + " - " + errorBody);
                }
                
                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                
                return DeviceCodeResponse.fromGitHubResponse(
                    json.get("device_code").getAsString(),
                    json.get("user_code").getAsString(),
                    json.get("verification_uri").getAsString(),
                    json.get("expires_in").getAsInt(),
                    json.has("interval") ? json.get("interval").getAsInt() : GitHubConfig.DEFAULT_DEVICE_POLL_INTERVAL
                );
            } catch (IOException e) {
                throw new OAuthException("Network error during device flow initiation", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<AccessTokenResponse> pollForAccessToken(String deviceCode) {
        CompletableFuture<AccessTokenResponse> future = new CompletableFuture<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        
        // Start polling with the default interval
        pollForTokenInternal(deviceCode, GitHubConfig.DEFAULT_DEVICE_POLL_INTERVAL, 
            0, MAX_POLL_ATTEMPTS, future, completed);
        
        return future;
    }
    
    private void pollForTokenInternal(String deviceCode, int intervalSeconds, 
            int attempt, int maxAttempts, CompletableFuture<AccessTokenResponse> future,
            AtomicBoolean completed) {
        
        if (completed.get() || attempt >= maxAttempts) {
            if (!completed.get()) {
                future.completeExceptionally(new OAuthException("Polling timed out waiting for user authorization"));
            }
            return;
        }
        
        scheduler.schedule(() -> {
            try {
                AccessTokenResponse response = tryGetAccessToken(deviceCode);
                if (response != null) {
                    completed.set(true);
                    future.complete(response);
                    return;
                }
                // Continue polling
                pollForTokenInternal(deviceCode, intervalSeconds, attempt + 1, maxAttempts, future, completed);
            } catch (SlowDownException e) {
                // GitHub asked us to slow down, increase interval
                pollForTokenInternal(deviceCode, intervalSeconds + 5, attempt + 1, maxAttempts, future, completed);
            } catch (AuthorizationPendingException e) {
                // User hasn't authorized yet, continue polling
                pollForTokenInternal(deviceCode, intervalSeconds, attempt + 1, maxAttempts, future, completed);
            } catch (Exception e) {
                completed.set(true);
                future.completeExceptionally(e);
            }
        }, intervalSeconds, TimeUnit.SECONDS);
    }

    private AccessTokenResponse tryGetAccessToken(String deviceCode) 
            throws IOException, SlowDownException, AuthorizationPendingException, OAuthException {
        
        String requestBody = "client_id=" + clientId + 
            "&device_code=" + deviceCode + 
            "&grant_type=urn:ietf:params:oauth:grant-type:device_code";
        
        Request request = new Request.Builder()
            .url(GitHubConfig.ACCESS_TOKEN_URL)
            .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
            .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
            .post(RequestBody.create(requestBody, FORM))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            
            // Check for error responses
            if (json.has("error")) {
                String error = json.get("error").getAsString();
                switch (error) {
                    case "authorization_pending":
                        throw new AuthorizationPendingException();
                    case "slow_down":
                        throw new SlowDownException();
                    case "expired_token":
                        throw new OAuthException("Device code expired. Please restart authentication.");
                    case "access_denied":
                        throw new OAuthException("User denied authorization.");
                    default:
                        String description = json.has("error_description") ? 
                            json.get("error_description").getAsString() : error;
                        throw new OAuthException("OAuth error: " + description);
                }
            }
            
            // Success - we have an access token
            if (json.has("access_token")) {
                return AccessTokenResponse.fromGitHubResponse(
                    json.get("access_token").getAsString(),
                    json.has("token_type") ? json.get("token_type").getAsString() : "bearer",
                    json.has("scope") ? json.get("scope").getAsString() : "",
                    json.has("refresh_token") ? json.get("refresh_token").getAsString() : null,
                    json.has("expires_in") ? json.get("expires_in").getAsInt() : 0
                );
            }
            
            return null;
        }
    }
    
    @Override
    public CompletableFuture<AccessTokenResponse> refreshAccessToken(String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalArgumentException("Refresh token cannot be null or empty");
            }
            
            String requestBody = "client_id=" + clientId + 
                "&refresh_token=" + refreshToken + 
                "&grant_type=refresh_token";
            
            Request request = new Request.Builder()
                .url(GitHubConfig.ACCESS_TOKEN_URL)
                .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
                .post(RequestBody.create(requestBody, FORM))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                
                if (json.has("error")) {
                    String error = json.get("error").getAsString();
                    String description = json.has("error_description") ? 
                        json.get("error_description").getAsString() : error;
                    throw new OAuthException("Token refresh failed: " + description);
                }
                
                if (json.has("access_token")) {
                    return AccessTokenResponse.fromGitHubResponse(
                        json.get("access_token").getAsString(),
                        json.has("token_type") ? json.get("token_type").getAsString() : "bearer",
                        json.has("scope") ? json.get("scope").getAsString() : "",
                        json.has("refresh_token") ? json.get("refresh_token").getAsString() : refreshToken,
                        json.has("expires_in") ? json.get("expires_in").getAsInt() : 0
                    );
                }
                
                throw new OAuthException("Invalid response from token refresh endpoint");
            } catch (IOException e) {
                throw new OAuthException("Network error during token refresh", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> revokeToken(String accessToken) {
        return CompletableFuture.runAsync(() -> {
            if (accessToken == null || accessToken.isBlank()) {
                return; // Nothing to revoke
            }
            
            // GitHub doesn't have a standard token revocation endpoint for OAuth apps
            // Token revocation is typically done through the GitHub UI or by deleting the OAuth app authorization
            // For now, we just log that revocation was requested
            LOGGER.info("Token revocation requested. User should revoke access via GitHub settings.");
        });
    }
    
    @Override
    public CompletableFuture<Boolean> validateToken(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (accessToken == null || accessToken.isBlank()) {
                return false;
            }
            
            Request request = new Request.Builder()
                .url(GitHubConfig.USER_ENDPOINT)
                .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
                .header(GitHubConfig.AUTHORIZATION_HEADER, "Bearer " + accessToken)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error validating token", e);
                return false;
            }
        });
    }
    
    /**
     * Shuts down the scheduler used for polling.
     * Should be called when the service is no longer needed.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Internal exception classes for polling flow control
    private static class AuthorizationPendingException extends Exception {
        AuthorizationPendingException() {
            super("Authorization pending");
        }
    }
    
    private static class SlowDownException extends Exception {
        SlowDownException() {
            super("Slow down requested");
        }
    }
}
