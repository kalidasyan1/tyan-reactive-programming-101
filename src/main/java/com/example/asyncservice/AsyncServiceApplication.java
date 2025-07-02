package com.example.asyncservice;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.scheduler.Schedulers;


/**
 * Async Service with Handle Pattern
 *
 * Service Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                    ASYNC SERVICE DESIGN                         │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Client Request → Time Check → Immediate/Handle Response        │
 * │                                                                 │
 * │  ┌─────────────────┐    ┌──────────────────┐                   │
 * │  │   < 30 seconds  │    │   >= 30 seconds  │                   │
 * │  │   Direct Reply  │    │   Return Handle  │                   │
 * │  └─────────────────┘    └──────────────────┘                   │
 * │                                  │                             │
 * │                         ┌─────────────────┐                    │
 * │                         │ Background Task │                    │
 * │                         │   Processing    │                    │
 * │                         └─────────────────┘                    │
 * │                                  │                             │
 * │                         ┌─────────────────┐                    │
 * │                         │ Result Storage  │                    │
 * │                         │ (In-Memory Map) │                    │
 * │                         └─────────────────┘                    │
 * └─────────────────────────────────────────────────────────────────┘
 */
@SpringBootApplication
public class AsyncServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(AsyncServiceApplication.class);
    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    private final AtomicLong taskCounter = new AtomicLong(0);

    public static void main(String[] args) {
        log.info("=== ASYNC SERVICE WITH HANDLE PATTERN ===");
        log.info("Starting async service on port 8081...");
        System.setProperty("server.port", "8081");
        SpringApplication.run(AsyncServiceApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> asyncRoutes() {
        return RouterFunctions
            // Submit a processing task
            .route(RequestPredicates.POST("/api/process"),
                request -> request.bodyToMono(ProcessRequest.class)
                    .flatMap(this::handleProcessRequest))

            // Get task status and result
            .andRoute(RequestPredicates.GET("/api/tasks/{taskId}"),
                request -> {
                    String taskId = request.pathVariable("taskId");
                    return getTaskStatus(taskId);
                })

            // List all tasks (for demo purposes)
            .andRoute(RequestPredicates.GET("/api/tasks"),
                request -> ServerResponse.ok()
                    .bodyValue(taskResults.keySet()))

            // Health check
            .andRoute(RequestPredicates.GET("/api/health"),
                request -> ServerResponse.ok()
                    .bodyValue("Async Service is running"));
    }

    private Mono<ServerResponse> handleProcessRequest(ProcessRequest request) {
        String taskId = "task-" + taskCounter.incrementAndGet();

        log.info("Received process request: {} with {} seconds processing time",
            request.getData(), request.getProcessingTimeSeconds());

        if (request.getProcessingTimeSeconds() <= 30) {
            // Process immediately for short tasks
            return processTaskImmediately(request)
                .flatMap(result -> ServerResponse.ok()
                    .bodyValue(new ImmediateResponse(result, "COMPLETED")));
        } else {
            // Return handle for long tasks
            TaskResult taskResult = new TaskResult(taskId, "PROCESSING", null,
                LocalDateTime.now(), request);
            taskResults.put(taskId, taskResult);

            // Start background processing
            processTaskInBackground(taskId, request);

            HandleResponse handle = new HandleResponse(taskId, "ACCEPTED",
                "Task is being processed. Use taskId to check status.");

            return ServerResponse.accepted().bodyValue(handle);
        }
    }

    private Mono<String> processTaskImmediately(ProcessRequest request) {
        return Mono.fromCallable(() -> {
            // Simulate processing
            try {
                Thread.sleep(request.getProcessingTimeSeconds() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processing interrupted", e);
            }
            return "Processed: " + request.getData().toUpperCase() +
                " (completed in " + request.getProcessingTimeSeconds() + "s)";
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void processTaskInBackground(String taskId, ProcessRequest request) {
        Mono.fromCallable(() -> {
            log.info("Starting background processing for task: " + taskId);

            try {
                // Simulate long processing
                Thread.sleep(request.getProcessingTimeSeconds() * 1000L);

                String result = "Background processed: " + request.getData().toUpperCase() +
                    " (completed after " + request.getProcessingTimeSeconds() + "s)";

                // Update task result
                TaskResult taskResult = taskResults.get(taskId);
                if (taskResult != null) {
                    taskResult.setStatus("COMPLETED");
                    taskResult.setResult(result);
                    taskResult.setCompletedAt(LocalDateTime.now());
                }

                log.info("Background processing completed for task: " + taskId);
                return result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                // Update task result with error
                TaskResult taskResult = taskResults.get(taskId);
                if (taskResult != null) {
                    taskResult.setStatus("FAILED");
                    taskResult.setResult("Processing was interrupted");
                    taskResult.setCompletedAt(LocalDateTime.now());
                }

                throw new RuntimeException("Background processing interrupted", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            result -> log.info("Task " + taskId + " completed successfully"),
            error -> {
                log.error("Task " + taskId + " failed: " + error.getMessage());

                // Update task result with error
                TaskResult taskResult = taskResults.get(taskId);
                if (taskResult != null) {
                    taskResult.setStatus("FAILED");
                    taskResult.setResult("Error: " + error.getMessage());
                    taskResult.setCompletedAt(LocalDateTime.now());
                }
            }
        );
    }

    private Mono<ServerResponse> getTaskStatus(String taskId) {
        TaskResult taskResult = taskResults.get(taskId);

        if (taskResult == null) {
            return ServerResponse.notFound().build();
        }

        return ServerResponse.ok().bodyValue(taskResult);
    }

    // Data classes
    public static class ProcessRequest {
        private String data;
        private int processingTimeSeconds;

        // Constructors
        public ProcessRequest() {}

        public ProcessRequest(String data, int processingTimeSeconds) {
            this.data = data;
            this.processingTimeSeconds = processingTimeSeconds;
        }

        // Getters and setters
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public int getProcessingTimeSeconds() { return processingTimeSeconds; }
        public void setProcessingTimeSeconds(int processingTimeSeconds) {
            this.processingTimeSeconds = processingTimeSeconds;
        }
    }

    public static class ImmediateResponse {
        private String result;
        private String status;

        public ImmediateResponse(String result, String status) {
            this.result = result;
            this.status = status;
        }

        public String getResult() { return result; }
        public String getStatus() { return status; }
    }

    public static class HandleResponse {
        private String taskId;
        private String status;
        private String message;

        public HandleResponse(String taskId, String status, String message) {
            this.taskId = taskId;
            this.status = status;
            this.message = message;
        }

        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class TaskResult {
        private String taskId;
        private String status;
        private String result;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private ProcessRequest originalRequest;

        public TaskResult(String taskId, String status, String result,
                         LocalDateTime createdAt, ProcessRequest originalRequest) {
            this.taskId = taskId;
            this.status = status;
            this.result = result;
            this.createdAt = createdAt;
            this.originalRequest = originalRequest;
        }

        // Getters and setters
        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public ProcessRequest getOriginalRequest() { return originalRequest; }
    }
}
