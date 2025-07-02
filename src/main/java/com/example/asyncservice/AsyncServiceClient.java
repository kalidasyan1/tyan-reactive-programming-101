package com.example.asyncservice;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Client for testing the Async Service
 * Demonstrates how to interact with the handle-based async service
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

        // Test immediate processing (< 30 seconds)
        client.testImmediateProcessing();

        // Test async processing with handle (>= 30 seconds)
        client.testAsyncProcessingWithHandle();

        // Keep the client running to see results
        Thread.sleep(45000); // Wait for long task to complete
    }

    public void testImmediateProcessing() {
        log.info("--- TESTING IMMEDIATE PROCESSING ---");

        ProcessRequest request = new ProcessRequest("Quick Task", 5);

        webClient.post()
            .uri("/api/process")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Object.class)
            .subscribe(
                response -> log.info("✓ Immediate response: {}", response),
                error -> log.error("✗ Immediate error: {}", error.getMessage())
            );
    }

    public void testAsyncProcessingWithHandle() {
        log.info("\n--- TESTING ASYNC PROCESSING WITH HANDLE ---");

        ProcessRequest request = new ProcessRequest("Long Running Task", 35);

        webClient.post()
            .uri("/api/process")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(HandleResponse.class)
            .doOnNext(handleResponse -> {
                log.info("✓ Received handle: {}", handleResponse.getTaskId());

                // Poll for results
                pollForResult(handleResponse.getTaskId())
                    .subscribe(
                        finalResult -> log.info("✓ Final result: {}", finalResult),
                        error -> log.error("✗ Polling error: {}", error.getMessage())
                    );
            })
            .subscribe();
    }

    private Flux<TaskResult> pollForResult(String taskId) {
        return Flux.interval(Duration.ofSeconds(2))
            .doOnNext(tick ->
                log.info("Polling for task: {} (attempt {})", taskId, tick + 1))
            .flatMap(tick ->
                webClient.get()
                    .uri("/api/tasks/{taskId}", taskId)
                    .retrieve()
                    .bodyToMono(TaskResult.class)
                    .onErrorReturn(createPendingTaskResult(taskId))
            )
            .filter(result -> !"PENDING".equals(result.getStatus()))
            .take(1);
    }

    private TaskResult createPendingTaskResult(String taskId) {
        TaskResult result = new TaskResult();
        result.setTaskId(taskId);
        result.setStatus("PENDING");
        result.setResult(null);
        return result;
    }

    // Data classes (copied from the service for simplicity)
    public static class ProcessRequest {
        private String data;
        private int processingTimeSeconds;

        public ProcessRequest() {}

        public ProcessRequest(String data, int processingTimeSeconds) {
            this.data = data;
            this.processingTimeSeconds = processingTimeSeconds;
        }

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public int getProcessingTimeSeconds() { return processingTimeSeconds; }
        public void setProcessingTimeSeconds(int processingTimeSeconds) {
            this.processingTimeSeconds = processingTimeSeconds;
        }
    }

    public static class HandleResponse {
        private String taskId;
        private String status;
        private String message;

        public HandleResponse() {}

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class TaskResult {
        private String taskId;
        private String status;
        private String result;

        public TaskResult() {}

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
