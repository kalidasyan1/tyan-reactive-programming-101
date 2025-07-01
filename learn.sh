#!/bin/bash

# Reactive Programming 101 - Learning Launcher
# This script helps you run the examples in the correct order

echo "🚀 Welcome to Reactive Programming 101!"
echo "======================================="
echo ""

show_menu() {
    echo "Choose your learning path:"
    echo ""
    echo "📚 PHASE 1: Fundamentals (30 min)"
    echo "  1) Basic Reactor Concepts (Mono, Flux, Operators)"
    echo "  2) Framework Comparison (Reactor vs RxJava vs CompletableFuture)"
    echo "  3) NIO Relationships (How reactive builds on Java NIO)"
    echo ""
    echo "🔬 PHASE 2: Deep Dive (45 min)"
    echo "  4) Project Reactor Internals"
    echo "  5) WebClient Deep Dive"
    echo "  6) Spring WebFlux Server"
    echo ""
    echo "🚀 PHASE 3: Real Projects (60 min)"
    echo "  7) Async Service with Handle Pattern"
    echo "  8) Reactive Chat Application"
    echo ""
    echo "  0) Exit"
    echo ""
    echo -n "Enter your choice [0-8]: "
}

run_example() {
    case $1 in
        1)
            echo "🎯 Running: Basic Reactor Concepts..."
            mvn exec:java -Dexec.mainClass="com.example.basics.ReactorBasics"
            ;;
        2)
            echo "🎯 Running: Framework Comparison..."
            mvn exec:java -Dexec.mainClass="com.example.comparison.FrameworkComparison"
            ;;
        3)
            echo "🎯 Running: NIO Relationships..."
            mvn exec:java -Dexec.mainClass="com.example.nio.NioReactiveRelationship"
            ;;
        4)
            echo "🎯 Running: Project Reactor Internals..."
            mvn exec:java -Dexec.mainClass="com.example.reactor.ReactorInternals"
            ;;
        5)
            echo "🎯 Running: WebClient Deep Dive..."
            mvn exec:java -Dexec.mainClass="com.example.webclient.WebClientInternals"
            ;;
        6)
            echo "🎯 Starting: Spring WebFlux Server..."
            echo "💡 Server will start at http://localhost:8080"
            echo "💡 Press Ctrl+C to stop the server"
            mvn spring-boot:run -Dstart-class="com.example.webflux.ReactiveWebApplication"
            ;;
        7)
            echo "🎯 Starting: Async Service..."
            echo "💡 Service will start at http://localhost:8081"
            echo "💡 Open another terminal and run: mvn exec:java -Dexec.mainClass=\"com.example.asyncservice.AsyncServiceClient\""
            echo "💡 Press Ctrl+C to stop the service"
            mvn spring-boot:run -Dstart-class="com.example.asyncservice.AsyncServiceApplication"
            ;;
        8)
            echo "🎯 Starting: Reactive Chat Application..."
            echo "💡 Chat server will start at http://localhost:8082"
            echo "💡 Open browser: http://localhost:8082/chat.html"
            echo "💡 Press Ctrl+C to stop the server"
            mvn spring-boot:run -Dstart-class="com.example.chat.ReactiveChatApplication"
            ;;
        0)
            echo "👋 Happy learning! Don't forget to check the README.md for detailed explanations."
            exit 0
            ;;
        *)
            echo "❌ Invalid option. Please choose 0-8."
            ;;
    esac
}

# Main loop
while true; do
    show_menu
    read choice
    echo ""

    if [[ "$choice" =~ ^[0-8]$ ]]; then
        run_example $choice
        if [ $choice -ne 0 ]; then
            echo ""
            echo "✅ Example completed. Press Enter to return to menu..."
            read
            echo ""
        fi
    else
        echo "❌ Invalid input. Please enter a number between 0-8."
        echo ""
    fi
done
