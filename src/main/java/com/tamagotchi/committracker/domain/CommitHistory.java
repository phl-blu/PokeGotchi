package com.tamagotchi.committracker.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Domain model that stores and manages commit history data for Pokemon state calculations.
 */
public class CommitHistory {
    private List<Commit> recentCommits;
    private Map<LocalDate, Integer> dailyCommitCounts;
    private LocalDateTime lastCommitTime;
    private int currentStreak;
    private double averageCommitsPerDay;
    
    public CommitHistory() {
        this.recentCommits = new ArrayList<>();
        this.dailyCommitCounts = new HashMap<>();
        this.currentStreak = 0;
        this.averageCommitsPerDay = 0.0;
    }
    
    public CommitHistory(List<Commit> recentCommits, Map<LocalDate, Integer> dailyCommitCounts,
                         LocalDateTime lastCommitTime, int currentStreak, double averageCommitsPerDay) {
        this.recentCommits = recentCommits != null ? new ArrayList<>(recentCommits) : new ArrayList<>();
        this.dailyCommitCounts = dailyCommitCounts != null ? new HashMap<>(dailyCommitCounts) : new HashMap<>();
        this.lastCommitTime = lastCommitTime;
        this.currentStreak = currentStreak;
        this.averageCommitsPerDay = averageCommitsPerDay;
    }
    
    public List<Commit> getRecentCommits() {
        return new ArrayList<>(recentCommits);
    }
    
    public void setRecentCommits(List<Commit> recentCommits) {
        this.recentCommits = recentCommits != null ? new ArrayList<>(recentCommits) : new ArrayList<>();
    }
    
    public void addCommit(Commit commit) {
        if (commit != null) {
            recentCommits.add(commit);
            updateDailyCommitCounts(commit);
            updateLastCommitTime(commit);
        }
    }
    
    public Map<LocalDate, Integer> getDailyCommitCounts() {
        return new HashMap<>(dailyCommitCounts);
    }
    
    public void setDailyCommitCounts(Map<LocalDate, Integer> dailyCommitCounts) {
        this.dailyCommitCounts = dailyCommitCounts != null ? new HashMap<>(dailyCommitCounts) : new HashMap<>();
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
               '}';
    }
}