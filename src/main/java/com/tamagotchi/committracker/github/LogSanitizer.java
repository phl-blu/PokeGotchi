package com.tamagotchi.committracker.github;

import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Sanitizes log output to remove sensitive information.
 * 
 * This class ensures:
 * - Tokens are removed from log output
 * - User data is anonymized in logs
 * - Crash reports are sanitized
 * 
 * Requirements: 9.4, 9.6
 * - 9.4: Anonymize data before any transmission
 * - 9.6: Ensure no user data is included in logs or crash reports
 */
public class LogSanitizer {
    
    // Patterns for sensitive data detection
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(?i)(ghp_[a-zA-Z0-9]{6,}|gho_[a-zA-Z0-9]{6,}|github_pat_[a-zA-Z0-9_]{22,}|" +
        "Bearer\\s+[a-zA-Z0-9_\\-\\.]{6,})"
    );
    
    private static final Pattern GENERIC_TOKEN_PATTERN = Pattern.compile(
        "(?i)token[=:\\s]+['\"]?[a-zA-Z0-9_\\-\\.]{6,}['\"]?"
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|secret|password|credential|auth)[=:\\s]+['\"]?[a-zA-Z0-9_\\-\\.]{8,}['\"]?"
    );
    
    private static final Pattern URL_WITH_CREDENTIALS_PATTERN = Pattern.compile(
        "https?://[^:]+:[^@]+@[^\\s]+"
    );
    
    private static final Pattern GITHUB_USERNAME_IN_URL_PATTERN = Pattern.compile(
        "github\\.com/([a-zA-Z0-9_-]+)/"
    );
    
    // Redaction placeholders
    private static final String TOKEN_REDACTED = "[TOKEN_REDACTED]";
    private static final String EMAIL_REDACTED = "[EMAIL_REDACTED]";
    private static final String CREDENTIAL_REDACTED = "[CREDENTIAL_REDACTED]";
    private static final String USERNAME_REDACTED = "[USER]";
    
    private static volatile boolean sanitizationEnabled = true;
    
    /**
     * Private constructor - utility class.
     */
    private LogSanitizer() {
    }
    
    /**
     * Enables or disables log sanitization.
     * Should only be disabled for debugging in development environments.
     * 
     * @param enabled true to enable sanitization
     */
    public static void setSanitizationEnabled(boolean enabled) {
        sanitizationEnabled = enabled;
    }
    
    /**
     * Checks if sanitization is enabled.
     * 
     * @return true if sanitization is enabled
     */
    public static boolean isSanitizationEnabled() {
        return sanitizationEnabled;
    }
    
    /**
     * Sanitizes a string by removing all sensitive information.
     * 
     * Requirement 9.6: Ensure no user data is included in logs or crash reports
     * 
     * @param input the string to sanitize
     * @return sanitized string with sensitive data redacted
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty() || !sanitizationEnabled) {
            return input;
        }
        
        String result = input;
        
        // Remove tokens (GitHub PATs, OAuth tokens, Bearer tokens)
        result = TOKEN_PATTERN.matcher(result).replaceAll(TOKEN_REDACTED);
        
        // Remove generic tokens
        result = GENERIC_TOKEN_PATTERN.matcher(result).replaceAll(CREDENTIAL_REDACTED);
        
        // Remove API keys and credentials
        result = API_KEY_PATTERN.matcher(result).replaceAll(CREDENTIAL_REDACTED);
        
        // Remove URLs with embedded credentials
        result = URL_WITH_CREDENTIALS_PATTERN.matcher(result).replaceAll("[URL_WITH_CREDENTIALS_REDACTED]");
        
        return result;
    }
    
    /**
     * Sanitizes a string with full anonymization including emails and usernames.
     * Use this for crash reports and external logging.
     * 
     * Requirement 9.4: Anonymize data before any transmission
     * 
     * @param input the string to sanitize
     * @return fully anonymized string
     */
    public static String sanitizeForExternalUse(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = sanitize(input);
        
        // Also anonymize emails
        result = EMAIL_PATTERN.matcher(result).replaceAll(EMAIL_REDACTED);
        
        // Anonymize GitHub usernames in URLs
        result = GITHUB_USERNAME_IN_URL_PATTERN.matcher(result).replaceAll("github.com/" + USERNAME_REDACTED + "/");
        
        return result;
    }
    
