# MineHost — Master Product & Engineering Roadmap

> **Product promise:** Free, phone-hosted Minecraft servers with the lowest practical latency, cross-edition play, and international reach — operated through MineHost-owned software and infrastructure.
>
> **Status:** Living plan. A checkbox is not evidence of completion. Every implementation item must be backed by automated tests, build evidence, and (where hardware/network behaviour is involved) a reproducible device or external-network test.

Last reviewed: 2026-11-10  
Repository: https://github.com/nomanaqil56/MineHost  
Default branch at review: `main`

---

## 1. Product identity and non-negotiable principles

MineHost is not a cloud game-server host. The user's Android device runs the Minecraft server process and stores the world. MineHost supplies the Android management application and, when direct reachability is impossible, its own connection coordination and relay infrastructure.

**Positioning:** *Your device. Your world. Your network.*

1. **Phone-first hosting:** the actual game server and authoritative world remain on the host's Android device. No cloud game-server rental is required.
2. **Free core hosting:** creating and running a local server must not require a subscription. Any future paid offering must be optional and must not take away basic local hosting.
3. **Direct-first connectivity:** LAN and viable direct public routes are preferred. A MineHost-operated relay is the fallback for NAT/CGNAT and restrictive networks.
4. **MineHost-operated services:** no permanent dependency on a managed tunnel provider, third-party hosted control plane, hosted monitoring vendor, or external game-hosting provider.
5. **Open-source is acceptable; opaque service lock-in is not:** we may use and comply with well-maintained open-source libraries and server engines. MineHost must own deployment, configuration, operations, and user experience for its backend and relay services.
6. **Honest performance:** never promise zero lag. Measure RTT, jitter, packet loss, upload capacity, TPS, MSPT, memory, and thermal state separately. Do not label a tunnel "online" as proof that a player can join.
7. **World ownership and portability:** users can back up, restore, import, and export their worlds and server data without being locked into MineHost.
8. **Resilience:** an already-running local server should continue running if MineHost's control plane is temporarily unavailable. Existing relay sessions may survive according to their design; new remote sessions may be unavailable during an outage.
9. **No fake functionality:** no simulated processes, fabricated endpoints, fake player counts, guessed compatibility, or success states unsupported by a real probe.
10. **Preserve working product decisions:** do not redesign the established UI or replace sound architecture without a concrete reason and tests.

### Independence: what it means

MineHost can own its application, protocol adapters, coordinator, relay software, deployment configuration, domains, monitoring, and operational processes. It cannot be independent of the public internet, electricity, mobile carriers, ISPs, DNS registries, device manufacturers, or the physical data-centre/transit providers it rents from. The practical goal is to eliminate dependence on third-party *managed connectivity products*, not to claim ownership of the internet.

---

## 2. Current repository baseline (source review, not a fresh build)

The repository already contains a Kotlin/Jetpack Compose Android application, Room/Hilt layers, server abstractions, Bedrock and Java engine classes, a tunnel-provider abstraction with a Playit implementation, world backup utilities, diagnostics, foreground-service code, and unit-test sources.

The latest documented source audit identifies major unresolved execution and verification blockers. In particular:
- No validated Android-compatible PHP runtime is present for PocketMine.
- No validated desktop-compatible Java VM for Android is present for Paper/Geyser.
- Downloaded executable files in app-writable storage are not a safe assumed execution path on modern Android; Android's executable mapping and packaging restrictions need a proven design.
- The Playit integration is a third-party managed service and is not the target final architecture. Its agent download/execution path is not proven on Android.
- The existing test harness contains simulations and protocol-framing checks; these do not prove that a real Minecraft server runs or that external players can join.
- No physical-device, real-client, or international-network acceptance result is currently established by this source review.

**Do not describe MineHost as production-ready until the acceptance gates in this document have passed.** Update `docs/PRODUCTION_READINESS.md` and `docs/DEVELOPMENT_PROGRESS.md` whenever evidence changes.

---

## 3. Target architecture

### 3.1 Android host

