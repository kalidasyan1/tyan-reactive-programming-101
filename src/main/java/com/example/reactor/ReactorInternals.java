package com.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deep Dive: Project Reactor Internals
 *
 * Architecture Overview:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    PROJECT REACTOR CORE                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  Publisher (Mono/Flux) → Operator Chain → Subscriber       │
 * │                                                             │
 * │  ┌─────────────┐    ┌──────────────┐    ┌──────────────┐   │
 * │  │  Publisher  │───▶│   Operators  │───▶│  Subscriber  │   │
 * │  │ (Data Source)│    │ (Transform)  │    │ (Data Sink)  │   │
 * │  └─────────────┘    └──────────────┘    └──────────────┘   │
 * │                                                             │
 * │  ┌─────────────────────────────────────────────────────────┤
 * │  │                 SCHEDULER SYSTEM                        │
 * │  │  • immediate() - Current thread                         │
 * │  │  • single() - Single daemon thread                     │
 * │  │  • parallel() - CPU-bound tasks                        │
 * │  │  • boundedElastic() - I/O-bound tasks                  │
 * │  └─────────────────────────────────────────────────────────┤
 * │                                                             │
 * │  ┌─────────────────────────────────────────────────────────┤
 * │  │                CONTEXT PROPAGATION                      │
 * │  │  • Thread-local alternative for reactive chains        │
 * │  │  • Immutable key-value store                           │
 * │  └─────────────────────────────────────────────────────────┤
 * └─────────────────────────────────────────────────────────────┘
 */
public class ReactorInternals {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== PROJECT REACTOR INTERNALS DEEP DIVE ===\n");

        // Core building blocks
        demonstratePublisherSubscriberPattern();

        // Operator fusion optimization
        demonstrateOperatorFusion();

        // Scheduler system
        demonstrateSchedulerSystem();

        // Context propagation
        demonstrateContextPropagation();

        // Custom operators
        demonstrateCustomOperator();

        // Backpressure internals
        demonstrateBackpressureInternals();

