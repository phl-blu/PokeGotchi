package com.tamagotchi.committracker.github;

/**
 * Configuration class for GitHub OAuth and API endpoints.
 * Contains all constants needed for GitHub Device Flow authentication
 * and API interactions.
 * 
 * Requirements: 1.2, 9.1
 */
public final class GitHubConfig {
    
    private GitHubConfig() {
        // Utility class - prevent instantiation
    }
    
    // OAuth Device Flow Endpoints
    public static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    public static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    
    // GitHub API Base URL
    public static final String API_BASE_URL = "https://api.github.com";
    
    // API Endpoints
    public static final String USER_ENDPOINT = API_BASE_URL + "/user";
    public static final String USER_REPOS_ENDPOINT = API_BASE_URL + "/user/repos";
    public static final String RATE_LIMIT_ENDPOINT = API_BASE_URL + "/rate_limit";
    
    /**
     * OAuth scopes requested for the application.
     * - repo: Full control of private repositories (needed for commit access)
     * - read:user: Read user profile data
     * 
     * Requirement 9.1: Request only minimum necessary OAuth scopes
     */
    public static final String OAUTH_SCOPES = "repo read:user";
    
    /**
     * Client ID for the GitHub OAuth App.
     * This should be configured via environment variable or config file in production.
     */
    public static final String CLIENT_ID_ENV_VAR = "GITHUB_CLIENT_ID";
    
    // Default polling interval for Device Flow (in seconds)
    public static final int DEFAULT_DEVICE_POLL_INTERVAL = 5;
    
    // Device code expiration time (in seconds) - typically 15 minutes
    public static final int DEVICE_CODE_EXPIRATION = 900;
    
    // Rate limit thresholds
    public static final int RATE_LIMIT_TOTAL = 5000;
    public static final int RATE_LIMIT_CONSERVATIVE_THRESHOLD = 500;
    public static final int RATE_LIMIT_MINIMAL_THRESHOLD = 100;
    public static final int RATE_LIMIT_RESERVED_BUFFER = 500;
    
    // HTTP Headers
    public static final String ACCEPT_HEADER = "Accept";
    public static final String ACCEPT_JSON = "application/json";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String USER_AGENT_VALUE = "Pokemon-Commit-Tracker/1.0";
    
    // Rate Limit Headers
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    
    // Conditional Request Headers
    public static final String ETAG_HEADER = "ETag";
    public static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    public static final String IF_MODIFIED_SINCE_HEADER = "If-Modified-Since";
    
    /** Local config file path (gitignored) */
    private static final String CONFIG_FILE = "config/github.properties";

    /**
     * Gets the GitHub Client ID from environment variable first,
     * then falls back to config/github.properties.
     * @return the client ID or null if not configured
     */
    public static String getClientId() {
        String fromEnv = System.getenv(CLIENT_ID_ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        // Fallback: read from local config file (not committed to git)
        java.io.File configFile = new java.io.File(CONFIG_FILE);
        if (configFile.exists()) {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                props.load(fis);
                String fromFile = props.getProperty("github.client.id");
                if (fromFile != null && !fromFile.isBlank()) {
                    return fromFile.trim();
                }
            } catch (java.io.IOException e) {
                // fall through to return null
            }
        }
        return null;
    }
    
    /**
     * Gets the developer GitHub username from config/github.properties.
     * Used to gate dev-only shortcuts to a specific account.
     * This value is never committed to source control.
     * @return the dev username or null if not configured
     */
    public static String getDevUsername() {
        java.io.File configFile = new java.io.File(CONFIG_FILE);
        if (configFile.exists()) {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                props.load(fis);
                String username = props.getProperty("github.dev.username");
                if (username != null && !username.isBlank() && !username.equals("YOUR_GITHUB_USERNAME_HERE")) {
                    return username.trim();
                }
            } catch (java.io.IOException e) {
                // fall through
            }
        }
        return null;
    }
    
    /**
     * Checks if the GitHub OAuth is properly configured.
     * @return true if client ID is available
     */
    public static boolean isConfigured() {
        String clientId = getClientId();
        return clientId != null && !clientId.isBlank();
    }
    
    /**
     * Builds the repository commits endpoint URL.
     * @param owner repository owner
     * @param repo repository name
     * @return the commits endpoint URL
     */
    public static String getCommitsEndpoint(String owner, String repo) {
        return String.format("%s/repos/%s/%s/commits", API_BASE_URL, owner, repo);
    }
}
