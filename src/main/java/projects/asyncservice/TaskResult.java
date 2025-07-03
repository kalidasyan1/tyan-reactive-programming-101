package projects.asyncservice;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Unified response for all processing requests
 * Contains task status, results (when available), and metadata
 */
@Data
@NoArgsConstructor
public class TaskResult {
    private String taskId;
    private TaskStatus status; // PROCESSING, COMPLETED, FAILED
    private ProcessingResult result;  // ProcessingResult when completed, null when processing or failed
    private String errorMessage;     // Error message when status is FAILED
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private ProcessRequest originalRequest;

    public TaskResult(String taskId, TaskStatus status, ProcessingResult result,
                     LocalDateTime createdAt, ProcessRequest originalRequest) {
        this.taskId = taskId;
        this.status = status;
        this.result = result;
        this.createdAt = createdAt;
        this.originalRequest = originalRequest;
    }

    // Convenience constructor for failed tasks
    public TaskResult(String taskId, TaskStatus status, String errorMessage,
                     LocalDateTime createdAt, ProcessRequest originalRequest) {
        this.taskId = taskId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.originalRequest = originalRequest;
    }
}