- Kotlin + Jetpack Compose application (preserve current UI unless a justified change is approved).
- Server lifecycle controller and engine registry.
- Versioned runtime/engine installer with signed or trusted integrity metadata.
- Per-server app-private data directories.
- Foreground hosting service with a valid declared Android foreground-service type and subtype.
- Explicit process supervision, graceful stop, crash detection, and recovery.
- Network monitor for Wi-Fi, cellular, Ethernet, and IPv4/IPv6 capabilities.
- Thermal, memory, battery, storage, CPU, and upload monitoring.
- Backup/restore that is safe while a server is running.
- Local-first operation independent of cloud control-plane availability.

### 3.2 Server engine adapters

Keep each engine behind a clear interface: capability discovery, install/verify, configure, start, readiness probe, command, stop, logs, and version support.

Candidate engines (not yet a promise of compatibility):
- Bedrock: evaluate maintained implementations such as PocketMine-MP and PowerNukkitX. Validate gameplay parity, Android ARM64 runtime feasibility, maintenance health, licence, and support for the current Bedrock transport before selecting.
- Java: Paper is the initial performance-oriented candidate, subject to a validated Android-compatible JVM and its actual runtime requirements.
- Cross-play: Geyser and Floodgate only after a supported Java engine is running. Cross-play is a composition of Paper + Geyser (+ Floodgate where appropriate), not a separate standalone game server.
- Official Mojang Bedrock Dedicated Server is not assumed viable on ARM64 Android; do not rely on x86 emulation as the primary performance path.

**Engine selection gate:** no engine is marked supported until a version-pinned binary/runtime has installed, passed integrity checks, launched on a physical supported device, answered its real protocol readiness probe, accepted a real client, persisted a world, and restarted successfully.

### 3.3 MineHost control plane (control plane only)

A small self-operated service, initially implemented in a language such as Go, handles:
- host/device registration and revocable credentials;
- session and endpoint coordination;
- relay discovery/allocation;
- short-lived connection tickets and authorization;
- health/telemetry ingestion with data minimization;
- versioned configuration and compatibility metadata;
- abuse controls, rate limits, quotas, and operational health.

It must **not** run Minecraft worlds or become a required dependency for a locally connected server. Avoid putting game packets through the coordinator.

### 3.4 MineHost data plane

MineHost operates its own regional relays. The relay forwards traffic; it does not simulate or host the Minecraft world.

- Direct LAN first.
- Direct public IPv6 when reachable and safe.
- Protocol-appropriate NAT traversal when supported.
- Self-hosted TCP forwarding for Java.
- Self-hosted UDP forwarding for the selected Bedrock transport.
- Relay fallback when direct connectivity fails.
- Region/path selection based on measured end-to-end path quality and capacity, not geography alone.
- Encryption and authenticated host-to-relay sessions.
- Rate limiting, abuse detection, DDoS mitigation strategy, and resource isolation.

Do not assume a generic WebRTC data channel or UDP hole-punching library can carry all Minecraft traffic. Java gameplay is TCP; Bedrock transport requirements vary by server/client protocol version and must be verified against current official specifications and selected server engine.

### 3.5 Network path selection

Recommended decision order:
1. Same-LAN direct connection.
2. Direct public IPv6 where available.
3. Direct public IPv4 where the host is genuinely reachable.
4. Protocol-compatible direct NAT traversal when it can be safely established.
5. MineHost-operated regional relay.

Measure both player-to-relay and relay-to-host paths, plus loss, jitter, capacity, and relay load. A geographically nearby relay is not necessarily the best route. A relay cannot defeat physical distance or a poor host upload link.

### 3.6 Wi-Fi and mobile data

Both are supported. MineHost should:
- observe network availability and capabilities through Android's connectivity APIs;
- prefer the healthier route based on measurements and user data-cost preference;
- keep the server local and continue LAN hosting where possible;
- detect network changes and re-establish the tunnel/endpoint;
- communicate that established TCP sessions may disconnect when the host's network changes;
- never claim seamless migration unless it is proven end-to-end.

Do not combine Wi-Fi and cellular bandwidth by default. Multipath bonding adds complexity and requires compatible transport at both ends.

---

## 4. Performance and reliability requirements

