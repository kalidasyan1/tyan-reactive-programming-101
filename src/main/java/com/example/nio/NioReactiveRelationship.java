package com.example.nio;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Understanding the Relationship: Reactive Frameworks, NIO, and Netty
 *
 * This class demonstrates how reactive frameworks build upon Java NIO
 * and how Netty provides the foundation for high-performance reactive applications.
 *
 * Architecture Stack:
 * ┌─────────────────────────┐
 * │   Spring WebFlux        │  (High-level reactive web framework)
 * ├─────────────────────────┤
 * │   Project Reactor       │  (Reactive streams implementation)
 * ├─────────────────────────┤
 * │   Reactor Netty         │  (Reactive network layer)
 * ├─────────────────────────┤
 * │   Netty                 │  (Async event-driven network framework)
 * ├─────────────────────────┤
 * │   Java NIO              │  (Non-blocking I/O foundation)
 * └─────────────────────────┘
 */
public class NioReactiveRelationship {

    public static void main(String[] args) throws Exception {
        System.out.println("=== NIO AND REACTIVE FRAMEWORKS RELATIONSHIP ===\n");

        // Demonstrate raw NIO
        demonstrateRawNio();

        // Show how Reactor builds on NIO concepts
        demonstrateReactorOverNio();

        // Show Reactor Netty in action
        demonstrateReactorNetty();

        // Explain the architecture
        explainArchitecture();

        Thread.sleep(3000);
    }

    private static void demonstrateRawNio() {
        System.out.println("--- RAW JAVA NIO EXAMPLE ---");
        System.out.println("Non-blocking file read using AsynchronousFileChannel:");

        try {
            // Create a temporary file for demonstration
            String content = "Hello NIO World!\nThis is reactive programming foundation.";
            java.nio.file.Files.write(Paths.get("temp.txt"), content.getBytes());

            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get("temp.txt"), StandardOpenOption.READ);

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            // Non-blocking read with callback
            fileChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    byte[] data = new byte[attachment.remaining()];
                    attachment.get(data);
                    System.out.println("  ✓ NIO Read completed: " + new String(data));

                    try {
                        fileChannel.close();
                        java.nio.file.Files.deleteIfExists(Paths.get("temp.txt"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    System.err.println("  ✗ NIO Read failed: " + exc.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("NIO example failed: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateReactorOverNio() {
        System.out.println("--- REACTOR BUILDING ON NIO CONCEPTS ---");

        // Reactor abstracts NIO complexity into streams
        System.out.println("Reactor's non-blocking approach (built on NIO principles):");

        // Simulate async I/O operations
        Mono<String> fileRead = Mono.fromCallable(() -> {
            // This would use NIO under the hood in real implementation
            return "File content from reactive stream";
        }).delayElement(Duration.ofMillis(100)); // Simulate I/O delay

        Mono<String> networkCall = Mono.fromCallable(() -> {
            // This would use Netty/NIO for network operations
            return "Network response from reactive stream";
        }).delayElement(Duration.ofMillis(150));

        // Combine multiple I/O operations without blocking
        Mono.zip(fileRead, networkCall)
            .map(tuple -> "Combined: " + tuple.getT1() + " + " + tuple.getT2())
            .subscribe(result -> System.out.println("  ✓ " + result));

        // Event-driven processing (core NIO concept)
        Flux.interval(Duration.ofMillis(200))
            .take(3)
            .map(tick -> "Event " + tick + " processed non-blocking")
            .subscribe(event -> System.out.println("  ✓ " + event));

        System.out.println();
    }

    private static void demonstrateReactorNetty() {
        System.out.println("--- REACTOR NETTY IN ACTION ---");

        // Create a simple HTTP server using Reactor Netty
        HttpServer server = HttpServer.create()
            .port(8080)
            .route(routes -> routes
                .get("/hello", (request, response) ->
                    response.sendString(Mono.just("Hello from Reactor Netty!")))
                .get("/stream", (request, response) ->
                    response.sendString(
                        Flux.interval(Duration.ofSeconds(1))
                            .take(5)
                            .map(i -> "Stream item " + i + "\n")))
            );

        // Start server (non-blocking)
        server.bindNow(); // In production, use .bind() which returns Mono<DisposableServer>
        System.out.println("  ✓ Reactor Netty server started on port 8080");

        // Create HTTP client
        HttpClient client = HttpClient.create();

        // Make non-blocking HTTP calls
        client.get()
            .uri("http://localhost:8080/hello")
            .responseContent()
            .asString()
            .subscribe(
                response -> System.out.println("  ✓ Client received: " + response),
                error -> System.err.println("  ✗ Client error: " + error.getMessage())
            );

        System.out.println();
    }

    private static void explainArchitecture() {
        System.out.println("--- ARCHITECTURE EXPLANATION ---");

        System.out.println("JAVA NIO (Foundation):");
        System.out.println("  • Non-blocking I/O operations");
        System.out.println("  • Selectors for multiplexing");
        System.out.println("  • Channels and Buffers");
        System.out.println("  • Event-driven programming model");

        System.out.println("\nNETTY (Network Layer):");
        System.out.println("  • Built on Java NIO");
        System.out.println("  • Event loop groups for handling I/O");
        System.out.println("  • Channel pipeline for processing");
        System.out.println("  • High-performance async network operations");

        System.out.println("\nREACTOR NETTY (Reactive Network):");
        System.out.println("  • Reactive wrapper around Netty");
        System.out.println("  • Publisher/Subscriber pattern");
        System.out.println("  • Backpressure support");
        System.out.println("  • Integration with Project Reactor");

        System.out.println("\nPROJECT REACTOR (Reactive Streams):");
        System.out.println("  • Implements Reactive Streams specification");
        System.out.println("  • Mono and Flux publishers");
        System.out.println("  • Rich operator library");
        System.out.println("  • Scheduler abstraction over thread pools");

        System.out.println("\nSPRING WEBFLUX (Application Framework):");
        System.out.println("  • Built on Project Reactor");
        System.out.println("  • Uses Reactor Netty as default server");
        System.out.println("  • Functional and annotation-based programming");
        System.out.println("  • Reactive database integration (R2DBC)");

        System.out.println("\nSHARED PRINCIPLES:");
        System.out.println("  ✓ Non-blocking I/O");
        System.out.println("  ✓ Event-driven architecture");
        System.out.println("  ✓ Resource efficiency");
        System.out.println("  ✓ Asynchronous processing");
        System.out.println("  ✓ Backpressure handling");

        explainEventLoopModel();
    }

    private static void explainEventLoopModel() {
        System.out.println("\n--- EVENT LOOP MODEL ---");

        // Demonstrate event loop concept
        LoopResources eventLoop = LoopResources.create("demo-event-loop", 2, true);

        System.out.println("Event Loop Characteristics:");
        System.out.println("  • Single-threaded event processing");
        System.out.println("  • Non-blocking operations");
        System.out.println("  • Efficient resource utilization");

        // Show how tasks are handled in event loop
        Flux.range(1, 5)
            .flatMap(i -> Mono.fromCallable(() -> {
                System.out.println("  ✓ Task " + i + " on thread: " +
                    Thread.currentThread().getName());
                return "Result " + i;
            }).subscribeOn(Schedulers.boundedElastic()))
            .subscribe();

        // Cleanup
        eventLoop.dispose();
    }
}
