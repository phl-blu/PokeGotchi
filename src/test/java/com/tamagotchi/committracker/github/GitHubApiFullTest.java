package com.tamagotchi.committracker.github;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Full API test - fetches repos and commits from GitHub
 */
public class GitHubApiFullTest {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         GitHub API Full Test - Repos & Commits               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Get stored token
        SecureTokenStorage tokenStorage = new SecureTokenStorageImpl();
        Optional<String> token = tokenStorage.getAccessToken();
        
        if (token.isEmpty()) {
            System.err.println("❌ No stored token found! Run GitHubIntegrationManualTest first.");
            return;
        }
        
        System.out.println("✅ Found stored token");
        
        // Create API client
        RateLimitManager rateLimitManager = new RateLimitManagerImpl();
        GitHubApiClient apiClient = new GitHubApiClientImpl(rateLimitManager);
        apiClient.setAccessToken(token.get());
        
        try {
            // Fetch user info
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("FETCHING USER INFO...");
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            GitHubUser user = apiClient.fetchAuthenticatedUser().get(30, TimeUnit.SECONDS);
            System.out.println("👤 Username: " + user.login());
            System.out.println("🆔 ID: " + user.id());
            
            // Fetch repositories
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("FETCHING REPOSITORIES...");
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            List<GitHubRepository> repos = apiClient.fetchUserRepositories().get(60, TimeUnit.SECONDS);
            System.out.println("📁 Found " + repos.size() + " repositories:");
            System.out.println();
            
            int count = 0;
            GitHubRepository repoToCheck = null;
            for (GitHubRepository repo : repos) {
                if (count < 10) {
                    System.out.println("  • " + repo.fullName() + (repo.isPrivate() ? " 🔒" : " 🌐"));
                }
                if (repoToCheck == null) {
                    repoToCheck = repo;
                }
                count++;
            }
            if (count > 10) {
                System.out.println("  ... and " + (count - 10) + " more");
            }
            
            // Fetch commits from first repo
            if (repoToCheck != null) {
                System.out.println();
                System.out.println("═══════════════════════════════════════════════════════════════");
                System.out.println("FETCHING COMMITS FROM: " + repoToCheck.fullName());
                System.out.println("═══════════════════════════════════════════════════════════════");
                
                String[] parts = repoToCheck.fullName().split("/");
                Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
                CommitFetchResult result = apiClient.fetchCommits(parts[0], parts[1], since, null)
                    .get(30, TimeUnit.SECONDS);
                
                List<GitHubCommit> commits = result.commits();
                System.out.println("📝 Found " + commits.size() + " commits in last 30 days:");
                System.out.println();
                
                int commitCount = 0;
                for (GitHubCommit commit : commits) {
                    if (commitCount >= 5) break;
                    String msg = commit.message();
                    if (msg.contains("\n")) msg = msg.substring(0, msg.indexOf("\n"));
                    if (msg.length() > 50) msg = msg.substring(0, 47) + "...";
                    System.out.println("  • " + commit.sha().substring(0, 7) + " - " + msg);
                    System.out.println("    by " + commit.authorName() + " on " + commit.committedAt());
                    commitCount++;
                }
                if (commits.size() > 5) {
                    System.out.println("  ... and " + (commits.size() - 5) + " more");
                }
            }
            
            // Show rate limit status
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("RATE LIMIT STATUS");
            System.out.println("═══════════════════════════════════════════════════════════════");
            RateLimitStatus status = apiClient.getRateLimitStatus();
            if (status != null) {
                System.out.println("📈 Remaining: " + status.remaining() + " / " + status.limit());
            }
            System.out.println("🔄 Polling Mode: " + rateLimitManager.getCurrentPollingMode());
            
            System.out.println();
            System.out.println("🎉 GitHub API is fully working!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
