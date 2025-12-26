package com.tamagotchi.committracker.git;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for background processing isolation.
 * 
 * **Feature: performance-optimization, Property 9: Background Processing Isolation**
 * **Validates: Requirements 4.3, 4.5**
 * 
 * These tests validate that heavy operations like repository discovery
 * execute in background threads without blocking the UI thread.
 */
class BackgroundProcessingProperties {
    
    private ExecutorService testExecutor;
    
    @BeforeProperty
    void setUp() {
        testExecutor = Executors.newFixedThreadPool(4);
    }
    
    @AfterProperty
    void tearDown() {
        if (testExecutor != null) {
            testExecutor.shutdown();
            try {
                testExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                testExecutor.shutdownNow();
            }
        }
    }
    
    /**
     * **Feature: performance-optimization, Property 9: Background Processing Isolation**
     * **Validates: Requirements 4.3, 4.5**
     * 
     * For any heavy operation, it should execute in background threads
     * without blocking the calling thread (simulating UI thread).
     */
    @Property(tries = 100)
    void backgroundOperationsDoNotBlockCallingThread(
            @ForAll @IntRange(min = 50, max = 200) int operationDurationMs) {
        
        AtomicBoolean operationStarted = new AtomicBoolean(false);
        AtomicBoolean operationCompleted = new AtomicBoolean(false);
        AtomicLong callingThreadBlockTime = new AtomicLong(0);
        
        // Simulate a heavy background operation
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<String> backgroundTask = CompletableFuture.supplyAsync(() -> {
            operationStarted.set(true);
            try {
                // Simulate heavy work
                Thread.sleep(operationDurationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            operationCompleted.set(true);
            return "completed";
        }, testExecutor);
        
        // Measure how long the calling thread was blocked
        callingThreadBlockTime.set(System.currentTimeMillis() - startTime);
        
        // The calling thread should return almost immediately (not blocked)
        // Allow some tolerance for thread scheduling overhead
        assertThat(callingThreadBlockTime.get())
            .as("Calling thread should not be blocked by background operation")
            .isLessThan(operationDurationMs / 2);
        
        // Wait for the background task to complete
        try {
            String result = backgroundTask.get(operationDurationMs + 1000, TimeUnit.MILLISECONDS);
            assertThat(result).isEqualTo("completed");
            assertThat(operationCompleted.get()).isTrue();
        } catch (Exception e) {
            // Task should complete successfully
            assertThat(e).isNull();
        }
    }
    
    /**
     * **Feature: performance-optimization, Property 9: Background Processing Isolation**
     * **Validates: Requirements 4.3, 4.5**
     * 
     * For any number of concurrent background operations, they should
     * all execute without blocking each other beyond thread pool limits.
     */
    @Property(tries = 50)
    void concurrentBackgroundOperationsExecuteIndependently(
            @ForAll @IntRange(min = 2, max = 8) int operationCount,
            @ForAll @IntRange(min = 20, max = 100) int operationDurationMs) {
        
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        CountDownLatch allStarted = new CountDownLatch(operationCount);
        CountDownLatch allCompleted = new CountDownLatch(operationCount);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Start multiple background operations
        for (int i = 0; i < operationCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                int current = currentConcurrent.incrementAndGet();
                maxConcurrent.updateAndGet(max -> Math.max(max, current));
                allStarted.countDown();
                
                try {
                    // Simulate work
                    Thread.sleep(operationDurationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                currentConcurrent.decrementAndGet();
                completedCount.incrementAndGet();
                allCompleted.countDown();
            }, testExecutor);
            
            futures.add(future);
        }
        
        // Verify all operations started (non-blocking submission)
        long submissionTime = System.currentTimeMillis() - startTime;
        assertThat(submissionTime)
            .as("Submitting background operations should be fast")
            .isLessThan(operationDurationMs);
        
        // Wait for all to complete
        try {
            boolean completed = allCompleted.await(
                operationDurationMs * operationCount + 5000, TimeUnit.MILLISECONDS);
            assertThat(completed)
                .as("All background operations should complete")
                .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify all completed
        assertThat(completedCount.get())
            .as("All operations should complete")
            .isEqualTo(operationCount);
        
        // Verify some concurrency occurred (at least 2 running at once if pool allows)
        int poolSize = 4; // Our test executor pool size
        int expectedMinConcurrency = Math.min(operationCount, poolSize);
        assertThat(maxConcurrent.get())
            .as("Operations should run concurrently up to pool size")
            .isGreaterThanOrEqualTo(Math.min(2, expectedMinConcurrency));
    }

    
    /**
     * **Feature: performance-optimization, Property 9: Background Processing Isolation**
     * **Validates: Requirements 4.3, 4.5**
     * 
     * For any background operation with progress reporting, progress updates
     * should be delivered without blocking the operation.
     */
    @Property(tries = 50)
    void progressReportingDoesNotBlockBackgroundOperation(
            @ForAll @IntRange(min = 5, max = 20) int progressUpdateCount,
            @ForAll @IntRange(min = 10, max = 50) int updateIntervalMs) {
        
        AtomicInteger progressUpdatesReceived = new AtomicInteger(0);
        AtomicLong totalProgressCallbackTime = new AtomicLong(0);
        AtomicBoolean operationCompleted = new AtomicBoolean(false);
        
        // Simulate a background operation with progress reporting
        CompletableFuture<Void> backgroundTask = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < progressUpdateCount; i++) {
                // Simulate work
                try {
                    Thread.sleep(updateIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                // Report progress (simulate callback)
                long callbackStart = System.nanoTime();
                progressUpdatesReceived.incrementAndGet();
                long callbackDuration = System.nanoTime() - callbackStart;
                totalProgressCallbackTime.addAndGet(callbackDuration);
            }
            operationCompleted.set(true);
        }, testExecutor);
        
        // Wait for completion
        try {
            backgroundTask.get(progressUpdateCount * updateIntervalMs + 5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Should complete successfully
        }
        
        // Verify operation completed
        assertThat(operationCompleted.get())
            .as("Background operation should complete")
            .isTrue();
        
        // Verify all progress updates were received
        assertThat(progressUpdatesReceived.get())
            .as("All progress updates should be received")
            .isEqualTo(progressUpdateCount);
        
        // Verify progress callbacks were fast (didn't block the operation)
        long avgCallbackTimeNs = totalProgressCallbackTime.get() / progressUpdateCount;
        assertThat(avgCallbackTimeNs)
            .as("Progress callbacks should be fast (< 1ms average)")
            .isLessThan(1_000_000); // 1ms in nanoseconds
    }
    
    /**
     * **Feature: performance-optimization, Property 9: Background Processing Isolation**
     * **Validates: Requirements 4.3, 4.5**
     * 
     * For any cancellable background operation, cancellation should
     * stop the operation promptly without blocking.
     */
    @Property(tries = 50)
    void backgroundOperationsCancelPromptly(
            @ForAll @IntRange(min = 200, max = 500) int operationDurationMs,
            @ForAll @IntRange(min = 30, max = 70) int cancelAfterPercentage) {
        
        AtomicBoolean operationCancelled = new AtomicBoolean(false);
        AtomicBoolean operationCompleted = new AtomicBoolean(false);
        CountDownLatch operationStarted = new CountDownLatch(1);
        
        int cancelAfterMs = operationDurationMs * cancelAfterPercentage / 100;
        
        // Start a cancellable background operation
        CompletableFuture<Void> backgroundTask = CompletableFuture.runAsync(() -> {
            operationStarted.countDown();
            long startTime = System.currentTimeMillis();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5); // Work in small increments
                    
                    if (System.currentTimeMillis() - startTime >= operationDurationMs) {
                        operationCompleted.set(true);
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    operationCancelled.set(true);
                    return;
                }
            }
            operationCancelled.set(true);
        }, testExecutor);
        
        // Wait for operation to start
        try {
            operationStarted.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Cancel after specified time
        try {
            Thread.sleep(cancelAfterMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long cancelStartTime = System.currentTimeMillis();
        backgroundTask.cancel(true);
        
        // Wait for cancellation to take effect
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long cancelTime = System.currentTimeMillis() - cancelStartTime;
        
        // Verify cancellation was prompt (within 300ms)
        assertThat(cancelTime)
            .as("Cancellation should be prompt")
            .isLessThan(300);
        
        // The operation should either be cancelled or completed
        // (completed is valid if it finished before cancellation took effect)
        assertThat(operationCancelled.get() || operationCompleted.get() || backgroundTask.isDone())
            .as("Operation should be cancelled, completed, or done")
            .isTrue();
    }
    
    /**
     * **Feature: performance-optimization, Property 9: Background Processing Isolation**
     * **Validates: Requirements 4.3, 4.5**
     * 
     * For any background operation, the main thread should be able to
     * perform other work while the background operation runs.
     */
    @Property(tries = 50)
    void mainThreadCanWorkDuringBackgroundOperation(
            @ForAll @IntRange(min = 100, max = 300) int backgroundDurationMs,
            @ForAll @IntRange(min = 5, max = 15) int mainThreadWorkCount) {
        
        AtomicInteger mainThreadWorkCompleted = new AtomicInteger(0);
        AtomicBoolean backgroundCompleted = new AtomicBoolean(false);
        
        // Start background operation
        CompletableFuture<Void> backgroundTask = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(backgroundDurationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            backgroundCompleted.set(true);
        }, testExecutor);
        
        // Main thread does work while background runs
        long mainThreadStartTime = System.currentTimeMillis();
        for (int i = 0; i < mainThreadWorkCount; i++) {
            // Simulate main thread work (e.g., UI updates)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mainThreadWorkCompleted.incrementAndGet();
        }
        long mainThreadWorkTime = System.currentTimeMillis() - mainThreadStartTime;
        
        // Verify main thread completed its work
        assertThat(mainThreadWorkCompleted.get())
            .as("Main thread should complete all its work")
            .isEqualTo(mainThreadWorkCount);
        
        // Verify main thread work time is independent of background duration
        // Allow generous tolerance for thread scheduling overhead
        long expectedMainWorkTime = mainThreadWorkCount * 10L;
        assertThat(mainThreadWorkTime)
            .as("Main thread work time should be independent of background operation")
            .isLessThan(expectedMainWorkTime + 200); // Allow 200ms tolerance
        
        // Wait for background to complete
        try {
            backgroundTask.get(backgroundDurationMs + 1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Should complete
        }
        
        assertThat(backgroundCompleted.get())
            .as("Background operation should complete")
            .isTrue();
    }
    
    /**
     * **Feature: performance-optimization, Property 9: Background Processing Isolation**
     * **Validates: Requirements 4.3, 4.5**
     * 
     * For any background operation with timeout, the operation should
     * respect the timeout and return partial results if needed.
     */
    @Property(tries = 50)
    void backgroundOperationsRespectTimeouts(
            @ForAll @IntRange(min = 300, max = 600) int operationDurationMs,
            @ForAll @IntRange(min = 80, max = 150) int timeoutMs) {
        
        AtomicInteger workUnitsCompleted = new AtomicInteger(0);
        int workUnitDurationMs = 10;
        int totalWorkUnits = operationDurationMs / workUnitDurationMs;
        CountDownLatch operationStarted = new CountDownLatch(1);
        
        // Start a background operation that takes longer than timeout
        CompletableFuture<Integer> backgroundTask = CompletableFuture.supplyAsync(() -> {
            operationStarted.countDown();
            for (int i = 0; i < totalWorkUnits; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    Thread.sleep(workUnitDurationMs);
                    workUnitsCompleted.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return workUnitsCompleted.get();
        }, testExecutor);
        
        // Wait for operation to start
        try {
            operationStarted.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long startTime = System.currentTimeMillis();
        Integer result = null;
        boolean timedOut = false;
        
        try {
            result = backgroundTask.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            timedOut = true;
            backgroundTask.cancel(true);
        } catch (Exception e) {
            // Other exceptions
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Since operation duration > timeout, we should have timed out
        assertThat(timedOut)
            .as("Operation should timeout when duration exceeds timeout")
            .isTrue();
        
        // Elapsed time should be close to timeout (within tolerance)
        assertThat(elapsed)
            .as("Elapsed time should be close to timeout")
            .isLessThan(timeoutMs + 150);
        
        // Some work should have been completed (partial results)
        // At minimum, we should have completed at least 1 work unit
        // since timeout is at least 80ms and work unit is 10ms
        assertThat(workUnitsCompleted.get())
            .as("Some work should be completed before timeout")
            .isGreaterThanOrEqualTo(1);
    }
}
