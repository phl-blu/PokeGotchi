package com.tamagotchi.committracker.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages network connectivity detection and monitoring.
 * Provides offline/online state tracking and notifies listeners of connectivity changes.
 * 
 * Requirements: 7.1, 7.2, 7.3
 * - 7.1: Continue displaying cached commit data when offline
 * - 7.2: Display offline indicator without disrupting Pokemon display
 * - 7.3: Sync missed commits automatically when connectivity is restored
 */
public class NetworkConnectivityManager {
    
    private static final Logger LOGGER = Logger.getLogger(NetworkConnectivityManager.class.getName());
    
    // GitHub API endpoint for connectivity check
    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    
    // Default check interval
    private static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(30);
    private static final Duration OFFLINE_CHECK_INTERVAL = Duration.ofSeconds(10);
    
    private final ScheduledExecutorService scheduler;
    private final List<ConnectivityListener> listeners;
    private final AtomicBoolean isOnline;
    private final AtomicReference<Instant> lastOnlineTime;
    private final AtomicReference<Instant> lastOfflineTime;
    private final AtomicBoolean isMonitoring;
    
    private ScheduledFuture<?> monitoringTask;
    private Duration checkInterval;
    
    /**
     * Listener interface for connectivity changes.
     */
    public interface ConnectivityListener {
        /**
         * Called when connectivity status changes.
         * 
         * @param online true if now online, false if now offline
         */
        void onConnectivityChanged(boolean online);
        
