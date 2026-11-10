# MineHost — Master Product & Engineering Roadmap

> **Product promise:** Free, phone-hosted Minecraft servers with the lowest practical latency, cross-platform play, and MineHost-operated connectivity. The Minecraft world and game process stay on the host's Android device.
>
> **Status:** Engineering roadmap, not a claim of production readiness. Last reviewed: 2026-11-10.

## 1. Product identity

**MineHost — Your device. Your world. Your network.**

MineHost is not a cloud game-server rental service and must not silently move a user's game process or world to a cloud VM. The Android device is the game host. MineHost software helps the device run a server, makes it reachable, and provides management, diagnostics, recovery and portability.

### Non-negotiable product principles

1. **Phone-first hosting:** The real Minecraft server process and primary world data run and remain on the user's Android device.
2. **Free local hosting:** Hosting on the user's own device is free. Same-LAN play must not require a MineHost account, cloud service or relay.
3. **Direct-first networking:** Prefer LAN, then a reachable direct public path, then protocol-compatible NAT traversal where feasible, and only then a MineHost-operated relay.
4. **Infrastructure ownership:** MineHost owns and operates its control plane and relay software. No third-party managed tunnel is a permanent production dependency.
5. **No false claims:** Never show a server as online, a tunnel as connected, a player as online, or a route as low-latency without evidence.
6. **World ownership and portability:** Users can back up, restore and export their worlds and server files. No lock-in.
7. **Performance before breadth:** Validate one engine and one end-to-end path before adding more engines, versions or advanced features.
8. **Local resilience:** Loss of MineHost's control plane must not stop an already-running local server. New remote sessions may be unavailable until coordination recovers.
9. **Sustainable free service:** Direct connectivity is preferred to reduce relay costs. Relay capacity must have transparent fair-use and sustainable funding; do not promise unlimited free relaying without a funded capacity model.
10. **Honest performance language:** Target very low latency and stable ticks; never promise zero lag or universal uninterrupted service. Distance, ISP routing, radio quality, host upload bandwidth, Android scheduling and device thermals are outside MineHost's complete control.

## 2. Product scope

### Android host application

- Preserve the existing Kotlin, Jetpack Compose, Room and Hilt architecture unless an audit proves a change is necessary.
- Server creation, install, start, stop, restart, delete and lifecycle recovery.
- Real process supervision and protocol-level readiness checks.
- Live console, logs, safe command dispatch and real player tracking.
- Server files, configuration, plugin management and diagnostics.
- Local world storage, import/export, verified backups, safe restore and migration.
- Foreground hosting service with truthful notification and user controls.
- Device capability checks, resource recommendations, thermal and memory monitoring.
- Wi-Fi and cellular connectivity, network-change detection and tunnel recovery.
- Screen-off and sustained-hosting reliability, subject to Android/OEM limits.

### Server engine strategy

Do not equate an engine adapter or downloader with a working server. An engine is supported only after its runtime, launch method, configuration, readiness protocol, local client join, persistence and stop/restart flow pass on physical Android hardware.

Candidates to evaluate, not promises:
- Bedrock: a maintained ARM64-compatible implementation. Re-evaluate PocketMine-MP's maintenance status and actual gameplay/transport compatibility before committing. Evaluate PowerNukkitX only if its Java runtime path is viable.
- Java: PaperMC with a real Android-compatible ARM64 JVM and complete runtime libraries.
- Cross-play: GeyserMC and Floodgate on a supported Java server, only after both server and Bedrock transport versions are verified.
- Official Mojang Bedrock Dedicated Server is not assumed viable on ARM64 Android; do not use heavy CPU emulation as the default performance path.

Maintain a versioned engine registry and capability matrix. Disable unsupported engine/version combinations rather than silently falling back to another engine.

### Runtime and artifact management

- Resolve exact versions and release assets; do not blindly follow mutable `latest` releases for production.
- Verify artifacts against a trusted published checksum/signature before installation. If no trusted digest exists, clearly label the artifact unverified and decide whether policy permits installation.
- Download to a temporary file, verify, then atomically install.
- Handle cancellation, retry, disk-space checks, partial downloads and rollback.
- Audit Android execution constraints carefully. A shared object named `libphp.so` or `libjava.so` is not automatically a valid CLI executable. Validate ELF type, interpreter, libc dependencies, executable path policy and process invocation on each supported Android version.
- Never assume a binary downloaded into app-writable storage can execute on Android. Prove the selected packaging/loader strategy on devices.
- Keep the APK reasonably small; install optional engine/runtime components on demand where policy-compliant.
- Maintain third-party notices and a software bill of materials.

