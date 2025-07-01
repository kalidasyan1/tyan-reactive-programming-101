package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactive Chat Application - Complete Mini-Project
 *
 * Chat Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                    REACTIVE CHAT SYSTEM                         │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  WebSocket → Chat Handler → Message Processor → Broadcast       │
 * │                                                                 │
 * │  ┌─────────────────┐    ┌──────────────────┐                   │
 * │  │   WebSocket     │    │   Chat Rooms     │                   │
 * │  │   Sessions      │    │   Management     │                   │
 * │  └─────────────────┘    └──────────────────┘                   │
 * │                                                                 │
 * │  ┌─────────────────────────────────────────────────────────────┤
 * │  │               MESSAGE BROADCASTING                          │
 * │  │  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐     │
 * │  │  │   Sinks     │    │   Message    │    │   User      │     │
 * │  │  │  (Hot       │───▶│   Routing    │───▶│  Presence   │     │
 * │  │  │  Streams)   │    │              │    │             │     │
 * │  │  └─────────────┘    └──────────────┘    └─────────────┘     │
 * │  └─────────────────────────────────────────────────────────────┤
 * │                                                                 │
 * │  ┌─────────────────────────────────────────────────────────────┤
 * │  │                MESSAGE PERSISTENCE                          │
 * │  │  • In-memory message history                               │
 * │  │  • User session management                                 │
 * │  │  • Room-based message routing                              │
 * │  └─────────────────────────────────────────────────────────────┤
 * └─────────────────────────────────────────────────────────────────┘
 */
@SpringBootApplication
public class ReactiveChatApplication {

    private static final String SYSTEM_TYPE = "system";

    public static void main(String[] args) {
        System.out.println("=== REACTIVE CHAT APPLICATION ===");
        System.out.println("Starting chat server on port 8082...");
        System.setProperty("server.port", "8082");
        SpringApplication.run(ReactiveChatApplication.class, args);
    }

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/chat", new ChatWebSocketHandler());

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    /**
     * WebSocket handler for chat functionality
     */
    public static class ChatWebSocketHandler implements WebSocketHandler {

        private final ChatService chatService = new ChatService();
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public Mono<Void> handle(WebSocketSession session) {
            String userId = session.getHandshakeInfo().getUri().getQuery();
            if (userId != null && userId.startsWith("userId=")) {
                userId = userId.substring(7); // Remove "userId=" prefix
            } else {
                userId = "anonymous-" + System.currentTimeMillis();
            }

            final String finalUserId = userId; // Make it effectively final

            System.out.println("New WebSocket connection: " + finalUserId);

            // Register user
            chatService.addUser(finalUserId, session);

            // Handle incoming messages
            Mono<Void> input = session.receive()
                .map(message -> message.getPayloadAsText())
                .flatMap(payload -> {
                    try {
                        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
                        chatMessage.setSender(finalUserId);
                        chatMessage.setTimestamp(LocalDateTime.now());

                        return chatService.processMessage(chatMessage);
                    } catch (JsonProcessingException e) {
                        System.err.println("Error parsing message: " + e.getMessage());
                        return Mono.empty();
                    }
                })
                .then();

            // Send outgoing messages
            Mono<Void> output = session.send(
                chatService.getMessagesForUser(finalUserId)
                    .map(message -> {
                        try {
                            return session.textMessage(objectMapper.writeValueAsString(message));
                        } catch (JsonProcessingException e) {
                            System.err.println("Error serializing message: " + e.getMessage());
                            return session.textMessage("{\"error\":\"Serialization error\"}");
                        }
                    })
            );

            // Handle disconnection
            return input.mergeWith(output)
                .doFinally(signalType -> {
                    System.out.println("WebSocket disconnected: " + finalUserId + " (" + signalType + ")");
                    chatService.removeUser(finalUserId);
                })
                .then();
        }
    }

    /**
     * Chat service handling message routing and user management
     */
    public static class ChatService {

        private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
        private final Map<String, String> userRooms = new ConcurrentHashMap<>(); // userId -> roomId
        private final Map<String, Sinks.Many<ChatMessage>> roomSinks = new ConcurrentHashMap<>();
        private final AtomicLong messageIdCounter = new AtomicLong(0);

        public void addUser(String userId, WebSocketSession session) {
            userSessions.put(userId, session);

            // Send welcome message
            ChatMessage welcomeMessage = new ChatMessage();
            welcomeMessage.setId(messageIdCounter.incrementAndGet());
            welcomeMessage.setType(SYSTEM_TYPE);
            welcomeMessage.setSender(SYSTEM_TYPE);
            welcomeMessage.setContent("Welcome to the chat, " + userId + "!");
            welcomeMessage.setTimestamp(LocalDateTime.now());

            sendMessageToUser(userId, welcomeMessage);

            // Broadcast user joined
            broadcastUserPresence(userId, "joined");
        }

