package com.tamagotchi.committracker.github;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of GitHubApiClient using OkHttp and Gson.
 * 
 * Handles HTTP requests with authentication headers, response parsing,
 * and rate limit header extraction.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.3, 5.1, 6.1, 6.2
 */
public class GitHubApiClientImpl implements GitHubApiClient {
    
    private static final Logger LOGGER = Logger.getLogger(GitHubApiClientImpl.class.getName());
    
    // Pagination constants
    private static final int DEFAULT_PER_PAGE = 100;
    private static final int MAX_PAGES = 100; // Safety limit
    
    // Link header pattern for pagination
    private static final Pattern LINK_NEXT_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final RateLimitManager rateLimitManager;
    
    private volatile String accessToken;
    private volatile GitHubUser authenticatedUser;
    
    /**
     * Creates a new GitHubApiClientImpl with default configuration.
     */
    public GitHubApiClientImpl() {
        this(new RateLimitManagerImpl());
    }
    
    /**
     * Creates a new GitHubApiClientImpl with a custom RateLimitManager.
     * 
     * @param rateLimitManager the rate limit manager to use
     */
    public GitHubApiClientImpl(RateLimitManager rateLimitManager) {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        this.gson = new GsonBuilder()
            .create();
        
        this.rateLimitManager = rateLimitManager != null ? rateLimitManager : new RateLimitManagerImpl();
    }
    
