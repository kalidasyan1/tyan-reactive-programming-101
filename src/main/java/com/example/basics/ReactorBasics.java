package com.example.basics;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Arrays;

/**
 * Introduction to Project Reactor - The foundation of Spring WebFlux
 *
 * Key Concepts:
 * - Mono: 0 or 1 element
 * - Flux: 0 to N elements
 * - Cold vs Hot streams
 * - Backpressure handling
 */
public class ReactorBasics {

    private static final Logger log = LoggerFactory.getLogger(ReactorBasics.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== REACTIVE PROGRAMMING BASICS ===\n");

        ReactorBasics demo = new ReactorBasics();

        // Basic Mono examples
        demo.monoExamples();
        Thread.sleep(1500); // Wait for delayed Mono

        // Basic Flux examples
        demo.fluxExamples();
        Thread.sleep(2000); // Wait for interval flux

        // Transformation operators
        demo.transformationExamples();
        Thread.sleep(500);

        // Error handling
        demo.errorHandlingExamples();
        Thread.sleep(1000);

        // Backpressure demonstration
        demo.backpressureExample();

        log.info("\n=== ALL EXAMPLES COMPLETED ===");
    }

    private void monoExamples() {
        log.info("--- MONO EXAMPLES ---");

        // Creating Monos
        Mono<String> helloMono = Mono.just("Hello Reactive World!");

        // Subscribe and consume
        helloMono.subscribe(
            value -> log.info("✓ Received: {}", value),
            error -> log.error("✗ Error: {}", error.getMessage()),
            () -> log.info("✓ Completed")
        );

        // Delayed Mono
        Mono<String> delayedMono = Mono.just("Delayed message")
            .delayElement(Duration.ofMillis(1000));

        delayedMono.subscribe(value ->
            log.info("✓ Delayed: {}", value));

        log.info("");
    }

    private void fluxExamples() {
        log.info("--- FLUX EXAMPLES ---");

        // Creating Flux from various sources
        Flux<Integer> numbersFlux = Flux.just(1, 2, 3, 4, 5);
        Flux<String> listFlux = Flux.fromIterable(
            Arrays.asList("Apple", "Banana", "Cherry"));
        Flux<Long> intervalFlux = Flux.interval(Duration.ofMillis(500))
            .take(3);

        // Subscribe to number flux
        numbersFlux.subscribe(
            number -> log.info("✓ Number: {}", number)
        );

        // Subscribe to list flux
        listFlux.subscribe(
            fruit -> log.info("✓ Fruit: {}", fruit)
        );

        // Subscribe to interval flux
        intervalFlux.subscribe(
            tick -> log.info("✓ Tick: {}", tick)
        );

        log.info("");
    }

    private void transformationExamples() {
        log.info("--- TRANSFORMATION EXAMPLES ---");

        // Map operator
        Flux.just("hello", "reactive", "world")
            .map(word -> word.toUpperCase())
            .subscribe(word -> log.info("✓ Uppercase: {}", word));

        // Filter operator
        Flux.range(1, 10)
            .filter(n -> n % 2 == 0)
            .subscribe(even -> log.info("✓ Even: {}", even));

        // FlatMap for nested operations
        Flux.just("user1", "user2", "user3")
            .flatMap(this::fetchUserData)
            .subscribe(userData -> log.info("✓ User data: {}", userData));

        // Zip combining streams
        Flux<String> names = Flux.just("Alice", "Bob", "Charlie");
        Flux<Integer> ages = Flux.just(25, 30, 35);

        Flux.zip(names, ages)
            .map(tuple -> tuple.getT1() + " is " + tuple.getT2() + " years old")
            .subscribe(info -> log.info("✓ Info: {}", info));

        log.info("");
    }

    private void errorHandlingExamples() {
        log.info("--- ERROR HANDLING EXAMPLES ---");

        // OnError resume
        Flux.just(1, 2, 0, 4)
            .map(n -> 10 / n)
            .onErrorResume(error -> {
                log.info("✓ Error handled: {}", error.getMessage());
                return Flux.just(-1); // fallback value
            })
            .subscribe(result -> log.info("✓ Result: {}", result));

        // Retry mechanism
        Flux.just("data")
            .map(data -> {
                if (Math.random() > 0.7) {
                    throw new RuntimeException("Random failure");
                }
                return data.toUpperCase();
            })
            .retry(3)
            .subscribe(
                data -> log.info("✓ Success: {}", data),
                error -> log.info("✗ Failed after retries: {}", error.getMessage())
            );

        log.info("");
    }

    private void backpressureExample() {
        log.info("--- BACKPRESSURE EXAMPLE ---");

        // Example 1: Backpressure overflow (will actually trigger error)
        log.info("1. Backpressure Overflow Example:");
        Flux.interval(Duration.ofMillis(10)) // Very fast producer (100 items/second)
            .onBackpressureBuffer(5) // Small buffer - will overflow quickly
            .take(50) // Take enough items to cause overflow
            .delayElements(Duration.ofMillis(200)) // Very slow consumer (5 items/second)
            .subscribe(
                value -> log.info("  ✓ Processed: {}", value),
                error -> log.info("  ✗ Backpressure error: {}", error.getMessage()),
                () -> log.info("  ✓ Backpressure overflow demo completed")
            );

        // Wait a bit to see the overflow
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        log.info("\n2. Backpressure Drop Strategy:");
        // Example 2: Drop strategy (drops newest items when buffer is full)
        Flux.interval(Duration.ofMillis(50)) // Fast producer
            .onBackpressureDrop(dropped ->
                log.info("  ⚠️ Dropped item: {}", dropped))
            .take(20)
            .delayElements(Duration.ofMillis(300)) // Slow consumer
            .subscribe(
                value -> log.info("  ✓ Processed (drop): {}", value),
                error -> log.info("  ✗ Drop error: {}", error.getMessage()),
                () -> log.info("  ✓ Drop strategy demo completed")
            );

        // Wait to see the drop behavior
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        log.info("\n3. Backpressure Latest Strategy:");
        // Example 3: Latest strategy (keeps only the latest item)
        Flux.interval(Duration.ofMillis(50)) // Fast producer
            .onBackpressureLatest()
            .take(15)
            .delayElements(Duration.ofMillis(400)) // Very slow consumer
            .subscribe(
                value -> log.info("  ✓ Processed (latest): {}", value),
                error -> log.info("  ✗ Latest error: {}", error.getMessage()),
                () -> log.info("  ✓ Latest strategy demo completed")
            );

        log.info("");
    }

    // Helper method to simulate async data fetching
    private Mono<String> fetchUserData(String userId) {
        return Mono.just("Data for " + userId)
            .delayElement(Duration.ofMillis(100));
    }
}
