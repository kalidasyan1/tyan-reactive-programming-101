package com.example.nio;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
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

    private static final Logger log = LoggerFactory.getLogger(NioReactiveRelationship.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== NIO AND REACTIVE FRAMEWORKS RELATIONSHIP ===\n");

        NioReactiveRelationship demo = new NioReactiveRelationship();

        // Demonstrate raw NIO
        try {
            demo.demonstrateRawNio();
        } catch (IOException e) {
            log.error("Error in NIO demonstration: {}", e.getMessage());
        }

        // Show how Reactor builds on NIO concepts
        demo.demonstrateReactorOnNio();

        // Show Reactor Netty in action
        demo.demonstrateReactorNetty();

        Thread.sleep(3000);
    }

    private void demonstrateRawNio() throws IOException, InterruptedException {
        log.info("--- RAW JAVA NIO EXAMPLE ---");
        log.info("Non-blocking file read using AsynchronousFileChannel:");

        // Create a test file
        Path testFile = Paths.get("test-nio.txt");
        java.nio.file.Files.write(testFile, "Hello NIO World!".getBytes());

        // Asynchronous file reading with NIO - using try-with-resources
        try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                testFile, StandardOpenOption.READ)) {

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            CompletableFuture<String> future = new CompletableFuture<>();

            fileChannel.read(buffer, 0, null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(Integer result, Void attachment) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String content = new String(data);
                    future.complete(content);
                    log.info("  ✓ NIO Read completed: {}", content);
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    future.completeExceptionally(exc);
                    log.error("  ✗ NIO Read failed: {}", exc.getMessage());
                }
            });

            // Wait for the async operation to complete
            Thread.sleep(500);
        }

        // Clean up
        try {
            java.nio.file.Files.deleteIfExists(testFile);
        } catch (IOException e) {
            log.warn("Could not delete test file: {}", e.getMessage());
        }

        log.info("");
    }

    private void demonstrateReactorOnNio() {
        log.info("--- REACTOR BUILDING ON NIO CONCEPTS ---");

        // Reactor provides higher-level abstractions over NIO
        log.info("Reactor's non-blocking approach (built on NIO principles):");

        // Non-blocking file operations with Reactor
        Flux.just("file1.txt", "file2.txt", "file3.txt")
            .flatMap(filename ->
                Mono.fromCallable(() -> "Content of " + filename)
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            )
            .doOnNext(content -> log.info("  ✓ {}", content))
            .blockLast(); // Only for demo - normally you'd subscribe()

        // Event-driven processing (similar to NIO's callback model)
        Flux.interval(Duration.ofMillis(200))
            .take(5)
            .map(i -> "Event " + i + " processed")
            .subscribe(event -> log.info("  ✓ {}", event));

        log.info("");
    }

    private void demonstrateReactorNetty() throws InterruptedException {
        log.info("--- REACTOR NETTY IN ACTION ---");

        // Start a simple HTTP server using Reactor Netty
        DisposableServer server = HttpServer.create()
            .port(8090)
            .route(routes ->
                routes.get("/hello", (request, response) ->
                    response.sendString(Mono.just("Hello from Reactor Netty!")))
            )
            .bindNow();

        log.info("  ✓ Reactor Netty server started on port 8090");

        // Make a client request
        Thread.sleep(1000); // Give server time to start

        HttpClient.create()
            .get()
            .uri("http://localhost:8090/hello")
            .responseContent()
            .aggregate()
            .asString()
            .subscribe(
                response -> log.info("  ✓ Client received: {}", response),
                error -> log.error("  ✗ Client error: {}", error.getMessage())
            );

        Thread.sleep(2000); // Let the request complete
        server.dispose();
        log.info("  ✓ Server stopped");

        printNioReactiveComparison();
    }

    private void printNioReactiveComparison() {
        log.info("\n--- NIO vs REACTIVE COMPARISON ---");
        log.info("RAW JAVA NIO:");
        log.info("  ✓ Direct control over channels and buffers");
        log.info("  ✓ Maximum performance potential");
        log.info("  ✗ Complex callback-based programming");
        log.info("  ✗ Manual resource management");
        log.info("  ✗ Difficult error handling and composition");

        log.info("\nREACTIVE FRAMEWORKS (Reactor):");
        log.info("  ✓ High-level abstractions over NIO");
        log.info("  ✓ Declarative stream processing");
        log.info("  ✓ Built-in backpressure handling");
        log.info("  ✓ Excellent error handling and recovery");
        log.info("  ✓ Easy composition and transformation");
        log.info("  ⚠️ Small performance overhead for abstraction");

        log.info("\nREACTOR NETTY:");
        log.info("  ✓ Best of both worlds - NIO performance + Reactive APIs");
        log.info("  ✓ Production-ready HTTP/TCP servers and clients");
        log.info("  ✓ Integration with Spring WebFlux");
        log.info("  ✓ Automatic resource management");

        explainArchitecture();
    }

    private void explainArchitecture() {
        log.info("\n--- ARCHITECTURE EXPLANATION ---");
        log.info("JAVA NIO (Foundation):");
        log.info("  • Non-blocking I/O operations");
        log.info("  • Selectors for multiplexing");
        log.info("  • Channels and Buffers");
        log.info("  • Event-driven programming model");

        log.info("\nNETTY (Network Layer):");
        log.info("  • Built on top of Java NIO");
        log.info("  • Event loop and channel pipeline");
        log.info("  • High-performance network protocols");
        log.info("  • Memory management and zero-copy");

        log.info("\nREACTOR NETTY (Reactive Network):");
        log.info("  • Reactive streams over Netty");
        log.info("  • Backpressure-aware network operations");
        log.info("  • HTTP/TCP/UDP reactive servers and clients");
        log.info("  • Integration with Project Reactor");

        log.info("\nPROJECT REACTOR (Reactive Streams):");
        log.info("  • Publisher/Subscriber implementation");
        log.info("  • Mono (0-1) and Flux (0-N) abstractions");
        log.info("  • Operator chains and transformations");
        log.info("  • Scheduler abstraction");

        log.info("\nSPRING WEBFLUX (Web Framework):");
        log.info("  • Reactive web programming model");
        log.info("  • Functional and annotation-based routing");
        log.info("  • WebClient for reactive HTTP clients");
        log.info("  • Integration with reactive data access");
    }
}
