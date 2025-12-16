package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.AuthenticationType;
import com.tamagotchi.committracker.domain.Repository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Discovers and monitors Git repositories on the user's system.
 * Handles authentication using existing Git credentials.
 */
public class RepositoryScanner {
    private static final Logger logger = Logger.getLogger(RepositoryScanner.class.getName());
    
    private final Map<Path, Repository> discoveredRepositories = new ConcurrentHashMap<>();
    private final Set<Path> searchPaths = new HashSet<>();
    
    public RepositoryScanner() {
        initializeSearchPaths();
    }
    
    /**
     * Initialize common search paths for Git repositories.
     */
    private void initializeSearchPaths() {
        String userHome = System.getProperty("user.home");
        
        // Common development directories (more focused to avoid scanning entire home directory)
        searchPaths.add(Paths.get(userHome, "Documents"));
        searchPaths.add(Paths.get(userHome, "Projects"));
        searchPaths.add(Paths.get(userHome, "Development"));
        searchPaths.add(Paths.get(userHome, "Code"));
        searchPaths.add(Paths.get(userHome, "workspace"));
        searchPaths.add(Paths.get(userHome, "dev"));
        searchPaths.add(Paths.get(userHome, "src"));
        
        // Add current working directory
        searchPaths.add(Paths.get(System.getProperty("user.dir")));
    }
    
    /**
     * Discovers all Git repositories on the system.
     * @return List of discovered repositories
     */
    public List<Repository> discoverRepositories() {
        logger.info("Starting repository discovery scan");
        discoveredRepositories.clear();
        
        for (Path searchPath : searchPaths) {
            if (Files.exists(searchPath) && Files.isDirectory(searchPath)) {
                try {
                    scanDirectory(searchPath, 0, 3); // Reduced max depth to 3 to avoid deep recursion
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to scan search path: " + searchPath, e);
                }
            }
        }
        
        logger.info("Repository discovery completed. Found " + discoveredRepositories.size() + " repositories");
        return new ArrayList<>(discoveredRepositories.values());
    }
    
    /**
     * Recursively scans a directory for Git repositories.
     */
    private void scanDirectory(Path directory, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            return;
        }
        
        try {
            // Check if current directory is a Git repository
            if (isGitRepository(directory)) {
                Repository repo = createRepositoryFromPath(directory);
                if (repo != null) {
                    discoveredRepositories.put(directory, repo);
                    logger.fine("Found Git repository: " + directory);
                    return; // Don't scan subdirectories of Git repos
                }
            }
            
            // Scan subdirectories with limited parallelism to avoid overwhelming the system
            try (Stream<Path> paths = Files.list(directory)) {
                paths.filter(Files::isDirectory)
                     .filter(path -> !isHiddenOrIgnored(path))
                     .limit(50) // Limit to 50 subdirectories per level to avoid excessive scanning
                     .forEach(subDir -> scanDirectory(subDir, currentDepth + 1, maxDepth));
            }
            
        } catch (IOException | SecurityException e) {
            logger.log(Level.FINE, "Error scanning directory: " + directory, e);
        }
    }
    
    /**
     * Checks if a directory is a Git repository.
     */
    public boolean isGitRepository(Path path) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return false;
        }
        
        Path gitDir = path.resolve(".git");
        return Files.exists(gitDir) && (Files.isDirectory(gitDir) || Files.isRegularFile(gitDir));
    }
    
    /**
     * Validates that a repository is accessible and readable.
     */
    public boolean validateRepository(Path path) {
        // First check if this is actually a Git repository
        if (!isGitRepository(path)) {
            return false;
        }
        
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository jgitRepo = builder
                .setGitDir(path.resolve(".git").toFile())
                .readEnvironment()
                .build();
            
            // Try to read the repository configuration
            Config config = jgitRepo.getConfig();
            jgitRepo.close();
            return true;
            
        } catch (Exception e) {
            logger.log(Level.FINE, "Repository validation failed for: " + path, e);
            return false;
        }
    }
    
    /**
     * Creates a Repository domain object from a file path.
     */
    private Repository createRepositoryFromPath(Path path) {
        try {
            if (!validateRepository(path)) {
                return null;
            }
            
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository jgitRepo = builder
                .setGitDir(path.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
            
            String name = path.getFileName().toString();
            String remoteUrl = getRemoteUrl(jgitRepo);
            AuthenticationType authType = determineAuthenticationType(remoteUrl);
            
            Repository repository = new Repository(
                name,
                path,
                remoteUrl,
                LocalDateTime.now(),
                true,
                authType
            );
            
            jgitRepo.close();
            return repository;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create repository from path: " + path, e);
            return null;
        }
    }
    
    /**
     * Gets the remote URL from a JGit repository.
     */
    private String getRemoteUrl(org.eclipse.jgit.lib.Repository jgitRepo) {
        try {
            Config config = jgitRepo.getConfig();
            String url = config.getString("remote", "origin", "url");
            return url != null ? url : "";
        } catch (Exception e) {
            logger.log(Level.FINE, "Could not determine remote URL", e);
            return "";
        }
    }
    
    /**
     * Determines the authentication type based on the remote URL.
     */
    private AuthenticationType determineAuthenticationType(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isEmpty()) {
            return AuthenticationType.NONE;
        }
        
        if (remoteUrl.startsWith("git@") || remoteUrl.startsWith("ssh://")) {
            return AuthenticationType.SSH_KEY;
        } else if (remoteUrl.startsWith("https://")) {
            return AuthenticationType.HTTPS_TOKEN;
        } else {
            return AuthenticationType.NONE;
        }
    }
    
    /**
     * Checks if a path should be ignored during scanning.
     */
    private boolean isHiddenOrIgnored(Path path) {
        String fileName = path.getFileName().toString();
        
        // Skip hidden directories and common non-development directories
        return fileName.startsWith(".") ||
               fileName.equals("node_modules") ||
               fileName.equals("target") ||
               fileName.equals("build") ||
               fileName.equals("dist") ||
               fileName.equals("out") ||
               fileName.equals("bin") ||
               fileName.equals("obj") ||
               fileName.equals("Debug") ||
               fileName.equals("Release") ||
               fileName.equals("__pycache__") ||
               fileName.equals("venv") ||
               fileName.equals("env");
    }
    
    /**
     * Gets all currently discovered repositories.
     */
    public List<Repository> getDiscoveredRepositories() {
        return new ArrayList<>(discoveredRepositories.values());
    }
    
    /**
     * Adds a custom search path for repository discovery.
     */
    public void addSearchPath(Path path) {
        if (Files.exists(path) && Files.isDirectory(path)) {
            searchPaths.add(path);
        }
    }
    
    /**
     * Removes a search path from repository discovery.
     */
    public void removeSearchPath(Path path) {
        searchPaths.remove(path);
    }
    
    /**
     * Clears all search paths. Useful for testing.
     */
    public void clearSearchPaths() {
        searchPaths.clear();
    }
    
    /**
     * Gets the current search paths. Useful for testing.
     */
    public Set<Path> getSearchPaths() {
        return new HashSet<>(searchPaths);
    }
}