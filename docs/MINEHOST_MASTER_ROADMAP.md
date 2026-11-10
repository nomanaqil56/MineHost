# MineHost Master Roadmap

> **Status:** Active development — NOT production-ready.
> **Product contract:** The Minecraft server and world run on the host's Android device. MineHost supplies the Android host application and, when direct reachability is impossible, MineHost-operated coordination and relay infrastructure.

This is the canonical product and engineering roadmap. Update status only when supported by code review, automated tests, a real Android device, and real Minecraft clients as appropriate. A screen, interface, configuration generator, or unit test alone does not prove a feature works.

## 1. Product vision and non-negotiable principles

**MineHost: Your device. Your world. Your network.**

- Free local Minecraft hosting: the user's phone executes the actual game server.
- Local-first ownership: worlds, server files, configurations, and backups remain on the device unless the user explicitly exports them.
- Performance first: optimize server tick time, CPU, memory, thermal behavior, packet loss, jitter, and end-to-end latency.
- International access: friends can connect over the public internet from supported Minecraft clients and networks.
- Wi-Fi and mobile data: both are supported; choose the best healthy route without pretending two links automatically double bandwidth.
- Cross-platform play: Bedrock and Java clients can play together only when the selected server engine, bridge, Minecraft version, authentication mode, and transport are verified compatible.
- Connectivity independence: no permanent dependency on a managed tunnel vendor. MineHost owns and operates its coordination and relay software.
- Resilience: an existing local server should continue running if MineHost's control plane becomes temporarily unavailable, subject to Android process and network constraints.
- Honest status: never display a server as online, a tunnel as connected, a player as online, or a feature as supported without evidence.
- Sustainable free tier: local hosting and direct connectivity remain free; relayed bandwidth needs transparent fair-use limits and sustainable funding.
- No zero-lag promises: measure and minimize latency; never guarantee zero lag or uninterrupted service across arbitrary devices, ISPs, and countries.

**Independence clarification:** MineHost can avoid depending on third-party managed tunnel and game-hosting products. It cannot eliminate dependence on internet providers, data-centre connectivity, DNS registrars, power, Android, or upstream open-source projects. Initially renting VPS/colocation capacity while operating MineHost's own software is acceptable and is not the same as outsourcing the product to a managed hosting service.

## 2. Current verified baseline

The 2026-11-09/10 source audit found a substantial Android application foundation, but no end-to-end server hosting has been demonstrated on a physical device.

- Android app shell, Compose UI, Room, Hilt, server management screens, storage and backup code: present in source; Android build/device verification remains incomplete in the latest audit.
- Bedrock/PocketMine: engine code exists, but **blocked** because no usable Android-compatible PHP runtime is supplied.
- Java/Paper: engine/configuration code exists, but **blocked** because no usable Android-compatible JVM is supplied.
- Geyser/Floodgate: configuration groundwork exists; actual cross-play execution is not implemented.
- Playit: provider code exists, but it is a third-party dependency and must not be the long-term default. Real public joins have not been verified.
- MineHost-owned control plane and relay network: not implemented.
- Direct public connectivity/NAT traversal: not implemented.
- Physical-device and international Minecraft-client testing: not completed.
- Release signing and store compliance: not release-verified.

See [Production Readiness](PRODUCTION_READINESS.md), [Testing Status](TESTING.md), and [Device Testing](DEVICE_TESTING.md). These documents take precedence over aspirational feature descriptions.

## 3. Workstream A — Android application reliability

- [ ] Reproduce a clean Gradle build and unit-test run in CI.
- [ ] Fix compile, lint, dependency, manifest, and resource issues.
- [ ] Preserve the existing UI and architecture unless a measured defect requires a change.
- [ ] Audit server lifecycle state transitions and process ownership.
- [ ] Ensure stop requests work while a server is STARTING.
- [ ] Ensure unexpected process death is detected and state is reconciled honestly.
- [ ] Ensure foreground-service startup, notification, stop action, and cleanup are correct.
- [ ] Validate Android 14+ special-use foreground-service declaration and Play Console justification.
- [ ] Validate current target SDK and current Play policies before release.
- [ ] Test screen-off operation, Doze, OEM battery management, low memory, app upgrade, and reboot behavior.
- [ ] Implement graceful shutdown and bounded forced-stop fallback.
- [ ] Avoid holding CPU/Wi-Fi wake locks when no server or required network activity is active.
- [ ] Add clear user guidance for battery, thermal, storage, and memory constraints.

