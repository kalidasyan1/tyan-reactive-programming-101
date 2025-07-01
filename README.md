# 🚀 Reactive Programming 101 - Complete Learning Guide

## Table of Contents
1. [What is Reactive Programming?](#what-is-reactive-programming)
2. [Quick Start](#quick-start)
3. [Framework Analysis](#framework-analysis)
4. [Learning Path](#learning-path)
5. [Real-World Projects](#real-world-projects)
6. [Deep Dive Architecture](#deep-dive-architecture)
7. [Step-by-Step Instructions](#step-by-step-instructions)

## What is Reactive Programming?

Reactive programming is a declarative programming paradigm concerned with **asynchronous data streams** and the **propagation of change**. It's built around the concept of observing and reacting to data as it flows through your application.

### 🎯 Core Principles (Reactive Manifesto)
- **Responsive**: The system responds in a timely manner
- **Resilient**: The system stays responsive in the face of failure  
- **Elastic**: The system stays responsive under varying workload
- **Message Driven**: The system relies on asynchronous message-passing

### 🏢 Main Use Cases
1. **High-throughput applications** (thousands of concurrent requests)
2. **Real-time data processing** (streaming, live updates)
3. **Microservices communication** (non-blocking I/O)
4. **Event-driven architectures**
5. **IoT and sensor data processing**
6. **Financial trading systems**

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+ (or use IDE with Maven support)
- Terminal/Command Line
- Web Browser

### Install and Run
```bash
# Clone or download the project
cd tyan-reactive-programming-101

# Install dependencies (if Maven is available)
mvn clean install

# Or run examples directly in your IDE
```

## Framework Analysis

### 🏆 Spring WebFlux + Project Reactor (DOMINANT FRAMEWORK)
**Why it's the industry standard:**
- ✅ **Largest adoption** in enterprise Java
- ✅ **Spring ecosystem integration** (Spring Boot, Security, Data)
- ✅ **High performance** built on Reactor Netty
- ✅ **Extensive documentation** and community support
- ✅ **Production-ready** with comprehensive testing tools

### 🔍 Framework Comparison

| Framework | Strengths | Use Cases | Learning Curve |
|-----------|-----------|-----------|----------------|
| **Spring WebFlux** | Spring integration, Enterprise ready | Web applications, Microservices | Medium |
| **RxJava** | Mature, Rich operators | Android, General reactive | Steep |
| **CompletableFuture** | Built-in Java, Simple | Basic async operations | Easy |

### 🔗 Relationship with Netty/NIO

```
┌─────────────────────────┐
│   Spring WebFlux        │  ← High-level web framework
├─────────────────────────┤
│   Project Reactor       │  ← Reactive streams implementation  
├─────────────────────────┤
│   Reactor Netty         │  ← Reactive network layer
├─────────────────────────┤
│   Netty                 │  ← Async event-driven network framework
├─────────────────────────┤
│   Java NIO              │  ← Non-blocking I/O foundation
└─────────────────────────┘
```

**Shared Principles:**
- Non-blocking I/O operations
- Event-driven architecture  
- Resource efficiency
- Asynchronous processing
- Backpressure handling

## Learning Path

### 📚 Phase 1: Fundamentals (30 minutes)
Run these examples to understand basic concepts:

```bash
# 1. Core Reactor concepts (Mono, Flux, operators)
mvn exec:java -Dexec.mainClass="com.example.basics.ReactorBasics"

# 2. Framework comparison (RxJava vs Reactor vs CompletableFuture)  
mvn exec:java -Dexec.mainClass="com.example.comparison.FrameworkComparison"

# 3. NIO relationships (How reactive builds on Java NIO)
mvn exec:java -Dexec.mainClass="com.example.nio.NioReactiveRelationship"
```

### 🔬 Phase 2: Deep Dive (45 minutes)
Focus on Spring WebFlux internals:

```bash
# 4. Project Reactor internals (Publisher-Subscriber, Schedulers, Context)
mvn exec:java -Dexec.mainClass="com.example.reactor.ReactorInternals"

# 5. WebClient implementation (HTTP clients, connection management)
mvn exec:java -Dexec.mainClass="com.example.webclient.WebClientInternals"

# 6. Spring WebFlux server (Web layer, functional routing)
mvn spring-boot:run -Dstart-class="com.example.webflux.ReactiveWebApplication"
```

### 🚀 Phase 3: Real Projects (60 minutes)
Build production-ready applications:

#### A. Async Service with Handle Pattern
```bash
# Terminal 1: Start the service
mvn spring-boot:run -Dstart-class="com.example.asyncservice.AsyncServiceApplication"

# Terminal 2: Test the service  
mvn exec:java -Dexec.mainClass="com.example.asyncservice.AsyncServiceClient"
```

#### B. Reactive Chat Application
```bash
# Terminal 1: Start chat server
mvn spring-boot:run -Dstart-class="com.example.chat.ReactiveChatApplication"

# Open in browser: http://localhost:8082/chat.html
```

## Real-World Projects

### 🔄 Project A: Async Service with Handle Pattern

**Problem Solved:** How to handle requests that might take a long time without blocking the server?

**Solution Architecture:**
```
Client Request → Time Check → Decision
                    ↓
            < 30s: Direct Response
            ≥ 30s: Return Handle → Background Processing → Poll for Results
```

**Key Features:**
- ✅ Immediate response for quick tasks (< 30 seconds)
- ✅ Handle-based polling for long tasks (≥ 30 seconds)  
- ✅ Non-blocking background processing
- ✅ RESTful API design

**Test Scenarios:**
1. **Quick task**: `{"data":"test","processingTimeSeconds":5}` → Immediate response
2. **Long task**: `{"data":"analysis","processingTimeSeconds":35}` → Handle + polling

### 💬 Project B: Reactive Chat Application

**Problem Solved:** Real-time messaging with multiple users and rooms

**Solution Architecture:**
```
WebSocket Connections → Chat Handler → Message Router → Broadcast
                                          ↓
                               Room Management + User Presence
```

**Key Features:**
- ✅ Real-time messaging via WebSockets
- ✅ Multiple chat rooms support
- ✅ Private messaging between users
- ✅ User presence tracking (join/leave notifications)
- ✅ Web UI for easy testing

**Test Scenarios:**
1. Open multiple browser tabs with different usernames
2. Join different rooms and observe message isolation
3. Send private messages between users
4. Watch real-time presence updates

## Deep Dive Architecture

### Spring WebFlux Internal Flow
```
HTTP Request → WebFilter Chain → Router/Controller → Handler
      ↓
Reactive Publisher (Mono/Flux) → Operator Chain → Subscriber
      ↓  
Reactor Netty → Event Loop → Channel Pipeline → Network
```

### Project Reactor Core Components
- **Publisher**: Data source (Mono for 0-1, Flux for 0-N)
- **Subscriber**: Data consumer with lifecycle callbacks
- **Subscription**: Controls demand and cancellation
- **Operators**: Transform, filter, combine data streams
- **Schedulers**: Control threading and execution context

### WebClient Architecture
```
Request Builder → Filters → Exchange Function → HTTP Connector
                                                      ↓
                                               Reactor Netty
                                                      ↓
                                            Connection Pool → Network
```

## Step-by-Step Instructions

### Running Individual Examples

#### Basic Concepts
```bash
# Learn Mono, Flux, and operators
mvn exec:java -Dexec.mainClass="com.example.basics.ReactorBasics"
```
**What you'll see:** Publisher creation, transformations, error handling, backpressure

#### Framework Comparison  
```bash
# Compare reactive frameworks
mvn exec:java -Dexec.mainClass="com.example.comparison.FrameworkComparison"
```
**What you'll see:** Same operations in Reactor, RxJava, and CompletableFuture

#### Deep Internals
```bash
# Understand how Reactor works internally
mvn exec:java -Dexec.mainClass="com.example.reactor.ReactorInternals"
```
**What you'll see:** Publisher-Subscriber pattern, operator fusion, schedulers, context propagation

### Testing the Async Service

1. **Start the service** (Terminal 1):
```bash
mvn spring-boot:run -Dstart-class="com.example.asyncservice.AsyncServiceApplication"
# Server starts on http://localhost:8081
```

2. **Test with client** (Terminal 2):
```bash
mvn exec:java -Dexec.mainClass="com.example.asyncservice.AsyncServiceClient"
# Watch quick vs long task handling
```

3. **Manual API testing**:
```bash
# Quick task (immediate response)
curl -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"data":"quick task","processingTimeSeconds":5}'

# Long task (returns handle)
curl -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"data":"long task","processingTimeSeconds":35}'

# Check task status (replace task-1 with actual taskId)
curl http://localhost:8081/api/tasks/task-1
```

### Testing the Chat Application

1. **Start chat server**:
```bash
mvn spring-boot:run -Dstart-class="com.example.chat.ReactiveChatApplication"
# Server starts on http://localhost:8082
```

2. **Open web interface**: http://localhost:8082/chat.html

3. **Test scenarios**:
   - Enter username and click "Connect"
   - Join a room (e.g., "general", "tech")  
   - Send public messages
   - Open multiple browser tabs to simulate different users
   - Test private messaging
   - Watch real-time presence updates

## Key Learnings Summary

### 🎯 Core Reactive Patterns
- **Asynchronous data streams**: Data flows through time
- **Publisher-Subscriber**: Demand-driven data flow
- **Backpressure**: Subscriber controls the pace
- **Error handling**: Resilient stream processing
- **Composition**: Building complex flows from simple operators

### 🛠️ Best Practices
- Use `subscribeOn()` to control subscription thread
- Use `publishOn()` to control emission thread  
- Handle errors with `onErrorResume()`, `retry()`
- Prefer composition over imperative code
- Use appropriate schedulers (boundedElastic for I/O, parallel for CPU)
- Implement proper backpressure handling

### 🚀 Production Considerations
- Connection pooling for HTTP clients
- Proper error handling and circuit breakers
- Monitoring and observability
- Resource management and cleanup
- Performance testing under load

## Troubleshooting

### Common Issues
1. **Dependencies**: If Maven isn't available, use your IDE to run the examples
2. **Port conflicts**: Ensure ports 8080, 8081, 8082 are available
3. **WebSocket issues**: Check browser developer console for connection errors
4. **Performance**: Monitor memory usage with large streams

### Next Steps
- Explore Spring Security reactive support
- Learn about R2DBC for reactive database access
- Study reactive testing with reactor-test
- Investigate reactive messaging with RSocket
- Practice with more complex streaming scenarios

---

🎉 **Congratulations!** You now have a comprehensive understanding of reactive programming with Spring WebFlux and Project Reactor - the dominant reactive framework in the Java ecosystem.
