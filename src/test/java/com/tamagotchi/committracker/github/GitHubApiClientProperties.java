package com.tamagotchi.committracker.github;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for GitHub API Client.
 * 
 * These tests validate the correctness properties defined in the design document
 * for commit baseline consistency and repository list completeness.
 */
public class GitHubApiClientProperties {

    /**
     * **Feature: github-api-integration, Property 4: Commit Baseline Consistency**
     * **Validates: Requirements 4.2, 4.3**
     * 
     * For any commit fetched via the API, the commit timestamp SHALL be 
     * greater than or equal to the commit baseline date.
     */
    @Property(tries = 100)
    void commitBaselineConsistency(
            @ForAll("commitListsWithBaseline") CommitBaselineTestData testData) {
        
        List<GitHubCommit> filteredCommits = testData.commits.stream()
                .filter(c -> c.committedAt() != null && c.committedAt().isAfter(testData.baseline))
                .toList();
        
        for (GitHubCommit commit : filteredCommits) {
            assertNotNull(commit.committedAt(), 
                "Filtered commit must have a non-null timestamp");
            assertTrue(commit.committedAt().isAfter(testData.baseline),
                "Filtered commit timestamp must be after baseline");
        }
        
        for (GitHubCommit commit : testData.commits) {
            if (commit.committedAt() != null && 
                (commit.committedAt().isBefore(testData.baseline) || 
                 commit.committedAt().equals(testData.baseline))) {
                assertFalse(filteredCommits.contains(commit),
                    "Commits at or before baseline should not be in filtered list");
            }
        }
    }


    /**
     * **Feature: github-api-integration, Property 4: Commit Baseline Consistency (Boundary)**
     * **Validates: Requirements 4.2, 4.3**
     */
    @Property(tries = 50)
    void commitBaselineExcludesExactMatch(@ForAll("instantsInPast") Instant baseline) {
        GitHubCommit exactCommit = new GitHubCommit(
                "abc123exact", "Commit at exact baseline", "Test Author",
                "test@example.com", baseline, "test/repo",
                "https://github.com/test/repo/commit/abc123exact");
        
        GitHubCommit afterCommit = new GitHubCommit(
                "def456after", "Commit after baseline", "Test Author",
                "test@example.com", baseline.plusSeconds(1), "test/repo",
                "https://github.com/test/repo/commit/def456after");
        
        GitHubCommit beforeCommit = new GitHubCommit(
                "ghi789before", "Commit before baseline", "Test Author",
                "test@example.com", baseline.minusSeconds(1), "test/repo",
                "https://github.com/test/repo/commit/ghi789before");
        
        List<GitHubCommit> allCommits = List.of(exactCommit, afterCommit, beforeCommit);
        
        List<GitHubCommit> filtered = allCommits.stream()
                .filter(c -> c.committedAt() != null && c.committedAt().isAfter(baseline))
                .toList();
        
        assertFalse(filtered.stream().anyMatch(c -> c.sha().equals("abc123exact")));
        assertTrue(filtered.stream().anyMatch(c -> c.sha().equals("def456after")));
        assertFalse(filtered.stream().anyMatch(c -> c.sha().equals("ghi789before")));
    }

    /**
     * **Feature: github-api-integration, Property 4: Commit Baseline Consistency (Null Handling)**
     * **Validates: Requirements 4.2, 4.3**
     */
    @Property(tries = 50)
    void commitBaselineExcludesNullTimestamps(
            @ForAll("instantsInPast") Instant baseline,
            @ForAll @IntRange(min = 1, max = 10) int nullCount) {
        
        List<GitHubCommit> commits = new ArrayList<>();
        
        for (int i = 0; i < nullCount; i++) {
            commits.add(new GitHubCommit(
                    "null-sha-" + i, "Commit with null timestamp", "Test Author",
                    "test@example.com", null, "test/repo",
                    "https://github.com/test/repo/commit/null-sha-" + i));
        }
        
        commits.add(new GitHubCommit(
                "valid-sha", "Valid commit", "Test Author",
                "test@example.com", baseline.plus(Duration.ofDays(1)), "test/repo",
                "https://github.com/test/repo/commit/valid-sha"));
        
        List<GitHubCommit> filtered = commits.stream()
                .filter(c -> c.committedAt() != null && c.committedAt().isAfter(baseline))
                .toList();
        
        for (GitHubCommit commit : filtered) {
            assertNotNull(commit.committedAt());
        }
        
        assertEquals(1, filtered.size());
        assertEquals("valid-sha", filtered.get(0).sha());
    }