## 3. MineHost networking architecture

### Target topology

```text
Minecraft client
      |
      | 1. LAN / reachable direct endpoint
      | 2. Direct public IPv6 or public IPv4 where available
      | 3. Protocol-compatible NAT traversal where feasible
      | 4. MineHost-operated regional relay as fallback
      v
Host Android device
  - Minecraft engine (actual game process)
  - Local world files
  - Host networking agent
  - Health and route monitor

MineHost control plane (coordination only)
  - Host registration and short-lived credentials
  - Session/endpoint negotiation
  - Relay discovery and allocation
  - Health, capacity and abuse controls

MineHost data plane (only when direct path fails)
  - Self-operated TCP and UDP forwarding
  - Regional nodes selected by measured end-to-end path quality
  - No Minecraft simulation and no world hosting
```

### Connection priority

1. **LAN:** Same subnet; no cloud coordination required.
2. **Direct public path:** Use a reachable IPv6 endpoint or explicitly configured public address when safe and viable.
3. **NAT traversal:** Evaluate ICE/STUN-style discovery and UDP hole punching. A generic ICE library is not automatically compatible with Minecraft transports; implement protocol-specific adapters.
4. **MineHost relay:** Use only when a direct path fails or is not available. Relay traffic must be encrypted/authenticated between trusted endpoints and forwarded without modifying Minecraft payloads.

### Java and Bedrock must be separate transport adapters

- Java gameplay is TCP. A TCP reverse tunnel/proxy or a carefully designed TCP traversal path is required. UDP hole punching does not solve Java TCP.
- Bedrock transport varies by server/client protocol version. Do not hard-code a timeless “Bedrock is RakNet UDP only” assumption. Track current official protocol/transport changes and each selected server engine's support. Validate signaling and gameplay sockets independently.
- Geyser introduces a Bedrock-facing transport and a Java-facing backend. Both legs must be reachable and tested.
- Preserve packet boundaries for datagram transports; preserve byte-stream semantics for TCP.
- Avoid compression or buffering unless measurements prove it helps. Optimize for low queueing delay, not theoretical throughput alone.

### Route selection and failover

Measure, do not guess. Candidate route scoring can include:
- Player-to-relay RTT and relay-to-host RTT (when measurable).
- End-to-end probe latency.
- Jitter and packet loss.
- Relay load, available bandwidth and queue depth.
- Host upload capacity and network type.
- Region and ISP route quality.

Geographic proximity is only a heuristic. A relay near the player can still produce a poor end-to-end path. Do not switch an active session's underlying network without a recovery plan: Wi-Fi/cellular changes can invalidate NAT mappings and TCP sessions. Re-establish tunnels, update endpoint state and let clients reconnect when seamless migration is not possible.

### Control plane and data plane requirements

- Control plane outage must not stop a running local server or LAN play.
- Use short-lived, scoped host credentials; never put long-lived relay secrets in logs or client-visible diagnostics.
- Relay nodes must authenticate hosts, enforce quotas, protect against open-proxy abuse and rate-limit allocation.
- Build observability for node health, capacity, traffic volume, errors and cost.
- Use redundant control-plane instances and backup/restore for its minimal state.
- Do not deploy multiple regions until a single node has passed real Java/Bedrock traffic tests and cost measurement.

### Independence boundary

MineHost can own its software, control plane, relay fleet and operational procedures. It cannot eliminate reliance on the public internet, ISPs, DNS registrars, electricity or data-centre/colocation suppliers. Renting bare VPS/colo capacity while operating our own software is compatible with the independence goal; relying on a third-party managed game tunnel as a permanent dependency is not.

## 4. Wi-Fi and cellular behaviour

- Observe network capabilities and changes with Android `ConnectivityManager` / `NetworkCallback`.
- Support both Wi-Fi and mobile data.
- Prefer a stable, high-quality connection based on measurements and user policy, not simply “Wi-Fi always wins.”
- Show mobile-data usage estimates and ask before unexpectedly consuming large amounts of cellular data.
- Detect loss of validated internet separately from loss of LAN.
- On network change: detect, rebind/recreate the tunnel, verify the new route and endpoint, update the share address if it changed, and communicate whether players must reconnect.
- Do not claim bandwidth aggregation from Wi-Fi + cellular unless a real multipath transport is implemented and measured.
- Test carrier CGNAT, symmetric NAT, IPv6-only networks, DNS behaviour and common home-router configurations.