        Thread.sleep(5000);
    }

    private static void demonstratePublisherSubscriberPattern() {
        System.out.println("--- PUBLISHER-SUBSCRIBER PATTERN INTERNALS ---");

        // Create a custom subscriber to see internals
        Flux<Integer> source = Flux.range(1, 5)
            .map(i -> i * 2)
            .filter(i -> i > 4);

        source.subscribe(new CoreSubscriber<Integer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                System.out.println("  ✓ onSubscribe: Subscription established");
                s.request(Long.MAX_VALUE); // Request all items
            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("  ✓ onNext: Received " + integer +
                    " on thread " + Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("  ✗ onError: " + t.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("  ✓ onComplete: Stream finished");
            }
        });

        System.out.println();
    }

    private static void demonstrateOperatorFusion() {
        System.out.println("--- OPERATOR FUSION OPTIMIZATION ---");

        // Reactor optimizes operator chains through fusion
        System.out.println("Demonstrating operator fusion (internal optimization):");

        // This chain will be optimized internally
        Flux<String> optimizedChain = Flux.range(1, 1000)
            .map(i -> "Item " + i)           // Can be fused
            .filter(s -> s.contains("5"))    // Can be fused
            .map(String::toUpperCase)        // Can be fused
            .take(5);                        // Limits the stream

        optimizedChain.subscribe(item ->
            System.out.println("  ✓ Fused result: " + item));

        // Demonstrate conditional operator (special fusion case)
        Flux.range(1, 10)
            .cast(Object.class)  // Forces non-fuseable path
            .map(o -> (Integer) o * 2)
            .subscribe(result ->
                System.out.println("  ✓ Non-fused result: " + result));

        System.out.println();
    }

    private static void demonstrateSchedulerSystem() {
        System.out.println("--- SCHEDULER SYSTEM INTERNALS ---");

        // Different schedulers for different workloads
        System.out.println("Scheduler types and their use cases:");

        // Immediate scheduler (current thread)
        Mono.just("immediate")
            .subscribeOn(Schedulers.immediate())
            .subscribe(value -> System.out.println("  ✓ Immediate: " + value +
                " on " + Thread.currentThread().getName()));

        // Single scheduler (single daemon thread)
        Mono.just("single")
            .subscribeOn(Schedulers.single())
            .subscribe(value -> System.out.println("  ✓ Single: " + value +
                " on " + Thread.currentThread().getName()));

        // Parallel scheduler (CPU-bound work)
        Flux.range(1, 4)
            .parallel()
            .runOn(Schedulers.parallel())
            .map(i -> {
                System.out.println("  ✓ Parallel processing " + i +
                    " on " + Thread.currentThread().getName());
                return i * i;
            })
            .sequential()
            .subscribe();

        // BoundedElastic scheduler (I/O-bound work)
        Mono.fromCallable(() -> {
            // Simulate I/O operation
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            return "I/O result";
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(value -> System.out.println("  ✓ BoundedElastic: " + value +
            " on " + Thread.currentThread().getName()));

        System.out.println();
    }

    private static void demonstrateContextPropagation() {
        System.out.println("--- CONTEXT PROPAGATION INTERNALS ---");

        // Context is Reactor's answer to ThreadLocal in reactive chains
        String userId = "user123";
        String traceId = "trace456";

        Mono.just("processing")
            .flatMap(data ->
                Mono.deferContextual(ctx -> {
                    String user = ctx.get("userId");
                    String trace = ctx.get("traceId");
                    System.out.println("  ✓ Processing with context - User: " + user +
                        ", Trace: " + trace);
                    return Mono.just(data + " completed");
                }))
            .contextWrite(Context.of("userId", userId, "traceId", traceId))
            .subscribe(result -> System.out.println("  ✓ Result: " + result));

        // Context is immutable and flows downstream
        Flux.range(1, 3)
            .flatMap(i ->
                Mono.deferContextual(ctx -> {
                    String context = ctx.getOrDefault("operation", "unknown");
                    return Mono.just("Item " + i + " in " + context);
                }))
            .contextWrite(Context.of("operation", "batch-processing"))
            .subscribe(item -> System.out.println("  ✓ " + item));

        System.out.println();
    }

    private static void demonstrateCustomOperator() {
        System.out.println("--- CUSTOM OPERATOR IMPLEMENTATION ---");

        // Create a custom operator that adds logging
        Flux<Integer> source = Flux.range(1, 5);

        source.transform(upstream ->
            upstream.doOnNext(item ->
                System.out.println("  ✓ Custom operator: Processing " + item))
            .map(item -> item * 10)
            .doOnNext(item ->
                System.out.println("  ✓ Custom operator: Transformed to " + item))
        ).subscribe(result -> System.out.println("  ✓ Final result: " + result));

        System.out.println();
    }

    private static void demonstrateBackpressureInternals() {
        System.out.println("--- BACKPRESSURE INTERNALS ---");

        // Custom subscriber that controls demand
        Flux.range(1, 100)
            .subscribe(new CoreSubscriber<Integer>() {
                private Subscription subscription;
                private final AtomicLong requestCount = new AtomicLong(0);

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    System.out.println("  ✓ Backpressure: Starting with demand for 3 items");
                    s.request(3); // Request only 3 items initially
                }

                @Override
                public void onNext(Integer integer) {
                    long count = requestCount.incrementAndGet();
                    System.out.println("  ✓ Backpressure: Received " + integer +
                        " (total: " + count + ")");

                    // Request more items after processing some
                    if (count % 3 == 0 && count < 10) {
                        System.out.println("  ✓ Backpressure: Requesting 2 more items");
                        subscription.request(2);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("  ✗ Backpressure error: " + t.getMessage());
                }

                @Override
                public void onComplete() {
                    System.out.println("  ✓ Backpressure: Completed with " +
                        requestCount.get() + " items processed");
                }
            });

        System.out.println();
        printReactorArchitectureSummary();
    }

    private static void printReactorArchitectureSummary() {
        System.out.println("--- REACTOR ARCHITECTURE SUMMARY ---");
        System.out.println("CORE COMPONENTS:");
        System.out.println("  • Publisher: Data source (Mono/Flux)");
        System.out.println("  • Subscriber: Data consumer with lifecycle callbacks");
        System.out.println("  • Subscription: Controls demand and cancellation");
        System.out.println("  • Processor: Both Publisher and Subscriber");

        System.out.println("\nOPTIMIZATIONS:");
        System.out.println("  • Operator Fusion: Combines operators for efficiency");
        System.out.println("  • Conditional: Optimizes filter operations");
        System.out.println("  • Queue Fusion: Reduces object allocations");

        System.out.println("\nSCHEDULING:");
        System.out.println("  • subscribeOn(): Controls subscription thread");
        System.out.println("  • publishOn(): Controls emission thread");
        System.out.println("  • Scheduler abstraction over thread pools");

        System.out.println("\nBACKPRESSURE:");
        System.out.println("  • Demand-driven: Subscribers control flow");
        System.out.println("  • Strategies: Buffer, Drop, Latest, Error");
        System.out.println("  • Built into the protocol");
    }
}
