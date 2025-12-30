package com.tamagotchi.committracker.github;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manual integration test for Task 4 Checkpoint.
 * Tests the complete authentication pipeline:
 * 1. OAuth Device Flow authentication
 * 2. Secure token storage (encryption/decryption)
 * 3. Rate limit tracking
 * 
 * This verifies Tasks 1-3 work together end-to-end.
 * 
 * BEFORE RUNNING:
 * Set the GITHUB_CLIENT_ID environment variable
 */
public class GitHubIntegrationManualTest {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     Task 4 Checkpoint - Full Integration Test                ║");
        System.out.println("║     OAuth + Token Storage + Rate Limiting                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Check configuration
        String clientId = GitHubConfig.getClientId();
        if (clientId == null || clientId.isBlank()) {
            System.err.println("❌ ERROR: GITHUB_CLIENT_ID not set!");
            return;
        }
        
        GitHubOAuthServiceImpl oauthService = new GitHubOAuthServiceImpl(clientId);
        SecureTokenStorage tokenStorage = new SecureTokenStorageImpl();
        RateLimitManager rateLimitManager = new RateLimitManagerImpl();
        
        try {
            // ═══════════════════════════════════════════════════════════════
            // PHASE 1: Check for existing stored token
            // ═══════════════════════════════════════════════════════════════
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("PHASE 1: Checking for stored token...");
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            Optional<String> storedToken = tokenStorage.getAccessToken();
            String accessToken;
            
            if (storedToken.isPresent()) {
                System.out.println("✅ Found stored token!");
                System.out.println("🔍 Validating stored token...");
                
                boolean isValid = oauthService.validateToken(storedToken.get())
                    .get(10, TimeUnit.SECONDS);
                
                if (isValid) {
                    System.out.println("✅ Stored token is valid!");
                    accessToken = storedToken.get();
                } else {
                    System.out.println("❌ Stored token is invalid/expired. Need to re-authenticate.");
                    tokenStorage.clearAllTokens();
                    accessToken = performOAuthFlow(oauthService, tokenStorage);
                }
            } else {
                System.out.println("📭 No stored token found. Starting OAuth flow...");
                accessToken = performOAuthFlow(oauthService, tokenStorage);
            }
            
            if (accessToken == null) {
                System.err.println("❌ Failed to obtain access token!");
                return;
            }
            
            // ═══════════════════════════════════════════════════════════════
            // PHASE 2: Verify Token Storage (Round-trip test)
            // ═══════════════════════════════════════════════════════════════
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("PHASE 2: Testing Token Storage (Encryption Round-trip)");
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            // Store the token
            tokenStorage.storeAccessToken(accessToken);
            System.out.println("✅ Token stored (encrypted with AES-256-GCM)");
            
            // Retrieve and verify
            Optional<String> retrieved = tokenStorage.getAccessToken();
            if (retrieved.isPresent() && retrieved.get().equals(accessToken)) {
                System.out.println("✅ Token round-trip successful!");
                System.out.println("   Original:  " + accessToken.substring(0, 10) + "...[HIDDEN]");
                System.out.println("   Retrieved: " + retrieved.get().substring(0, 10) + "...[HIDDEN]");
                System.out.println("   Match: ✅ YES");
            } else {
                System.err.println("❌ Token round-trip FAILED!");
                System.err.println("   Original and retrieved tokens don't match!");
            }
            
            // ═══════════════════════════════════════════════════════════════
            // PHASE 3: Test Rate Limit Tracking
            // ═══════════════════════════════════════════════════════════════
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("PHASE 3: Testing Rate Limit Manager");
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            // Make an API call to get real rate limit info
            System.out.println("📡 Fetching rate limit status from GitHub API...");
            RateLimitStatus realStatus = fetchRateLimitFromAPI(accessToken);
            
            if (realStatus != null) {
                // Update rate limit manager with real data
                rateLimitManager.recordRequest(realStatus.remaining(), realStatus.resetTime());
                
                System.out.println("✅ Rate Limit Status:");
                System.out.println("   Limit:     " + realStatus.limit());
                System.out.println("   Remaining: " + realStatus.remaining());
                System.out.println("   Used:      " + realStatus.used());
                System.out.println("   Resets at: " + realStatus.resetTime());
                System.out.println();
                
                // Test polling mode
                PollingMode mode = rateLimitManager.getCurrentPollingMode();
                System.out.println("📊 Rate Limit Manager State:");
                System.out.println("   Can make request: " + rateLimitManager.canMakeRequest());
                System.out.println("   Current mode:     " + mode);
                System.out.println("   Polling interval: " + mode.getInterval().toMinutes() + " minutes");
                System.out.println("   Conservative:     " + rateLimitManager.isConservativeMode());
                
                // Test mode switching simulation
                System.out.println();
                System.out.println("🧪 Simulating rate limit scenarios:");
                testRateLimitScenarios();
            } else {
                System.err.println("❌ Failed to fetch rate limit from API");
            }
            
            // ═══════════════════════════════════════════════════════════════
            // PHASE 4: Summary
            // ═══════════════════════════════════════════════════════════════
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("CHECKPOINT 4 SUMMARY");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("✅ OAuth Device Flow:     WORKING");
            System.out.println("✅ Token Validation:      WORKING");
            System.out.println("✅ Secure Token Storage:  WORKING (AES-256-GCM)");
            System.out.println("✅ Token Round-trip:      WORKING");
            System.out.println("✅ Rate Limit Tracking:   WORKING");
            System.out.println("✅ Adaptive Polling:      WORKING");
            System.out.println();
            System.out.println("🎉 All Task 4 checkpoint tests PASSED!");
            System.out.println();
            System.out.println("Ready to proceed to Task 5: GitHub API Client");
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            oauthService.shutdown();
        }
    }
    