## 5. Performance and reliability

### Network metrics
- End-to-end RTT (median and p95), jitter, packet loss, throughput and reconnect time.
- Separate host-local server health from remote endpoint reachability.
- Measure both relay path and direct path where possible.
- Use protocol-aware probes; a TCP connect is not a valid Bedrock UDP health check.

### Server metrics
- TPS and MSPT where the engine exposes them.
- CPU, memory, storage, thread/process state and player count from real sources.
- Distinguish network latency from tick lag and client rendering lag.
- Provide hardware-aware RAM/player recommendations; never guarantee player capacity from RAM alone.

### Android lifecycle
- Correct foreground-service type, permissions, subtype declaration and Play policy justification.
- Partial wake lock only while required, released on every stop/error path.
- Thermal status and memory pressure warnings; optional safe save/shutdown thresholds.
- OEM battery restrictions and user guidance.
- Crash/stale-process reconciliation on app relaunch.
- Graceful stop with timeout and truthful forced-stop reporting.
- Sustained screen-off tests and host-device safety guidance.

## 6. World data, backups and portability

- App-private storage with documented paths and scoped access.
- Atomic backup creation with server save/flush coordination where supported.
- Archive integrity manifest and trusted checksum verification.
- Zip-slip/path traversal protection.
- Restore to a temporary directory, validate, create a safety snapshot, atomic swap, and rollback on failure.
- Never restore a world while the server is writing to it.
- Import/export worlds and complete server directories.
- Verify persistence after stop/start, process death, app upgrade and restore.
- Do not promise perfect recovery from abrupt power loss; document filesystem and engine limits.

## 7. Security, privacy, legal and store readiness

- Secure local file permissions and secret storage (Android Keystore where applicable).
- Encrypt authenticated host-to-relay tunnels; authenticate relay endpoints.
- Validate downloads and updates; pin versions and record provenance.
- Protect control-plane APIs, rate limits and relay allocation.
- No secrets, claim codes, tokens or player personal data in logs.
- Data minimization, privacy policy, retention/deletion policy and user-visible disclosures.
- Audit every dependency's licence and redistribution terms; ship notices and source offers where required.
- Minecraft/Mojang branding and non-affiliation disclaimer must comply with current official guidelines.
- Review Google Play policies for foreground services, downloaded executable code, user data, target API, testing and store listing before release.
- Release signing must use an owner-controlled production key stored outside the repository. Never use a debug key or commit signing secrets.
- Legal review may be prudent for commercial distribution; do not assume every open-source component or connectivity service is free of conditions.

## 8. Development phases and exit gates

### Phase 0 — Baseline audit (current priority)
- [ ] Read the complete repository and current main history.
- [ ] Reconcile stale/contradictory docs and status claims.
- [ ] Verify Gradle wrapper, dependency versions, manifest, target SDK and release configuration.
- [ ] Establish reproducible CI build and unit-test workflow.
- [ ] Run lint, unit tests and static checks in a real Android build environment.
- [ ] Inventory licences, runtime assets and download provenance.
- **Exit gate:** Reproducible clean build and truthful baseline; no undocumented production claims.

### Phase 1 — One real local Bedrock server
- [ ] Choose a maintained engine based on real protocol support and Android ARM64 viability.
- [ ] Produce or integrate a valid Android ARM64 runtime/launcher.
- [ ] Verify process launch, stdout/stderr, stdin, graceful stop and crash handling.
- [ ] Confirm ONLINE only after a real version-correct protocol response.
- [ ] Join from a second Bedrock device on the same LAN.
- [ ] Verify world persistence, backup and restore on a real world.
- [ ] Test screen-off operation, thermal behaviour and sustained load.
- **Exit gate:** Physical-device acceptance test passes repeatedly; no fake server state.

### Phase 2 — Independent public connectivity
- [ ] Define transport adapters for the selected Bedrock version and Java TCP.
- [ ] Build a minimal self-operated relay node and host-side agent.
- [ ] Build control-plane registration, auth, endpoint allocation and health APIs.
- [ ] Verify authenticated encrypted host-to-relay transport.
- [ ] Join from a genuinely external network and another country.
- [ ] Test Wi-Fi, cellular, CGNAT, IPv6, loss and reconnect.
- [ ] Measure direct-vs-relay latency, jitter, packet loss, bandwidth and cost.
- **Exit gate:** Reproducible international join and sustained play without a managed third-party tunnel.

