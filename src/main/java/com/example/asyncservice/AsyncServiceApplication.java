package com.example.asyncservice;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeoutException;
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
 * Async Service with Handle Pattern - Advanced Reactive Architecture
 *
 * OVERVIEW:
 * This service demonstrates a sophisticated async processing pattern that uses a timeout-based
 * approach to determine response strategy. It showcases reactive programming principles using
 * Spring WebFlux and Project Reactor with a 30-second SLA commitment.
 *
 * CORE DESIGN PATTERN:
 * The service implements a "Timeout-Based Handle Pattern" with SERVER-SIDE SLA enforcement:
 * - ALL tasks start processing immediately
 * - If processing completes ≤30s: Return result directly to client
 * - If processing exceeds 30s: Return handle and continue processing in background
 *
 * ARCHITECTURE OVERVIEW:
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                           ASYNC SERVICE ARCHITECTURE                                │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │                                                                                     │
 * │  ┌─────────────┐     ┌──────────────────┐     ┌────────────────────────┐           │
 * │  │   CLIENT    │────▶│  REQUEST ROUTER  │────▶│    START PROCESSING    │           │
 * │  │             │     │   (WebFlux)      │     │    (ALL REQUESTS)      │           │
 * │  └─────────────┘     └──────────────────┘     └────────────────────────┘           │
 * │                                                           │                        │
 * │                               ┌───────────────────────────┼───────────────────────┐│
 * │                               │                           ▼                       ││
 * │  ┌─────────────────────────────┼─────────────┐    ┌──────────────────────────────┐││
 * │  │      IMMEDIATE RESPONSE     │             │    │      TIMEOUT RESPONSE       │││
 * │  │     (Completed ≤ 30s)       │             │    │     (Exceeds 30s)            │││
 * │  │                             │             │    │                              │││
 * │  │  ┌─────────────────────┐    │             │    │  ┌─────────────────────┐     │││
 * │  │  │   30s Timeout       │    │             │    │  │   Return Handle     │     │││
 * │  │  │   Processing        │    │             │    │  │   (Task ID)         │     │││
 * │  │  │   Completes         │    │             │    │  └─────────────────────┘     │││
 * │  │  └─────────────────────┘    │             │    │            │                 │││
 * │  │            │                │             │    │            ▼                 │││
 * │  │            ▼                │             │    │  ┌─────────────────────┐     │││
 * │  │  ┌─────────────────────┐    │             │    │  │   Continue Task     │     │││
 * │  │  │   Direct Response   │    │             │    │  │   in Background     │     │││
 * │  │  │   (Completed)       │    │             │    │  │   (No Time Limit)   │     │││
 * │  │  └─────────────────────┘    │             │    │  └─────────────────────┘     │││
 * │  └─────────────────────────────┼─────────────┘    │            │                 │││
 * │                               │                  │            ▼                 │││
 * │                               │                  │  ┌─────────────────────┐     │││
 * │                               │                  │  │   In-Memory Store   │     │││
 * │                               │                  │  │ (ConcurrentHashMap) │     │││
 * │                               │                  │  │  Task Results &     │     │││
 * │                               │                  │  │     Status          │     │││
 * │                               │                  │  └─────────────────────┘     │││
 * │                               │                  └──────────────────────────────┘││
 * │                               │                                                  ││
 * │                               │  ┌──────────────────────────────────────────────┘│
 * │                               │  │                                               │
 * │                               │  ▼                                               │
 * │                               │  ┌─────────────────────────────────────────────┐ │
 * │                               │  │           POLLING INTERFACE                 │ │
 * │                               │  │                                             │ │
 * │                               │  │  GET /api/tasks/{taskId}                   │ │
 * │                               │  │  - Check task status                       │ │
 * │                               │  │  - Retrieve results when complete          │ │
 * │                               │  │  - Handle error states                     │ │
 * │                               │  └─────────────────────────────────────────────┘ │
 * │                               └────────────────────────────────────────────────────│
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * DETAILED WORKFLOW:
 *
 * 1. REQUEST INGESTION:
 *    - Client sends POST /api/process with { data, complexity? }
 *    - WebFlux router receives and parses the request
 *    - Service immediately starts processing for ALL requests
 *
 * 2. TIMEOUT-BASED RESPONSE STRATEGY:
 *    - Server starts processing immediately
 *    - Waits up to 30 seconds for completion
 *    - IF processing completes ≤ 30 seconds:
 *      → Return complete result directly to client
 *      → No handle needed, single request-response cycle
 *    - IF processing exceeds 30 seconds:
 *      → Generate unique task ID (task-{counter})
 *      → Store task metadata and continue processing in background
 *      → Return HTTP 202 (Accepted) with task handle
 *      → Client can poll for results later
 *
 * 3. BACKGROUND PROCESSING (Timeout Cases):
 *    - Task continues running on Scheduler.boundedElastic() thread pool
 *    - No time constraints for background continuation
 *    - Progress tracked in shared task result store
 *    - Supports error handling and status updates
 *    - Thread-safe updates using ConcurrentHashMap
 *
 * 4. RESULT RETRIEVAL:
 *    - Client polls GET /api/tasks/{taskId}
 *    - Returns current status: PROCESSING, COMPLETED, FAILED
 *    - Includes result data when available
 *    - Handles task not found scenarios
 *
 * KEY DESIGN BENEFITS:
 * - Real-time 30-second SLA enforcement through timeout
 * - No upfront estimation required - let actual processing determine strategy
 * - Prevents long-running requests from blocking the event loop
 * - Efficient resource utilization through reactive scheduling
 * - Scalable handle-based async pattern for long-running tasks
 * - Non-blocking I/O for both immediate and deferred responses
 * - Thread-safe concurrent task management
 * - Graceful error handling and status tracking
 *
 * REACTIVE PRINCIPLES DEMONSTRATED:
 * - Mono/Flux for async processing pipelines
 * - Scheduler abstraction for thread pool management
 * - Non-blocking I/O with WebFlux
 * - Real-time timeout handling for SLA enforcement
 * - Backpressure handling through reactive streams
 * - Functional routing and request handling
 * - Reactive timeout patterns with fallback strategies
 *
 * TECHNICAL STACK:
 * - Spring WebFlux: Reactive web framework
 * - Project Reactor: Reactive streams implementation
 * - Scheduler.boundedElastic(): Thread pool for blocking operations
 * - ConcurrentHashMap: Thread-safe in-memory storage
 * - RouterFunction: Functional web routing
 * - Mono.timeout(): Real-time SLA enforcement
 * - onErrorResume(): Timeout fallback handling
 *
 * ENDPOINTS:
 * - POST /api/process        → Submit processing task
 * - GET  /api/tasks/{id}     → Check task status and retrieve results
 * - GET  /api/tasks          → List all task IDs (debugging)
 * - GET  /api/health         → Service health check
 */