## 4. Workstream B — Real server runtimes and engines

### Bedrock
- [ ] Research and select a maintained Bedrock server implementation with a suitable licence.
- [ ] Verify current Minecraft protocol support, including Bedrock transport changes and version compatibility.
- [ ] Prove an Android ARM64 runtime strategy before advertising PocketMine as runnable.
- [ ] Build or obtain a reproducible, auditable Android-compatible PHP runtime; document toolchain, ABI, dependencies, licence, and checksums.
- [ ] Validate Android linker/Bionic compatibility and Android executable-memory/W^X constraints.
- [ ] Do not assume a desktop Linux/glibc binary or a downloaded executable can run on Android.
- [ ] Implement runtime installation, version pinning, integrity verification, rollback, and clear failure reporting.
- [ ] Start a real server, answer a real protocol readiness probe, join from a real Bedrock client, and verify persistence after restart.

### Java
- [ ] Research an Android-compatible ARM64 JVM strategy (Bionic-native runtime or a fully tested userspace approach).
- [ ] Validate Java version requirements for each supported Paper/Minecraft release.
- [ ] Prove Paper can launch and bind its server port on a physical Android device.
- [ ] Implement EULA consent and retain an auditable consent record.
- [ ] Pin Paper builds and verify published checksums.
- [ ] Test real Java client join, commands, plugins, graceful stop, and world persistence.

### Cross-play
- [ ] Do not expose cross-play until it genuinely starts a compatible Java server plus Geyser (and Floodgate only where appropriate).
- [ ] Validate the required Java runtime first.
- [ ] Verify Geyser/Floodgate compatibility for each supported Minecraft version and authentication mode.
- [ ] Test Bedrock-to-Java joins with real clients and document unsupported console/network configurations.

**Engine strategy:** ship one verified engine first. Add additional engines only after runtime and compatibility are real and maintainable. Avoid supporting many nominal engines that cannot actually launch.

## 5. Workstream C — MineHost-owned connectivity

### Architecture
- [ ] Define a versioned transport abstraction separating Java TCP from each supported Bedrock transport.
- [ ] Build a small control plane for host registration, authenticated session negotiation, endpoint allocation, and relay selection.
- [ ] Keep gameplay traffic off the control plane.
- [ ] Design direct-first routing: LAN/direct public path where reachable; relay only when required.
- [ ] Implement IPv6 reachability where host and client networks support it.
- [ ] Investigate UDP NAT traversal with STUN/ICE concepts; use direct UDP only for transports that support it and are verified.
- [ ] Treat Java TCP traversal separately. Do not claim generic UDP hole punching solves Java TCP.
- [ ] Build MineHost-operated UDP relay support for compatible Bedrock transports.
- [ ] Build MineHost-operated TCP forwarding for Java.
- [ ] Add authenticated, encrypted host-to-relay tunnels and protect public endpoints against unauthorized use.
- [ ] Add relay quotas, abuse controls, connection limits, observability, and emergency shutdown controls.
- [ ] Support multiple relay regions behind MineHost's own provider abstraction, with MineHost-operated nodes as the production path.
- [ ] Ensure local LAN hosting does not require an account or backend availability.
- [ ] Ensure a control-plane outage does not stop a locally running server; define and test behavior for existing tunnels during outage.

### Wi-Fi and mobile data
- [ ] Monitor Android networks using ConnectivityManager/NetworkCallback.
- [ ] Measure route quality: RTT, jitter, packet loss, upload capacity, and reachability.
- [ ] Prefer stable Wi-Fi by default when healthy, but allow a better mobile route according to user settings.
- [ ] Provide a mobile-data warning and optional data cap.
- [ ] Detect Wi-Fi/mobile handovers.
- [ ] Re-establish tunnel and update endpoint after IP/NAT changes.
- [ ] Do not promise seamless session migration where the Minecraft transport cannot preserve a connection.
- [ ] Do not claim bandwidth bonding unless a multipath transport and server-side support are implemented and tested.

