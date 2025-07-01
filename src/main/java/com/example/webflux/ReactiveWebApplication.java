package com.example.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;

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

    public static void main(String[] args) {
        System.out.println("=== SPRING WEBFLUX DEEP DIVE ===");
        System.out.println("Starting reactive web server...");
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

            // Streaming endpoint
            .andRoute(RequestPredicates.GET("/api/stream"),
                request -> ServerResponse.ok()
                    .body(Flux.interval(Duration.ofSeconds(1))
                        .map(i -> "Stream item " + i)
                        .take(10), String.class))

            // Reactive data processing
            .andRoute(RequestPredicates.GET("/api/process/{count}"),
                request -> {
                    int count = Integer.parseInt(request.pathVariable("count"));
                    return ServerResponse.ok()
                        .body(processDataReactively(count), String.class);
                })

            // Error handling demonstration
            .andRoute(RequestPredicates.GET("/api/error"),
                request -> ServerResponse.ok()
                    .body(demonstrateErrorHandling(), String.class))

            // Backpressure demonstration
            .andRoute(RequestPredicates.GET("/api/backpressure"),
                request -> ServerResponse.ok()
                    .body(demonstrateBackpressure(), String.class));
    }

    private Flux<String> processDataReactively(int count) {
        return Flux.range(1, count)
            .map(i -> "Processing item " + i)
            .delayElements(Duration.ofMillis(100))
            .doOnNext(item -> System.out.println("WebFlux processing: " + item))
            .onErrorReturn("Error occurred during processing");
    }

    private Flux<String> demonstrateErrorHandling() {
        return Flux.just("data1", "data2", "error", "data3")
            .map(data -> {
                if ("error".equals(data)) {
                    throw new RuntimeException("Simulated error");
                }
                return "Processed: " + data;
            })
            .onErrorResume(error -> {
                System.out.println("WebFlux error handled: " + error.getMessage());
                return Flux.just("Fallback data");
            });
    }

    private Flux<String> demonstrateBackpressure() {
        return Flux.range(1, 1000)
            .map(i -> "High volume item " + i)
            .onBackpressureBuffer(50)
            .delayElements(Duration.ofMillis(10))
            .take(20);
    }
}