    /**
     * Creates a new GitHubApiClientImpl with custom OkHttpClient (for testing).
     */
    public GitHubApiClientImpl(OkHttpClient httpClient, RateLimitManager rateLimitManager) {
        this.httpClient = httpClient;
        this.gson = new GsonBuilder().create();
        this.rateLimitManager = rateLimitManager != null ? rateLimitManager : new RateLimitManagerImpl();
    }

    
    @Override
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.authenticatedUser = null; // Clear cached user on token change
    }
    
    @Override
    public void clearAuthentication() {
        this.accessToken = null;
        this.authenticatedUser = null;
    }
    
    @Override
    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank();
    }
    
    @Override
    public RateLimitStatus getRateLimitStatus() {
        return rateLimitManager.getRateLimitStatus();
    }
    
    // ==================== Repository Fetching ====================
    
    @Override
    public CompletableFuture<List<GitHubRepository>> fetchUserRepositories() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                throw new GitHubApiException("Not authenticated", 401);
            }
            
            List<GitHubRepository> allRepos = new ArrayList<>();
            String url = GitHubConfig.USER_REPOS_ENDPOINT + 
                        "?per_page=" + DEFAULT_PER_PAGE + 
                        "&type=all&sort=pushed&direction=desc";
            
            int pageCount = 0;
            
            while (url != null && pageCount < MAX_PAGES) {
                pageCount++;
                
                // Check rate limit before making request
                if (!rateLimitManager.canMakeRequest()) {
                    LOGGER.warning("Rate limit reached, stopping repository fetch");
                    break;
                }
                
                try {
                    Request request = buildAuthenticatedRequest(url);
                    
                    try (Response response = httpClient.newCall(request).execute()) {
                        extractAndUpdateRateLimit(response);
                        
                        if (!response.isSuccessful()) {
                            handleErrorResponse(response, "fetch repositories");
                        }
                        
                        ResponseBody body = response.body();
                        if (body == null) {
                            break;
                        }
                        
                        String json = body.string();
                        List<GitHubRepository> repos = parseRepositories(json);
                        allRepos.addAll(repos);
                        
                        // Get next page URL from Link header
                        url = extractNextPageUrl(response.header("Link"));
                        
                        LOGGER.fine("Fetched page " + pageCount + " with " + repos.size() + " repositories");
                    }
                } catch (IOException e) {
                    throw new GitHubApiException("Network error fetching repositories", e);
                }
            }
            
            LOGGER.info("Fetched " + allRepos.size() + " repositories in " + pageCount + " pages");
            return allRepos;
        });
    }
    
    private List<GitHubRepository> parseRepositories(String json) {
        List<GitHubRepository> repos = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            repos.add(parseRepository(obj));
        }
        
        return repos;
    }
    
    private GitHubRepository parseRepository(JsonObject obj) {
        long id = obj.get("id").getAsLong();
        String name = obj.get("name").getAsString();
        String fullName = obj.get("full_name").getAsString();
        
        JsonObject ownerObj = obj.getAsJsonObject("owner");
        String owner = ownerObj.get("login").getAsString();
        
        boolean isPrivate = obj.get("private").getAsBoolean();
        String defaultBranch = getStringOrNull(obj, "default_branch");
        
        Instant pushedAt = null;
        if (obj.has("pushed_at") && !obj.get("pushed_at").isJsonNull()) {
            pushedAt = parseInstant(obj.get("pushed_at").getAsString());
        }
        
        return new GitHubRepository(id, name, fullName, owner, isPrivate, defaultBranch, pushedAt, null);
    }

    
    // ==================== Commit Fetching ====================
    
    @Override
    public CompletableFuture<CommitFetchResult> fetchCommits(
            String owner, String repo, Instant since, String etag) {
        
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                throw new GitHubApiException("Not authenticated", 401);
            }
            
            if (owner == null || owner.isBlank() || repo == null || repo.isBlank()) {
                throw new IllegalArgumentException("Owner and repo must not be null or blank");
            }
            
            // Check rate limit before making request
            if (!rateLimitManager.canMakeRequest()) {
                LOGGER.warning("Rate limit reached, cannot fetch commits");
                throw new GitHubApiException("Rate limit exceeded", 429);
            }
            
            String url = buildCommitsUrl(owner, repo, since);
            
            try {
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                    .header(GitHubConfig.AUTHORIZATION_HEADER, "Bearer " + accessToken)
                    .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE);
                
                // Add conditional request headers (Requirement 6.1, 6.2)
                if (etag != null && !etag.isBlank()) {
                    requestBuilder.header(GitHubConfig.IF_NONE_MATCH_HEADER, etag);
                }
                
                Request request = requestBuilder.build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    int remaining = extractAndUpdateRateLimit(response);
                    
                    // Handle 304 Not Modified (conditional request success)
                    if (response.code() == 304) {
                        LOGGER.fine("Commits not modified for " + owner + "/" + repo);
                        return CommitFetchResult.notModified(etag, remaining);
                    }
                    
                    if (!response.isSuccessful()) {
                        handleErrorResponse(response, "fetch commits for " + owner + "/" + repo);
                    }
                    
                    ResponseBody body = response.body();
                    if (body == null) {
                        return CommitFetchResult.withCommits(List.of(), null, remaining);
                    }
                    
                    String json = body.string();
                    List<GitHubCommit> commits = parseCommits(json, owner + "/" + repo);
                    
                    // Filter commits by authenticated user if we have user info
                    if (authenticatedUser != null) {
                        commits = filterCommitsByUser(commits, authenticatedUser);
                    }
                    
                    String newEtag = response.header(GitHubConfig.ETAG_HEADER);
                    
                    LOGGER.fine("Fetched " + commits.size() + " commits for " + owner + "/" + repo);
                    return CommitFetchResult.withCommits(commits, newEtag, remaining);
                }
            } catch (IOException e) {
                throw new GitHubApiException("Network error fetching commits", e);
            }
        });
    }
    
    private String buildCommitsUrl(String owner, String repo, Instant since) {
        StringBuilder url = new StringBuilder(GitHubConfig.getCommitsEndpoint(owner, repo));
        url.append("?per_page=").append(DEFAULT_PER_PAGE);
        
        if (since != null) {
            // Format as ISO 8601 for GitHub API
            String sinceStr = DateTimeFormatter.ISO_INSTANT.format(since);
            url.append("&since=").append(sinceStr);
        }
        
        return url.toString();
    }
    
    private List<GitHubCommit> parseCommits(String json, String repoFullName) {
        List<GitHubCommit> commits = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            GitHubCommit commit = parseCommit(obj, repoFullName);
            if (commit != null) {
                commits.add(commit);
            }
        }
        
        return commits;
    }
    
    private GitHubCommit parseCommit(JsonObject obj, String repoFullName) {
        try {
            String sha = obj.get("sha").getAsString();
            String url = obj.get("html_url").getAsString();
            
            JsonObject commitObj = obj.getAsJsonObject("commit");
            String message = commitObj.get("message").getAsString();
            
            JsonObject authorObj = commitObj.getAsJsonObject("author");
            String authorName = getStringOrNull(authorObj, "name");
            String authorEmail = getStringOrNull(authorObj, "email");
            
            Instant committedAt = null;
            if (authorObj.has("date") && !authorObj.get("date").isJsonNull()) {
                committedAt = parseInstant(authorObj.get("date").getAsString());
            }
            
            return new GitHubCommit(sha, message, authorName, authorEmail, committedAt, repoFullName, url);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse commit", e);
            return null;
        }
    }
    
    /**
     * Filters commits to only include those by the authenticated user.
     * Matches by email address.
     * 
     * Requirement: 4.1 - fetch commits authored by the authenticated user
     */
    private List<GitHubCommit> filterCommitsByUser(List<GitHubCommit> commits, GitHubUser user) {
        if (user == null || user.email() == null) {
            return commits; // Can't filter without user email
        }
        
        return commits.stream()
            .filter(c -> user.email().equalsIgnoreCase(c.authorEmail()) ||
                        user.login().equalsIgnoreCase(c.authorName()))
            .toList();
    }

    
    // ==================== User Fetching ====================
    
    @Override
    public CompletableFuture<GitHubUser> fetchAuthenticatedUser() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                throw new GitHubApiException("Not authenticated", 401);
            }
            
            // Return cached user if available
            if (authenticatedUser != null) {
                return authenticatedUser;
            }
            
            // Check rate limit before making request
            if (!rateLimitManager.canMakeRequest()) {
                LOGGER.warning("Rate limit reached, cannot fetch user");
                throw new GitHubApiException("Rate limit exceeded", 429);
            }
            
            try {
                Request request = buildAuthenticatedRequest(GitHubConfig.USER_ENDPOINT);
                
                try (Response response = httpClient.newCall(request).execute()) {
                    extractAndUpdateRateLimit(response);
                    
                    if (!response.isSuccessful()) {
                        handleErrorResponse(response, "fetch authenticated user");
                    }
                    
                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new GitHubApiException("Empty response body", 500);
                    }
                    
                    String json = body.string();
                    authenticatedUser = parseUser(json);
                    
                    LOGGER.info("Authenticated as: " + authenticatedUser.login());
                    return authenticatedUser;
                }
            } catch (IOException e) {
                throw new GitHubApiException("Network error fetching user", e);
            }
        });
    }
    
    private GitHubUser parseUser(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        
        long id = obj.get("id").getAsLong();
        String login = obj.get("login").getAsString();
        String name = getStringOrNull(obj, "name");
        String email = getStringOrNull(obj, "email");
        String avatarUrl = getStringOrNull(obj, "avatar_url");
        
        Instant createdAt = null;
        if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
            createdAt = parseInstant(obj.get("created_at").getAsString());
        }
        
        return new GitHubUser(id, login, name, email, avatarUrl, createdAt);
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Builds an authenticated request with standard headers.
     */
    private Request buildAuthenticatedRequest(String url) {
        return new Request.Builder()
            .url(url)
            .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
            .header(GitHubConfig.AUTHORIZATION_HEADER, "Bearer " + accessToken)
            .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
            .build();
    }
    
    /**
     * Extracts rate limit information from response headers and updates the manager.
     * 
     * Requirement 5.1: Track remaining requests using X-RateLimit headers.
     * 
     * @return the remaining rate limit
     */
    private int extractAndUpdateRateLimit(Response response) {
        String remainingStr = response.header(GitHubConfig.RATE_LIMIT_REMAINING_HEADER);
        String resetStr = response.header(GitHubConfig.RATE_LIMIT_RESET_HEADER);
        
        int remaining = GitHubConfig.RATE_LIMIT_TOTAL;
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        if (remainingStr != null) {
            try {
                remaining = Integer.parseInt(remainingStr);
            } catch (NumberFormatException e) {
                LOGGER.warning("Failed to parse rate limit remaining: " + remainingStr);
            }
        }
        
        if (resetStr != null) {
            try {
                long resetEpoch = Long.parseLong(resetStr);
                resetTime = Instant.ofEpochSecond(resetEpoch);
            } catch (NumberFormatException e) {
                LOGGER.warning("Failed to parse rate limit reset: " + resetStr);
            }
        }
        
        rateLimitManager.recordRequest(remaining, resetTime);
        return remaining;
    }
    
    /**
     * Extracts the next page URL from the Link header.
     */
    private String extractNextPageUrl(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        
        Matcher matcher = LINK_NEXT_PATTERN.matcher(linkHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Handles error responses from the API.
     */
    private void handleErrorResponse(Response response, String operation) throws IOException {
        int code = response.code();
        String message = "Failed to " + operation + ": HTTP " + code;
        
        ResponseBody body = response.body();
        if (body != null) {
            try {
                String errorJson = body.string();
                JsonObject errorObj = JsonParser.parseString(errorJson).getAsJsonObject();
                if (errorObj.has("message")) {
                    message = errorObj.get("message").getAsString();
                }
            } catch (Exception e) {
                // Ignore parsing errors for error response
            }
        }
        
        throw new GitHubApiException(message, code);
    }
    
    /**
     * Safely gets a string value from a JSON object.
     */
    private String getStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
    
    /**
     * Parses an ISO 8601 date string to Instant.
     */
    private Instant parseInstant(String dateStr) {
        try {
            return ZonedDateTime.parse(dateStr).toInstant();
        } catch (Exception e) {
            LOGGER.warning("Failed to parse date: " + dateStr);
            return null;
        }
    }
}
