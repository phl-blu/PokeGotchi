package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.AuthenticationType;
import com.tamagotchi.committracker.domain.Repository;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.util.ResourceManager;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Discovers and monitors Git repositories on the user's system.
 * Handles authentication using existing Git credentials.
 * Supports background discovery with progress reporting for UI responsiveness.
 * 
 * Requirements: 4.3, 4.5 - Background repository discovery and progress reporting
 */
public class RepositoryScanner {
    private static final Logger logger = Logger.getLogger(RepositoryScanner.class.getName());
    
    // Thread pool configuration
    private static final int SCAN_THREAD_POOL_SIZE = 4;
    private static final int SCAN_TIMEOUT_SECONDS = 30;
    
    private final Map<Path, Repository> discoveredRepositories = new ConcurrentHashMap<>();
    private final Set<Path> searchPaths = new HashSet<>();
    private final ExecutorService scanExecutor;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final AtomicInteger scannedDirectories = new AtomicInteger(0);
    private final AtomicInteger totalDirectories = new AtomicInteger(0);
    private volatile boolean shutdownRequested = false;
    
    // Progress listener for UI updates
    private Consumer<ScanProgress> progressListener;
    
    /**
     * Progress information for repository scanning.
     */
    public static class ScanProgress {
        private final int scannedCount;
        private final int totalCount;
        private final int repositoriesFound;
        private final String currentPath;
        private final boolean isComplete;
        
        public ScanProgress(int scannedCount, int totalCount, int repositoriesFound, 
                          String currentPath, boolean isComplete) {
            this.scannedCount = scannedCount;
            this.totalCount = totalCount;
            this.repositoriesFound = repositoriesFound;
            this.currentPath = currentPath;
            this.isComplete = isComplete;
        }
        
        public int getScannedCount() { return scannedCount; }
        public int getTotalCount() { return totalCount; }
        public int getRepositoriesFound() { return repositoriesFound; }
        public String getCurrentPath() { return currentPath; }
        public boolean isComplete() { return isComplete; }
        public double getProgressPercentage() {
            return totalCount > 0 ? (double) scannedCount / totalCount * 100 : 0;
        }
    }
    
