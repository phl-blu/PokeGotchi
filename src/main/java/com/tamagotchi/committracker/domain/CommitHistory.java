package com.tamagotchi.committracker.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain model that stores and manages commit history data for Pokemon state calculations.
 */
public class CommitHistory {
    private List<Commit> recentCommits;
    private Map<LocalDate, Integer> dailyCommitCounts;
    private LocalDateTime lastCommitTime;
    private int currentStreak;
    private double averageCommitsPerDay;
    
    // TODO: Implement commit history management in task 2
}