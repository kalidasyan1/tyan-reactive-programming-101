package com.example.comparison;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Framework Comparison: RxJava vs Project Reactor vs CompletableFuture
 *
 * This class demonstrates the same operations across different frameworks
 * to highlight similarities and differences.
 */
public class FrameworkComparison {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== REACTIVE FRAMEWORKS COMPARISON ===\n");

        // Basic creation patterns
        creationPatterns();

        // Transformation operations
        transformationComparison();

        // Error handling approaches
        errorHandlingComparison();

        // Composition patterns
        compositionPatterns();

        // Performance characteristics
        performanceCharacteristics();

        Thread.sleep(3000);
    }

    private static void creationPatterns() {
        System.out.println("--- CREATION PATTERNS ---");

        // PROJECT REACTOR
        System.out.println("Project Reactor:");
        Mono<String> reactorMono = Mono.just("Hello");
        Flux<Integer> reactorFlux = Flux.just(1, 2, 3, 4, 5);

        reactorMono.subscribe(value -> System.out.println("  Reactor Mono: " + value));
        reactorFlux.subscribe(value -> System.out.println("  Reactor Flux: " + value));

        // RXJAVA
        System.out.println("RxJava:");
        Single<String> rxSingle = Single.just("Hello");
        Observable<Integer> rxObservable = Observable.fromArray(1, 2, 3, 4, 5);

        rxSingle.subscribe(value -> System.out.println("  RxJava Single: " + value));
        rxObservable.subscribe(value -> System.out.println("  RxJava Observable: " + value));

        // COMPLETABLE FUTURE
        System.out.println("CompletableFuture:");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");
        future.thenAccept(value -> System.out.println("  CompletableFuture: " + value));

        System.out.println();
    }

    private static void transformationComparison() {
        System.out.println("--- TRANSFORMATION COMPARISON ---");

        List<String> data = Arrays.asList("apple", "banana", "cherry");

        // PROJECT REACTOR
        System.out.println("Project Reactor transformations:");
        Flux.fromIterable(data)
            .map(String::toUpperCase)
            .filter(fruit -> fruit.length() > 5)
            .subscribe(result -> System.out.println("  Reactor: " + result));

        // RXJAVA
        System.out.println("RxJava transformations:");
        Observable.fromIterable(data)
            .map(String::toUpperCase)
            .filter(fruit -> fruit.length() > 5)
            .subscribe(result -> System.out.println("  RxJava: " + result));

        // COMPLETABLE FUTURE (more verbose)
        System.out.println("CompletableFuture transformations:");
        CompletableFuture.supplyAsync(() -> data)
            .thenApply(list -> list.stream()
                .map(String::toUpperCase)
                .filter(fruit -> fruit.length() > 5)
                .collect(java.util.stream.Collectors.toList()))
            .thenAccept(results -> results.forEach(result ->
                System.out.println("  CompletableFuture: " + result)));

        System.out.println();
    }

    private static void errorHandlingComparison() {
        System.out.println("--- ERROR HANDLING COMPARISON ---");

        // PROJECT REACTOR
        System.out.println("Project Reactor error handling:");
        Mono.fromCallable(() -> riskyOperation())
            .onErrorReturn("Reactor fallback")
            .subscribe(result -> System.out.println("  Reactor: " + result));

        // RXJAVA
        System.out.println("RxJava error handling:");
        Single.fromCallable(FrameworkComparison::riskyOperation)
            .onErrorReturnItem("RxJava fallback")
            .subscribe(result -> System.out.println("  RxJava: " + result));

        // COMPLETABLE FUTURE
        System.out.println("CompletableFuture error handling:");
        CompletableFuture.supplyAsync(FrameworkComparison::riskyOperation)
            .exceptionally(throwable -> "CompletableFuture fallback")
            .thenAccept(result -> System.out.println("  CompletableFuture: " + result));

        System.out.println();
    }

    private static void compositionPatterns() {
        System.out.println("--- COMPOSITION PATTERNS ---");

        // PROJECT REACTOR - Combining streams
        System.out.println("Project Reactor composition:");
        Mono<String> user = Mono.just("John");
        Mono<Integer> age = Mono.just(30);

        Mono.zip(user, age)
            .map(tuple -> tuple.getT1() + " is " + tuple.getT2())
            .subscribe(result -> System.out.println("  Reactor: " + result));

        // RXJAVA - Combining streams
        System.out.println("RxJava composition:");
        Single<String> rxUser = Single.just("John");
        Single<Integer> rxAge = Single.just(30);

        Single.zip(rxUser, rxAge, (u, a) -> u + " is " + a)
            .subscribe(result -> System.out.println("  RxJava: " + result));

        // COMPLETABLE FUTURE - Combining futures
        System.out.println("CompletableFuture composition:");
        CompletableFuture<String> futureUser = CompletableFuture.supplyAsync(() -> "John");
        CompletableFuture<Integer> futureAge = CompletableFuture.supplyAsync(() -> 30);

        futureUser.thenCombine(futureAge, (u, a) -> u + " is " + a)
            .thenAccept(result -> System.out.println("  CompletableFuture: " + result));

        System.out.println();
    }

    private static void performanceCharacteristics() {
        System.out.println("--- PERFORMANCE CHARACTERISTICS ---");

        int itemCount = 1000;

        // PROJECT REACTOR - Optimized for throughput
        long reactorStart = System.currentTimeMillis();
        Flux.range(1, itemCount)
            .map(i -> i * 2)
            .filter(i -> i % 4 == 0)
            .reduce(0, Integer::sum)
            .subscribe(sum -> {
                long reactorTime = System.currentTimeMillis() - reactorStart;
                System.out.println("  Reactor processing time: " + reactorTime + "ms, Sum: " + sum);
            });

        // RXJAVA - Similar performance but different scheduling
        long rxStart = System.currentTimeMillis();
        Observable.range(1, itemCount)
            .map(i -> i * 2)
            .filter(i -> i % 4 == 0)
            .reduce(0, Integer::sum)
            .subscribe(sum -> {
                long rxTime = System.currentTimeMillis() - rxStart;
                System.out.println("  RxJava processing time: " + rxTime + "ms, Sum: " + sum);
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
            System.out.println("  CompletableFuture processing time: " + futureTime + "ms, Sum: " + sum);
        });

        System.out.println();
        printFrameworkAnalysis();
    }

    private static void printFrameworkAnalysis() {
        System.out.println("--- FRAMEWORK ANALYSIS ---");
        System.out.println("PROJECT REACTOR (DOMINANT CHOICE):");
        System.out.println("  ✓ Spring ecosystem integration");
        System.out.println("  ✓ Optimized for high throughput");
        System.out.println("  ✓ Built-in backpressure support");
        System.out.println("  ✓ Extensive operator library");
        System.out.println("  ✓ Active development and community");

        System.out.println("\nRXJAVA:");
        System.out.println("  ✓ Mature and stable");
        System.out.println("  ✓ Rich operator set");
        System.out.println("  ✓ Good Android support");
        System.out.println("  ✗ Less Spring integration");
        System.out.println("  ✗ More complex threading model");

        System.out.println("\nCOMPLETABLE FUTURE:");
        System.out.println("  ✓ Part of Java standard library");
        System.out.println("  ✓ Simple for basic async operations");
        System.out.println("  ✗ Limited reactive capabilities");
        System.out.println("  ✗ No backpressure support");
        System.out.println("  ✗ Verbose for complex compositions");
    }

    private static String riskyOperation() {
        if (Math.random() > 0.5) {
            throw new RuntimeException("Random failure");
        }
        return "Success";
    }
}
