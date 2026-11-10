# MineHost — System Architecture

This document distinguishes current source code from the target independent-hosting architecture. **Target components are not claims of implementation.** See [Production Readiness](PRODUCTION_READINESS.md), [Development Progress](DEVELOPMENT_PROGRESS.md), and [Master Roadmap](MINEHOST_MASTER_ROADMAP.md).

## 1. Android application layers

- **Presentation:** Jetpack Compose, Material 3, Navigation Compose, ViewModels and StateFlow.
- **Domain:** server models, use cases, repository contracts, engine abstractions and lifecycle rules.
- **Data:** Room database, repositories, DataStore preferences and app-private server/world files.
- **Platform:** foreground service, notifications, wake locks, thermal monitoring, network callbacks and process management.
- **Engine adapters:** Axolotl-PM Bedrock adapter and Paper Java adapter behind the server-engine interface.
- **Networking adapters:** local network helpers, connection health, experimental provider interface and future MineHost edge/TURN clients.

The UI must display real state from the server process and protocol probes. A process being alive, a TCP socket being open, or a relay assigning an endpoint is not sufficient to declare gameplay ready.

## 2. Current Android runtime topology

```text
Compose UI
   |
ViewModels / Use Cases
   |
ServerController ---- TunnelManager ---- TunnelProvider interface
   |                         |
ServerEngineRegistry     Experimental Playit provider
   |
Axolotl-PM Bedrock adapter / Paper Java adapter
   |
RuntimeManager + app-private server directories
   |
PHP 8.4 Android ARM64 static binary in APK jniLibs
   |  ext-webrtc module required for NetherNet
Managed child process (only when runtime + PHAR are verified)
   |
Actual Minecraft server process
```

The Bedrock adapter is pinned to Axolotl-PM 5.51.1 / Bedrock 1.26.60 and generates NetherNet configuration. The app verifies that the PHP runtime launches and lists the `webrtc` module before starting. None of this is considered device-verified until a physical Android phone starts the server and a real Bedrock client joins.

The original `pmmp/PocketMine-MP` repository is archived; Axolotl-PM is the maintained candidate. Paper/Java remains blocked on a compatible Android JVM. The Playit provider is experimental only and is not the intended production dependency.

## 3. Target independent networking topology

```text
                      MineHost Control Plane
             host registration / tickets / region choice
                              |
Player client ---- signalling HTTPS edge ---- TCP reverse tunnel ---- Android host
      |                                                                |
      +------ direct WebRTC ICE candidate pair ------------------------+
      |
      +------ MineHost TURN (UDP fallback) -----------------------------+
                                                                       |
                                                               Axolotl-PM
                                                               NetherNet
```

Java Edition uses a separate TCP public endpoint through the MineHost edge/reverse tunnel. Bedrock NetherNet separates HTTPS/HTTP signalling (TCP) from WebRTC gameplay (UDP). The control plane coordinates sessions but must not proxy gameplay packets. MineHost TURN relays Bedrock gameplay only when direct ICE fails; it does not host the Minecraft world.

Connection priority:
1. LAN direct.
2. Direct public IPv6 or genuinely reachable public IPv4.
3. Direct NetherNet WebRTC ICE candidate pair.
4. MineHost-operated TURN for Bedrock UDP when direct ICE fails.
5. MineHost TCP edge/reverse tunnel for Java TCP and Bedrock HTTPS signalling when the phone is behind NAT/CGNAT.

A successful NetherNet `GET /v1/join` probe proves signalling only. It does not prove the UDP WebRTC gameplay channel. Real client joins are mandatory.

See [NETWORKING.md](NETWORKING.md) for protocol and relay details.

## 4. Server lifecycle

Expected lifecycle:
- Validate server record and selected engine.
- Verify exact runtime, extension and server artifact versions/digests.
- Validate configuration and port availability.
- Start the foreground hosting service with a valid Android service type.
- Launch the real engine process.
- Capture stdout/stderr without blocking.
- Require an engine-specific protocol readiness response before declaring ONLINE.
- Start networking independently; tunnel failure must not stop local hosting.
- Stop tunnel and server gracefully on user request.
- Detect unexpected process exit and persist CRASHED/ERROR state.
- Release wake locks, thermal listeners and service resources on all terminal paths.
- Pause/flush world saving before live backup; refuse restore while the target server is running.

## 5. Storage layout

The app stores user data under private app storage:

```text
files/minehost/
  servers/<server-id>/
    worlds/<world-name>/
    backups/<backup-id>.zip
    backups/<backup-id>.json
    pocketmine.yml
    server.properties
    nethernet.key
  engines/
  tunnels/                 # experimental only
```

The PHP executable is packaged in the APK's `jniLibs` and extracted to Android's native library directory. The exact layout and backup format are documented in [STORAGE.md](STORAGE.md).

World data remains local by default and must be exportable. Backup and restore must preserve the NetherNet identity key, validate content, and be tested against actual engine worlds.

## 6. Android platform constraints

- Android ART does not execute desktop Java server JARs.
- PHP and Java runtimes must be compiled/packaged and validated for the target Android ABI and dynamic linker.
- Do not assume a downloaded ELF in writable app data can execute on modern Android.
- Foreground services improve process priority but do not guarantee survival against OEM/OS termination or memory pressure.
- MineHost uses Android's `specialUse` foreground-service type for user-initiated continuous hosting on supported Android versions; older versions use the compatible declared type. Play Console declaration/review is still required.
- Wake locks and Wi-Fi locks have battery costs; acquire only while required and release on every failure/stop path.
- Thermal and memory pressure must be surfaced to the user, with configurable safe behaviour.

## 7. Security and privacy boundaries

- Axolotl-PM auto-updater, auto-report and anonymous statistics are disabled in MineHost-generated configuration; MineHost manages engine versions and diagnostics.
- Server console input is Minecraft command input only; never execute it through a system shell.
- Keep server/world files in app-private storage unless the user explicitly exports them.
- Use least-privilege permissions and non-exported internal components.
- Protect relay and control-plane credentials with scoped, revocable, short-lived tokens.
- Encrypt host-to-edge traffic and use ephemeral TURN credentials.
- Redact secrets from logs.
- Verify downloaded artifacts against pinned trusted digests.
- Keep MineHost's local server operational if the control plane is temporarily offline.

## 8. Architecture decision rule

Prefer small, testable adapters over a monolithic rewrite. Preserve the current UI and stable data/domain contracts. Any architecture change must include:
- the problem it solves;
- alternatives considered;
- Android and protocol compatibility;
- security/licence implications;
- tests and rollback plan.

The target architecture is complete only when physical-device and external-network acceptance tests pass.
