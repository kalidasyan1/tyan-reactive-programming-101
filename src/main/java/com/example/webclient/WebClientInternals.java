package com.example.webclient;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

/**
 * Deep Dive: WebClient Internals and Implementation
 *
 * WebClient Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                        WEBCLIENT                                │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Request → Filters → Connector → HTTP Client → Network          │
 * │                                                                 │
 * │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
 * │  │   Builder   │  │   Filters   │  │    Client Connector     │  │
 * │  │   Config    │─▶│   Chain     │─▶│   (Reactor Netty)       │  │
 * │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
 * │                                                                 │
 * │  ┌─────────────────────────────────────────────────────────────┤
 * │  │              EXCHANGE STRATEGIES                            │
 * │  │  • Codecs for serialization/deserialization               │
 * │  │  • Message readers/writers                                 │
 * │  │  • Content type negotiation                               │
 * │  └─────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │  ┌─────────────────────────────────────────────────────────────┤
 * │  │              CONNECTION MANAGEMENT                          │
 * │  │  • Connection pooling                                      │
 * │  │  • Keep-alive handling                                     │
 * │  │  • SSL/TLS configuration                                   │
 * │  └─────────────────────────────────────────────────────────────┤
 * └─────────────────────────────────────────────────────────────────┘
 */
public class WebClientInternals {

    private static final Logger log = LoggerFactory.getLogger(WebClientInternals.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== WEBCLIENT INTERNALS DEEP DIVE ===\n");

        WebClientInternals demo = new WebClientInternals();

        // Basic WebClient usage
        demo.demonstrateBasicUsage();
        Thread.sleep(3000); // Wait for async operations

        // Advanced configuration
        demo.demonstrateAdvancedConfiguration();
        Thread.sleep(3000);

        // Connection pooling and management
        demo.demonstrateConnectionManagement();
        Thread.sleep(6000);

        // Error handling and retries
        demo.demonstrateErrorHandlingAndRetries();
        Thread.sleep(6000);

        // Streaming responses
        demo.demonstrateStreamingResponses();
        Thread.sleep(3000);

        // Performance optimization
        demo.demonstratePerformanceOptimization();
        Thread.sleep(8000); // Wait for parallel requests

        log.info("\n=== ALL WEBCLIENT EXAMPLES COMPLETED ===");
    }

    private void demonstrateBasicUsage() {
        log.info("--- BASIC WEBCLIENT USAGE ---");

        WebClient client = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();

        // GET request
        client.get()
            .uri("/posts/1")
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> log.info("  ✓ GET Response: {}...", response.substring(0, Math.min(100, response.length()))),
                error -> log.error("  ✗ GET Error: {}", error.getMessage())
            );

