package com.example.reactor.multiplesubscribers;

import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * This example demonstrates how to create a hot stream using cache in Reactor.
 * The first subscriber will trigger the execution, and subsequent subscribers will receive the cached result.
 * If a subscriber times out, it will not affect the execution of the original stream.
 */
public class HotStreamViaCacheExample {
    public static void main(String[] args) throws InterruptedException {
      Mono<String> cachedMono = Mono.fromCallable(() -> {
        System.out.println(Thread.currentThread().getName() + ": Executing the callable");
        Thread.sleep(10000); // 10 seconds
        return "result";
      }).subscribeOn(Schedulers.boundedElastic()).cache();

      cachedMono.subscribe(
          v -> System.out.println(Thread.currentThread().getName() + ": First got: " + v),
          e -> System.err.println("First error: " + e)
      );

      cachedMono.timeout(Duration.ofSeconds(2))
          .subscribe(
              v -> System.out.println(Thread.currentThread().getName() + ": Second got: " + v),
              e -> System.err.println(Thread.currentThread().getName() + ": Second error: " + e)
          );

      cachedMono.subscribe(
          v -> System.out.println(Thread.currentThread().getName() + ": Third got: " + v),
          e -> System.err.println("Third error: " + e)
      );

      Thread.sleep(15000);
    }
}
