package com.example.asyncservice;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Client for testing the Async Service
 * Demonstrates how to interact with the handle-based async service
 */
public class AsyncServiceClient {

    private final WebClient webClient;

    public AsyncServiceClient() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:8081")
            .build();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ASYNC SERVICE CLIENT DEMO ===\n");

        AsyncServiceClient client = new AsyncServiceClient();

        // Test immediate processing (< 30 seconds)
        client.testImmediateProcessing();

        // Test async processing with handle (>= 30 seconds)
        client.testAsyncProcessing();

        // Keep the client running to see results
        Thread.sleep(45000); // Wait for long task to complete
    }

    private void testImmediateProcessing() {
        System.out.println("--- TESTING IMMEDIATE PROCESSING ---");

        ProcessRequest request = new ProcessRequest("Quick Task", 5);

        webClient.post()
            .uri("/api/process")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Object.class)
            .subscribe(
                response -> System.out.println("✓ Immediate response: " + response),
                error -> System.err.println("✗ Immediate error: " + error.getMessage())
            );
    }

    private void testAsyncProcessing() {
        System.out.println("\n--- TESTING ASYNC PROCESSING WITH HANDLE ---");

        ProcessRequest request = new ProcessRequest("Long Running Task", 35);

        webClient.post()
            .uri("/api/process")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(HandleResponse.class)
            .flatMap(handleResponse -> {
                System.out.println("✓ Received handle: " + handleResponse.getTaskId());

                // Poll for results
                return pollForResult(handleResponse.getTaskId());
            })
            .subscribe(
                finalResult -> System.out.println("✓ Final result: " + finalResult),
                error -> System.err.println("✗ Async error: " + error.getMessage())
            );
    }

    private Mono<String> pollForResult(String taskId) {
        return Flux.interval(Duration.ofSeconds(2))
            .flatMap(tick -> {
                System.out.println("Polling for task: " + taskId + " (attempt " + (tick + 1) + ")");

                return webClient.get()
                    .uri("/api/tasks/{taskId}", taskId)
                    .retrieve()
                    .bodyToMono(TaskResult.class);
            })
            .filter(taskResult -> "COMPLETED".equals(taskResult.getStatus()) ||
                                "FAILED".equals(taskResult.getStatus()))
            .take(1)
            .map(TaskResult::getResult)
            .next();
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
