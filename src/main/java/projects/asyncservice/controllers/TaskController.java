package projects.asyncservice.controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projects.asyncservice.models.TaskResult;
import projects.asyncservice.models.TaskStatus;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequestMapping("/api/task")
public class TaskController {
  static final Map<String, TaskResult> taskResults = new ConcurrentHashMap<>();
  /**
   * Retrieves the result of a task by its ID.
   * This method is used to get the result of a task that has been processed asynchronously.
   * The task result is only removed from the cache after successful response creation.
   *
   * @param taskId The ID of the task to retrieve the result for.
   * @return A Mono containing the ResponseEntity with the TaskResult.
   */
  @GetMapping("/result/{taskId}")
  public Mono<ResponseEntity<TaskResult>> getTaskResult(@PathVariable("taskId") String taskId) {
    return Mono.fromCallable(() -> {
      TaskResult result = taskResults.get(taskId); // First, just get without removing
      if (result != null) {
        log.info("Retrieved result for task {}: {}", taskId, result);
        return ResponseEntity.ok(result);
      } else {
        log.warn("No result found for task {}", taskId);
        return ResponseEntity.notFound().<TaskResult>build();
      }
    })
    .doOnSuccess(TaskController::removeTaskResultFromCache);
  }

  static void removeTaskResultFromCache(ResponseEntity<TaskResult> responseEntity) {
    if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody() &&
        responseEntity.getBody().getStatus() == TaskStatus.COMPLETED) {
      String taskId = responseEntity.getBody().getTaskId();
      TaskResult removedResult = taskResults.remove(taskId);
      if (removedResult != null) {
        log.info("Successfully removed task {} from cache after successful response", taskId);
      }
    } else {
      log.warn("Task result not removed from cache: {}", responseEntity);
    }
  }

  /**
   * Retrieves the list of all task IDs.
   * This method is used to get a list of all tasks that have been processed.
   *
   * @return A Mono containing the ResponseEntity with the list of task IDs.
   */
  @GetMapping("/list")
  public Mono<ResponseEntity<List<String>>> getTaskList() {
    return Mono.fromCallable(() -> {
      List<String> taskIds = taskResults.keySet().stream().toList();
      log.info("Retrieved task list: {}", taskIds);
      return ResponseEntity.ok(taskIds);
    });
  }
}
