package com.example.reactor.multiplesubscribers;

import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * If the upstream is implemented using a blocking FutureTask, as with Mono.fromCallable, cancelling that subscription
 * will attempt to interrupt the running task.
 * <p>
 * Each subscription to a cold Mono creates an independent execution—so each subscriber gets its own thread, its own
 * sleep, and its own lifecycle. The timeout and interruption of the second subscriber do not impact the first or the
 * third subscriber at all.
 */
public class BlockingTimeoutExample {
  public static void main(String[] args) throws InterruptedException {
    Mono<String> mono = Mono.fromCallable(() -> {
      System.out.println(Thread.currentThread().getName() + ": Executing the callable");
      Thread.sleep(10000); // 10 seconds
      return "result";
    }).subscribeOn(Schedulers.boundedElastic()); // offload blocking to elastic scheduler

    // Subscriber 1: No timeout
    mono.subscribe(v -> System.out.println(Thread.currentThread().getName() + ": First got: " + v),
        e -> System.err.println("First error: " + e));

    // Subscriber 2: With 2-second timeout
    mono.timeout(Duration.ofSeconds(2))
        .subscribe(v -> System.out.println("Second got: " + v),
            e -> System.err.println(Thread.currentThread().getName() + ": Second error: " + e));

    // Subscriber 3: No timeout
    mono.subscribe(v -> System.out.println(Thread.currentThread().getName() + ": Third got: " + v),
        e -> System.err.println("Third error: " + e));

    Thread.sleep(15000);
  }
}