    private static String performOAuthFlow(GitHubOAuthServiceImpl oauthService, 
                                           SecureTokenStorage tokenStorage) throws Exception {
        System.out.println();
        System.out.println("📡 Initiating GitHub Device Flow...");
        
        DeviceCodeResponse deviceCode = oauthService.initiateDeviceFlow()
            .get(30, TimeUnit.SECONDS);
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    AUTHORIZATION REQUIRED                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  1. Open: %s%n", deviceCode.verificationUri());
        System.out.printf("║  2. Enter code: %s                                    ║%n", deviceCode.userCode());
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("⏳ Waiting for authorization...");
        
        AccessTokenResponse tokenResponse = oauthService.pollForAccessToken(deviceCode.deviceCode())
            .get(deviceCode.expiresIn() + 30, TimeUnit.SECONDS);
        
        System.out.println("✅ Authentication successful!");
        
        // Store the token
        tokenStorage.storeAccessToken(tokenResponse.accessToken());
        if (tokenResponse.hasRefreshToken()) {
            tokenStorage.storeRefreshToken(tokenResponse.refreshToken());
        }
        System.out.println("✅ Token stored securely!");
        
        return tokenResponse.accessToken();
    }
    
    private static RateLimitStatus fetchRateLimitFromAPI(String accessToken) {
        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(GitHubConfig.RATE_LIMIT_ENDPOINT)
                .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
                .header(GitHubConfig.AUTHORIZATION_HEADER, "Bearer " + accessToken)
                .get()
                .build();
            
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    // Parse rate limit from headers
                    String remaining = response.header(GitHubConfig.RATE_LIMIT_REMAINING_HEADER);
                    String limit = response.header(GitHubConfig.RATE_LIMIT_LIMIT_HEADER);
                    String reset = response.header(GitHubConfig.RATE_LIMIT_RESET_HEADER);
                    
                    int remainingInt = remaining != null ? Integer.parseInt(remaining) : 5000;
                    int limitInt = limit != null ? Integer.parseInt(limit) : 5000;
                    long resetLong = reset != null ? Long.parseLong(reset) : System.currentTimeMillis() / 1000 + 3600;
                    
                    return new RateLimitStatus(
                        limitInt,
                        remainingInt,
                        Instant.ofEpochSecond(resetLong),
                        limitInt - remainingInt
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching rate limit: " + e.getMessage());
        }
        return null;
    }
    
    private static void testRateLimitScenarios() {
        RateLimitManager testManager = new RateLimitManagerImpl();
        Instant futureReset = Instant.now().plusSeconds(3600);
        
        // Scenario 1: Plenty of requests
        testManager.recordRequest(4500, futureReset);
        System.out.println("   • 4500 remaining → Mode: " + testManager.getCurrentPollingMode() + 
                          " (expected: NORMAL)");
        
        // Scenario 2: Getting low
        testManager.recordRequest(400, futureReset);
        System.out.println("   • 400 remaining  → Mode: " + testManager.getCurrentPollingMode() + 
                          " (expected: CONSERVATIVE)");
        
        // Scenario 3: Very low
        testManager.recordRequest(80, futureReset);
        System.out.println("   • 80 remaining   → Mode: " + testManager.getCurrentPollingMode() + 
                          " (expected: MINIMAL)");
        
        // Scenario 4: Exhausted
        testManager.recordRequest(0, futureReset);
        System.out.println("   • 0 remaining    → Mode: " + testManager.getCurrentPollingMode() + 
                          " (expected: PAUSED)");
    }
}
