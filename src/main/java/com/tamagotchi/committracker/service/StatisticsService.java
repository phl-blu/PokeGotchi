package com.tamagotchi.committracker.service;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.util.FileUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating commit statistics and productivity metrics.
 * Provides data for charts and analytics displays.
 * Also tracks and persists evolution history.
 * 
 * Requirements: 5.5
 */
public class StatisticsService {
    
    private static final String EVOLUTION_HISTORY_FILE = "evolution-history.properties";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // In-memory evolution history (persisted to file)
    private List<EvolutionEntry> evolutionHistory = new ArrayList<>();
    
    public StatisticsService() {
        loadEvolutionHistory();
    }
    
    /**
     * Represents daily commit statistics.
     */
    public static class DailyStats {
        private final LocalDate date;
        private final int commitCount;
        private final int xpGained;
        private final Set<String> repositories;
        
        public DailyStats(LocalDate date, int commitCount, int xpGained, Set<String> repositories) {
            this.date = date;
            this.commitCount = commitCount;
            this.xpGained = xpGained;
            this.repositories = new HashSet<>(repositories);
        }
        
        public LocalDate getDate() { return date; }
        public int getCommitCount() { return commitCount; }
        public int getXpGained() { return xpGained; }
        public Set<String> getRepositories() { return repositories; }
        public int getRepositoryCount() { return repositories.size(); }
    }
    
    /**
     * Represents weekly commit statistics.
     */
    public static class WeeklyStats {
        private final LocalDate weekStart;
        private final int commitCount;
        private final int xpGained;
        private final int activeDays;
        private final Set<String> repositories;
        
        public WeeklyStats(LocalDate weekStart, int commitCount, int xpGained, int activeDays, Set<String> repositories) {
            this.weekStart = weekStart;
            this.commitCount = commitCount;
            this.xpGained = xpGained;
            this.activeDays = activeDays;
            this.repositories = new HashSet<>(repositories);
        }
        
        public LocalDate getWeekStart() { return weekStart; }
        public int getCommitCount() { return commitCount; }
        public int getXpGained() { return xpGained; }
        public int getActiveDays() { return activeDays; }
        public Set<String> getRepositories() { return repositories; }
        public int getRepositoryCount() { return repositories.size(); }
    }
    
    /**
     * Represents evolution history entry.
     */
    public static class EvolutionEntry {
        private final LocalDateTime timestamp;
        private final PokemonSpecies species;
        private final EvolutionStage fromStage;
        private final EvolutionStage toStage;
        private final int xpAtEvolution;
        private final int streakAtEvolution;
        
        public EvolutionEntry(LocalDateTime timestamp, PokemonSpecies species, 
                            EvolutionStage fromStage, EvolutionStage toStage, 
                            int xpAtEvolution, int streakAtEvolution) {
            this.timestamp = timestamp;
            this.species = species;
            this.fromStage = fromStage;
            this.toStage = toStage;
            this.xpAtEvolution = xpAtEvolution;
            this.streakAtEvolution = streakAtEvolution;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public PokemonSpecies getSpecies() { return species; }
        public EvolutionStage getFromStage() { return fromStage; }
        public EvolutionStage getToStage() { return toStage; }
        public int getXpAtEvolution() { return xpAtEvolution; }
        public int getStreakAtEvolution() { return streakAtEvolution; }
    }
    
    /**
     * Represents productivity metrics summary.
     */
    public static class ProductivityMetrics {
        private final double averageCommitsPerDay;
        private final double averageXpPerDay;
        private final int longestStreak;
        private final int currentStreak;
        private final int totalCommits;
        private final int totalXp;
        private final int totalRepositories;
        private final LocalDate firstCommitDate;
        private final LocalDate lastCommitDate;
        private final int activeDays;
        
        public ProductivityMetrics(double averageCommitsPerDay, double averageXpPerDay,
                                 int longestStreak, int currentStreak, int totalCommits,
                                 int totalXp, int totalRepositories, LocalDate firstCommitDate,
                                 LocalDate lastCommitDate, int activeDays) {
            this.averageCommitsPerDay = averageCommitsPerDay;
            this.averageXpPerDay = averageXpPerDay;
            this.longestStreak = longestStreak;
            this.currentStreak = currentStreak;
            this.totalCommits = totalCommits;
            this.totalXp = totalXp;
            this.totalRepositories = totalRepositories;
            this.firstCommitDate = firstCommitDate;
            this.lastCommitDate = lastCommitDate;
            this.activeDays = activeDays;
        }
        