### Relay rollout
- [ ] Start with one self-operated test relay and measure it.
- [ ] Add regions only when real user geography and measurements justify them (candidate test regions: Mumbai, Singapore, Frankfurt, US East).
- [ ] Select relay using measured end-to-end path quality and capacity, not geography alone.
- [ ] Track relay bandwidth cost per active server/player-hour.
- [ ] Add redundancy and operational runbooks before public scale.
- [ ] Keep Playit as an optional development/test adapter only; do not make it a production requirement.

## 6. Workstream D — Performance and diagnostics

- [ ] Track network RTT (median and p95), jitter, packet loss, throughput, and tunnel reconnects.
- [ ] Track Minecraft TPS and MSPT independently from network latency.
- [ ] Track CPU, process memory, available memory, storage, battery, and thermal status.
- [ ] Add protocol-correct local and remote health probes.
- [ ] Never equate an open TCP port with Bedrock UDP reachability.
- [ ] Optimize only after profiling on representative physical devices.
- [ ] Provide hardware-aware RAM and player-count recommendations.
- [ ] Detect thermal throttling and recommend safe settings rather than promising fixed capacity.
- [ ] Benchmark Wi-Fi, mobile data, LAN, direct public paths, and relayed paths.
- [ ] Report honest diagnostics; never fabricate ping, relay region, player counts, or uptime.

Initial engineering targets (not user-facing guarantees): stable 20 TPS under a declared test load; MSPT below 50 ms for that load; packet loss below 0.5% on a healthy test path; and relay overhead measured against a direct baseline. Define test devices, Minecraft version, player count, plugins, duration, and network conditions for every result.

## 7. Workstream E — Worlds, backups, and ownership

- [ ] Keep world data in app-private storage by default.
- [ ] Ensure live backups use the engine's safe-save mechanism and always restore saving afterward.
- [ ] Verify backup checksums before restore.
- [ ] Extract to a temporary directory, validate paths/content, create a safety snapshot, and perform rollback-safe replacement.
- [ ] Refuse restore while a server is starting/running/stopping.
- [ ] Test backup/restore with real Bedrock LevelDB and Java worlds.
- [ ] Support import/export and full server-directory portability.
- [ ] Provide clear storage usage and cleanup controls.
- [ ] Never upload world data unless the user explicitly opts in.

## 8. Workstream F — Security, privacy, and supply chain

- [ ] Create a dependency and licence inventory (SBOM).
- [ ] Record exact versions, source URLs, licence texts, and redistribution permissions for every engine/runtime/native binary.
- [ ] Pin release versions; do not blindly install latest in production.
- [ ] Verify signed releases or trusted published SHA-256 checksums; fail closed if the integrity policy requires a checksum and none is available.
- [ ] Use atomic downloads and rollback.
- [ ] Store secrets using Android Keystore-backed encryption where feasible; never log secrets.
- [ ] Authenticate host, relay, and control-plane requests.
- [ ] Apply least privilege, input validation, rate limits, and abuse protection.
- [ ] Redact tokens and sensitive process output.
- [ ] Add a privacy policy and data-retention statement.
- [ ] Review Google Play policy on dynamically downloaded executable code and interpreters before choosing runtime distribution.
- [ ] Review Minecraft/Mojang branding and commercial-use guidelines; do not imply affiliation or redistribute game assets.

## 9. Workstream G — Product experience

- [ ] Preserve the current visual direction; do not redesign without a separate product decision.
- [ ] Make setup explain engine/runtime requirements and device compatibility.
- [ ] Show separate server, local-network, public-route, relay, and player states.
- [ ] Display LAN address and public address only after real verification.
- [ ] Explain whether a connection is direct or relayed.
- [ ] Show network quality and server performance as separate metrics.
- [ ] Provide live console, player administration, file/configuration management, and diagnostics only when backed by real server state.
- [ ] Make world export and migration straightforward.
- [ ] Make joining effortless for guests: guests use their normal Minecraft client and do not need MineHost installed.