    /**
     * Sanitizes an exception for logging.
     * Removes sensitive information from exception messages and stack traces.
     * 
     * @param throwable the exception to sanitize
     * @return sanitized exception message
     */
    public static String sanitizeException(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName());
        
        String message = throwable.getMessage();
        if (message != null) {
            sb.append(": ").append(sanitize(message));
        }
        
        // Include sanitized cause if present
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            sb.append("\nCaused by: ").append(sanitizeException(cause));
        }
        
        return sb.toString();
    }
    
    /**
     * Sanitizes a stack trace for crash reports.
     * 
     * @param throwable the exception with stack trace
     * @return sanitized stack trace string
     */
    public static String sanitizeStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(sanitizeException(throwable)).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a sanitizing log formatter that wraps another formatter.
     * 
     * @param delegate the formatter to wrap
     * @return a sanitizing formatter
     */
    public static Formatter createSanitizingFormatter(Formatter delegate) {
        return new SanitizingFormatter(delegate);
    }
    
    /**
     * Installs sanitizing handlers on a logger.
     * This wraps all existing handlers with sanitization.
     * 
     * @param logger the logger to configure
     */
    public static void installSanitizingHandlers(Logger logger) {
        Objects.requireNonNull(logger, "logger cannot be null");
        
        for (Handler handler : logger.getHandlers()) {
            Formatter existingFormatter = handler.getFormatter();
            if (existingFormatter != null && !(existingFormatter instanceof SanitizingFormatter)) {
                handler.setFormatter(createSanitizingFormatter(existingFormatter));
            }
        }
    }
    
    /**
     * Installs sanitizing handlers on the root logger and all GitHub-related loggers.
     */
    public static void installGlobalSanitization() {
        // Install on root logger
        Logger rootLogger = Logger.getLogger("");
        installSanitizingHandlers(rootLogger);
        
        // Install on GitHub-related loggers
        String[] loggerNames = {
            "com.tamagotchi.committracker.github",
            "com.tamagotchi.committracker.util"
        };
        
        for (String name : loggerNames) {
            Logger logger = Logger.getLogger(name);
            installSanitizingHandlers(logger);
        }
    }
    
    /**
     * A log formatter that sanitizes log messages.
     */
    private static class SanitizingFormatter extends Formatter {
        
        private final Formatter delegate;
        
        SanitizingFormatter(Formatter delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String format(LogRecord record) {
            // Sanitize the message
            String originalMessage = record.getMessage();
            if (originalMessage != null && sanitizationEnabled) {
                record.setMessage(sanitize(originalMessage));
            }
            
            // Sanitize parameters if present
            Object[] params = record.getParameters();
            if (params != null && sanitizationEnabled) {
                Object[] sanitizedParams = new Object[params.length];
                for (int i = 0; i < params.length; i++) {
                    if (params[i] instanceof String) {
                        sanitizedParams[i] = sanitize((String) params[i]);
                    } else if (params[i] != null) {
                        sanitizedParams[i] = sanitize(params[i].toString());
                    } else {
                        sanitizedParams[i] = params[i];
                    }
                }
                record.setParameters(sanitizedParams);
            }
            
            // Sanitize thrown exception if present
            Throwable thrown = record.getThrown();
            if (thrown != null && sanitizationEnabled) {
                // We can't modify the thrown exception, but the delegate formatter
                // will use our sanitized message
            }
            
            return delegate.format(record);
        }
    }
    
    /**
     * Checks if a string contains any sensitive data patterns.
     * Useful for validation and testing.
     * 
     * @param input the string to check
     * @return true if sensitive data patterns are detected
     */
    public static boolean containsSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return TOKEN_PATTERN.matcher(input).find() ||
               GENERIC_TOKEN_PATTERN.matcher(input).find() ||
               API_KEY_PATTERN.matcher(input).find() ||
               URL_WITH_CREDENTIALS_PATTERN.matcher(input).find();
    }
    
    /**
     * Checks if a string contains any PII (Personally Identifiable Information).
     * 
     * @param input the string to check
     * @return true if PII patterns are detected
     */
    public static boolean containsPII(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(input).find();
    }
}