        public double getAverageCommitsPerDay() { return averageCommitsPerDay; }
        public double getAverageXpPerDay() { return averageXpPerDay; }
        public int getLongestStreak() { return longestStreak; }
        public int getCurrentStreak() { return currentStreak; }
        public int getTotalCommits() { return totalCommits; }
        public int getTotalXp() { return totalXp; }
        public int getTotalRepositories() { return totalRepositories; }
        public LocalDate getFirstCommitDate() { return firstCommitDate; }
        public LocalDate getLastCommitDate() { return lastCommitDate; }
        public int getActiveDays() { return activeDays; }
    }
    
    /**
     * Calculates daily commit statistics for the last N days.
     * 
     * @param commits List of commits to analyze
     * @param days Number of days to include (default 30)
     * @return List of daily statistics
     */
    public List<DailyStats> calculateDailyStats(List<Commit> commits, int days) {
        if (commits == null || commits.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        Map<LocalDate, List<Commit>> commitsByDate = commits.stream()
            .filter(c -> c.getTimestamp() != null)
            .filter(c -> {
                LocalDate commitDate = c.getTimestamp().toLocalDate();
                return !commitDate.isBefore(startDate) && !commitDate.isAfter(endDate);
            })
            .collect(Collectors.groupingBy(c -> c.getTimestamp().toLocalDate()));
        
        List<DailyStats> dailyStats = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Commit> dayCommits = commitsByDate.getOrDefault(date, new ArrayList<>());
            
            int commitCount = dayCommits.size();
            int xpGained = dayCommits.stream().mapToInt(this::calculateXP).sum();
            Set<String> repositories = dayCommits.stream()
                .map(Commit::getRepositoryName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            dailyStats.add(new DailyStats(date, commitCount, xpGained, repositories));
        }
        
        return dailyStats;
    }
    
    /**
     * Calculates weekly commit statistics for the last N weeks.
     * 
     * @param commits List of commits to analyze
     * @param weeks Number of weeks to include (default 12)
     * @return List of weekly statistics
     */
    public List<WeeklyStats> calculateWeeklyStats(List<Commit> commits, int weeks) {
        if (commits == null || commits.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeks);
        
        List<Commit> filteredCommits = commits.stream()
            .filter(c -> c.getTimestamp() != null)
            .filter(c -> {
                LocalDate commitDate = c.getTimestamp().toLocalDate();
                return !commitDate.isBefore(startDate) && !commitDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        List<WeeklyStats> weeklyStats = new ArrayList<>();
        
        for (int i = 0; i < weeks; i++) {
            LocalDate weekStart = endDate.minusWeeks(i + 1);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            List<Commit> weekCommits = filteredCommits.stream()
                .filter(c -> {
                    LocalDate commitDate = c.getTimestamp().toLocalDate();
                    return !commitDate.isBefore(weekStart) && !commitDate.isAfter(weekEnd);
                })
                .collect(Collectors.toList());
            
            int commitCount = weekCommits.size();
            int xpGained = weekCommits.stream().mapToInt(this::calculateXP).sum();
            
            Set<LocalDate> activeDates = weekCommits.stream()
                .map(c -> c.getTimestamp().toLocalDate())
                .collect(Collectors.toSet());
            int activeDays = activeDates.size();
            
            Set<String> repositories = weekCommits.stream()
                .map(Commit::getRepositoryName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            weeklyStats.add(new WeeklyStats(weekStart, commitCount, xpGained, activeDays, repositories));
        }
        
        Collections.reverse(weeklyStats); // Oldest first
        return weeklyStats;
    }
    
    /**
     * Calculates overall productivity metrics.
     * 
     * @param commits List of commits to analyze
     * @param currentStreak Current commit streak
     * @return Productivity metrics summary
     */
    public ProductivityMetrics calculateProductivityMetrics(List<Commit> commits, int currentStreak) {
        if (commits == null || commits.isEmpty()) {
            return new ProductivityMetrics(0, 0, 0, currentStreak, 0, 0, 0, null, null, 0);
        }
        
        // Basic counts
        int totalCommits = commits.size();
        int totalXp = commits.stream().mapToInt(this::calculateXP).sum();
        
        Set<String> repositories = commits.stream()
            .map(Commit::getRepositoryName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        int totalRepositories = repositories.size();
        
        // Date range
        List<LocalDateTime> timestamps = commits.stream()
            .map(Commit::getTimestamp)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());
        
        LocalDate firstCommitDate = timestamps.isEmpty() ? null : timestamps.get(0).toLocalDate();
        LocalDate lastCommitDate = timestamps.isEmpty() ? null : timestamps.get(timestamps.size() - 1).toLocalDate();
        
        // Active days
        Set<LocalDate> activeDates = commits.stream()
            .map(Commit::getTimestamp)
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .collect(Collectors.toSet());
        int activeDays = activeDates.size();
        
        // Calculate averages
        double averageCommitsPerDay = 0;
        double averageXpPerDay = 0;
        
        if (firstCommitDate != null && lastCommitDate != null) {
            long totalDays = ChronoUnit.DAYS.between(firstCommitDate, lastCommitDate) + 1;
            if (totalDays > 0) {
                averageCommitsPerDay = (double) totalCommits / totalDays;
                averageXpPerDay = (double) totalXp / totalDays;
            }
        }
        
        // Calculate longest streak
        int longestStreak = calculateLongestStreak(commits);
        
        return new ProductivityMetrics(
            averageCommitsPerDay, averageXpPerDay, longestStreak, currentStreak,
            totalCommits, totalXp, totalRepositories, firstCommitDate, lastCommitDate, activeDays
        );
    }
    
    /**
     * Calculates the longest commit streak from commit history.
     * 
     * @param commits List of commits to analyze
     * @return Longest streak in days
     */
    private int calculateLongestStreak(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return 0;
        }
        
        Set<LocalDate> commitDates = commits.stream()
            .map(Commit::getTimestamp)
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .collect(Collectors.toSet());
        
        if (commitDates.isEmpty()) {
            return 0;
        }
        
        List<LocalDate> sortedDates = commitDates.stream()
            .sorted()
            .collect(Collectors.toList());
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate prevDate = sortedDates.get(i - 1);
            LocalDate currentDate = sortedDates.get(i);
            
            if (ChronoUnit.DAYS.between(prevDate, currentDate) == 1) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        
        return longestStreak;
    }
    
    /**
     * Calculates XP for a single commit.
     * Simple implementation - can be enhanced based on commit complexity.
     * Uses the same formula as XPSystem for consistency.
     * 
     * @param commit The commit to calculate XP for
     * @return XP value
     */
    private int calculateXP(Commit commit) {
        if (commit == null || commit.getMessage() == null) {
            return 0;
        }
        
        int baseXP = 6; // Minimum XP for any commit
        int bonusXP = 0; // Up to 4 bonus XP (total max: 10)
        
        String message = commit.getMessage().toLowerCase().trim();
        
        // Bonus for meaningful commit messages (longer than 10 characters)
        if (message.length() > 10) {
            bonusXP += 1;
        }
        
        // Bonus for conventional commit prefixes (high quality commits)
        if (message.startsWith("feat:") || message.startsWith("feature:")) {
            bonusXP += 2; // New features get more XP
        } else if (message.startsWith("fix:") || message.startsWith("bugfix:")) {
            bonusXP += 1; // Bug fixes get moderate XP
        } else if (message.startsWith("docs:") || message.startsWith("doc:")) {
            bonusXP += 1; // Documentation gets some XP
        } else if (message.startsWith("refactor:") || message.startsWith("style:")) {
            bonusXP += 1; // Code improvements get moderate XP
        } else if (message.startsWith("test:") || message.startsWith("tests:")) {
            bonusXP += 1; // Tests are valuable
        }
        
        // Bonus for descriptive messages (contains common development keywords)
        if (message.contains("implement") || message.contains("add") || 
            message.contains("create") || message.contains("update")) {
            bonusXP += 1;
        }
        
        // Ensure XP stays within 6-10 range
        int totalXP = baseXP + bonusXP;
        return Math.min(totalXP, 10); // Cap at 10 XP maximum
    }
    
    /**
     * Records an evolution event and persists it to storage.
     * Call this when a Pokemon evolves.
     * 
     * @param species The Pokemon species that evolved
     * @param fromStage The stage before evolution
     * @param toStage The stage after evolution
     * @param xpAtEvolution XP at the time of evolution
     * @param streakAtEvolution Streak days at the time of evolution
     */
    public void recordEvolution(PokemonSpecies species, EvolutionStage fromStage, 
                               EvolutionStage toStage, int xpAtEvolution, int streakAtEvolution) {
        EvolutionEntry entry = new EvolutionEntry(
            LocalDateTime.now(), species, fromStage, toStage, xpAtEvolution, streakAtEvolution
        );
        evolutionHistory.add(entry);
        saveEvolutionHistory();
        System.out.println("📝 Evolution recorded: " + species + " " + fromStage + " -> " + toStage);
    }
    
    /**
     * Gets the evolution history for display.
     * Returns actual recorded evolution events.
     * 
     * @param species Current Pokemon species (for filtering, can be null for all)
     * @param currentStage Current evolution stage (unused, kept for API compatibility)
     * @return List of evolution entries
     */
    public List<EvolutionEntry> getEvolutionHistory(PokemonSpecies species, EvolutionStage currentStage) {
        if (species == null) {
            return new ArrayList<>(evolutionHistory);
        }
        
        // Filter by species if specified
        return evolutionHistory.stream()
            .filter(e -> e.getSpecies() == species)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all evolution history regardless of species.
     * 
     * @return List of all evolution entries
     */
    public List<EvolutionEntry> getAllEvolutionHistory() {
        return new ArrayList<>(evolutionHistory);
    }
    
    /**
     * Clears evolution history (for testing purposes).
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void clearEvolutionHistory() {
        evolutionHistory.clear();
        saveEvolutionHistory();
        System.out.println("🧪 TESTING: Evolution history cleared");
    }
    
    /**
     * Loads evolution history from persistent storage.
     */
    private void loadEvolutionHistory() {
        try {
            Properties props = FileUtils.loadProperties(EVOLUTION_HISTORY_FILE);
            evolutionHistory.clear();
            
            int count = Integer.parseInt(props.getProperty("evolution.count", "0"));
            
            for (int i = 0; i < count; i++) {
                String prefix = "evolution." + i + ".";
                
                String timestampStr = props.getProperty(prefix + "timestamp");
                String speciesStr = props.getProperty(prefix + "species");
                String fromStageStr = props.getProperty(prefix + "fromStage");
                String toStageStr = props.getProperty(prefix + "toStage");
                String xpStr = props.getProperty(prefix + "xp", "0");
                String streakStr = props.getProperty(prefix + "streak", "0");
                
                if (timestampStr != null && speciesStr != null && fromStageStr != null && toStageStr != null) {
                    try {
                        LocalDateTime timestamp = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER);
                        PokemonSpecies species = PokemonSpecies.valueOf(speciesStr);
                        EvolutionStage fromStage = EvolutionStage.valueOf(fromStageStr);
                        EvolutionStage toStage = EvolutionStage.valueOf(toStageStr);
                        int xp = Integer.parseInt(xpStr);
                        int streak = Integer.parseInt(streakStr);
                        
                        evolutionHistory.add(new EvolutionEntry(timestamp, species, fromStage, toStage, xp, streak));
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to parse evolution entry " + i + ": " + e.getMessage());
                    }
                }
            }
            
            System.out.println("📖 Loaded " + evolutionHistory.size() + " evolution history entries");
            
        } catch (IOException e) {
            // No history file yet - that's fine
            System.out.println("📝 No evolution history file found - starting fresh");
        }
    }
    
    /**
     * Saves evolution history to persistent storage.
     */
    private void saveEvolutionHistory() {
        try {
            Properties props = new Properties();
            props.setProperty("evolution.count", String.valueOf(evolutionHistory.size()));
            
            for (int i = 0; i < evolutionHistory.size(); i++) {
                EvolutionEntry entry = evolutionHistory.get(i);
                String prefix = "evolution." + i + ".";
                
                props.setProperty(prefix + "timestamp", entry.getTimestamp().format(TIMESTAMP_FORMATTER));
                props.setProperty(prefix + "species", entry.getSpecies().name());
                props.setProperty(prefix + "fromStage", entry.getFromStage().name());
                props.setProperty(prefix + "toStage", entry.getToStage().name());
                props.setProperty(prefix + "xp", String.valueOf(entry.getXpAtEvolution()));
                props.setProperty(prefix + "streak", String.valueOf(entry.getStreakAtEvolution()));
            }
            
            FileUtils.saveProperties(props, EVOLUTION_HISTORY_FILE);
            
        } catch (IOException e) {
            System.err.println("⚠️ Failed to save evolution history: " + e.getMessage());
        }
    }
}