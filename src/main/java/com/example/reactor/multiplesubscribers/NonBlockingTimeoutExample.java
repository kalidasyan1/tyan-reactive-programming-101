package com.example.reactor.multiplesubscribers;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Mono;


/**
 * When your source is non-blocking (e.g., timer, async I/O), cancellation is just a signal to stop propagating
 * results—no threads are interrupted.
 */
public class NonBlockingTimeoutExample {
  public static void main(String[] args) throws InterruptedException {
    Mono<String> mono = Mono.fromFuture(() -> {
      // Simulate slow async I/O
      CompletableFuture<String> future = new CompletableFuture<>();
      // Schedule completion after 10 seconds
      new Thread(() -> {
        System.out.println(Thread.currentThread().getName() + ": Executing the callable");
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          System.out.println("Thread interrupted: " + e.getMessage());
          throw new RuntimeException(e);
        }
        future.complete("async result");
      }).start();
      return future;
    });

    mono.subscribe(v -> System.out.println(Thread.currentThread().getName() + ": First got: " + v),
        e -> System.err.println("First error: " + e));

    mono.timeout(Duration.ofSeconds(2))
        .subscribe(v -> System.out.println(Thread.currentThread().getName() + ": Second got: " + v),
            e -> System.err.println(Thread.currentThread().getName() + ": Second error: " + e));

    Thread.sleep(15000);
  }
}