        /**
         * Called when connectivity is restored after being offline.
         * 
         * @param offlineDuration how long the system was offline
         */
        default void onConnectivityRestored(Duration offlineDuration) {}
    }

    
    /**
     * Creates a NetworkConnectivityManager.
     */
    public NetworkConnectivityManager() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NetworkConnectivityMonitor");
            t.setDaemon(true);
            return t;
        });
        this.listeners = Collections.synchronizedList(new ArrayList<>());
        this.isOnline = new AtomicBoolean(true); // Assume online initially
        this.lastOnlineTime = new AtomicReference<>(Instant.now());
        this.lastOfflineTime = new AtomicReference<>(null);
        this.isMonitoring = new AtomicBoolean(false);
        this.checkInterval = DEFAULT_CHECK_INTERVAL;
    }
    
    /**
     * Checks if the system is currently online.
     * 
     * @return true if online
     */
    public boolean isOnline() {
        return isOnline.get();
    }
    
    /**
     * Checks if the system is currently offline.
     * 
     * @return true if offline
     */
    public boolean isOffline() {
        return !isOnline.get();
    }
    
    /**
     * Gets the time when the system was last online.
     * 
     * @return the last online time
     */
    public Instant getLastOnlineTime() {
        return lastOnlineTime.get();
    }
    
    /**
     * Gets the time when the system went offline (if currently offline).
     * 
     * @return the offline start time, or null if online
     */
    public Instant getOfflineStartTime() {
        return lastOfflineTime.get();
    }
    
    /**
     * Gets how long the system has been offline.
     * 
     * @return duration offline, or Duration.ZERO if online
     */
    public Duration getOfflineDuration() {
        Instant offlineStart = lastOfflineTime.get();
        if (offlineStart == null || isOnline.get()) {
            return Duration.ZERO;
        }
        return Duration.between(offlineStart, Instant.now());
    }
    
    /**
     * Performs an immediate connectivity check.
     * 
     * @return true if online
     */
    public boolean checkConnectivity() {
        boolean online = performConnectivityCheck();
        updateConnectivityState(online);
        return online;
    }
    
    /**
     * Starts monitoring network connectivity.
     * Will periodically check connectivity and notify listeners of changes.
     */
    public void startMonitoring() {
        if (isMonitoring.getAndSet(true)) {
            LOGGER.warning("Connectivity monitoring already started");
            return;
        }
        
        // Perform initial check
        checkConnectivity();
        
        // Schedule periodic checks
        scheduleNextCheck();
        
        LOGGER.info("Network connectivity monitoring started");
    }
    
    /**
     * Stops monitoring network connectivity.
     */
    public void stopMonitoring() {
        isMonitoring.set(false);
        
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
            monitoringTask = null;
        }
        
        LOGGER.info("Network connectivity monitoring stopped");
    }
    
    /**
     * Checks if monitoring is active.
     * 
     * @return true if monitoring
     */
    public boolean isMonitoring() {
        return isMonitoring.get();
    }

    
    /**
     * Adds a connectivity listener.
     * 
     * @param listener the listener to add
     */
    public void addListener(ConnectivityListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a connectivity listener.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(ConnectivityListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Sets the check interval for connectivity monitoring.
     * 
     * @param interval the interval between checks
     */
    public void setCheckInterval(Duration interval) {
        this.checkInterval = interval != null ? interval : DEFAULT_CHECK_INTERVAL;
    }
    
    /**
     * Shuts down the connectivity manager.
     */
    public void shutdown() {
        stopMonitoring();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        listeners.clear();
        LOGGER.info("NetworkConnectivityManager shutdown complete");
    }
    
    // ==================== Private Methods ====================
    
    /**
     * Performs the actual connectivity check.
     * Tries to connect to GitHub API to verify connectivity.
     */
    private boolean performConnectivityCheck() {
        // First, try a simple socket connection to GitHub
        if (!checkSocketConnection("api.github.com", 443)) {
            LOGGER.fine("Socket connection to GitHub failed");
            return false;
        }
        
        // Then verify with an HTTP request
        return checkHttpConnection();
    }
    
    /**
     * Checks connectivity using a socket connection.
     */
    private boolean checkSocketConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            LOGGER.fine("Socket connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks connectivity using an HTTP request to GitHub API.
     */
    private boolean checkHttpConnection() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(GITHUB_API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", "Pokemon-Commit-Tracker");
            
            int responseCode = connection.getResponseCode();
            // Any response (even 403 rate limit) means we're online
            return responseCode > 0;
        } catch (IOException e) {
            LOGGER.fine("HTTP connection failed: " + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    
    /**
     * Updates the connectivity state and notifies listeners if changed.
     */
    private void updateConnectivityState(boolean online) {
        boolean wasOnline = isOnline.getAndSet(online);
        
        if (wasOnline != online) {
            // State changed
            if (online) {
                // Came back online
                Instant offlineStart = lastOfflineTime.get();
                lastOnlineTime.set(Instant.now());
                lastOfflineTime.set(null);
                
                Duration offlineDuration = offlineStart != null 
                    ? Duration.between(offlineStart, Instant.now()) 
                    : Duration.ZERO;
                
                LOGGER.info("Network connectivity restored after " + 
                           offlineDuration.toMinutes() + " minutes offline");
                
                notifyConnectivityRestored(offlineDuration);
            } else {
                // Went offline
                lastOfflineTime.set(Instant.now());
                LOGGER.warning("Network connectivity lost");
            }
            
            notifyConnectivityChanged(online);
        }
    }
    
    /**
     * Schedules the next connectivity check.
     */
    private void scheduleNextCheck() {
        if (!isMonitoring.get()) {
            return;
        }
        
        // Use shorter interval when offline to detect recovery faster
        Duration interval = isOnline.get() ? checkInterval : OFFLINE_CHECK_INTERVAL;
        
        monitoringTask = scheduler.schedule(() -> {
            try {
                boolean online = performConnectivityCheck();
                updateConnectivityState(online);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Connectivity check failed", e);
                updateConnectivityState(false);
            } finally {
                scheduleNextCheck();
            }
        }, interval.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Notifies listeners of connectivity change.
     */
    private void notifyConnectivityChanged(boolean online) {
        for (ConnectivityListener listener : listeners) {
            try {
                listener.onConnectivityChanged(online);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on connectivity change", e);
            }
        }
    }
    
    /**
     * Notifies listeners of connectivity restoration.
     */
    private void notifyConnectivityRestored(Duration offlineDuration) {
        for (ConnectivityListener listener : listeners) {
            try {
                listener.onConnectivityRestored(offlineDuration);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on connectivity restored", e);
            }
        }
    }
    
    /**
     * Gets the current connectivity status for display.
     * 
     * @return connectivity status
     */
    public ConnectivityStatus getStatus() {
        return new ConnectivityStatus(
            isOnline.get(),
            lastOnlineTime.get(),
            lastOfflineTime.get(),
            getOfflineDuration()
        );
    }
    
    /**
     * Connectivity status for display purposes.
     */
    public record ConnectivityStatus(
        boolean online,
        Instant lastOnlineTime,
        Instant offlineStartTime,
        Duration offlineDuration
    ) {
        public String getDisplayText() {
            if (online) {
                return "Online";
            } else {
                long minutes = offlineDuration.toMinutes();
                if (minutes < 1) {
                    return "Offline (just now)";
                } else if (minutes < 60) {
                    return "Offline (" + minutes + " min)";
                } else {
                    return "Offline (" + (minutes / 60) + " hr)";
                }
            }
        }
    }
}