    /**
     * **Feature: github-api-integration, Property 8: Repository List Completeness**
     * **Validates: Requirements 3.1, 3.2**
     */
    @Property(tries = 100)
    void repositoryListCompleteness(
            @ForAll("repositoryLists") List<GitHubRepository> repositories) {
        
        List<GitHubRepository> cached = new ArrayList<>(repositories);
        
        Set<Long> originalIds = repositories.stream()
                .map(GitHubRepository::id)
                .collect(Collectors.toSet());
        
        Set<Long> cachedIds = cached.stream()
                .map(GitHubRepository::id)
                .collect(Collectors.toSet());
        
        assertEquals(originalIds, cachedIds);
        assertEquals(repositories.size(), cached.size());
    }

    /**
     * **Feature: github-api-integration, Property 8: Repository List Completeness (Full Names)**
     * **Validates: Requirements 3.1, 3.2**
     */
    @Property(tries = 100)
    void repositoryListPreservesFullNames(
            @ForAll("repositoryLists") List<GitHubRepository> repositories) {
        
        List<GitHubRepository> cached = new ArrayList<>(repositories);
        
        Set<String> originalFullNames = repositories.stream()
                .map(GitHubRepository::fullName)
                .collect(Collectors.toSet());
        
        Set<String> cachedFullNames = cached.stream()
                .map(GitHubRepository::fullName)
                .collect(Collectors.toSet());
        
        assertEquals(originalFullNames, cachedFullNames);
    }

    /**
     * **Feature: github-api-integration, Property 8: Repository List Completeness (Public and Private)**
     * **Validates: Requirements 3.1, 3.2**
     */
    @Property(tries = 50)
    void repositoryListIncludesPublicAndPrivate(
            @ForAll("mixedVisibilityRepositories") List<GitHubRepository> repositories) {
        
        List<GitHubRepository> cached = new ArrayList<>(repositories);
        
        long originalPublic = repositories.stream().filter(r -> !r.isPrivate()).count();
        long originalPrivate = repositories.stream().filter(GitHubRepository::isPrivate).count();
        
        long cachedPublic = cached.stream().filter(r -> !r.isPrivate()).count();
        long cachedPrivate = cached.stream().filter(GitHubRepository::isPrivate).count();
        
        assertEquals(originalPublic, cachedPublic);
        assertEquals(originalPrivate, cachedPrivate);
    }

    /**
     * **Feature: github-api-integration, Property 8: Repository List Completeness (Owner Extraction)**
     * **Validates: Requirements 3.1, 3.2**
     */
    @Property(tries = 100)
    void repositoryOwnerExtractionConsistency(
            @ForAll("repositoryLists") List<GitHubRepository> repositories) {
        
        for (GitHubRepository repo : repositories) {
            String fullName = repo.fullName();
            
            if (fullName != null && fullName.contains("/")) {
                String extractedOwner = GitHubRepository.extractOwner(fullName);
                String extractedRepoName = GitHubRepository.extractRepoName(fullName);
                
                assertEquals(repo.owner(), extractedOwner);
                assertEquals(repo.name(), extractedRepoName);
                
                String reconstructed = extractedOwner + "/" + extractedRepoName;
                assertEquals(fullName, reconstructed);
            }
        }
    }


    @Provide
    Arbitrary<CommitBaselineTestData> commitListsWithBaseline() {
        Arbitrary<Instant> baselineArb = instantsInPast();
        
        return baselineArb.flatMap(baseline -> {
            Arbitrary<List<GitHubCommit>> commitsArb = Arbitraries.integers()
                    .between(1, 20)
                    .flatMap(count -> generateCommitsAroundBaseline(baseline, count));
            
            return commitsArb.map(commits -> new CommitBaselineTestData(baseline, commits));
        });
    }