    public RepositoryScanner() {
        initializeSearchPaths();
        this.scanExecutor = Executors.newFixedThreadPool(SCAN_THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "RepositoryScanner-Worker");
            t.setDaemon(true);
            return t;
        });
        
        // Register with ResourceManager for cleanup
        ResourceManager.getInstance().registerExecutorService("repositoryScanner-executor", scanExecutor);
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
     * Discovers all Git repositories on the system with configurable limits.
     * @return List of discovered repositories
     */
    public List<Repository> discoverRepositories() {
        logger.info("Starting repository discovery scan");
        discoveredRepositories.clear();
        scannedDirectories.set(0);
        totalDirectories.set(0);
        isScanning.set(true);
        
        int maxDepth = AppConfig.getScanDepth();
        
        try {
            for (Path searchPath : searchPaths) {
                if (shutdownRequested) {
                    break;
                }
                if (Files.exists(searchPath) && Files.isDirectory(searchPath)) {
                    try {
                        scanDirectory(searchPath, 0, maxDepth);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to scan search path: " + searchPath, e);
                    }
                }
            }
        } finally {
            isScanning.set(false);
            notifyProgress(true);
        }
        
        logger.info("Repository discovery completed. Found " + discoveredRepositories.size() + " repositories");
        return new ArrayList<>(discoveredRepositories.values());
    }
    
    /**
     * Discovers repositories in background threads without blocking the UI.
     * 
     * @param callback Called when discovery completes with the list of repositories
     * @return CompletableFuture that completes when discovery is done
     * 
     * Requirements: 4.3 - Perform initial scanning in background threads
     */
    public CompletableFuture<List<Repository>> discoverRepositoriesAsync(
            Consumer<List<Repository>> callback) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<Repository> repos = discoverRepositories();
            if (callback != null && !shutdownRequested) {
                callback.accept(repos);
            }
            return repos;
        }, scanExecutor);
    }
    
    /**
     * Discovers repositories with timeout to prevent hanging.
     * 
     * @param timeoutSeconds Maximum time to wait for discovery
     * @return List of discovered repositories (may be partial if timeout occurs)
     * 
     * Requirements: 3.1 - Use configurable timeouts to prevent hanging operations
     */
    public List<Repository> discoverRepositoriesWithTimeout(int timeoutSeconds) {
        CompletableFuture<List<Repository>> future = discoverRepositoriesAsync(null);
        
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warning("Repository discovery timed out after " + timeoutSeconds + " seconds");
            shutdownRequested = true;
            future.cancel(true);
            // Return whatever we found so far
            return new ArrayList<>(discoveredRepositories.values());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Repository discovery was interrupted");
            return new ArrayList<>(discoveredRepositories.values());
        } catch (ExecutionException e) {
            logger.log(Level.WARNING, "Repository discovery failed", e.getCause());
            return new ArrayList<>(discoveredRepositories.values());
        }
    }
    
    /**
     * Sets a progress listener for UI updates during scanning.
     * 
     * @param listener Consumer that receives progress updates
     * 
     * Requirements: 4.3 - Add progress reporting for long-running operations
     */
    public void setProgressListener(Consumer<ScanProgress> listener) {
        this.progressListener = listener;
    }
    
    /**
     * Notifies the progress listener of current scan status.
     */
    private void notifyProgress(boolean isComplete) {
        if (progressListener != null) {
            ScanProgress progress = new ScanProgress(
                scannedDirectories.get(),
                Math.max(totalDirectories.get(), scannedDirectories.get()),
                discoveredRepositories.size(),
                "",
                isComplete
            );
            progressListener.accept(progress);
        }
    }
    
    /**
     * Notifies the progress listener with current path information.
     */
    private void notifyProgress(String currentPath) {
        if (progressListener != null) {
            ScanProgress progress = new ScanProgress(
                scannedDirectories.get(),
                Math.max(totalDirectories.get(), scannedDirectories.get()),
                discoveredRepositories.size(),
                currentPath,
                false
            );
            progressListener.accept(progress);
        }
    }
    
    /**
     * Checks if scanning is currently in progress.
     */
    public boolean isScanning() {
        return isScanning.get();
    }
    
    /**
     * Cancels any ongoing repository discovery.
     */
    public void cancelDiscovery() {
        shutdownRequested = true;
        isScanning.set(false);
    }
    
    /**
     * Recursively scans a directory for Git repositories with configurable depth.
     */
    private void scanDirectory(Path directory, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth || shutdownRequested) {
            return;
        }
        
        scannedDirectories.incrementAndGet();
        notifyProgress(directory.toString());
        
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
            
            // Scan subdirectories with configurable limits to avoid overwhelming the system
            try (Stream<Path> paths = Files.list(directory)) {
                List<Path> subDirs = paths.filter(Files::isDirectory)
                     .filter(path -> !isHiddenOrIgnored(path))
                     .limit(50) // Limit to 50 subdirectories per level to avoid excessive scanning
                     .toList();
                
                totalDirectories.addAndGet(subDirs.size());
                
                for (Path subDir : subDirs) {
                    if (shutdownRequested) {
                        break;
                    }
                    scanDirectory(subDir, currentDepth + 1, maxDepth);
                }
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
    
    /**
     * Shuts down the repository scanner and releases resources.
     * 
     * Requirements: 1.5 - Clean up all resources including threads
     */
    public void shutdown() {
        shutdownRequested = true;
        isScanning.set(false);
        
        if (scanExecutor != null && !scanExecutor.isShutdown()) {
            scanExecutor.shutdown();
            try {
                if (!scanExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scanExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scanExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Unregister from ResourceManager
        ResourceManager.getInstance().cleanupResource("repositoryScanner-executor");
        
        logger.info("RepositoryScanner shutdown complete");
    }
    
    /**
     * Gets the current scan progress.
     */
    public ScanProgress getCurrentProgress() {
        return new ScanProgress(
            scannedDirectories.get(),
            Math.max(totalDirectories.get(), scannedDirectories.get()),
            discoveredRepositories.size(),
            "",
            !isScanning.get()
        );
    }
}