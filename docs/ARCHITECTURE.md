# MineHost — System Architecture

This document describes the current Android code structure and the target independent-hosting architecture. **Target components are not claims of implementation.** See [Production Readiness](PRODUCTION_READINESS.md) and [Master Roadmap](MINEHOST_MASTER_ROADMAP.md) for verified status.

## 1. Android application layers

- **Presentation:** Jetpack Compose, Material 3, Navigation Compose, ViewModels and StateFlow.
- **Domain:** server models, use cases, repository contracts, engine abstractions and lifecycle rules.
- **Data:** Room database, repositories, DataStore preferences, app-private server/world files.
- **Platform:** foreground service, notifications, wake locks, thermal monitoring, network callbacks and process management.
- **Engine adapters:** separate Bedrock and Java implementations behind the server-engine interface.
- **Networking adapters:** provider abstraction, local network helpers, connection health and tunnel lifecycle.

The UI must display real state from the server process, protocol probes and connection provider. A process being alive is not sufficient to declare a server ready.

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
Bedrock adapter / Java adapter
   |
RuntimeManager + app-private server directories
   |
Managed child process (only when a valid runtime is installed)
   |
Actual Minecraft server process
```

Current source contains adapters for PocketMine and Paper, but their required Android-compatible PHP/Java runtimes have not been validated or supplied. A downloaded PHAR/JAR is not itself a runnable engine.

## 3. Target independent networking topology

```text
                 MineHost Control Plane
      registration / auth / rendezvous / relay allocation
                         |
                  connection ticket
                         |
Player client ---- path negotiation ---- Android host
       |                                  |
       +--------- direct path ------------+
       |
       +--------- MineHost relay ---------- Android host
                    (fallback)
```

The control plane coordinates sessions and must not carry gameplay packets. The data plane is a MineHost-operated relay for sessions that cannot connect directly. The relay does not host Minecraft worlds.

Connection priority:
1. LAN direct.
2. Direct public IPv6 or genuinely reachable public IPv4.
3. Protocol-compatible NAT traversal where supported.
4. MineHost-operated relay.

Java gameplay is TCP. Bedrock transport depends on the current server/client protocol and must be implemented as a separate adapter. See [NETWORKING.md](NETWORKING.md).

## 4. Server lifecycle

Expected lifecycle:
- Validate server record and selected engine.
- Verify exact runtime and server artifact versions.
- Validate configuration and port availability.
- Start the foreground hosting service with a valid Android service type.
- Launch the real engine process.
- Capture stdout/stderr without blocking.
- Require a protocol-aware readiness response before declaring ONLINE.
- Start networking independently; tunnel failure must not stop local hosting.
- Stop tunnel and server gracefully on user request.
- Detect unexpected process exit and persist CRASHED/ERROR state.
- Release wake locks, thermal listeners and service resources on all terminal paths.

## 5. Storage layout

The app stores user data under private app storage. The exact layout is owned by the storage implementation and must not be inferred from this diagram:

```text
files/minehost/
  servers/<server-id>/
    worlds/
    plugins/
    configs/
    logs/
    backups/
  engines/
  runtimes/
  tunnels/
```

World data must remain local by default and be exportable. Backup and restore must be atomic, validated, and tested against actual engine worlds.

## 6. Android platform constraints

- Android ART does not execute desktop Java server JARs.
- PHP and Java runtimes must be compiled/packaged and validated for the target Android ABI and dynamic linker.
- Do not assume a downloaded ELF in writable app data can execute on modern Android.
- Foreground services improve process priority but do not guarantee survival against OEM/OS termination or memory pressure.
- Wake locks and Wi-Fi locks have battery costs; acquire only while required and release on every failure/stop path.
- Thermal and memory pressure must be surfaced to the user, with configurable safe behaviour.

## 7. Security boundaries

- Server console input is Minecraft command input only; never execute it through a system shell.
- Keep server/world files in app-private storage unless the user explicitly exports them.
- Use least-privilege permissions and non-exported internal components.
- Protect relay and control-plane credentials with scoped, revocable, short-lived tokens.
- Encrypt host-to-relay traffic.
- Redact secrets from logs.
- Verify downloaded artifacts against trusted upstream checksums/signatures and pin versions.
- Keep MineHost's local server operational if the control plane is temporarily offline.

## 8. Architecture decision rule

Prefer small, testable adapters over a monolithic rewrite. Preserve the current UI and stable data/domain contracts. Any architecture change must include:
- the problem it solves;
- alternatives considered;
- Android and protocol compatibility;
- security/licence implications;
- tests and rollback plan.

The target architecture is complete only when the physical-device and external-network acceptance tests pass.
