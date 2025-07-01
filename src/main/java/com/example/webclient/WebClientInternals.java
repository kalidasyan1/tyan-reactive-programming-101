package com.example.webclient;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import java.time.Duration;
import java.util.List;

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

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== WEBCLIENT INTERNALS DEEP DIVE ===\n");

        // Basic WebClient usage
        demonstrateBasicWebClient();

        // Advanced configuration
        demonstrateAdvancedConfiguration();

        // Connection pooling and management
        demonstrateConnectionManagement();

        // Error handling and retries
        demonstrateErrorHandlingAndRetries();

        // Streaming responses
        demonstrateStreamingResponses();

        // Performance optimization
        demonstratePerformanceOptimization();

        Thread.sleep(5000);
    }

    private static void demonstrateBasicWebClient() {
        System.out.println("--- BASIC WEBCLIENT USAGE ---");

        // Create a basic WebClient
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();

        // Simple GET request
        webClient.get()
            .uri("/posts/1")
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> System.out.println("  ✓ GET Response: " + response.substring(0, 100) + "..."),
                error -> System.err.println("  ✗ GET Error: " + error.getMessage())
            );

        // POST request with body
        String postData = "{\"title\":\"Reactive Post\",\"body\":\"WebClient example\",\"userId\":1}";
        webClient.post()
            .uri("/posts")
            .bodyValue(postData)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> System.out.println("  ✓ POST Response: " + response.substring(0, 100) + "..."),
                error -> System.err.println("  ✗ POST Error: " + error.getMessage())
            );

        System.out.println();
    }

    private static void demonstrateAdvancedConfiguration() {
        System.out.println("--- ADVANCED WEBCLIENT CONFIGURATION ---");

        // Configure custom HttpClient with Reactor Netty
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(10))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .doOnConnected(conn ->
                System.out.println("  ✓ Connection established to: " + conn.channel().remoteAddress()));

        // Configure custom ExchangeStrategies
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
                System.out.println("  ✓ Custom codec configuration applied");
            })
            .build();

        // Build WebClient with advanced configuration
        WebClient advancedClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .filter((request, next) -> {
                System.out.println("  ✓ Request filter: " + request.method() + " " + request.url());
                return next.exchange(request)
                    .doOnNext(response ->
                        System.out.println("  ✓ Response filter: " + response.statusCode()));
            })
            .defaultHeader("User-Agent", "WebClient-Demo/1.0")
            .build();

        // Use the configured client
        advancedClient.get()
            .uri("https://httpbin.org/get")
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> System.out.println("  ✓ Advanced client response received"),
                error -> System.err.println("  ✗ Advanced client error: " + error.getMessage())
            );

        System.out.println();
    }

    private static void demonstrateConnectionManagement() {
        System.out.println("--- CONNECTION MANAGEMENT ---");

        // Configure connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("demo-pool")
            .maxConnections(10)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(5))
            .evictInBackground(Duration.ofSeconds(120))
            .build();

        HttpClient pooledHttpClient = HttpClient.create(connectionProvider)
            .doOnConnected(conn ->
                System.out.println("  ✓ Pooled connection: " + conn.channel().id()))
            .doOnDisconnected(conn ->
                System.out.println("  ✓ Connection released: " + conn.channel().id()));

        WebClient pooledClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(pooledHttpClient))
            .build();

        // Make multiple requests to see connection reuse
        Flux.range(1, 3)
            .flatMap(i ->
                pooledClient.get()
                    .uri("https://httpbin.org/delay/1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> "Request " + i + " completed"))
            .subscribe(
                result -> System.out.println("  ✓ " + result),
                error -> System.err.println("  ✗ Pool error: " + error.getMessage())
            );

        System.out.println();
    }

    private static void demonstrateErrorHandlingAndRetries() {
        System.out.println("--- ERROR HANDLING AND RETRIES ---");

        WebClient errorClient = WebClient.create("https://httpbin.org");

        // Demonstrate different error scenarios
        errorClient.get()
            .uri("/status/500") // This will return 500 error
            .retrieve()
            .onStatus(status -> status.is5xxServerError(),
                response -> {
                    System.out.println("  ✓ Handling 5xx error: " + response.statusCode());
                    return Mono.error(new RuntimeException("Server error occurred"));
                })
            .bodyToMono(String.class)
            .retry(2)
            .onErrorReturn("Fallback response after retries")
            .subscribe(
                response -> System.out.println("  ✓ Error handling result: " + response),
                error -> System.err.println("  ✗ Final error: " + error.getMessage())
            );

        // Exponential backoff retry
        errorClient.get()
            .uri("/status/503")
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(100))
                .doBeforeRetry(retrySignal ->
                    System.out.println("  ✓ Retry attempt: " + retrySignal.totalRetries())))
            .onErrorReturn("Final fallback")
            .subscribe(
                response -> System.out.println("  ✓ Retry result: " + response),
                error -> System.err.println("  ✗ Retry exhausted: " + error.getMessage())
            );

        System.out.println();
    }

    private static void demonstrateStreamingResponses() {
        System.out.println("--- STREAMING RESPONSES ---");

        WebClient streamClient = WebClient.create("https://httpbin.org");

        // Stream JSON array response
        streamClient.get()
            .uri("/json")
            .retrieve()
            .bodyToFlux(String.class)
            .take(1) // Limit for demo
            .subscribe(
                chunk -> System.out.println("  ✓ Streamed chunk: " + chunk.substring(0, Math.min(50, chunk.length())) + "..."),
                error -> System.err.println("  ✗ Stream error: " + error.getMessage()),
                () -> System.out.println("  ✓ Stream completed")
            );

        // Handle large responses with streaming
        streamClient.get()
            .uri("/bytes/1024") // Get 1KB of random data
            .retrieve()
            .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
            .map(buffer -> {
                int size = buffer.readableByteCount();
                org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                return size;
            })
            .reduce(0, Integer::sum)
            .subscribe(
                totalBytes -> System.out.println("  ✓ Total bytes streamed: " + totalBytes),
                error -> System.err.println("  ✗ Streaming error: " + error.getMessage())
            );

        System.out.println();
    }

    private static void demonstratePerformanceOptimization() {
        System.out.println("--- PERFORMANCE OPTIMIZATION ---");

        // Measure performance of different approaches
        WebClient perfClient = WebClient.create("https://httpbin.org");

        long startTime = System.currentTimeMillis();

        // Sequential requests (slower)
        System.out.println("Sequential requests:");
        Flux.range(1, 3)
            .concatMap(i ->
                perfClient.get()
                    .uri("/delay/1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> "Sequential " + i))
            .collectList()
            .subscribe(
                results -> {
                    long sequentialTime = System.currentTimeMillis() - startTime;
                    System.out.println("  ✓ Sequential completed in: " + sequentialTime + "ms");
                    results.forEach(result -> System.out.println("    " + result));

                    // Parallel requests (faster)
                    testParallelRequests(perfClient);
                },
                error -> System.err.println("  ✗ Sequential error: " + error.getMessage())
            );
    }

    private static void testParallelRequests(WebClient client) {
        System.out.println("Parallel requests:");
        long startTime = System.currentTimeMillis();

        Flux.range(1, 3)
            .flatMap(i ->
                client.get()
                    .uri("/delay/1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> "Parallel " + i))
            .collectList()
            .subscribe(
                results -> {
                    long parallelTime = System.currentTimeMillis() - startTime;
                    System.out.println("  ✓ Parallel completed in: " + parallelTime + "ms");
                    results.forEach(result -> System.out.println("    " + result));

                    printWebClientArchitectureSummary();
                },
                error -> System.err.println("  ✗ Parallel error: " + error.getMessage())
            );
    }

    private static void printWebClientArchitectureSummary() {
        System.out.println("\n--- WEBCLIENT ARCHITECTURE SUMMARY ---");
        System.out.println("CORE COMPONENTS:");
        System.out.println("  • WebClient.Builder: Configuration and setup");
        System.out.println("  • ExchangeFunction: Core request/response handling");
        System.out.println("  • ClientHttpConnector: Abstraction over HTTP clients");
        System.out.println("  • ExchangeStrategies: Codec and message handling");

        System.out.println("\nREACTOR NETTY INTEGRATION:");
        System.out.println("  • Non-blocking I/O with Netty event loops");
        System.out.println("  • Connection pooling and management");
        System.out.println("  • HTTP/2 and WebSocket support");
        System.out.println("  • SSL/TLS configuration");

        System.out.println("\nPERFORMANCE FEATURES:");
        System.out.println("  • Connection reuse and pooling");
        System.out.println("  • Streaming request/response bodies");
        System.out.println("  • Backpressure handling");
        System.out.println("  • Parallel request processing");

        System.out.println("\nERROR HANDLING:");
        System.out.println("  • Status-based error handling");
        System.out.println("  • Retry mechanisms with backoff");
        System.out.println("  • Circuit breaker integration");
        System.out.println("  • Timeout configuration");
    }
}
