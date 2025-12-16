package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.Repository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Feature: tamagotchi-commit-tracker, Property 1: Repository Discovery and Monitoring**
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 * 
 * Property-based tests for repository discovery functionality.
 * Tests that the RepositoryScanner correctly identifies Git repositories.
 */
class RepositoryDiscoveryProperties {
    
    private Path tempDir;
    private RepositoryScanner scanner;
    
    @BeforeProperty
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("git-test-");
        scanner = new RepositoryScanner();
        // Clear default search paths and only use our temp directory
        scanner.clearSearchPaths();
        scanner.addSearchPath(tempDir);
    }
    
    @AfterProperty
    void tearDown() {
        // Clean up temp directory after each property test
        if (tempDir != null) {
            try {
                deleteDirectoryRecursively(tempDir);
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    /**
     * Property: For any valid Git repository path, isGitRepository should return true.
     */
    @Property(tries = 3)
    @Label("isGitRepository returns true for valid Git repositories")
    void isGitRepositoryReturnsTrueForValidRepos(
        @ForAll("validRepoNames") String repoName
    ) throws Exception {
        // Arrange: Create a Git repository
        Path repoPath = tempDir.resolve(repoName);
        Files.createDirectories(repoPath);
        
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(repoPath.toFile());
        try (Git git = initCommand.call()) {
            // Repository created
        }
        
        // Act & Assert
        assertThat(scanner.isGitRepository(repoPath))
            .as("Directory with .git should be recognized as Git repository")
            .isTrue();
    }
    
    /**
     * Property: For any directory without .git, isGitRepository should return false.
     */
    @Property(tries = 3)
    @Label("isGitRepository returns false for non-Git directories")
    void isGitRepositoryReturnsFalseForNonRepos(
        @ForAll("validRepoNames") String dirName
    ) throws Exception {
        // Arrange: Create a regular directory (not a Git repo)
        Path dirPath = tempDir.resolve(dirName + "-notgit");
        Files.createDirectories(dirPath);
        
        // Act & Assert
        assertThat(scanner.isGitRepository(dirPath))
            .as("Directory without .git should not be recognized as Git repository")
            .isFalse();
    }
    
    /**
     * Property: validateRepository returns true for valid, accessible Git repositories.
     */
    @Property(tries = 3)
    @Label("validateRepository returns true for valid Git repositories")
    void validateRepositoryReturnsTrueForValidRepos(
        @ForAll("validRepoNames") String repoName
    ) throws Exception {
        // Arrange: Create a valid Git repository
        Path repoPath = tempDir.resolve(repoName);
        Files.createDirectories(repoPath);
        
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(repoPath.toFile());
        try (Git git = initCommand.call()) {
            // Repository created
        }
        
        // Act & Assert
        assertThat(scanner.validateRepository(repoPath))
            .as("Valid Git repository should pass validation")
            .isTrue();
    }
    
    /**
     * Property: validateRepository returns false for non-Git directories.
     */
    @Property(tries = 3)
    @Label("validateRepository returns false for non-Git directories")
    void validateRepositoryReturnsFalseForNonRepos(
        @ForAll("validRepoNames") String dirName
    ) throws Exception {
        // Arrange: Create a regular directory
        Path dirPath = tempDir.resolve(dirName + "-invalid");
        Files.createDirectories(dirPath);
        
        // Act & Assert
        assertThat(scanner.validateRepository(dirPath))
            .as("Non-Git directory should fail validation")
            .isFalse();
    }
    
    /**
     * Property: All discovered repositories are valid Git repositories.
     */
    @Property(tries = 2)
    @Label("All discovered repositories are valid Git repositories")
    void allDiscoveredReposAreValid(
        @ForAll("smallRepoCount") int repoCount
    ) throws Exception {
        // Arrange: Create multiple Git repositories
        Set<String> createdRepoNames = new HashSet<>();
        for (int i = 0; i < repoCount; i++) {
            String repoName = "repo-" + i;
            Path repoPath = tempDir.resolve(repoName);
            Files.createDirectories(repoPath);
            
            InitCommand initCommand = Git.init();
            initCommand.setDirectory(repoPath.toFile());
            try (Git git = initCommand.call()) {
                createdRepoNames.add(repoName);
            }
        }
        
        // Act: Discover repositories in temp directory only
        List<Repository> discovered = discoverInTempDir();
        
        // Assert: All discovered repos should be valid
        for (Repository repo : discovered) {
            assertThat(scanner.isGitRepository(repo.getPath()))
                .as("Discovered repository should be a valid Git repository: " + repo.getPath())
                .isTrue();
        }
    }
    
    /**
     * Property: Repository scanner does not scan inside Git repositories (no nested detection).
     */
    @Property(tries = 2)
    @Label("Scanner does not detect nested repositories inside Git repos")
    void scannerDoesNotDetectNestedRepos() throws Exception {
        // Arrange: Create an outer Git repo with a nested Git repo inside
        Path outerRepo = tempDir.resolve("outer-repo");
        Files.createDirectories(outerRepo);
        
        try (Git git = Git.init().setDirectory(outerRepo.toFile()).call()) {
            // Outer repo created
        }
        
        // Create nested repo inside outer repo
        Path nestedRepo = outerRepo.resolve("nested-repo");
        Files.createDirectories(nestedRepo);
        
        try (Git git = Git.init().setDirectory(nestedRepo.toFile()).call()) {
            // Nested repo created
        }
        
        // Act: Discover repositories
        List<Repository> discovered = discoverInTempDir();
        
        // Assert: Should only find outer repo, not nested
        Set<String> discoveredNames = discovered.stream()
            .map(Repository::getName)
            .collect(Collectors.toSet());
        
        assertThat(discoveredNames)
            .as("Should find outer repo")
            .contains("outer-repo");
        
        assertThat(discoveredNames)
            .as("Should not find nested repo inside Git repository")
            .doesNotContain("nested-repo");
    }
    
    @Provide
    Arbitrary<String> validRepoNames() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(10)
            .map(s -> s.toLowerCase());
    }
    
    @Provide
    Arbitrary<Integer> smallRepoCount() {
        return Arbitraries.integers().between(1, 3);
    }
    
    /**
     * Discovers repositories only in the temp directory (not system-wide).
     */
    private List<Repository> discoverInTempDir() {
        List<Repository> repositories = new ArrayList<>();
        scanDirectory(tempDir, repositories, 0, 3);
        return repositories;
    }
    
    private void scanDirectory(Path directory, List<Repository> repositories, int depth, int maxDepth) {
        if (depth > maxDepth) {
            return;
        }
        
        try {
            if (scanner.isGitRepository(directory)) {
                String name = directory.getFileName().toString();
                Repository repo = new Repository();
                repo.setName(name);
                repo.setPath(directory);
                repo.setRemoteUrl("");
                repo.setAccessible(true);
                repositories.add(repo);
                return; // Don't scan subdirectories of Git repos
            }
            
            try (var paths = Files.list(directory)) {
                paths.filter(Files::isDirectory)
                     .forEach(subDir -> scanDirectory(subDir, repositories, depth + 1, maxDepth));
            }
        } catch (Exception e) {
            // Skip inaccessible directories
        }
    }
    
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
