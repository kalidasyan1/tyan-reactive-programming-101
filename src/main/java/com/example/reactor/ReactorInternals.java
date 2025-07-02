package com.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ReactorInternals.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== PROJECT REACTOR INTERNALS DEEP DIVE ===\n");

        // Core building blocks
        demonstratePublisherSubscriberPattern();
        Thread.sleep(1000);

        // Operator fusion optimization
        demonstrateOperatorFusion();
        Thread.sleep(1000);

        // Scheduler system
        demonstrateSchedulerSystem();
        Thread.sleep(2000);

        // Context propagation
        demonstrateContextPropagation();
        Thread.sleep(1000);

        // Custom operators
        demonstrateCustomOperator();
        Thread.sleep(1000);

        // Backpressure internals
        demonstrateBackpressureInternals();

        log.info("\n=== ALL REACTOR INTERNALS EXAMPLES COMPLETED ===");
    }

    private static void demonstratePublisherSubscriberPattern() {
        log.info("--- PUBLISHER-SUBSCRIBER PATTERN INTERNALS ---");

        // Create a custom subscriber to see internals
        Flux<Integer> source = Flux.range(1, 5)
            .map(i -> i * 2)
            .filter(i -> i > 4);

        source.subscribe(new CoreSubscriber<Integer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                log.info("  ✓ onSubscribe: Subscription established");
                s.request(Long.MAX_VALUE); // Request all items
            }

            @Override
            public void onNext(Integer integer) {
                log.info("  ✓ onNext: Received {} on thread {}", integer, Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable t) {
                log.error("  ✗ onError: {}", t.getMessage());
            }

            @Override
            public void onComplete() {
                log.info("  ✓ onComplete: Stream finished");
            }
        });

        log.info("");
    }

    private static void demonstrateOperatorFusion() {
        log.info("--- OPERATOR FUSION OPTIMIZATION ---");

        // Reactor optimizes operator chains through fusion
        log.info("Demonstrating operator fusion (internal optimization):");

        // This chain will be optimized internally
        Flux<String> optimizedChain = Flux.range(1, 1000)
            .map(i -> "Item " + i)           // Can be fused
            .filter(s -> s.contains("5"))    // Can be fused
            .map(String::toUpperCase)        // Can be fused
            .take(5);                        // Limits the stream

        optimizedChain.subscribe(item ->
            log.info("  ✓ Fused result: {}", item));

        // Demonstrate conditional operator (special fusion case)
        Flux.range(1, 10)
            .cast(Object.class)  // Forces non-fuseable path
            .map(o -> (Integer) o * 2)
            .subscribe(result ->
                log.info("  ✓ Non-fused result: {}", result));

        log.info("");
    }

    private static void demonstrateSchedulerSystem() {
        log.info("--- SCHEDULER SYSTEM INTERNALS ---");

        // Different schedulers for different workloads
        log.info("Scheduler types and their use cases:");

        // Immediate scheduler (current thread)
        Mono.just("immediate")
            .subscribeOn(Schedulers.immediate())
            .subscribe(value -> log.info("  ✓ Immediate: {} on {}", value, Thread.currentThread().getName()));

        // Single scheduler (single daemon thread)
        Mono.just("single")
            .subscribeOn(Schedulers.single())
            .subscribe(value -> log.info("  ✓ Single: {} on {}", value, Thread.currentThread().getName()));

        // Parallel scheduler (CPU-bound work)
        Flux.range(1, 4)
            .parallel()
            .runOn(Schedulers.parallel())
            .map(i -> {
                log.info("  ✓ Parallel processing {} on {}", i, Thread.currentThread().getName());
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
        .subscribe(value -> log.info("  ✓ BoundedElastic: {} on {}", value, Thread.currentThread().getName()));

        log.info("");
    }

    private static void demonstrateContextPropagation() {
        log.info("--- CONTEXT PROPAGATION INTERNALS ---");

        // Context is Reactor's answer to ThreadLocal in reactive chains
        String userId = "user123";
        String traceId = "trace456";

        Mono.just("processing")
            .flatMap(data ->
                Mono.deferContextual(ctx -> {
                    String user = ctx.get("userId");
                    String trace = ctx.get("traceId");
                    log.info("  ✓ Processing with context - User: {}, Trace: {}", user, trace);
                    return Mono.just(data + " completed");
                }))
            .contextWrite(Context.of("userId", userId, "traceId", traceId))
            .subscribe(result -> log.info("  ✓ Result: {}", result));

        // Context is immutable and flows downstream
        Flux.range(1, 3)
            .flatMap(i ->
                Mono.deferContextual(ctx -> {
                    String context = ctx.getOrDefault("operation", "unknown");
                    return Mono.just("Item " + i + " in " + context);
                }))
            .contextWrite(Context.of("operation", "batch-processing"))
            .subscribe(item -> log.info("  ✓ " + item));

        log.info("");
    }

    private static void demonstrateCustomOperator() {
        log.info("--- CUSTOM OPERATOR IMPLEMENTATION ---");

        // Create a custom operator that adds logging
        Flux<Integer> source = Flux.range(1, 5);

        source.transform(upstream ->
            upstream.doOnNext(item ->
                log.info("  ✓ Custom operator: Processing " + item))
            .map(item -> item * 10)
            .doOnNext(item ->
                log.info("  ✓ Custom operator: Transformed to " + item))
        ).subscribe(result -> log.info("  ✓ Final result: " + result));

        log.info("");
    }

    private static void demonstrateBackpressureInternals() {
        log.info("--- BACKPRESSURE INTERNALS ---");

        // Custom subscriber that controls demand
        Flux.range(1, 100)
            .subscribe(new CoreSubscriber<Integer>() {
                private Subscription subscription;
                private final AtomicLong requestCount = new AtomicLong(0);

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    log.info("  ✓ Backpressure: Starting with demand for 3 items");
                    s.request(3); // Request only 3 items initially
                }

                @Override
                public void onNext(Integer integer) {
                    long count = requestCount.incrementAndGet();
                    log.info("  ✓ Backpressure: Received " + integer +
                        " (total: " + count + ")");

                    // Request more items after processing some
                    if (count % 3 == 0 && count < 10) {
                        log.info("  ✓ Backpressure: Requesting 2 more items");
                        subscription.request(2);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("  ✗ Backpressure error: " + t.getMessage());
                }

                @Override
                public void onComplete() {
                    log.info("  ✓ Backpressure: Completed with " +
                        requestCount.get() + " items processed");
                }
            });

        log.info("");
        printReactorArchitectureSummary();
    }

    private static void printReactorArchitectureSummary() {
        log.info("--- REACTOR ARCHITECTURE SUMMARY ---");
        log.info("CORE COMPONENTS:");
        log.info("  • Publisher: Data source (Mono/Flux)");
        log.info("  • Subscriber: Data consumer with lifecycle callbacks");
        log.info("  • Subscription: Controls demand and cancellation");
        log.info("  • Processor: Both Publisher and Subscriber");

        log.info("\nOPTIMIZATIONS:");
        log.info("  • Operator Fusion: Combines operators for efficiency");
        log.info("  • Conditional: Optimizes filter operations");
        log.info("  • Queue Fusion: Reduces object allocations");

        log.info("\nSCHEDULING:");
        log.info("  • subscribeOn(): Controls subscription thread");
        log.info("  • publishOn(): Controls emission thread");
        log.info("  • Scheduler abstraction over thread pools");

        log.info("\nBACKPRESSURE:");
        log.info("  • Demand-driven: Subscribers control flow");
        log.info("  • Strategies: Buffer, Drop, Latest, Error");
        log.info("  • Built into the protocol");
    }
}