    private Arbitrary<List<GitHubCommit>> generateCommitsAroundBaseline(Instant baseline, int count) {
        return Arbitraries.integers()
                .between(-30, 30)
                .list()
                .ofSize(count)
                .map(offsets -> {
                    List<GitHubCommit> commits = new ArrayList<>();
                    int index = 0;
                    for (int dayOffset : offsets) {
                        Instant commitTime = baseline.plus(dayOffset, ChronoUnit.DAYS);
                        commits.add(new GitHubCommit(
                                "sha" + index + "_" + System.nanoTime(),
                                "Test commit " + index, "Test Author",
                                "test@example.com", commitTime, "test/repo",
                                "https://github.com/test/repo/commit/sha" + index));
                        index++;
                    }
                    return commits;
                });
    }

    @Provide
    Arbitrary<Instant> instantsInPast() {
        return Arbitraries.longs()
                .between(1, 365)
                .map(daysAgo -> Instant.now().minus(daysAgo, ChronoUnit.DAYS));
    }

    @Provide
    Arbitrary<List<GitHubRepository>> repositoryLists() {
        return Arbitraries.integers()
                .between(0, 30)
                .flatMap(count -> {
                    if (count == 0) {
                        return Arbitraries.just(List.of());
                    }
                    return generateRepository()
                            .list()
                            .ofSize(count)
                            .map(this::ensureUniqueIds);
                });
    }

    @Provide
    Arbitrary<List<GitHubRepository>> mixedVisibilityRepositories() {
        return Arbitraries.integers()
                .between(2, 20)
                .flatMap(count -> {
                    Arbitrary<GitHubRepository> publicRepo = generateRepository()
                            .map(r -> new GitHubRepository(r.id(), r.name(), r.fullName(), 
                                    r.owner(), false, r.defaultBranch(), r.pushedAt(), r.etag()));
                    Arbitrary<GitHubRepository> privateRepo = generateRepository()
                            .map(r -> new GitHubRepository(r.id() + 10000, r.name() + "priv", 
                                    r.owner() + "/" + r.name() + "priv", r.owner(), true, 
                                    r.defaultBranch(), r.pushedAt(), r.etag()));
                    
                    return Combinators.combine(publicRepo, privateRepo)
                            .as((pub, priv) -> {
                                List<GitHubRepository> repos = new ArrayList<>();
                                repos.add(pub);
                                repos.add(priv);
                                for (int i = 2; i < count; i++) {
                                    boolean isPrivate = i % 2 == 0;
                                    String name = "repo" + i;
                                    repos.add(new GitHubRepository(
                                            i * 1000L + System.nanoTime() % 1000,
                                            name, "owner/" + name, "owner",
                                            isPrivate, "main",
                                            Instant.now().minus(i, ChronoUnit.DAYS), null));
                                }
                                return repos;
                            });
                });
    }


    private Arbitrary<GitHubRepository> generateRepository() {
        Arbitrary<Long> idArb = Arbitraries.longs().between(1, 1_000_000);
        Arbitrary<String> nameArb = Arbitraries.strings()
                .alpha().ofMinLength(3).ofMaxLength(20).map(String::toLowerCase);
        Arbitrary<Boolean> privateArb = Arbitraries.of(true, false);
        Arbitrary<String> branchArb = Arbitraries.of("main", "master", "develop");
        
        return Combinators.combine(idArb, nameArb, privateArb, branchArb)
                .as((id, name, isPrivate, branch) -> new GitHubRepository(
                        id, name, "owner/" + name, "owner",
                        isPrivate, branch,
                        Instant.now().minus(id % 365, ChronoUnit.DAYS), null));
    }

    private List<GitHubRepository> ensureUniqueIds(List<GitHubRepository> repos) {
        List<GitHubRepository> result = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        long nextId = 1;
        int nameCounter = 0;
        
        for (GitHubRepository repo : repos) {
            long id = repo.id();
            String name = repo.name();
            
            while (usedIds.contains(id)) {
                id = nextId++;
            }
            usedIds.add(id);
            
            while (usedNames.contains(name)) {
                name = repo.name() + nameCounter++;
            }
            usedNames.add(name);
            
            String fullName = repo.owner() + "/" + name;
            
            result.add(new GitHubRepository(
                    id, name, fullName, repo.owner(),
                    repo.isPrivate(), repo.defaultBranch(), repo.pushedAt(), repo.etag()));
        }
        
        return result;
    }

    record CommitBaselineTestData(Instant baseline, List<GitHubCommit> commits) {}
}
