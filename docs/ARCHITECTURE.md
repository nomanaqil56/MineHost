# MineHost — System Architecture

**This document describes both the current Android app structure and the target architecture.** The target network and some runtime layers are not implemented yet. See [MINEHOST_MASTER_ROADMAP.md](../MINEHOST_MASTER_ROADMAP.md).

## 1. Product boundary

The Android device is the actual Minecraft host:
- Minecraft server process runs on the phone.
- Primary world data stays in app-private device storage.
- MineHost's remote infrastructure coordinates connectivity and forwards traffic only when a direct path is unavailable.
- MineHost cloud must not host or simulate the Minecraft game server.

Local server operation and same-LAN play must not require the MineHost control plane.

## 2. Current Android application layers

- **Presentation:** Jetpack Compose, Material 3, Navigation Compose, StateFlow and ViewModels.
- **Domain:** server engine abstractions, use cases, lifecycle controller, tunnel provider interface, world/backup management.
- **Data:** Room metadata/log/player repositories, DataStore preferences and app-private file I/O.
- **Platform:** foreground service, wake-lock management, thermal monitoring, network callbacks/probes and process execution.

Current source includes screens for dashboard, server creation/detail, console, players, diagnostics and backups. Source presence does not prove device functionality.

## 3. Current execution components

- `ServerController`: coordinates server lifecycle and serializes start/stop operations.
- `ServerEngine`: engine adapter contract.
- `BedrockEngine`: PocketMine adapter source; PHP runtime/launcher remains unverified.
- `JavaEngine`: Paper adapter source; complete Android-compatible JVM remains unverified.
- `ManagedServerProcess`: process I/O, stop handling and readiness polling.
- `ReadinessProbe`: protocol probes; current Bedrock probe is RakNet-specific and must be version/engine validated.
- `RuntimeManager`: release metadata, downloads, checksums and local component inventory.
- `WorldManager`, `WorldBackupManager`, `WorldSwap`: server directories, backup and restore safeguards.
- `ServerForegroundService`, `WakeLockManager`, `ThermalManager`: Android lifecycle and device monitoring.

A successful file download or process launch is not enough to mark a server ONLINE. ONLINE requires a valid response from the selected server protocol.

## 4. Target architecture

```text
Android MineHost app
  UI / ViewModels
       |
  ServerController ---- World/Backup Manager
       |                       |
  Engine Adapter          App-private storage
       |
  Runtime + Launcher
       |
  Real Minecraft process on Android
       |
  Local protocol port
       |
  Connectivity Engine
       |---------------- LAN direct
       |---------------- Reachable public direct path
       |---------------- NAT traversal where supported
       |---------------- MineHost relay fallback

MineHost control plane (target, not yet implemented)
  registration / short-lived auth / session negotiation /
  relay discovery / endpoint lifecycle / health / abuse controls

MineHost data plane (target, not yet implemented)
  self-operated protocol-aware TCP and UDP relays
```

## 5. Server engine abstraction

The current `ServerEngine` interface isolates engine-specific operations such as install, configure, start, stop and command dispatch.

Only expose capabilities an engine actually supports. Cross-play is not a single Bedrock engine; it requires a Java server plus Geyser and optional Floodgate, with both transport legs and authentication configured. The current app correctly treats cross-play as unavailable until the integration is real.

## 6. Lifecycle state machine

Expected states:

```text
NOT_CREATED -> INSTALLING -> STOPPED
STOPPED -> STARTING -> ONLINE
STARTING -> ERROR or CRASHED
ONLINE -> STOPPING -> STOPPED
ONLINE -> CRASHED
```

Rules:
- Serialize start/stop requests.
- Do not claim success if a process handle is missing.
- Do not mark ONLINE from an open port or a live PID alone.
- Confirm readiness using the exact engine/client protocol.
- Distinguish requested stop from unexpected process exit.
- Clear public endpoint and release wake locks/services on every terminal path.
- Reconcile stale persisted ONLINE/STARTING state after app process death.

## 7. Networking abstraction

Current source has a `TunnelProvider` interface and a Playit-specific implementation. The current DI binding is Playit. This is not the final independence architecture.

Target providers/components:
- `LocalNetworkProvider`: LAN address/discovery only.
- `DirectConnectivityProvider`: reachable public path and protocol-specific NAT traversal.
- `MineHostRelayProvider`: MineHost-operated TCP/UDP relay path.
- Optional development adapters may exist, but a third-party managed tunnel must not be a mandatory production dependency.

Java TCP and Bedrock transport must have separate adapters. Bedrock transport support is engine/client-version dependent; do not assume one UDP/RakNet implementation is universal.

Control plane and gameplay data plane must be separated. If the control plane is offline, local hosting and LAN play continue.

## 8. Storage architecture

App-private root: `context.filesDir/minehost/`.

Expected layout:
```text
minehost/
  servers/<server-id>/
    worlds/
    plugins/
    logs/
    backups/
    config/
  engines/
  runtimes/
  tunnels/
  metadata/
```

Exact engine-specific world/config paths must be owned by the engine adapter. Do not assume every server uses the same directory layout.

## 9. Security model — requirements, not all implemented

- Keep worlds and local metadata in app-private storage.
- Validate all imported archives against path traversal and resource exhaustion.
- Verify downloaded runtime/server artifacts using trusted provenance.
- Keep credentials out of source, logs and crash reports.
- Use Android Keystore-backed protection for long-lived local secrets where applicable.
- Use short-lived scoped credentials for relay sessions.
- Authenticate both ends of host-to-relay channels and encrypt traffic.
- Apply rate limits, session limits and abuse controls to relay services.
- Do not pass user console commands through an OS shell; validate commands and send to the server stdin.
- Do not claim Keystore-protected tunnel credentials until implemented and tested.

## 10. Android policy and lifecycle

The server uses a user-visible foreground service and appropriate wake-lock/thermal handling. Before release:
- Verify service types and permissions for each supported Android API.
- Justify `specialUse` use and comply with current Play policy.
- Test screen-off behaviour and OEM process management.
- Avoid misusing time-limited foreground-service types.
- Release locks and stop monitoring on every stop/crash/error path.
- Explain battery, data and thermal implications to users.

## 11. Architecture constraints

1. Preserve working UI and app architecture unless a change is necessary.
2. Keep game-server process and world on the phone.
3. Keep LAN hosting independent of cloud services.
4. Prefer direct paths; relay only when needed.
5. Own the control plane and relay software for production.
6. Never claim zero latency or universal uptime.
7. Treat runtime and protocol compatibility as verified capabilities, not configuration flags.