## 10. Workstream H — Sustainable operations and business

- [ ] Keep local hosting and LAN multiplayer free.
- [ ] Keep direct public connectivity free where it incurs no MineHost relay cost.
- [ ] Define transparent fair-use limits for MineHost-relayed traffic.
- [ ] Model bandwidth, relay compute, transit, monitoring, domain, and support costs.
- [ ] Consider optional donations, sponsorships, paid backup storage, or premium management features; never charge users merely to use their own phone's CPU.
- [ ] Publish uptime/incident status and support documentation before scaling.
- [ ] Recruit a small beta cohort and measure activation, successful sessions, retention, and willingness to pay before investing in a large relay fleet.

## 11. Required verification levels

Every feature must carry one of these labels:

1. **SOURCE IMPLEMENTED** — code exists; not proof it works.
2. **UNIT TESTED** — deterministic logic tests pass.
3. **ANDROID BUILD VERIFIED** — clean Gradle build and lint pass in CI.
4. **EMULATOR VERIFIED** — Android lifecycle/UI behavior verified where meaningful.
5. **PHYSICAL DEVICE VERIFIED** — tested on real ARM64 hardware.
6. **REAL MINECRAFT CLIENT VERIFIED** — a real supported client joined and gameplay was exercised.
7. **EXTERNAL NETWORK VERIFIED** — joined from a different network and, for international claims, a different country.
8. **SUSTAINED LOAD VERIFIED** — long-duration tests with documented device, thermal, player, and network conditions.

Never promote a feature to a higher level without evidence.

## 12. Release gates — all must pass

- [ ] Clean release build with current supported target SDK.
- [ ] No debug signing in release; production key handled securely outside source control.
- [ ] Dependency/licence/SBOM review complete.
- [ ] Google Play and Minecraft branding compliance reviewed.
- [ ] At least one Bedrock engine launches on a supported physical Android device.
- [ ] At least one real Bedrock client joins locally and plays.
- [ ] At least one real Java engine launches on a supported device before Java is advertised.
- [ ] Cross-play is enabled only after real Java + Geyser + Bedrock testing.
- [ ] Public connectivity works behind CGNAT through MineHost-operated relay infrastructure.
- [ ] Wi-Fi and mobile data scenarios tested, including network handover and recovery.
- [ ] International external join tested from at least two independent regions.
- [ ] Sustained screen-off and thermal testing completed.
- [ ] World persistence, backup, restore, and export verified on real worlds.
- [ ] Crash recovery and honest state reconciliation verified.
- [ ] Privacy, security, support, incident, and backup documentation published.

## 13. Immediate next actions

1. Reconcile source and documentation so no page claims production readiness or working features that are blocked.
2. Get a reproducible Android CI build and tests running on every pull request.
3. Resolve the PHP runtime feasibility blocker with a technical spike and physical-device proof before adding more Bedrock features.
4. In parallel, research Java-on-Android feasibility; do not advertise Paper or cross-play until it runs.
5. Validate the existing Playit adapter only as a temporary diagnostic baseline; do not invest in it as the long-term architecture.
6. Specify and prototype MineHost's own Java TCP and Bedrock transport relay separately.
7. Complete the first real local Bedrock join and world-persistence test.
8. Complete the first external CGNAT join through a MineHost-operated relay.
9. Measure latency, packet loss, TPS/MSPT, thermal behavior, and battery under sustained load.
10. Expand engines, relay regions, and premium features only after the core proof works.

## 14. Decision log

- **Hosting model:** the phone runs the game server; MineHost cloud services do not host or simulate the Minecraft world.
- **Networks:** support Wi-Fi and mobile data; choose based on measured quality and user preference.
- **Connectivity:** direct-first, MineHost-operated relay fallback; Java TCP and Bedrock transports handled separately.
- **Third-party tunnel providers:** not a permanent production dependency.
- **Server engines:** select based on current maintenance, Android runtime feasibility, protocol support, and licence—not brand preference.
- **UI:** preserve existing UI unless the user explicitly changes direction or a usability defect requires a scoped fix.
- **Release policy:** no feature is marketed as working until it reaches the appropriate verification level above.
