package com.tamagotchi.committracker.domain;

import com.tamagotchi.committracker.config.AppConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Domain model that stores and manages commit history data for Pokemon state calculations.
 * Optimized for memory usage with configurable limits and automatic pruning.
 */
public class CommitHistory {
    private final LinkedList<Commit> recentCommits; // Use LinkedList for efficient removal from head
    private final Map<LocalDate, Integer> dailyCommitCounts;
    private LocalDateTime lastCommitTime;
    private int currentStreak;
    private double averageCommitsPerDay;
    
    public CommitHistory() {
        this.recentCommits = new LinkedList<>();
        this.dailyCommitCounts = new HashMap<>();
        this.currentStreak = 0;
        this.averageCommitsPerDay = 0.0;
    }
    
    public CommitHistory(List<Commit> recentCommits, Map<LocalDate, Integer> dailyCommitCounts,
                         LocalDateTime lastCommitTime, int currentStreak, double averageCommitsPerDay) {
        this.recentCommits = recentCommits != null ? new LinkedList<>(recentCommits) : new LinkedList<>();
        this.dailyCommitCounts = dailyCommitCounts != null ? new HashMap<>(dailyCommitCounts) : new HashMap<>();
        this.lastCommitTime = lastCommitTime;
        this.currentStreak = currentStreak;
        this.averageCommitsPerDay = averageCommitsPerDay;
        
        // Ensure we don't exceed memory limits on construction
        pruneOldCommits();
    }
    
    public List<Commit> getRecentCommits() {
        return new ArrayList<>(recentCommits);
    }
    
    public void setRecentCommits(List<Commit> recentCommits) {
        this.recentCommits.clear();
        if (recentCommits != null) {
            this.recentCommits.addAll(recentCommits);
            pruneOldCommits();
        }
    }
    
    public void addCommit(Commit commit) {
        if (commit != null) {
            recentCommits.addLast(commit);
            updateDailyCommitCounts(commit);
            updateLastCommitTime(commit);
            
            // Prune old commits to prevent memory leaks
            pruneOldCommits();
        }
    }
    
    /**
     * Prunes old commits to keep memory usage under control.
     * Removes commits beyond the configured limit, keeping the most recent ones.
     */
    private void pruneOldCommits() {
        int limit = AppConfig.getCommitHistoryLimit();
        while (recentCommits.size() > limit) {
            recentCommits.removeFirst(); // Remove oldest commits
        }
        
        // Also prune daily commit counts older than 90 days to prevent unbounded growth
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        dailyCommitCounts.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoffDate));
    }
    
    /**
     * Gets recent commits with optional limit for UI display.
     * @param limit Maximum number of commits to return (0 for all)
     * @return List of recent commits, limited if specified
     */
    public List<Commit> getRecentCommits(int limit) {
        if (limit <= 0 || limit >= recentCommits.size()) {
            return new ArrayList<>(recentCommits);
        }
        
        // Return the most recent commits up to the limit
        List<Commit> limited = new ArrayList<>();
        Iterator<Commit> iterator = recentCommits.descendingIterator();
        int count = 0;
        while (iterator.hasNext() && count < limit) {
            limited.add(iterator.next());
            count++;
        }
        return limited;
    }
    
    public Map<LocalDate, Integer> getDailyCommitCounts() {
        return new HashMap<>(dailyCommitCounts);
    }
    
    public void setDailyCommitCounts(Map<LocalDate, Integer> dailyCommitCounts) {
        this.dailyCommitCounts.clear();
        if (dailyCommitCounts != null) {
            this.dailyCommitCounts.putAll(dailyCommitCounts);
        }
    }
    
    public LocalDateTime getLastCommitTime() {
        return lastCommitTime;
    }
    
    public void setLastCommitTime(LocalDateTime lastCommitTime) {
        this.lastCommitTime = lastCommitTime;
    }
    
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = Math.max(0, currentStreak);
    }
    
    public double getAverageCommitsPerDay() {
        return averageCommitsPerDay;
    }
    
    public void setAverageCommitsPerDay(double averageCommitsPerDay) {
        this.averageCommitsPerDay = Math.max(0.0, averageCommitsPerDay);
    }
    
    /**
     * Gets the current memory usage statistics for monitoring.
     * @return Map containing memory usage information
     */
    public Map<String, Integer> getMemoryUsageStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("recentCommitsCount", recentCommits.size());
        stats.put("dailyCommitCountsSize", dailyCommitCounts.size());
        stats.put("memoryLimit", AppConfig.getCommitHistoryLimit());
        return stats;
    }
    
    /**
     * Calculates the current commit streak based on daily commit counts.
     */
    public void calculateCurrentStreak() {
        if (dailyCommitCounts.isEmpty()) {
            currentStreak = 0;
            return;
        }
        
        LocalDate today = LocalDate.now();
        int streak = 0;
        
        // Check if there were commits today or yesterday (to account for timezone differences)
        if (dailyCommitCounts.getOrDefault(today, 0) > 0 || 
            dailyCommitCounts.getOrDefault(today.minusDays(1), 0) > 0) {
            
            LocalDate checkDate = today;
            while (dailyCommitCounts.getOrDefault(checkDate, 0) > 0) {
                streak++;
                checkDate = checkDate.minusDays(1);
            }
        }
        
        currentStreak = streak;
    }
    
    /**
     * Calculates the average commits per day over the last 30 days.
     */
    public void calculateAverageCommitsPerDay() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate today = LocalDate.now();
        
        int totalCommits = 0;
        int daysWithData = 0;
        
        for (LocalDate date = thirtyDaysAgo; !date.isAfter(today); date = date.plusDays(1)) {
            Integer commits = dailyCommitCounts.get(date);
            if (commits != null) {
                totalCommits += commits;
                daysWithData++;
            }
        }
        
        averageCommitsPerDay = daysWithData > 0 ? (double) totalCommits / daysWithData : 0.0;
    }
    
    private void updateDailyCommitCounts(Commit commit) {
        if (commit.getTimestamp() != null) {
            LocalDate commitDate = commit.getTimestamp().toLocalDate();
            dailyCommitCounts.merge(commitDate, 1, Integer::sum);
        }
    }
    
    private void updateLastCommitTime(Commit commit) {
        if (commit.getTimestamp() != null && 
            (lastCommitTime == null || commit.getTimestamp().isAfter(lastCommitTime))) {
            lastCommitTime = commit.getTimestamp();
        }
    }
    
    @Override
    public String toString() {
        return "CommitHistory{" +
               "recentCommitsCount=" + recentCommits.size() +
               ", dailyCommitCountsSize=" + dailyCommitCounts.size() +
               ", lastCommitTime=" + lastCommitTime +
               ", currentStreak=" + currentStreak +
               ", averageCommitsPerDay=" + averageCommitsPerDay +
               ", memoryLimit=" + AppConfig.getCommitHistoryLimit() +
               '}';
    }
}
