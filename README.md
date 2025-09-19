# Scrabble Game Implementation

A comprehensive two-player Scrabble game implementation using Java 21, Spring Boot, and JavaFX.

## Project Overview

This project implements a full-featured Scrabble game with the following stages:
- **Stage 1 (COMPLETED)**: Core Game & User Interface with client-server architecture
- **Stage 2 (COMPLETED)**: JavaFX Client Application with lobby and server communication
- **Stage 3**: Secure Game Clock System (10-minute countdown per player)  
- **Stage 4**: Basic AI Bot Player
- **Stage 5**: Advanced AI with Strategic Play

## Architecture

The project follows a multi-module Maven architecture:

```
scrabble-parent/
├── scrabble-common/     # Shared models and game logic
├── scrabble-server/     # Spring Boot server with WebSocket support
└── scrabble-client/     # JavaFX client application
```

## Stage 1 - Completed Features

### Core Game Components (scrabble-common)
- ✅ **Tile System**: Complete tile implementation with standard Scrabble values and blank tile support
- ✅ **Game Board**: 15x15 board with premium squares (double/triple letter/word, center star)
- ✅ **Player Management**: Player racks, scoring, and game state tracking
- ✅ **Move Validation**: Comprehensive move validation and scoring engine
- ✅ **Dictionary**: CSW24 word validation with 280,887+ valid words
- ✅ **Game State**: Complete game state management and turn handling

### Server Features (scrabble-server)
- ✅ **REST API**: Lobby management and game control endpoints
- ✅ **WebSocket Support**: Real-time game communication
- ✅ **Game Service**: Server-side game logic and state management
- ✅ **Lobby Service**: Matchmaking and bot opponent support
- ✅ **Concurrent Games**: Support for multiple simultaneous games

### Game Rules Implemented
- ✅ All tiles must be placed in a single line (horizontal or vertical)
- ✅ First move must cover the center star
- ✅ Subsequent moves must connect to existing tiles
- ✅ All formed words must be valid according to dictionary
- ✅ Premium squares (double/triple letter/word bonuses)
- ✅ Bingo bonus (50 points for using all 7 tiles)
- ✅ Tile exchange functionality
- ✅ Pass turn functionality
- ✅ Game end conditions (6 consecutive passes, empty rack + empty bag)

### Client Features (scrabble-client)
- ✅ **JavaFX Application**: Modern UI with dark theme and lime accents
- ✅ **Welcome Screen**: Player name input, server connection, and game mode selection
- ✅ **Lobby System**: Matchmaking interface with progress indicators
- ✅ **WebSocket Communication**: Real-time client-server messaging
- ✅ **HTTP REST Client**: API integration for lobby operations
- ✅ **Scene Management**: Smooth transitions between welcome, lobby, and game screens
- ✅ **Connection Management**: Auto-reconnection and error handling
- ✅ **Professional UI**: CSS styling with responsive design

## Quick Start

### Prerequisites
- Java 21
- Maven 3.9+

### Build and Test

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Start the server (Terminal 1)
cd scrabble-server
mvn spring-boot:run

# Start the JavaFX client (Terminal 2)
cd scrabble-client
mvn javafx:run
```

The server will start on port 8081 and load the complete CSW24 dictionary.
The JavaFX client will open a modern GUI application for gameplay.

### API Endpoints

#### Health Check
```bash
curl http://localhost:8081/api/health
```

#### Join Lobby (Human vs Human)
```bash
curl -X POST http://localhost:8081/api/lobby/join \
  -H "Content-Type: application/json" \
  -d '{"id":"player1","name":"Alice","isBot":false}'
```

#### Play Against Bot
```bash
curl -X POST http://localhost:8081/api/lobby/bot \
  -H "Content-Type: application/json" \
  -d '{"id":"player1","name":"Alice","isBot":false}'
```

#### Get Lobby Stats
```bash
curl http://localhost:8081/api/lobby/stats
```

### WebSocket Communication

Connect to `ws://localhost:8081/game` for real-time game communication.

**Message Types:**
- `JOIN_GAME`: Join an existing game
- `MAKE_MOVE`: Submit a move (tile placement, pass, or exchange)
- `REQUEST_GAME_STATE`: Get current game state
- `CHAT_MESSAGE`: Send chat messages

**Example Move Message:**
```json
{
  "type": "MAKE_MOVE",
  "gameId": "game-uuid",
  "playerId": "player-uuid",
  "payload": {
    "type": "PLACE_TILES",
    "tilePlacements": {
      "H8": {"letter": "H", "value": 4},
      "H9": {"letter": "E", "value": 1}
    }
  }
}
```

## Testing

The project includes comprehensive unit tests:

```bash
# Run all tests
mvn test

# Test results summary:
# - 23 tests passing
# - Dictionary: 280,887 words loaded from CSW24.txt
# - All core game mechanics validated
```

### Key Test Coverage
- Tile creation, blank tile assignment, and scoring
- Board premium square layout and tile placement
- Dictionary loading and word validation
- Game rules enforcement
- Move validation and scoring calculations

## Project Statistics

- **Lines of Code**: ~3,500 lines of Java
- **Test Coverage**: 23 comprehensive unit tests
- **Dictionary Size**: 280,887 valid Scrabble words (CSW24)
- **Supported Features**: Complete Stage 1 implementation

## Next Stages

### Stage 3: Game Board Implementation
- Interactive 15x15 game board with drag-and-drop
- Tile rack management and scoring display
- Move validation and visual feedback
- Real-time game state synchronization

### Stage 4: Secure Game Clock System
- Server-side timer management (10 minutes per player)
- WebSocket timer synchronization
- Automatic timeout handling
- Visual countdown timers in client

### Stage 5: Basic AI Bot Player
- Simple bot strategy (finds first valid move)
- 30-second move timeout
- Random tile selection from rack
- Integration with existing lobby system

### Stage 6: Advanced AI with Strategic Play
- Trie/DAWG word generation algorithms
- Move evaluation and scoring
- Multi-turn lookahead analysis
- Configurable difficulty levels

## Technology Stack

- **Java 21**: Modern Java features including pattern matching and records
- **Spring Boot 3.2.1**: RESTful API and WebSocket support
- **Maven**: Multi-module project management
- **JUnit 5**: Comprehensive testing framework
- **Jackson**: JSON serialization/deserialization
- **JavaFX 21**: Rich client application framework (planned)

## Development Highlights

This implementation demonstrates:
- **Clean Architecture**: Separation of concerns between common, server, and client modules
- **Test-Driven Development**: Comprehensive unit tests for all core functionality
- **Real-time Communication**: WebSocket-based client-server communication
- **Concurrent Programming**: Thread-safe game state management
- **Performance**: Efficient dictionary loading and word validation
- **Scalability**: Support for multiple concurrent games

## Contributing

This project follows standard Java conventions and includes:
- Maven-based build system
- Comprehensive unit testing
- JavaDoc documentation
- JSON-based API communication
- WebSocket real-time updates

---

**Total Development Time**: ~4 hours for complete Stage 1 implementation
**Status**: Stage 1 Complete ✅ | Server Running ✅ | Tests Passing ✅