package com.example.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring WebFlux Application - Demonstrating Reactive Web Architecture
 *
 * WebFlux Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                      SPRING WEBFLUX                             │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  HTTP Request → WebFilter → Handler → WebFilter → HTTP Response │
 * │                                                                 │
 * │  ┌─────────────────┐    ┌──────────────────┐                   │
 * │  │  Reactive Web   │    │   Functional     │                   │
 * │  │  (Annotation)   │    │   Programming    │                   │
 * │  │  @Controller    │    │   RouterFunction │                   │
 * │  └─────────────────┘    └──────────────────┘                   │
 * │                                                                 │
 * │  ┌─────────────────────────────────────────────────────────────┤
 * │  │                  REACTOR NETTY                              │
 * │  │  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐     │
 * │  │  │ Event Loop  │    │   Channel    │    │   Handler   │     │
 * │  │  │   Groups    │    │   Pipeline   │    │   Chain     │     │
 * │  │  └─────────────┘    └──────────────┘    └─────────────┘     │
 * │  └─────────────────────────────────────────────────────────────┤
 * └─────────────────────────────────────────────────────────────────┘
 */
@SpringBootApplication
public class ReactiveWebApplication {

    private static final Logger log = LoggerFactory.getLogger(ReactiveWebApplication.class);

    /**
     * Custom exception for demonstrating error handling in reactive streams
     */
    public static class ReactiveProcessingException extends RuntimeException {
        public ReactiveProcessingException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        log.info("=== SPRING WEBFLUX DEEP DIVE ===");
        log.info("Starting reactive web server...");
        log.info("\nTry these endpoints:");
        log.info("- curl localhost:8080/api/stream (basic streaming)");
        log.info("- curl localhost:8080/api/stream-sse (Server-Sent Events)");
        log.info("- curl localhost:8080/api/stream-json (JSON streaming)");
        log.info("- curl localhost:8080/api/backpressure-demo (backpressure simulation)");
        SpringApplication.run(ReactiveWebApplication.class, args);
    }

    /**
     * Functional Router Configuration
     * Demonstrates WebFlux functional programming model
     */
    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions
            // Basic reactive endpoint
            .route(RequestPredicates.GET("/api/hello"),
                request -> ServerResponse.ok()
                    .bodyValue("Hello from WebFlux!"))

            // Basic streaming endpoint with proper delimiters
            .andRoute(RequestPredicates.GET("/api/stream"),
                request -> ServerResponse.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(Flux.interval(Duration.ofSeconds(1))
                        .map(i -> "Stream item " + i + " at " + LocalTime.now() + "\n")
                        .doOnNext(item -> log.info("Streaming: {}", item.trim()))
                        .take(10), String.class))

            // Server-Sent Events streaming (best for real-time updates)
            .andRoute(RequestPredicates.GET("/api/stream-sse"),
                request -> ServerResponse.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(Flux.interval(Duration.ofMillis(500))
                        .map(i -> "data: {\"id\": " + i + ", \"message\": \"SSE item " + i + "\", \"timestamp\": \"" + LocalTime.now() + "\"}\n\n")
                        .doOnNext(item -> log.info("SSE: {}", item.trim()))
                        .take(20), String.class))

            // JSON streaming with proper content type
            .andRoute(RequestPredicates.GET("/api/stream-json"),
                request -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_NDJSON) // Newline Delimited JSON
                    .body(Flux.interval(Duration.ofMillis(800))
                        .map(i -> "{\"id\": " + i + ", \"data\": \"JSON item " + i + "\", \"timestamp\": \"" + LocalTime.now() + "\"}\n")
                        .doOnNext(item -> log.info("JSON: {}", item.trim()))
                        .take(15), String.class))

            // Reactive data processing
            .andRoute(RequestPredicates.GET("/api/process/{count}"),
                request -> {
                    int count = Integer.parseInt(request.pathVariable("count"));
                    return ServerResponse.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(processDataReactively(count), String.class);
                })

            // Error handling demonstration
            .andRoute(RequestPredicates.GET("/api/error"),
                request -> ServerResponse.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(demonstrateErrorHandling(), String.class))

            // Enhanced backpressure demonstration
            .andRoute(RequestPredicates.GET("/api/backpressure"),
                request -> ServerResponse.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(demonstrateBackpressure(), String.class))

            // New: Detailed backpressure demonstration
            .andRoute(RequestPredicates.GET("/api/backpressure-demo"),
                request -> ServerResponse.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(demonstrateDetailedBackpressure(), String.class));
    }

    private Flux<String> processDataReactively(int count) {
        return Flux.range(1, count)
            .map(i -> "Processing item " + i + "\n")
            .delayElements(Duration.ofMillis(100))
            .doOnNext(item -> log.info("WebFlux processing: {}", item.trim()))
            .onErrorReturn("Error occurred during processing\n");
    }

    private Flux<String> demonstrateErrorHandling() {
        return Flux.just("data1", "data2", "error", "data3")
            .map(data -> {
                if ("error".equals(data)) {
                    throw new ReactiveProcessingException("Simulated error in reactive stream");
                }
                return "Processed: " + data + "\n";
            })
            .onErrorResume(error -> {
                log.warn("WebFlux error handled: {}", error.getMessage());
                return Flux.just("Fallback data\n");
            });
    }

    private Flux<String> demonstrateBackpressure() {
        return Flux.range(1, 1000)
            .map(i -> "High volume item " + i + "\n")
            .onBackpressureBuffer(50)
            .delayElements(Duration.ofMillis(10))
            .take(20);
    }

    // New method to better demonstrate backpressure
    private Flux<String> demonstrateDetailedBackpressure() {
        log.info("=== BACKPRESSURE DEMONSTRATION ===");

        return Flux.create(sink -> {
            // Simulate a fast producer
            for (int i = 1; i <= 100; i++) {
                String item = "Fast producer item " + i + " (generated at " + LocalTime.now() + ")\n";
                sink.next(item);
                log.debug("Producer: Generated item {}", i);

                // Instead of blocking Thread.sleep, we'll rely on the reactive scheduling
                // This demonstrates proper non-blocking reactive patterns
            }
            sink.complete();
            log.info("Producer: Finished generating all items");
        })
        .onBackpressureBuffer(10, // Small buffer to trigger backpressure
            item -> log.warn("BACKPRESSURE: Dropping item: {}", item.toString().trim()))
        .delayElements(Duration.ofMillis(200)) // Slow consumer - this is non-blocking
        .doOnNext(item -> log.info("Consumer: Processing {}", item.toString().trim()))
        .doOnComplete(() -> log.info("Consumer: Completed processing"))
        .doOnCancel(() -> log.info("Consumer: Cancelled"))
        .cast(String.class);
    }
}
