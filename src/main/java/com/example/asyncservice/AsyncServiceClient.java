package com.example.asyncservice;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Client for testing the Async Service
 * Demonstrates how to interact with the timeout-based async service
 */
public class AsyncServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AsyncServiceClient.class);
    private final WebClient webClient;

    public AsyncServiceClient() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:8081")
            .build();
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("=== ASYNC SERVICE CLIENT DEMO ===\n");

        AsyncServiceClient client = new AsyncServiceClient();

        // Test with deterministic complexity-based processing times (1-10 scale)
        log.info("Testing service with complexity-based processing times...\n");
        log.info("Complexity mapping: 1→6s, 3→18s, 5→30s, 7→42s, 10→60s\n");

        // Test different complexity levels to see both immediate and async responses
        client.testProcessingRequest("Simple Task", 1);      // ~6s - should complete immediately
        Thread.sleep(1000);

        client.testProcessingRequest("Medium Task", 3);      // ~18s - should complete immediately
        Thread.sleep(1000);

        client.testProcessingRequest("Complex Task", 5);     // ~30s - may timeout to background
        Thread.sleep(1000);

        client.testProcessingRequest("Very Complex Task", 7); // ~42s - will definitely timeout
        Thread.sleep(1000);

        client.testProcessingRequest("Maximum Task", 10);    // ~60s - will definitely timeout

        // Keep the client running to see results
        Thread.sleep(100000); // Wait for potentially long tasks to complete
    }

    public void testProcessingRequest(String data, int complexity) {
        log.info("--- TESTING PROCESSING REQUEST: {} (complexity: {}) ---", data, complexity);

        ProcessRequest request = new ProcessRequest(data, complexity);

        webClient.post()
            .uri("/api/process")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TaskResult.class)
            .subscribe(
                taskResult -> handleTaskResult(taskResult),
                error -> log.error("✗ Request error: {}", error.getMessage())
            );
    }

    private void handleTaskResult(TaskResult taskResult) {
        if (TaskStatus.COMPLETED.equals(taskResult.getStatus())) {
            // Task completed immediately (within 30 seconds)
            log.info("✓ Task completed immediately: {}", taskResult.getTaskId());
            logProcessingResult(taskResult);
        } else if (TaskStatus.PROCESSING.equals(taskResult.getStatus())) {
            // Task is running in background, start polling
            log.info("⏳ Task is processing in background: {}", taskResult.getTaskId());
            pollForResult(taskResult.getTaskId())
                .subscribe(
                    finalResult -> {
                        log.info("✓ Background task completed: {}", finalResult.getTaskId());
                        logProcessingResult(finalResult);
                    },
                    error -> log.error("✗ Polling error for task {}: {}", taskResult.getTaskId(), error.getMessage())
                );
        } else if (TaskStatus.FAILED.equals(taskResult.getStatus())) {
            // Task failed immediately
            log.error("✗ Task failed immediately: {} - {}", taskResult.getTaskId(), taskResult.getErrorMessage());
        }
    }

    private void logProcessingResult(TaskResult taskResult) {
        if (taskResult.getResult() != null) {
            ProcessingResult result = taskResult.getResult();
            log.info("   📋 Processed Data: {}", result.getProcessedData());
            log.info("   📝 Message: {}", result.getMessage());
            log.info("   ⏱️ Timestamp: {}", new java.util.Date(result.getTimestamp()));
            log.info("   🎯 Complexity: {}", result.getComplexity());

            // Calculate actual processing duration
            if (taskResult.getCreatedAt() != null && taskResult.getCompletedAt() != null) {
                Duration duration = Duration.between(taskResult.getCreatedAt(), taskResult.getCompletedAt());
                log.info("   ⌛ Duration: {} seconds", duration.getSeconds());
            }
        } else if (taskResult.getErrorMessage() != null) {
            log.error("   ❌ Error: {}", taskResult.getErrorMessage());
        }
    }

    private Flux<TaskResult> pollForResult(String taskId) {
        return Flux.interval(Duration.ofSeconds(3))
            .doOnNext(tick ->
                log.info("   🔍 Polling for task: {} (attempt {})", taskId, tick + 1))
            .flatMap(tick ->
                webClient.get()
                    .uri("/api/tasks/{taskId}", taskId)
                    .retrieve()
                    .bodyToMono(TaskResult.class)
                    .onErrorReturn(createErrorTaskResult(taskId, "Polling failed"))
            )
            .filter(result -> !TaskStatus.PROCESSING.equals(result.getStatus()))
            .take(1)
            .timeout(Duration.ofMinutes(2)); // Timeout after 2 minutes of polling
    }

    private TaskResult createErrorTaskResult(String taskId, String error) {
        return new TaskResult(taskId, TaskStatus.FAILED, error, LocalDateTime.now(), null);
    }
}