@SpringBootApplication
public class AsyncServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(AsyncServiceApplication.class);
    private final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
    private final AtomicLong taskCounter = new AtomicLong(0);

    // Server-side SLA configuration
    private static final int SLA_SECONDS = 30;

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
                    return getTaskResult(taskId);
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

        log.info("Received process request: {} - starting immediate processing", request.getData());

        // Start processing immediately for ALL requests
        Mono<ProcessingResult> processingMono = startProcessing(request, taskId);

        // Try to complete within 30 seconds, otherwise return handle
        return processingMono
            .timeout(Duration.ofSeconds(SLA_SECONDS))
            .flatMap(result -> {
                // Processing completed within SLA - return TaskResult directly
                log.info("Task {} completed within {} second SLA", taskId, SLA_SECONDS);
                TaskResult taskResult = new TaskResult(taskId, TaskStatus.COMPLETED, result,
                    LocalDateTime.now(), request);
                taskResult.setCompletedAt(LocalDateTime.now());

                return ServerResponse.ok().bodyValue(taskResult);
            })
            .onErrorResume(throwable -> {
                if (throwable instanceof TimeoutException) {
                    // Processing exceeded SLA - return TaskResult with PROCESSING status
                    log.info("Task {} exceeded {} second SLA, returning TaskResult for background processing", taskId, SLA_SECONDS);

                    // Store task for background continuation
                    TaskResult taskResult = new TaskResult(taskId, TaskStatus.PROCESSING, (ProcessingResult) null,
                        LocalDateTime.now(), request);
                    taskResults.put(taskId, taskResult);

                    // Continue processing in background (the original processingMono continues)
                    continueProcessingInBackground(taskId, processingMono);

                    return ServerResponse.accepted().bodyValue(taskResult);
                } else {
                    // Handle other errors - return TaskResult with FAILED status using error constructor
                    log.error("Task {} failed with error: {}", taskId, throwable.getMessage());
                    TaskResult taskResult = new TaskResult(taskId, TaskStatus.FAILED,
                        "Processing failed: " + throwable.getMessage(),
                        LocalDateTime.now(), request);
                    taskResult.setCompletedAt(LocalDateTime.now());

                    return ServerResponse.status(500).bodyValue(taskResult);
                }
            });
    }

    /**
     * Start processing immediately - this method simulates actual work
     */
    private Mono<ProcessingResult> startProcessing(ProcessRequest request, String taskId) {
        return Mono.fromCallable(() -> {
            log.info("Starting processing for task: {}", taskId);

            // Calculate processing time based on complexity (1-10 scale)
            // Complexity 1 → 0.1 factor, Complexity 10 → 1.0 factor
            // Linear mapping: factor = (complexity - 1) / 9 * 0.9 + 0.1
            double complexityFactor = (request.getComplexity() - 1) / 9.0 * 0.9 + 0.1;
            long processingTimeMs = (long) (complexityFactor * 60000); // Scale to 0-60 seconds

            log.info("Task {} (complexity: {}) will take {} ms to process (factor: {})",
                taskId, request.getComplexity(), processingTimeMs, complexityFactor);

            // Simulate actual processing work based on complexity
            try {
                Thread.sleep(processingTimeMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processing interrupted", e);
            }

            // Create a structured result object
            ProcessingResult result = new ProcessingResult(
                request.getData().toUpperCase(),
                "Data processed successfully",
                System.currentTimeMillis(),
                request.getComplexity()
            );

            log.info("Processing completed for task: {}", taskId);
            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Continue processing in background after timeout
     */
    private void continueProcessingInBackground(String taskId, Mono<ProcessingResult> originalProcessing) {
        originalProcessing
            .doOnSuccess(result -> {
                // Update task result when background processing completes
                TaskResult taskResult = taskResults.get(taskId);
                if (taskResult != null) {
                    taskResult.setStatus(TaskStatus.COMPLETED);
                    taskResult.setResult(result);
                    taskResult.setCompletedAt(LocalDateTime.now());
                    log.info("Background processing completed for task: {}", taskId);
                }
            })
            .doOnError(error -> {
                // Update task result if background processing fails
                TaskResult taskResult = taskResults.get(taskId);
                if (taskResult != null) {
                    taskResult.setStatus(TaskStatus.FAILED);
                    taskResult.setResult(null);
                    taskResult.setErrorMessage("Error: " + error.getMessage());
                    taskResult.setCompletedAt(LocalDateTime.now());
                    log.error("Background processing failed for task: {}", taskId, error);
                }
            })
            .subscribe(); // Subscribe to continue processing in background
    }

    /**
     * Get task result by ID
     * Returns the complete TaskResult with current status and result (if available)
     */
    private Mono<ServerResponse> getTaskResult(String taskId) {
        TaskResult taskResult = taskResults.get(taskId);

        if (taskResult == null) {
            return ServerResponse.notFound().build();
        }

        return ServerResponse.ok().bodyValue(taskResult);
    }
}