        public void removeUser(String userId) {
            userSessions.remove(userId);
            String roomId = userRooms.remove(userId);

            if (roomId != null) {
                broadcastUserPresence(userId, "left");
            }
        }

        public Mono<Void> processMessage(ChatMessage message) {
            message.setId(messageIdCounter.incrementAndGet());

            return Mono.fromRunnable(() -> {
                switch (message.getType()) {
                    case "join_room":
                        handleJoinRoom(message);
                        break;
                    case "chat":
                        handleChatMessage(message);
                        break;
                    case "private":
                        handlePrivateMessage(message);
                        break;
                    default:
                        System.out.println("Unknown message type: " + message.getType());
                }
            });
        }

        private void handleJoinRoom(ChatMessage message) {
            String userId = message.getSender();
            String roomId = message.getContent();

            // Leave current room
            String currentRoom = userRooms.get(userId);
            if (currentRoom != null) {
                broadcastToRoom(currentRoom, createSystemMessage(userId + " left the room"));
            }

            // Join new room
            userRooms.put(userId, roomId);
            roomSinks.computeIfAbsent(roomId, k -> Sinks.many().multicast().onBackpressureBuffer());

            // Send confirmation
            ChatMessage confirmation = createSystemMessage("You joined room: " + roomId);
            sendMessageToUser(userId, confirmation);

            // Broadcast to room
            broadcastToRoom(roomId, createSystemMessage(userId + " joined the room"));

            System.out.println("User " + userId + " joined room " + roomId);
        }

        private void handleChatMessage(ChatMessage message) {
            String userId = message.getSender();
            String roomId = userRooms.get(userId);

            if (roomId == null) {
                sendMessageToUser(userId, createSystemMessage("You must join a room first"));
                return;
            }

            System.out.println("Chat message in room " + roomId + ": " + message.getContent());
            broadcastToRoom(roomId, message);
        }

        private void handlePrivateMessage(ChatMessage message) {
            String targetUser = message.getTarget();

            if (targetUser == null || !userSessions.containsKey(targetUser)) {
                sendMessageToUser(message.getSender(),
                    createSystemMessage("User " + targetUser + " not found"));
                return;
            }

            // Send to target user
            message.setType("private");
            sendMessageToUser(targetUser, message);

            // Send confirmation to sender
            ChatMessage confirmation = createSystemMessage("Private message sent to " + targetUser);
            sendMessageToUser(message.getSender(), confirmation);

            System.out.println("Private message from " + message.getSender() + " to " + targetUser);
        }

        private void broadcastToRoom(String roomId, ChatMessage message) {
            Sinks.Many<ChatMessage> sink = roomSinks.get(roomId);
            if (sink != null) {
                sink.tryEmitNext(message);
            }
        }

        private void broadcastUserPresence(String userId, String action) {
            String roomId = userRooms.get(userId);
            if (roomId != null) {
                ChatMessage presenceMessage = createSystemMessage(userId + " " + action);
                presenceMessage.setType("presence");
                broadcastToRoom(roomId, presenceMessage);
            }
        }

        private void sendMessageToUser(String userId, ChatMessage message) {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String messageJson = mapper.writeValueAsString(message);
                    session.send(Mono.just(session.textMessage(messageJson))).subscribe();
                } catch (Exception e) {
                    System.err.println("Error sending message to user " + userId + ": " + e.getMessage());
                }
            }
        }

        public Flux<ChatMessage> getMessagesForUser(String userId) {
            return Flux.create(sink -> {
                // Send immediate presence update
                ChatMessage presenceUpdate = createSystemMessage("Connected users: " +
                    userSessions.size());
                presenceUpdate.setType("presence");
                sink.next(presenceUpdate);

                // Subscribe to room messages when user joins a room
                Flux.interval(java.time.Duration.ofSeconds(1))
                    .map(tick -> userRooms.get(userId))
                    .filter(Objects::nonNull)
                    .distinctUntilChanged()
                    .flatMap(roomId -> {
                        Sinks.Many<ChatMessage> roomSink = roomSinks.get(roomId);
                        return roomSink != null ? roomSink.asFlux() : Flux.empty();
                    })
                    .subscribe(
                        sink::next,
                        sink::error,
                        sink::complete
                    );
            });
        }

        private ChatMessage createSystemMessage(String content) {
            ChatMessage message = new ChatMessage();
            message.setId(messageIdCounter.incrementAndGet());
            message.setType(SYSTEM_TYPE);
            message.setSender(SYSTEM_TYPE);
            message.setContent(content);
            message.setTimestamp(LocalDateTime.now());
            return message;
        }
    }

    /**
     * Chat message data structure
     */
    public static class ChatMessage {
        private Long id;
        private String type; // "chat", "system", "private", "join_room", "presence"
        private String sender;
        private String target; // For private messages
        private String content;
        private LocalDateTime timestamp;

        // Default constructor for Jackson
        public ChatMessage() {
            // Empty constructor for JSON deserialization
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        @Override
        public String toString() {
            return "ChatMessage{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
        }
    }
}
