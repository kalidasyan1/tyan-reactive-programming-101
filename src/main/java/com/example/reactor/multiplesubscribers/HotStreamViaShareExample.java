package com.example.reactor.multiplesubscribers;

import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * This example demonstrates how to create a hot stream using share in Reactor.
 * The first subscriber will trigger the execution, and subsequent subscribers will share the same result.
 * If a subscriber times out, it will not affect the execution of the original stream.
 * The share operator allows multiple subscribers to share the same source stream,
 * which is useful for scenarios where you want to avoid re-executing the source for each subscriber.
 */
public class HotStreamViaShareExample {
    public static void main(String[] args) throws InterruptedException {
      Mono<String> sharedMono = Mono.fromCallable(() -> {
        System.out.println(Thread.currentThread().getName() + ": Executing the callable");
        Thread.sleep(5000); // 10 seconds
        return "result";
      }).subscribeOn(Schedulers.boundedElastic()).share();

      sharedMono.subscribe(
          v -> System.out.println(Thread.currentThread().getName() + ": First got: " + v),
          e -> System.err.println("First error: " + e)
      );

      sharedMono.timeout(Duration.ofSeconds(2))
          .subscribe(
              v -> System.out.println("Second got: " + v),
              e -> System.err.println(Thread.currentThread().getName() + ": Second error: " + e)
          );

      Thread.sleep(8000); // Wait for the first subscriber to complete
      sharedMono.subscribe(
          v -> System.out.println(Thread.currentThread().getName() + ": Third got: " + v),
          e -> System.err.println("Third error: " + e)
      );

      Thread.sleep(15000);
    }
}
