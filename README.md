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
Refer to the reactor-core guide for basic concepts and operators: [Reactor Core Guide](https://www.baeldung.com/reactor-core)

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

# 5. Multiple Subscribers & Timeout Patterns (Cold vs Hot streams)
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.BlockingTimeoutExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.NonBlockingTimeoutExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.HotStreamViaCacheExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.HotStreamViaShareExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.NonBlockingTimeoutExample2"

# 6. WebClient implementation (HTTP clients, connection management)
mvn exec:java -Dexec.mainClass="com.example.webclient.WebClientInternals"

# 7. Spring WebFlux server (Web layer, functional routing)
mvn spring-boot:run -Dstart-class="com.example.webflux.ReactiveWebApplication"
```

### 🔍 Phase 2.5: Advanced Reactor Patterns (30 minutes)
Understanding multiple subscribers and timeout behavior:

#### Multiple Subscribers & Cold vs Hot Streams
These examples demonstrate critical concepts for building robust reactive applications:

```bash
# Blocking operations with timeouts - interruption behavior
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.BlockingTimeoutExample"
```
**What you'll learn:** Cold streams create independent executions per subscriber. Timeout on one subscriber interrupts its own thread but doesn't affect others.

```bash
# Non-blocking operations with timeouts - cancellation vs interruption  
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.NonBlockingTimeoutExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.NonBlockingTimeoutExample2"
```
**What you'll learn:** Non-blocking sources (CompletableFuture, Mono.delay) handle cancellation gracefully without thread interruption.

```bash
# Hot streams via cache() - shared execution
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.HotStreamViaCacheExample"
```
**What you'll learn:** `cache()` converts cold streams to hot - single execution shared among all subscribers. Late subscribers get cached results.

```bash
# Hot streams via share() - multicast behavior
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.HotStreamViaShareExample"
```
**What you'll learn:** `share()` multicasts to concurrent subscribers but late subscribers may miss emissions.

## Real-World Projects

### 🔄 Project A: Async Service with Handle Pattern

**Problem Solved:** How to handle requests that might take a long time without blocking the server?

**Two Implementation Approaches:**

#### 1. Controller-Based Implementation (DataProcessingRequestController)
- **Class**: `projects.asyncservice.ControllerBasedApplication`
- **Controller**: `DataProcessingRequestController` 
- **Approach**: Traditional Spring MVC style with `@RestController` annotations
- **Key Innovation**: Solves timeout interruption issues with proper background processing isolation

**Architecture Enhancement:**
```
Client Request → Background Processing (Cache) → Client Timeout Chain
                         ↓                            ↓
                Uninterrupted Execution    ≤30s: Direct Response
                         ↓                 >30s: Handle + Polling
                Background Completion
```

**Critical Fixes Applied:**
- ✅ **No timeout interruption**: Background processing continues regardless of client timeouts
- ✅ **No duplicate storage**: Single source of truth for result management  
- ✅ **Thread-safe operations**: Proper concurrent access to shared task results
- ✅ **Cache strategy**: Using `.cache()` to share execution without re-execution

#### 2. Router Functions Implementation (Alternative)
- **Class**: `projects.asyncservice.RouterFunctionsBasedApplication`
- **Approach**: Functional routing with WebFlux handlers
- **Use case**: When you prefer functional programming style over annotations

**Solution Architecture:**
```
Client Request → Immediate Processing Start → 30s Timeout Check
                         ↓
                ≤ 30s: Direct Response
                > 30s: Return Handle → Background Processing → Poll for Results
```

**Key Features:**
- ✅ Immediate response for quick tasks (≤ 30 seconds)
- ✅ Handle-based polling for long tasks (> 30 seconds)
- ✅ Complexity-based processing times (1-10 scale: 6s to 60s)
- ✅ Type-safe TaskStatus enum (PROCESSING, COMPLETED, FAILED)
- ✅ Lombok-powered shared data models
- ✅ Non-blocking background processing
- ✅ RESTful API design

**Test Scenarios:**
1. **Quick task**: `{"data":"simple task","complexity":1}` → ~6s, immediate response
2. **Medium task**: `{"data":"medium task","complexity":5}` → ~30s, may timeout to background
3. **Long task**: `{"data":"complex task","complexity":10}` → ~60s, handle + polling

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
mvn spring-boot:run -Dstart-class="projects.asyncservice.ControllerBasedApplication"
# Server starts on http://localhost:8081
```

2. **Test quick completion** (Terminal 2):
```bash
curl -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"data":"quick task","complexity":1}'
```
**Expected:** Immediate response with completed result

3. **Test background processing** (Terminal 2):
```bash
# Send long task
curl -X POST http://localhost:8081/api/process \
  -H "Content-Type: application/json" \
  -d '{"data":"long task","complexity":10}'

# You'll get a handle, then poll for results
curl http://localhost:8081/api/status/{taskId}
```
**Expected:** Handle response, then polling shows progression

### Testing the Chat Application

1. **Start the chat server** (Terminal 1):
```bash
mvn spring-boot:run -Dstart-class="projects.chat.ReactiveChatApplication"
# Server starts on http://localhost:8082
```

2. **Open the web interface:**
```
http://localhost:8082/chat.html
```

3. **Test scenarios:**
- Open multiple browser tabs
- Use different usernames in each tab
- Join different rooms
- Send public and private messages
- Watch real-time updates

### Advanced Testing with Multiple Subscribers

```bash
# Run each example and observe the console output
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.BlockingTimeoutExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.HotStreamViaCacheExample"
mvn exec:java -Dexec.mainClass="com.example.reactor.multiplesubscribers.HotStreamViaShareExample"
```

**What to observe:**
- Thread names and execution patterns
- How timeouts affect different subscriber types
- Cache vs Share behavior differences
- Background processing continuation

---

## 🎓 Learning Outcomes

After completing this guide, you'll understand:

### ✅ Reactive Programming Fundamentals
- Publisher-Subscriber pattern and backpressure
- Hot vs Cold streams and when to use each
- Operator chains and composition patterns
- Error handling and recovery strategies

### ✅ Spring WebFlux Production Patterns
- Building non-blocking web applications
- Handling long-running operations gracefully
- WebSocket integration for real-time features
- Testing reactive applications

### ✅ Advanced Reactor Techniques
- Multiple subscriber scenarios and timeout handling
- Cache vs Share operators for performance
- Background processing with proper isolation
- Thread-safe reactive patterns

### ✅ Real-World Architecture
- Microservice communication patterns
- Event-driven system design
- Resource management and connection pooling
- Production monitoring and debugging

## 🚀 Next Steps

1. **Explore the codebase** - Each example includes detailed comments
2. **Modify the examples** - Change parameters and observe behavior
3. **Build your own project** - Apply these patterns to your use cases
4. **Read the documentation** - [Project Reactor Reference](https://projectreactor.io/docs)
5. **Join the community** - [Spring WebFlux Community](https://spring.io/community)
