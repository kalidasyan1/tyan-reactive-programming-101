package com.example.basics;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== REACTIVE PROGRAMMING BASICS ===\n");

        ReactorBasics demo = new ReactorBasics();

        // Basic Mono examples
        demo.monoExamples();

        // Basic Flux examples
        demo.fluxExamples();

        // Transformation operators
        demo.transformationExamples();

        // Error handling
        demo.errorHandlingExamples();

        // Backpressure demonstration
        demo.backpressureExample();

        // Keep main thread alive to see async results
        Thread.sleep(5000);
    }

    private void monoExamples() {
        System.out.println("--- MONO EXAMPLES ---");

        // Creating Monos
        Mono<String> helloMono = Mono.just("Hello Reactive World!");

        // Subscribe and consume
        helloMono.subscribe(
            value -> System.out.println("✓ Received: " + value),
            error -> System.err.println("✗ Error: " + error.getMessage()),
            () -> System.out.println("✓ Completed")
        );

        // Delayed Mono
        Mono<String> delayedMono = Mono.just("Delayed message")
            .delayElement(Duration.ofMillis(1000));

        delayedMono.subscribe(value ->
            System.out.println("✓ Delayed: " + value));

        System.out.println();
    }

    private void fluxExamples() {
        System.out.println("--- FLUX EXAMPLES ---");

        // Creating Flux from various sources
        Flux<Integer> numbersFlux = Flux.just(1, 2, 3, 4, 5);
        Flux<String> listFlux = Flux.fromIterable(
            Arrays.asList("Apple", "Banana", "Cherry"));
        Flux<Long> intervalFlux = Flux.interval(Duration.ofMillis(500))
            .take(3);

        // Subscribe to number flux
        numbersFlux.subscribe(
            number -> System.out.println("✓ Number: " + number)
        );

        // Subscribe to list flux
        listFlux.subscribe(
            fruit -> System.out.println("✓ Fruit: " + fruit)
        );

        // Subscribe to interval flux
        intervalFlux.subscribe(
            tick -> System.out.println("✓ Tick: " + tick)
        );

        System.out.println();
    }

    private void transformationExamples() {
        System.out.println("--- TRANSFORMATION EXAMPLES ---");

        // Map operator
        Flux.just("hello", "reactive", "world")
            .map(word -> word.toUpperCase())
            .subscribe(word -> System.out.println("✓ Uppercase: " + word));

        // Filter operator
        Flux.range(1, 10)
            .filter(n -> n % 2 == 0)
            .subscribe(even -> System.out.println("✓ Even: " + even));

        // FlatMap for nested operations
        Flux.just("user1", "user2", "user3")
            .flatMap(this::fetchUserData)
            .subscribe(userData -> System.out.println("✓ User data: " + userData));

        // Zip combining streams
        Flux<String> names = Flux.just("Alice", "Bob", "Charlie");
        Flux<Integer> ages = Flux.just(25, 30, 35);

        Flux.zip(names, ages)
            .map(tuple -> tuple.getT1() + " is " + tuple.getT2() + " years old")
            .subscribe(info -> System.out.println("✓ Info: " + info));

        System.out.println();
    }

    private void errorHandlingExamples() {
        System.out.println("--- ERROR HANDLING EXAMPLES ---");

        // OnError resume
        Flux.just(1, 2, 0, 4)
            .map(n -> 10 / n)
            .onErrorResume(error -> {
                System.out.println("✓ Error handled: " + error.getMessage());
                return Flux.just(-1); // fallback value
            })
            .subscribe(result -> System.out.println("✓ Result: " + result));

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
                data -> System.out.println("✓ Success: " + data),
                error -> System.out.println("✗ Failed after retries: " + error.getMessage())
            );

        System.out.println();
    }

    private void backpressureExample() {
        System.out.println("--- BACKPRESSURE EXAMPLE ---");

        // Fast producer, slow consumer
        Flux.interval(Duration.ofMillis(100)) // Fast producer
            .onBackpressureBuffer(10) // Buffer up to 10 elements
            .take(5)
            .delayElements(Duration.ofMillis(500)) // Slow consumer
            .subscribe(
                value -> System.out.println("✓ Processed: " + value),
                error -> System.out.println("✗ Backpressure error: " + error.getMessage()),
                () -> System.out.println("✓ Backpressure demo completed")
            );

        System.out.println();
    }

    // Helper method to simulate async data fetching
    private Mono<String> fetchUserData(String userId) {
        return Mono.just("Data for " + userId)
            .delayElement(Duration.ofMillis(100));
    }
}