        // POST request
        client.post()
            .uri("/posts")
            .bodyValue(Map.of("title", "WebClient Test", "body", "Testing WebClient internals"))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> log.info("  ✓ POST Response: {}...", response.substring(0, Math.min(100, response.length()))),
                error -> log.error("  ✗ POST Error: {}", error.getMessage())
            );

        log.info("");
    }

    private void demonstrateAdvancedConfiguration() {
        log.info("--- ADVANCED WEBCLIENT CONFIGURATION ---");

        // Custom HttpClient with connection pooling
        HttpClient httpClient = HttpClient.create()
            .doOnConnected(conn ->
                log.info("  ✓ Connection established to: {}", conn.channel().remoteAddress()))
            .compress(true);

        WebClient advancedClient = WebClient.builder()
            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024);
                log.info("  ✓ Custom codec configuration applied");
            })
            .filter(logRequest())
            .filter(logResponse())
            .build();

        advancedClient.get()
            .uri("https://jsonplaceholder.typicode.com/posts/1")
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> log.info("  ✓ Advanced client response received"),
                error -> log.error("  ✗ Advanced client error: {}", error.getMessage())
            );

        log.info("");
    }

    private void demonstrateConnectionManagement() {
        log.info("--- CONNECTION MANAGEMENT ---");

        // Custom connection provider
        ConnectionProvider provider = ConnectionProvider.builder("custom")
            .maxConnections(50)
            .pendingAcquireMaxCount(100)
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .maxIdleTime(Duration.ofSeconds(20))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .doOnConnected(conn ->
                log.info("  ✓ Pooled connection: {}", conn.channel().id()))
            .doOnDisconnected(conn ->
                log.info("  ✓ Connection released: {}", conn.channel().id()));

        WebClient pooledClient = WebClient.builder()
            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
            .build();

        // Make multiple requests to see connection reuse
        Flux.range(1, 3)
            .flatMap(i ->
                pooledClient.get()
                    .uri("https://jsonplaceholder.typicode.com/posts/" + i)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> "Request " + i + " completed"))
            .subscribe(
                result -> log.info("  ✓ {}", result),
                error -> log.error("  ✗ Connection error: {}", error.getMessage())
            );

        log.info("");
    }

    private void demonstrateErrorHandlingAndRetries() {
        log.info("--- ERROR HANDLING AND RETRIES ---");

        WebClient client = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .filter(handle5xxErrors())
            .build();

        // Simulate error handling
        client.get()
            .uri("/posts/nonexistent")
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1)))
            .onErrorResume(error -> {
                log.warn("  ⚠️ Fallback triggered after retries: {}", error.getMessage());
                return Mono.just("Fallback response");
            })
            .subscribe(
                result -> log.info("  ✓ Final result: {}", result),
                error -> log.error("  ✗ Unhandled error: {}", error.getMessage())
            );

        log.info("");
        printWebClientArchitecture();
    }

    private void demonstrateStreamingResponses() {
        log.info("--- STREAMING RESPONSES ---");

        WebClient streamClient = WebClient.create("https://httpbin.org");

        // Stream JSON array response
        streamClient.get()
            .uri("/json")
            .retrieve()
            .bodyToFlux(String.class)
            .take(1) // Limit for demo
            .subscribe(
                chunk -> log.info("  ✓ Streamed chunk: " + chunk.substring(0, Math.min(50, chunk.length())) + "..."),
                error -> log.error("  ✗ Stream error: " + error.getMessage()),
                () -> log.info("  ✓ Stream completed")
            );

        // Handle large responses with streaming
        streamClient.get()
            .uri("/bytes/1024") // Get 1KB of random data
            .retrieve()
            .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
            .map(buffer -> {
                int size = buffer.readableByteCount();
                log.info("  ✓ Streamed buffer size: " + size + " bytes");
                org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                return size;
            })
            .reduce(0, Integer::sum)
            .subscribe(
                totalBytes -> log.info("  ✓ Total bytes streamed: " + totalBytes),
                error -> log.error("  ✗ Streaming error: " + error.getMessage())
            );

        log.info("");
    }

    private void demonstratePerformanceOptimization() {
        log.info("--- PERFORMANCE OPTIMIZATION ---");

        WebClient perfClient = WebClient.create("https://httpbin.org");

        // Variables to capture timing results
        final long[] sequentialTime = {0};
        final long[] parallelTime = {0};

        // Sequential requests (slower) - concatMap processes one at a time
        log.info("Sequential requests (using concatMap - processes one at a time):");
        long sequentialStartTime = System.currentTimeMillis();

        Flux.range(1, 3)
            .concatMap(i ->
                perfClient.get()
                    .uri("/delay/1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> "Sequential " + i))
            .collectList()
            .doOnNext(results -> {
                sequentialTime[0] = System.currentTimeMillis() - sequentialStartTime;
                log.info("  ✓ Sequential completed in: " + sequentialTime[0] + "ms");
                results.forEach(result -> log.info("    " + result));
            })
            .doOnError(error -> log.error("  ✗ Sequential error: " + error.getMessage()))
            .block(); // Block to ensure completion before continuing

        log.info("");

        // Parallel requests (faster) - flatMap subscribes to all at once
        log.info("Parallel requests (using flatMap - subscribes to all concurrently):");
        long parallelStartTime = System.currentTimeMillis();

        Flux.range(1, 3)
            .flatMap(i ->
                perfClient.get()
                    .uri("/delay/1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> "Parallel " + i))
            .collectList()
            .doOnNext(results -> {
                parallelTime[0] = System.currentTimeMillis() - parallelStartTime;
                log.info("  ✓ Parallel completed in: " + parallelTime[0] + "ms");
                results.forEach(result -> log.info("    " + result));
            })
            .doOnError(error -> log.error("  ✗ Parallel error: " + error.getMessage()))
            .block(); // Block to ensure completion

        log.info("");

        // Print actual performance comparison using measured times
        log.info("Performance Comparison (actual measurements):");
        log.info("  • Sequential (concatMap): " + sequentialTime[0] + "ms - waits for each request to complete");
        log.info("  • Parallel (flatMap): " + parallelTime[0] + "ms - all requests execute concurrently");
        if (parallelTime[0] > 0) {
            double improvement = (double) sequentialTime[0] / parallelTime[0];
            log.info("  • Improvement: " + String.format("%.1f", improvement) + "x faster with parallel execution");
        }

        log.info("\nKey Differences:");
        log.info("  • concatMap: Guarantees sequential processing - subscribes to next only after previous completes");
        log.info("  • flatMap: Allows concurrent processing - subscribes to all inner publishers immediately");
        log.info("  • For I/O operations like HTTP requests, flatMap typically provides better performance");

        log.info("");
        printWebClientArchitecture();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("  ✓ Request filter: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("  ✓ Response filter: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction handle5xxErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                log.info("  ✓ Handling 5xx error: {}", clientResponse.statusCode());
                return clientResponse.createException().flatMap(Mono::error);
            }
            return Mono.just(clientResponse);
        });
    }

    private void printWebClientArchitecture() {
        log.info("--- WEBCLIENT ARCHITECTURE SUMMARY ---");
        log.info("CORE COMPONENTS:");
        log.info("  • WebClient: High-level reactive HTTP client");
        log.info("  • HttpClient (Reactor Netty): Low-level HTTP transport");
        log.info("  • ConnectionProvider: Connection pooling and lifecycle");
        log.info("  • ExchangeFilterFunction: Request/response filtering");

        log.info("\nCONFIGURATION OPTIONS:");
        log.info("  • Base URL and default headers");
        log.info("  • Custom codecs and message readers/writers");
        log.info("  • Timeout and retry configuration");
        log.info("  • SSL/TLS configuration");

        log.info("\nPERFORMANCE FEATURES:");
        log.info("  • Connection pooling and reuse");
        log.info("  • HTTP/2 support");
        log.info("  • Compression and content negotiation");
        log.info("  • Non-blocking I/O with backpressure");

        log.info("\nERROR HANDLING:");
        log.info("  • Status-based error handling");
        log.info("  • Automatic retries with backoff");
        log.info("  • Circuit breaker patterns");
        log.info("  • Fallback mechanisms");
    }
}
