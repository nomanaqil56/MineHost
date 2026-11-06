# MineHost — System Architecture Specification

## 1. Architectural Overview

MineHost is built according to **Clean Architecture** principles and the modern Android development stack:
- **Presentation:** Jetpack Compose, Material 3, Navigation Compose, unidirectional data flow (UDF) with Kotlin StateFlow.
- **Domain:** Pure business logic, Use Cases, Engine interfaces, Tunnel abstractions, and Server state machine.
- **Data:** Repository pattern, Room Database for server metadata and logs, Jetpack DataStore for preferences, and direct file I/O for Minecraft world directories and configurations.
- **Platform Services:** Android Foreground Service, WakeLock management, Thermal Status monitoring, and process supervision via `ProcessBuilder`.

```mermaid
flowchart TD
    subgraph UI ["Presentation Layer (Jetpack Compose)"]
        Dashboard["Dashboard Screen"]
        ServerDetail["Server Detail Screen"]
        Console["Live Console Screen"]
        CreateServer["Create Server Wizard"]
        Settings["Server Settings Screen"]
        Players["Player Management"]
        WorldsBackups["World & Backup Manager"]
    end

    subgraph Domain ["Domain Layer"]
        ServerController["Server Controller"]
        ServerManager["Server Manager"]
        TunnelManager["Tunnel Manager"]
        WorldManager["World Manager"]
        BackupManager["Backup Manager"]
        PlayerManager["Player Manager"]
    end

    subgraph Data ["Data & Storage Layer"]
        ServerRepo["Server Repository"]
        RoomDB[("Room Database (SQLite)")]
        DataStore["User Preferences (DataStore)"]
        LocalFS["Local File System (/files/minehost)"]
    end

    subgraph Execution ["Platform & Process Execution"]
        ForegroundService["Server Foreground Service"]
        WakeLockMgr["WakeLock & Thermal Manager"]
        EngineProcess["Server Engine Process (ProcessBuilder)"]
        TunnelClient["Tunnel Client Process (Playit CLI)"]
    end

    UI --> Domain
    Domain --> Data
    Domain --> Execution
    Execution --> LocalFS
    Data --> RoomDB
    Data --> DataStore
    Data --> LocalFS
```

---

## 2. Subsystem Breakdown

### 2.1 Server Engine Abstraction Layer
MineHost does not bind directly to any specific server software. It abstracts all engines behind `ServerEngine`:

```mermaid
classDiagram
    class ServerEngine {
        <<interface>>
        +id: String
        +edition: MinecraftEdition
        +name: String
        +version: String
        +isInstalled(): Boolean
        +install(listener: InstallListener): Job
        +configure(config: ServerConfiguration): Unit
        +start(workDir: File, listener: ProcessListener): ProcessHandle
        +stop(timeoutSeconds: Int): Boolean
        +sendCommand(command: String): Unit
        +getStatus(): ServerStatus
    }
    class BedrockEngine {
        +pocketMineFlavor: PocketMineAdapter
        +powerNukkitFlavor: PowerNukkitAdapter
    }
    class JavaEngine {
        +paperFlavor: PaperAdapter
        +vanillaFlavor: VanillaAdapter
    }
    class CrossPlayEngine {
        +geyserAdapter: GeyserAdapter
        +floodgateAdapter: FloodgateAdapter
    }

    ServerEngine <|.. BedrockEngine
    ServerEngine <|.. JavaEngine
    ServerEngine <|.. CrossPlayEngine
```

### 2.2 Server Lifecycle State Machine
A server instance transitions through strict observable states:

```mermaid
stateDiagram-v2
    [*] --> NOT_CREATED
    NOT_CREATED --> INSTALLING: Create Server
    INSTALLING --> STOPPED: Installation Complete
    INSTALLING --> ERROR: Download/Config Failed
    
    STOPPED --> STARTING: Start Server
    STARTING --> ONLINE: Readiness Port Check Passed
    STARTING --> ERROR: Startup Failed / Port In Use
    
    ONLINE --> STOPPING: Stop Requested
    STOPPING --> STOPPED: Graceful Exit
    STOPPING --> CRASHED: Shutdown Timeout / SIGKILL
    
    ONLINE --> CRASHED: Process Exited Unexpectedly
    CRASHED --> STOPPED: Acknowledge / Recover
    ERROR --> STOPPED: Reset
```

### 2.3 Networking & Tunnel Architecture
External players connect across the internet without router configuration:

```mermaid
sequenceDiagram
    autonumber
    actor Player as External Player (Bedrock / Java)
    participant Relay as Public Relay (Anycast Edge)
    participant Client as Playit Tunnel Client (Android)
    participant Server as Local Minecraft Server (Android)
    
    Note over Client,Server: Android Device Local Execution
    Client->>Relay: Establish control tunnel (UDP/TLS)
    Relay-->>Client: Assign public endpoint (e.g. host.joinmc.link:19132)
    Client->>Server: Local binding verification (127.0.0.1:19132)
    
    Player->>Relay: Connect to host.joinmc.link:19132 (UDP RakNet)
    Relay->>Client: Forward packet through encrypted tunnel
    Client->>Server: Forward packet to local port 19132
    Server-->>Client: Server response packet
    Client-->>Relay: Tunnel response packet
    Relay-->>Player: Forward response to player
```

---

## 3. Storage Architecture

```
files/minehost/
├── servers/
│   └── <server_uuid>/
│       ├── server.json             # Server metadata & settings
│       ├── server.properties       # Minecraft vanilla server configuration
│       ├── worlds/                 # Active world save directory
│       │   └── default/
│       ├── plugins/                # Engine plugins / extensions
│       ├── logs/                   # Log history & crash reports
│       └── backups/                # Local compressed world snapshots (.zip)
├── engines/
│   ├── bedrock/                    # Bedrock engine binaries / phar files
│   └── java/                       # Java server JARs
├── runtimes/
│   └── jre/                        # Portable Java Runtime Environment
└── tunnels/
    └── playit/                     # Tunnel agent configuration & identity cache
```

---

## 4. Security & Isolation Model

1. **Local World Integrity:** All worlds and configurations are stored in private app storage (`context.filesDir`).
2. **Native Execution:** Executables adhere to Android API 29+ W^X policies by executing from `context.applicationInfo.nativeLibraryDir`.
3. **No Root Requirement:** Operates entirely within standard unprivileged Android user space.
4. **Console Shell Protection:** Command console strictly passes input to Minecraft server standard input; operating system shell commands (`/bin/sh`, PowerShell, cmd) are blocked.
5. **Relay Token Protection:** Playit authentication secret keys and sensitive tokens are encrypted using Android Keystore / EncryptedSharedPreferences.