### Network measurements
- RTT (median and p95), jitter, packet loss, throughput, and endpoint reachability.
- Distinguish LAN ping, direct WAN ping, relay RTT, and server response time.
- Protocol-aware probes: Java Server List Ping for Java; the selected Bedrock engine's current supported status/query probe for Bedrock.
- No TCP-only health check for a UDP gameplay endpoint.
- Keep probe traffic small and rate-limited.

### Server measurements
- TPS and MSPT where the engine exposes them.
- CPU use, process RSS, heap use (Java), available memory, storage, and process exit reason.
- Player count only from authoritative server events/query responses.
- Record thermal state and throttling signals separately from network lag.

### Reliability
- Startup readiness deadline based on actual protocol response, not just process liveness or an open port.
- Graceful shutdown and bounded force-stop fallback.
- Crash classification and safe recovery.
- Tunnel failure must not stop the local Minecraft process.
- Reconnect with bounded exponential backoff and jitter.
- Avoid infinite restart loops and battery-draining retries.
- Protect worlds from interrupted backup/restore and abrupt process death.
- Make every error actionable and visible in Diagnostics.

### Device protection
- Hardware-aware memory recommendations and conservative defaults.
- Thermal warnings and configurable graceful shutdown.
- Storage-space checks before downloads/backups.
- User-visible battery/network usage.
- Wake locks only while hosting requires them; release on every exit path.
- No unsupported claim that an Android foreground service can prevent OEM/OS process termination.

---

## 5. Data, backups, and portability

- Per-server isolated directories and explicit ownership.
- Atomic downloads and installs; never overwrite a working runtime with a failed download.
- Pin versions; verify a trusted checksum/signature before installation. If upstream publishes no trusted digest, mark the artifact unverified and do not silently treat a locally computed hash as authenticity verification.
- Validate archives against path traversal (Zip Slip), symlinks, unexpected file types, size limits, and decompression bombs.
- Safe backup of a live world using engine-supported save/flush barriers; fixed sleeps alone are not proof of a completed save.
- Atomic restore with pre-restore rollback snapshot.
- Import/export worlds and complete server directories.
- Clear backup retention controls and storage warnings.
- Keep user world data local by default; cloud backup is optional only if introduced later and must be explicitly opt-in.

---

## 6. Security, privacy, legal, and distribution

- No secrets in source control, logs, crash reports, or public diagnostics.
- Use Android Keystore-backed encryption for app-managed credentials where feasible.
- Short-lived, scoped relay credentials; revocation and rotation.
- Authenticated and encrypted host-to-relay channels.
- Validate server commands and never interpolate untrusted input into shell commands.
- Least-privilege Android permissions and non-exported internal components.
- Dependency inventory and licence notices for every shipped/downloaded component.
- Review upstream licences and redistribution conditions before bundling or downloading server engines/runtimes.
- Follow Mojang/Microsoft branding and commercial-use guidelines; do not imply affiliation or redistribute game assets.
- Review Google Play policies for downloaded/executed code, foreground services, background operation, user data, and app disclosures before distribution.
- Publish a privacy policy that accurately describes network coordination, relay metadata, diagnostics, and retention.
- Do not promise that "no third party" is involved in the public internet path; explain what MineHost operates and what the ISP/data-centre providers supply.

---

## 7. Delivery phases and exit criteria

### Phase 0 — Repository audit and truthful baseline
- [ ] Reproduce current build and unit tests in CI.
- [ ] Inventory dependencies, licences, binaries, and download sources.
- [ ] Reconcile README, architecture, networking, engine, progress, and readiness docs.
- [ ] Identify dead code, placeholders, false-positive tests, and unimplemented UI actions.
- [ ] Create reproducible build instructions and CI artifacts.
**Exit:** build/test status is independently reproducible and every subsystem has an evidence-based status.

### Phase 1 — One real Bedrock server on Android
- [ ] Select a maintained Bedrock engine after current protocol/maintenance review.
- [ ] Resolve Android ARM64 runtime and executable packaging legally and technically.
- [ ] Pin engine/runtime versions and trusted hashes.
- [ ] Install, launch, protocol-probe, stop, restart on a physical ARM64 phone.
- [ ] Join from a real Bedrock client over the same Wi-Fi.
- [ ] Verify world persistence and safe backup/restore.
**Exit:** repeated real-device local multiplayer acceptance test passes.

