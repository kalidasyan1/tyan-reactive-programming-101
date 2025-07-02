package com.example.comparison;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;
import java.util.List;

/**
 * Framework Comparison: RxJava vs Project Reactor vs CompletableFuture
 *
 * This class demonstrates the same operations across different frameworks
 * to highlight similarities and differences.
 */
public class FrameworkComparison {

    private static final Logger log = LoggerFactory.getLogger(FrameworkComparison.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== REACTIVE FRAMEWORKS COMPARISON ===\n");

        // Basic creation patterns
        creationPatterns();
        Thread.sleep(1000); // Wait for async operations

        // Transformation operations
        transformationComparison();
        Thread.sleep(500);

        // Error handling approaches
        errorHandlingComparison();
        Thread.sleep(1000);

        // Composition patterns
        compositionPatterns();
        Thread.sleep(500);

        // Performance characteristics
        performanceCharacteristics();
        Thread.sleep(2000);

        log.info("\n=== ALL FRAMEWORK COMPARISONS COMPLETED ===");
    }

    private static void creationPatterns() {
        log.info("--- CREATION PATTERNS ---");

        // PROJECT REACTOR
        log.info("Project Reactor:");
        Mono<String> reactorMono = Mono.just("Hello");
        Flux<Integer> reactorFlux = Flux.just(1, 2, 3, 4, 5);

        reactorMono.subscribe(value -> log.info("  Reactor Mono: {}", value));
        reactorFlux.subscribe(value -> log.info("  Reactor Flux: {}", value));

        // RXJAVA
        log.info("RxJava:");
        Single<String> rxSingle = Single.just("Hello");
        Observable<Integer> rxObservable = Observable.fromArray(1, 2, 3, 4, 5);

        rxSingle.subscribe(value -> log.info("  RxJava Single: {}", value));
        rxObservable.subscribe(value -> log.info("  RxJava Observable: {}", value));

        // COMPLETABLE FUTURE
        log.info("CompletableFuture:");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");
        future.thenAccept(value -> log.info("  CompletableFuture: {}", value));

        log.info("");
    }

    private static void transformationComparison() {
        log.info("--- TRANSFORMATION COMPARISON ---");

        List<String> data = Arrays.asList("apple", "banana", "cherry");

        // PROJECT REACTOR
        log.info("Project Reactor transformations:");
        Flux.fromIterable(data)
            .map(String::toUpperCase)
            .filter(fruit -> fruit.length() > 5)
            .subscribe(result -> log.info("  Reactor: {}", result));

        // RXJAVA
        log.info("RxJava transformations:");
        Observable.fromIterable(data)
            .map(String::toUpperCase)
            .filter(fruit -> fruit.length() > 5)
            .subscribe(result -> log.info("  RxJava: {}", result));

        // COMPLETABLE FUTURE (more verbose)
        log.info("CompletableFuture transformations:");
        CompletableFuture.supplyAsync(() -> data)
            .thenApply(list -> list.stream()
                .map(String::toUpperCase)
                .filter(fruit -> fruit.length() > 5)
                .collect(java.util.stream.Collectors.toList()))
            .thenAccept(results -> results.forEach(result ->
                log.info("  CompletableFuture: {}", result)));

        log.info("");
    }

    private static void errorHandlingComparison() {
        log.info("--- ERROR HANDLING COMPARISON ---");

        // PROJECT REACTOR
        log.info("Project Reactor error handling:");
        Mono.fromCallable(() -> riskyOperation())
            .onErrorReturn("Reactor fallback")
            .subscribe(result -> log.info("  Reactor: {}", result));

        // RXJAVA
        log.info("RxJava error handling:");
        Single.fromCallable(FrameworkComparison::riskyOperation)
            .onErrorReturnItem("RxJava fallback")
            .subscribe(result -> log.info("  RxJava: {}", result));

        // COMPLETABLE FUTURE
        log.info("CompletableFuture error handling:");
        CompletableFuture.supplyAsync(FrameworkComparison::riskyOperation)
            .exceptionally(throwable -> "CompletableFuture fallback")
            .thenAccept(result -> log.info("  CompletableFuture: {}", result));

        log.info("");
    }

