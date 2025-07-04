package com.example.reactor.multiplesubscribers;

import java.time.Duration;
import reactor.core.publisher.Mono;


/**
 * When your source is non-blocking (e.g., timer, async I/O), cancellation is just a signal to stop propagating
 * results—no threads are interrupted.
 */
public class NonBlockingTimeoutExample2 {
  public static void main(String[] args) throws InterruptedException {
    // Simulate a slow async operation (delayed emission)
    Mono<Long> mono = Mono.delay(Duration.ofSeconds(10));

    // Subscriber 1: No timeout
    mono.subscribe(v -> System.out.println(Thread.currentThread().getName() + ": First got: " + v),
        e -> System.err.println("First error: " + e));

    // Subscriber 2: With 2-second timeout
    mono.timeout(Duration.ofSeconds(2))
        .subscribe(v -> System.out.println("Second got: " + v),
            e -> System.err.println(Thread.currentThread().getName() + ": Second error: " + e));

    Thread.sleep(15000);
  }
}
