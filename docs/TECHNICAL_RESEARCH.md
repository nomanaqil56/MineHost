# MineHost — Technical Research & Feasibility

> This document records engineering constraints and candidate approaches. It is not proof that a candidate works on Android. Current delivery priorities and evidence requirements are in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).

## 1. Product and system constraints

MineHost runs the actual Minecraft server process on the user's Android phone. The phone stores the world and supplies CPU, memory, storage, battery/charging and upstream bandwidth. MineHost's backend coordinates remote access and provides self-operated relay fallback; it does not host the Minecraft world.

This creates three independent performance budgets:
1. **Game server:** CPU tick time, memory, storage I/O, plugin/mod load, thermal throttling.
2. **Host network:** upload bandwidth, carrier/router NAT, packet loss, jitter, route stability.
3. **Remote path:** direct route or player-to-relay-to-host route, relay capacity and peering.

A low ping does not prove healthy TPS. A 20 TPS server does not prove a low-latency network. Measure them separately.

## 2. Bedrock engine candidate: Axolotl-PM

The original `pmmp/PocketMine-MP` project ended support and was archived in July 2026. MineHost should not build its current-version promise around that archived repository.

[Axolotl-PM](https://github.com/axolotl-pm/PocketMine-MP) is the maintained continuation. Its current 5.51.1 release (2026-11-02) targets Bedrock 1.26.60 / v26.60 and includes support for NetherNet. Axolotl-PM is still not a vanilla-parity engine; gameplay differences such as world generation, redstone and mob AI must be disclosed.

### Android runtime
[Axolotl-PM PHP-Binaries](https://github.com/axolotl-pm/PHP-Binaries) publishes an Android ARM64 PHP 8.4 build. Its build script compiles `axolotl-pm/ext-webrtc` (version 0.3.1 in the inspected build) for NetherNet, and its Android build workflow checks that the PHP executable is statically linked.

The Android ARM64 PHP 8.4 asset SHA-256 published by GitHub Releases is:
`45b462ff5e290ee06d34eced589c325d27a2743185acab53acab291b0188e181`

MineHost now pins this asset in `tools/prepare_android_php_runtime.sh`, packages the executable through `jniLibs/arm64-v8a/libphp.so`, and checks at runtime for the PHP `webrtc` module before starting the server.

**Still unverified:** CI packaging, APK extraction, execution on physical Android ARM64, required extension availability, Axolotl-PM startup, NetherNet signalling, UDP ICE/DTLS data channel, and a real Bedrock client join. Review every PHP/native dependency licence and the upstream release/update policy before public release.

### NetherNet
Bedrock 1.26.60 uses NetherNet rather than legacy RakNet. NetherNet uses WebRTC (ICE, DTLS and SCTP DataChannels):
- TCP HTTP(S) signalling, including `GET /v1/join` and `POST /v1/join/{networkId}`.
- UDP WebRTC game traffic.
- Axolotl-PM's `ext-webrtc` extension backed by libdatachannel.
- STUN/TURN ICE servers for NAT traversal and relay.

The generated MineHost config enables `transport.name: nethernet`, built-in signalling, a persistent `nethernet.key`, and a fixed UDP port range sized from max players. `ice-udp-mux` remains disabled because Axolotl-PM documents that it cannot be combined with TURN.

A successful `GET /v1/join` is only a signalling readiness result. It does not prove that the UDP WebRTC game path is reachable. Real client joins are mandatory.

### MineHost connectivity implications
For Bedrock, do not reinvent NAT traversal if NetherNet already supplies WebRTC ICE. MineHost should:
1. Provide an authenticated outbound TCP reverse tunnel from the phone to a MineHost edge, allowing the edge to terminate HTTPS for the public signalling endpoint and forward to the phone's local Axolotl-PM HTTP signalling port.
2. Operate its own STUN/TURN infrastructure. Direct ICE candidate pairs should win when possible; TURN is the fallback for restrictive NAT/CGNAT.
3. Keep the TCP signalling path and UDP TURN data path separate and observable.
4. Avoid third-party managed STUN/TURN services in the production configuration.
5. Measure actual WebRTC gameplay latency separately from HTTP signalling RTT.

TURN relays all game traffic and consumes bandwidth. It must be regionally deployed, authenticated, capacity-limited and costed. Axolotl-PM warns that configured ICE servers must be reachable because candidate gathering can delay joins until timeout.

## 3. Java engine candidate

### Paper
- A performance-oriented Java server JAR with a mature plugin ecosystem.
- Android ART cannot directly execute desktop Java JARs. MineHost needs a real, compatible JVM and native libraries.
- The current adapter expects `libjava.so`; no validated Android JVM is present in the repository.
- Feasibility checks: Bionic/dynamic-linker compatibility, JVM native libraries, JIT behaviour, filesystem permissions, signal/process handling, memory mapping, supported Java version, sustained thermal behaviour.
- Test realistic memory allocation on physical phones before setting minimum device specifications.

## 4. Cross-play

Geyser translates compatible Bedrock client traffic for a Java server. Floodgate supports a Bedrock identity/authentication flow when configured with Geyser. This requires a working Java server and a compatible current Geyser version.

Do not advertise cross-play until Paper + Geyser (+ Floodgate where needed) execute on the same phone and real Java and Bedrock clients can join the same world.

## 5. Android process execution and packaging

- Android ART is not a desktop OpenJDK runtime.
- A server runtime must target the device ABI and compatible Android userspace/dynamic linker.
- A Linux binary compiled for glibc is not automatically executable on Android/Bionic.
- Downloading a file into app-private writable storage and setting its executable bit is not proof that Android will allow it to execute.
- Packaging native code in `jniLibs` has ABI, loader, library naming, linker and Play policy implications; a shared library is not automatically a standalone CLI executable.
- PRoot or a Linux rootfs may improve compatibility for some tools but adds storage, performance, security and maintenance costs. It must be justified by measurements.
- Pin every runtime and engine version; record source, ABI, licence, dependency set and trusted digest/signature.

## 6. Networking feasibility

### NAT and CGNAT
Mobile carriers and residential ISPs often prevent inbound connections. A listening server socket on the phone is therefore not necessarily reachable from the public internet.

### Java TCP
Java gameplay uses TCP. Direct LAN/IPv6/public IPv4 is preferred when reachable. Behind CGNAT, MineHost needs an authenticated outbound TCP reverse tunnel to its own edge, which exposes a public TCP endpoint and forwards byte streams to the local Paper port.

### Bedrock NetherNet
Axolotl-PM's current Bedrock transport is WebRTC:
- TCP HTTP(S) signalling on the configured server port (`GET /v1/join`, `POST /v1/join/{networkId}`).
- UDP gameplay using ICE, DTLS and SCTP DataChannels.
- Direct ICE candidate pairs should be selected when possible.
- MineHost-operated TURN is the fallback when direct UDP connectivity fails.

The phone's outbound TCP tunnel exposes the HTTPS signalling endpoint through a MineHost edge. A separate MineHost TURN service relays UDP gameplay when required. A TCP tunnel alone cannot carry or prove the WebRTC data path. A successful `GET /v1/join` is only signalling readiness, not proof that gameplay can connect.

Axolotl-PM documents that TURN and `ice-udp-mux` cannot be used together. MineHost therefore generates a fixed UDP port range sized from the configured max players and leaves mux disabled. TURN credentials must be short-lived and generated by MineHost; do not depend on public STUN/TURN services in production.

### MineHost-owned architecture
- LAN and direct public IPv6 where reachable.
- Direct WebRTC ICE for Bedrock when possible.
- MineHost TCP edge/reverse tunnel for Java TCP and Bedrock HTTPS signalling.
- MineHost TURN for Bedrock WebRTC UDP fallback.
- MineHost control plane for registration, short-lived credentials, rendezvous and relay allocation.
- Regional route selection based on measured RTT, jitter, loss, capacity and peering.
- Coordinator outage must not stop a locally running server.

TURN relays game traffic and consumes bandwidth. It cannot remove physical distance or compensate for weak host upload. Start with one self-managed region, measure per-server-hour traffic/cost, then expand based on actual demand.

## 7. Android background and device constraints

- A foreground service is necessary for a user-visible long-running host workload, but it does not guarantee process survival.
- Select an Android foreground-service type that accurately matches the work and current Android/Play policies; do not use a time-limited service type as a workaround for permanent hosting.
- OEM battery managers and low-memory killers can still terminate a process.
- Wake locks and high-performance Wi-Fi locks increase battery use.
- Thermal throttling can reduce CPU performance during sustained sessions.
- MineHost should recommend charging and stable Wi-Fi for dedicated hosting, while still supporting cellular when the user chooses it.
- Implement bounded restart/recovery and clearly report when the OS terminates a server.

## 8. Security and operational feasibility

- Self-hosting a relay means MineHost is responsible for patching, monitoring, capacity, abuse handling, DDoS response and incident response.
- Use authenticated encrypted host tunnels and short-lived scoped tickets.
- Rate-limit control-plane requests and relay sessions.
- Do not expose arbitrary port forwarding or open proxy functionality.
- Minimize telemetry and document retention.
- Budget relay bandwidth per active server-hour before promising unlimited free relaying.
- Use established open-source components where suitable, after licence and threat-model review.

## 9. Open research questions

- Which maintained Bedrock server has the best current protocol compatibility and can run efficiently on Android ARM64?
- Which PHP runtime and extension set can be distributed and executed lawfully and reliably on supported Android versions?
- Is there a maintainable Android JVM distribution suitable for Paper and Geyser?
- What transport adapter is needed for each supported Bedrock protocol generation?
- Which NAT traversal method works with the target mobile carriers, and what percentage still requires relay?
- What relay bandwidth and operating cost does a realistic Minecraft workload produce?
- Which foreground-service declaration and Play distribution approach is compliant for the exact host workload?

No candidate should be selected based on marketing claims or desktop benchmarks alone. Validate it on target devices with real clients.
