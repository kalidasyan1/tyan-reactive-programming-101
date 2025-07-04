package projects.asyncservice.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import projects.asyncservice.models.DataProcessingRequest;
import projects.asyncservice.models.DataProcessingResult;
import projects.asyncservice.models.TaskResult;
import projects.asyncservice.models.TaskStatus;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * Controller for handling process requests
 * This controller will handle incoming requests to the /api/process endpoint
 * and manage the processing of tasks asynchronously.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DataProcessingRequestController {

  private static final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();

  private static final int SLA_SECONDS = 30;

  @PostMapping("/process")
  public Mono<ResponseEntity<TaskResult>> handleRequest(@RequestBody DataProcessingRequest request) {
    log.info("Received process request: {} - starting immediate processing", request.getData());
    String taskId = UUID.randomUUID().toString();

    // Start processing immediately - this will run independently in background
    Mono<DataProcessingResult> dataProcessingResultMono = startProcessing(request, taskId);

    // Set up background processing that will not be interrupted
    setupBackgroundProcessing(taskId, request, dataProcessingResultMono);

    return dataProcessingResultMono
        .timeout(Duration.ofSeconds(SLA_SECONDS))
        .map(result -> {
          // Processing completed within SLA - return result directly
          log.info("Task {} completed within {} seconds SLA", taskId, SLA_SECONDS);
          return ResponseEntity.ok(getTaskResultFromCache(taskId));
        })
        .onErrorResume(throwable -> {
          if (throwable instanceof TimeoutException) {
            // Client timeout - but background processing continues!
            log.info("Task {} exceeded {} second SLA, returning handle for background processing", taskId, SLA_SECONDS);
            return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).body(getTaskResultFromCache(taskId)));
          } else {
            log.error("Error processing task {}: {}", taskId, throwable.getMessage());
            TaskResult taskResult = TaskResult.builder()
                .taskId(taskId)
                .status(TaskStatus.FAILED)
                .errorMessage(throwable.getMessage())
                .createdAt(LocalDateTime.now())
                .originalRequest(request)
                .build();
            taskResults.put(taskId, taskResult);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(taskResult));
          }
        });
  }

  /**
   * Helper method to get or create task result from cache
   */
  private TaskResult getTaskResultFromCache(String taskId) {
    return taskResults.computeIfAbsent(taskId, id ->
        TaskResult.builder()
            .taskId(id)
            .status(TaskStatus.PROCESSING)
            .createdAt(LocalDateTime.now())
            .build());
  }

  /**
   * Set up background processing that will NEVER be interrupted.
   * This method only handles the background continuation - no duplicate storage.
   */
  private void setupBackgroundProcessing(String taskId, DataProcessingRequest request,
                                        Mono<DataProcessingResult> backgroundProcessingMono) {
    log.info("Setting up background processing for task: {}", taskId);
    // Subscribe to background processing - this will NEVER be cancelled
    backgroundProcessingMono
        .doOnSuccess(result -> {
          log.info("Background processing completed for task: {}", taskId);
          TaskResult taskResult = taskResults.get(taskId);
          if (taskResult != null) {
            taskResult.setStatus(TaskStatus.COMPLETED);
            taskResult.setResult(result);
            taskResult.setCompletedAt(LocalDateTime.now());
          }
        })
        .doOnError(error -> {
          log.error("Background processing failed for task: {}", taskId, error);
          TaskResult taskResult = taskResults.get(taskId);
          if (taskResult != null) {
            taskResult.setStatus(TaskStatus.FAILED);
            taskResult.setErrorMessage("Processing failed: " + error.getMessage());
            taskResult.setCompletedAt(LocalDateTime.now());
          }
        })
        .subscribe();
    log.info("Background processing setup complete for task: {}", taskId);
  }

  private Mono<DataProcessingResult> startProcessing(DataProcessingRequest request, String taskId) {
    // Initialize the task result immediately
    TaskResult initialTaskResult = TaskResult.builder()
        .taskId(taskId)
        .status(TaskStatus.PROCESSING)
        .createdAt(LocalDateTime.now())
        .originalRequest(request)
        .build();
    taskResults.put(taskId, initialTaskResult);
    log.info("Task {} initialized with status: {}", taskId, TaskStatus.PROCESSING);

    return Mono.fromCallable(() -> {
      long processingTime = estimateProcessingTime(request.getComplexity());
      log.info("Thread {} starting uninterruptible processing for task {}: {} ms", Thread.currentThread().getName(), taskId, processingTime);

      try {
        Thread.sleep(processingTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Processing for task {} was interrupted externally", taskId);
        throw new RuntimeException("Processing interrupted", e);
      }

      log.info("Processing completed successfully for task: {}", taskId);
      return DataProcessingResult.builder()
          .processedData(request.getData() + " - processed")
          .message("Data processed successfully")
          .timestamp(System.currentTimeMillis())
          .complexity(request.getComplexity())
          .build();
    }).subscribeOn(Schedulers.boundedElastic()).cache();// Cache the result so multiple subscribers don't re-execute
  }

  private long estimateProcessingTime(int complexity) {
    // Calculate processing time based on complexity (1-10 scale)
    // Complexity 1 → 0.1 factor, Complexity 10 → 1.0 factor
    // Linear mapping: factor = (complexity - 1) / 9 * 0.9 + 0.1
    double complexityFactor = (complexity - 1) / 9.0 * 0.9 + 0.1;
    return (long) Math.ceil(complexityFactor * 60000); // Scale to 0-60 seconds
  }
}
