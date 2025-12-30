package com.tamagotchi.committracker.github;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Manual test harness for GitHub OAuth Device Flow.
 * 
 * BEFORE RUNNING:
 * 1. Create a GitHub OAuth App at: https://github.com/settings/developers
 *    - Click "New OAuth App"
 *    - Application name: Pokemon Commit Tracker (or any name)
 *    - Homepage URL: http://localhost (or any URL)
 *    - Authorization callback URL: http://localhost (not used for Device Flow)
 *    - Check "Enable Device Flow" in the app settings
 * 
 * 2. Set the GITHUB_CLIENT_ID environment variable:
 *    - Windows CMD: set GITHUB_CLIENT_ID=your_client_id_here
 *    - Windows PowerShell: $env:GITHUB_CLIENT_ID="your_client_id_here"
 *    - Or add to your IDE run configuration
 * 
 * 3. Run this test class
 * 
 * This is a MANUAL test - it requires user interaction to complete the OAuth flow.
 */
public class GitHubOAuthManualTest {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║       GitHub OAuth Device Flow - Manual Test                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Check if Client ID is configured
        String clientId = GitHubConfig.getClientId();
        if (clientId == null || clientId.isBlank()) {
            System.err.println("❌ ERROR: GITHUB_CLIENT_ID environment variable not set!");
            System.err.println();
            System.err.println("To set it:");
            System.err.println("  Windows CMD:        set GITHUB_CLIENT_ID=your_client_id");
            System.err.println("  Windows PowerShell: $env:GITHUB_CLIENT_ID=\"your_client_id\"");
            System.err.println();
            System.err.println("Get your Client ID from: https://github.com/settings/developers");
            System.err.println("Make sure to enable 'Device Flow' in your OAuth App settings!");
            return;
        }
        
        System.out.println("✅ Client ID found: " + clientId.substring(0, 8) + "...");
        System.out.println();
        
        GitHubOAuthServiceImpl oauthService = new GitHubOAuthServiceImpl(clientId);
        
        try {
            // Step 1: Initiate Device Flow
            System.out.println("📡 Initiating GitHub Device Flow...");
            DeviceCodeResponse deviceCode = oauthService.initiateDeviceFlow()
                .get(30, TimeUnit.SECONDS);
            
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    AUTHORIZATION REQUIRED                    ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.println("║                                                              ║");
            System.out.printf("║  1. Open this URL in your browser:                           ║%n");
            System.out.printf("║     🌐 %s%n", deviceCode.verificationUri());
            System.out.println("║                                                              ║");
            System.out.printf("║  2. Enter this code:                                         ║%n");
            System.out.printf("║     🔑 %s                                            ║%n", deviceCode.userCode());
            System.out.println("║                                                              ║");
            System.out.printf("║  Code expires in: %d seconds                                 ║%n", deviceCode.expiresIn());
            System.out.println("║                                                              ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("⏳ Waiting for you to authorize in browser...");
            System.out.println("   (Polling every " + deviceCode.interval() + " seconds)");
            System.out.println();
            
            // Step 2: Poll for access token
            AccessTokenResponse tokenResponse = oauthService.pollForAccessToken(deviceCode.deviceCode())
                .get(deviceCode.expiresIn() + 30, TimeUnit.SECONDS);
            
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ✅ AUTHENTICATION SUCCESS!                ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Token Details:");
            System.out.println("  • Token Type: " + tokenResponse.tokenType());
            System.out.println("  • Scopes: " + tokenResponse.scope());
            System.out.println("  • Has Refresh Token: " + tokenResponse.hasRefreshToken());
            System.out.println("  • Access Token: " + tokenResponse.accessToken().substring(0, 10) + "...[HIDDEN]");
            System.out.println();
            
            // Step 3: Validate the token
            System.out.println("🔍 Validating token by fetching user info...");
            boolean isValid = oauthService.validateToken(tokenResponse.accessToken())
                .get(10, TimeUnit.SECONDS);
            
            if (isValid) {
                System.out.println("✅ Token is valid! You can now use the GitHub API.");
                
                // Fetch user info to show it works
                System.out.println();
                System.out.println("📋 Fetching your GitHub profile...");
                fetchAndDisplayUserInfo(tokenResponse.accessToken());
            } else {
                System.out.println("❌ Token validation failed!");
            }
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ ERROR: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
        } finally {
            oauthService.shutdown();
        }
        
        System.out.println();
        System.out.println("Test complete. Press Enter to exit...");
        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
        }
    }
    
    private static void fetchAndDisplayUserInfo(String accessToken) {
        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(GitHubConfig.USER_ENDPOINT)
                .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
                .header(GitHubConfig.AUTHORIZATION_HEADER, "Bearer " + accessToken)
                .get()
                .build();
            
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    com.google.gson.JsonObject json = new com.google.gson.Gson()
                        .fromJson(body, com.google.gson.JsonObject.class);
                    
                    System.out.println();
                    System.out.println("╔══════════════════════════════════════════════════════════════╗");
                    System.out.println("║                    YOUR GITHUB PROFILE                       ║");
                    System.out.println("╚══════════════════════════════════════════════════════════════╝");
                    System.out.println("  👤 Username: " + json.get("login").getAsString());
                    if (json.has("name") && !json.get("name").isJsonNull()) {
                        System.out.println("  📛 Name: " + json.get("name").getAsString());
                    }
                    System.out.println("  🆔 ID: " + json.get("id").getAsLong());
                    System.out.println("  📊 Public Repos: " + json.get("public_repos").getAsInt());
                    System.out.println("  👥 Followers: " + json.get("followers").getAsInt());
                    System.out.println("  📅 Created: " + json.get("created_at").getAsString());
                    
                    // Check rate limit
                    String remaining = response.header(GitHubConfig.RATE_LIMIT_REMAINING_HEADER);
                    String limit = response.header(GitHubConfig.RATE_LIMIT_LIMIT_HEADER);
                    if (remaining != null && limit != null) {
                        System.out.println();
                        System.out.println("  📈 Rate Limit: " + remaining + " / " + limit + " remaining");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch user info: " + e.getMessage());
        }
    }
}