    private static void compositionPatterns() {
        log.info("--- COMPOSITION PATTERNS ---");

        // PROJECT REACTOR - Combining streams
        log.info("Project Reactor composition:");
        Mono<String> user = Mono.just("John");
        Mono<Integer> age = Mono.just(30);

        Mono.zip(user, age)
            .map(tuple -> tuple.getT1() + " is " + tuple.getT2())
            .subscribe(result -> log.info("  Reactor: {}", result));

        // RXJAVA - Combining streams
        log.info("RxJava composition:");
        Single<String> rxUser = Single.just("John");
        Single<Integer> rxAge = Single.just(30);

        Single.zip(rxUser, rxAge, (u, a) -> u + " is " + a)
            .subscribe(result -> log.info("  RxJava: {}", result));

        // COMPLETABLE FUTURE - Combining futures
        log.info("CompletableFuture composition:");
        CompletableFuture<String> futureUser = CompletableFuture.supplyAsync(() -> "John");
        CompletableFuture<Integer> futureAge = CompletableFuture.supplyAsync(() -> 30);

        futureUser.thenCombine(futureAge, (u, a) -> u + " is " + a)
            .thenAccept(result -> log.info("  CompletableFuture: {}", result));

        log.info("");
    }

    private static void performanceCharacteristics() {
        log.info("--- PERFORMANCE CHARACTERISTICS ---");

        int itemCount = 1000;

        // PROJECT REACTOR - Optimized for throughput
        long reactorStart = System.currentTimeMillis();
        Flux.range(1, itemCount)
            .map(i -> i * 2)
            .filter(i -> i % 4 == 0)
            .reduce(0, Integer::sum)
            .subscribe(sum -> {
                long reactorTime = System.currentTimeMillis() - reactorStart;
                log.info("  Reactor processing time: {}ms, Sum: {}", reactorTime, sum);
            });

        // RXJAVA - Similar performance but different scheduling
        long rxStart = System.currentTimeMillis();
        Observable.range(1, itemCount)
            .map(i -> i * 2)
            .filter(i -> i % 4 == 0)
            .reduce(0, Integer::sum)
            .subscribe(sum -> {
                long rxTime = System.currentTimeMillis() - rxStart;
                log.info("  RxJava processing time: {}ms, Sum: {}", rxTime, sum);
            });

        // COMPLETABLE FUTURE - Less optimized for streams
        long futureStart = System.currentTimeMillis();
        CompletableFuture.supplyAsync(() -> {
            return java.util.stream.IntStream.range(1, itemCount + 1)
                .map(i -> i * 2)
                .filter(i -> i % 4 == 0)
                .sum();
        }).thenAccept(sum -> {
            long futureTime = System.currentTimeMillis() - futureStart;
            log.info("  CompletableFuture processing time: {}ms, Sum: {}", futureTime, sum);
        });

        log.info("");
        printFrameworkAnalysis();
    }

    private static String riskyOperation() {
        // Simulate potential failure
        if (Math.random() > 0.5) {
            throw new RuntimeException("Simulated failure");
        }
        return "Success";
    }

    private static void printFrameworkAnalysis() {
        log.info("--- FRAMEWORK ANALYSIS ---");
        log.info("PROJECT REACTOR:");
        log.info("  ✓ Native Spring integration");
        log.info("  ✓ Optimized for high throughput");
        log.info("  ✓ Built-in backpressure handling");
        log.info("  ✓ Operator fusion optimizations");

        log.info("RXJAVA:");
        log.info("  ✓ Mature ecosystem");
        log.info("  ✓ Rich operator library");
        log.info("  ✓ Platform agnostic (Android, Server)");
        log.info("  ✓ Well-established patterns");

        log.info("COMPLETABLEFUTURE:");
        log.info("  ✓ Part of standard library");
        log.info("  ✓ Good for simple async tasks");
        log.info("  ✗ Limited stream processing capabilities");
        log.info("  ✗ No built-in backpressure");
    }
}