### Phase 2 — Reliable phone hosting
- [ ] Validate foreground-service declarations and Play policy.
- [ ] Test screen-off and sustained hosting.
- [ ] Test thermal/memory pressure and OEM battery settings.
- [ ] Test crash detection and recovery.
- [ ] Validate live backups against engine save completion.
**Exit:** sustained device test with no unexplained data loss and measured resource profile.

### Phase 3 — First MineHost-owned public relay
- [ ] Specify protocol-aware tunnel framing and threat model.
- [ ] Deploy one self-managed relay and coordinator in a controlled environment.
- [ ] Implement authenticated encrypted host tunnel.
- [ ] Implement public endpoint allocation and expiry.
- [ ] Test Java TCP and selected Bedrock transport independently.
- [ ] Test CGNAT host and external client on a different ISP.
- [ ] Measure RTT, jitter, loss, bandwidth, and relay resource cost.
**Exit:** real external client joins reliably without Playit or another managed tunnel.

### Phase 4 — Direct-first routing and network changes
- [ ] LAN direct path.
- [ ] IPv6 direct path.
- [ ] Protocol-compatible NAT traversal where feasible.
- [ ] MineHost relay fallback.
- [ ] Wi-Fi/cellular detection and user preference.
- [ ] Tunnel recovery on network change.
**Exit:** automated path selection is validated on multiple real networks; failure modes are reported accurately.

### Phase 5 — Java and cross-play
- [ ] Prove a supported Android JVM on target devices.
- [ ] Launch pinned Paper build and pass Java status/join tests.
- [ ] Add Geyser and Floodgate only after Java acceptance.
- [ ] Validate Bedrock transport compatibility for current releases.
- [ ] Measure combined RAM/CPU/thermal footprint.
**Exit:** real Java and Bedrock clients can play together on a physical phone-hosted world.

### Phase 6 — Global scale and sustainability
- [ ] Add relay regions based on real user distribution and measured routes.
- [ ] Capacity planning, quotas, abuse mitigation, DDoS response, and billing visibility.
- [ ] Redundant coordinator and monitoring.
- [ ] Operational alerts, backups, restore drills, incident runbooks.
- [ ] Sustainable free-tier/fair-use policy for relay bandwidth.
**Exit:** documented service objectives and a funded operating plan.

### Phase 7 — Public release
- [ ] Security review and threat-model sign-off.
- [ ] Licence and redistribution audit.
- [ ] Play policy review and privacy disclosures.
- [ ] Release signing and reproducible release process.
- [ ] Device compatibility matrix and known limitations.
- [ ] Closed beta with real users and crash/performance telemetry.
- [ ] Store listing, support, and incident response ready.
**Exit:** all release gates are met; no critical or high-severity known blocker remains.

---

## 8. Initial beta metrics (targets, not promises)

- At least 50 external beta testers.
- At least 90% successful engine installs on declared supported devices.
- At least 95% successful starts after a successful install.
- Stable 20 TPS under a published test workload where the device can sustain it.
- MSPT p95 below 50 ms for the published baseline workload.
- Packet loss below 0.5% in controlled network tests.
- Report p50 and p95 RTT by region and connection path.
- At least 30% seven-day return rate as an initial product signal.
- Track relay GB per active server-hour and cost per active server-hour before promising free unlimited relaying.

---

## 9. Definition of production-ready

MineHost is **not** production-ready merely because the APK builds or the dashboard looks complete.

Production-ready requires:
1. Real engine installation and execution on supported physical Android devices.
2. Real Minecraft client joins on LAN and over the public internet.
3. Java/Bedrock cross-play only where explicitly listed as supported and tested.
4. MineHost-operated relay connectivity for networks that cannot accept direct traffic.
5. Reliable screen-off operation within documented Android/OEM limitations.
6. No known world-loss path in backup/restore acceptance tests.
7. Reproducible CI build and passing automated tests.
8. Security, dependency/licence, privacy, and store-policy review.
9. Monitoring, incident response, relay capacity and a sustainable operating model.
10. Accurate compatibility matrix, limitations, and performance disclosures.

**Rule:** Never move a feature to "done" based on source code alone. Attach test evidence and the environment in which it passed.