### Phase 3 — Java and cross-play
- [ ] Validate an Android-compatible ARM64 JVM and launch path.
- [ ] Run Paper on-device and verify real Java client join.
- [ ] Integrate Geyser/Floodgate only after supported versions are confirmed.
- [ ] Test Bedrock-to-Java cross-play with real clients and authentication.
- **Exit gate:** Java and cross-play support claims match actual tested compatibility matrix.

### Phase 4 — Hardening and product completeness
- [ ] Multi-server lifecycle and concurrency safety.
- [ ] Advanced file/config/plugin management.
- [ ] Backup scheduling and migration.
- [ ] Dedicated phone mode and device recommendations.
- [ ] Security, privacy, abuse and dependency audits.
- [ ] Backend/relay redundancy and disaster recovery.
- [ ] Load/capacity/cost tests and sustainable fair-use policy.
- **Exit gate:** Operational runbooks, monitoring, support and recovery procedures exist.

### Phase 5 — Public beta and release
- [ ] Internal alpha on several Android devices.
- [ ] Closed beta with 30–50 real testers.
- [ ] Measure install success, start success, session duration, crashes, return rate and support issues.
- [ ] Fix critical/high bugs before expansion.
- [ ] Google Play policy and legal review.
- [ ] Production signing, privacy policy, listing, disclosures and support channel.
- [ ] Staged rollout with rollback plan.
- **Exit gate:** Release criteria below are met and no critical blocker remains.

## 9. Release acceptance criteria

These are proposed gates, not current results or universal guarantees.

- Clean reproducible release build; no debug signing.
- All unit tests and lint checks pass in CI.
- Physical-device test matrix completed for supported Android versions/ABIs.
- Real local Bedrock and Java tests for every engine advertised.
- Real external join over Wi-Fi and cellular, including CGNAT cases.
- International play test from at least two remote regions.
- Cross-play only advertised for tested client/server version combinations.
- No unresolved critical security, data-loss or process-lifecycle defects.
- Backups and restore tested against real worlds.
- p50/p95 RTT, jitter, loss, TPS/MSPT, crash rate and relay cost documented.
- Honest known-limitations page and support/recovery process.
- Sustainable relay funding/capacity plan approved before promising free global relay at scale.

## 10. Explicit non-goals and guardrails

- Do not build a cloud Minecraft hosting fleet.
- Do not promise zero lag, perfect uptime or unlimited free relay bandwidth.
- Do not add a third-party managed tunnel as a permanent hidden dependency.
- Do not invent endpoints, player counts, online status or latency.
- Do not claim Java, Bedrock or cross-play support before real-device tests.
- Do not force every user through a relay when direct connectivity works.
- Do not implement custom cryptography or a new TCP/IP stack.
- Do not add server engines faster than runtime, protocol and maintenance can be validated.
- Do not sacrifice world ownership or local LAN play when the backend is unavailable.

## 11. Current known baseline (2026-11-10)

Based on source inspection of the repository's `main` branch at the time this document was written:

- The Android application has a Kotlin/Compose/Room/Hilt foundation and server-management screens.
- Engine adapters, runtime download management, readiness probes, backup/restore safeguards, thermal/wake-lock services and a Playit tunnel adapter exist in source.
- The repository's latest development notes report 40 pure-Kotlin tests run outside Gradle with 40 passing.
- The latest development notes explicitly say Android-dependent code has not been compiled in that environment and no physical-device or real Minecraft-client test has been completed.
- PHP and Java runtime execution on Android remains an unproven critical blocker. A file named `libphp.so` or `libjava.so` must not be assumed to be a valid process executable.
- The current network provider is Playit-specific. MineHost-owned relay/control-plane infrastructure and direct NAT traversal are not implemented.
- Geyser cross-play is not currently operational; configuration scaffolding is not a working bridge.
- No production readiness claim is justified until the exit gates above are evidenced.

Update this section whenever new tests are actually run. Record the commit, device model, Android version, engine/runtime version, test network, client version and result. Do not promote planned or source-only work to “tested.”

## 12. Change-control rule for coding agents

Before changing code:
1. Read this roadmap plus the relevant subsystem docs.
2. Inspect the current implementation and tests; do not trust old status tables blindly.
3. Make the smallest coherent change on a feature branch.
4. Add or update tests for pure logic and failure cases.
5. Run every available check; clearly report checks that cannot run.
6. Update the progress and known-issues docs in the same change.
7. Never claim device, network, Play Store or production verification unless it actually happened.
8. Preserve existing UI and working architecture unless the change is necessary and explained.

**A task is not complete because code was written. It is complete when its acceptance test passes at the appropriate verification level.**
