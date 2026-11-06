# MineHost — Testing Strategy & Verification Suite

## 1. Testing Philosophy

MineHost requires verification across multiple tiers:
1. **Unit Testing:** Architecture contracts, Room DAOs, StateFlow models, configuration parsers, and command sanitizers.
2. **Integration Testing:** Server lifecycle state transitions, world directory persistence, zip compression/decompression, and tunnel status reporting.
3. **End-to-End Simulation Testing:** Verifying that the application responds to real process output, monitors UDP/TCP ports, updates online statuses, and handles crashes gracefully.

---

## 2. Test Catalog

### 2.1 Configuration & State Tests
- `ServerStateTest`: Verifies transitions between `NOT_CREATED`, `INSTALLING`, `STOPPED`, `STARTING`, `ONLINE`, `STOPPING`, `CRASHED`.
- `ServerConfigurationTest`: Tests `server.properties` and `config.yml` generation for Bedrock and Java.
- `PortAllocationTest`: Verifies that conflicting server instances cannot bind to identical ports.

### 2.2 Storage & Backup Tests
- `WorldPersistenceTest`: Simulates server shutdown and reboot to ensure world directory contents remain intact.
- `WorldBackupTest`: Tests atomic backup creation, SHA256 checksum calculation, and recovery snapshotting.
- `WorldValidationTest`: Validates `.mcworld` and Java world ZIP archives against corrupted headers.

### 2.3 Networking & Tunnel Tests
- `TunnelStateTest`: Verifies state flow between `DISCONNECTED`, `CONNECTING`, `CONNECTED`, and `ERROR`.
- `PublicEndpointTest`: Verifies hostname and port formatting for Bedrock and Java clients.
- `ConnectionMonitorTest`: Verifies latency calculation and auto-reconnect triggers upon packet loss.

### 2.4 Server Console & Process Tests
- `ConsoleCommandValidatorTest`: Verifies that shell commands (`rm`, `cat`, `cmd`, `powershell`) are blocked while Minecraft commands (`say`, `kick`, `op`, `save-all`) are passed to stdin.
- `LogStreamParserTest`: Verifies ANSI color stripping, timestamp extraction, and log level tagging (INFO, WARN, ERROR).

