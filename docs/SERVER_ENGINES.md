# MineHost — Server Engine Evaluation & Compatibility

> **Status: candidates under evaluation; no engine is production-supported until it passes the physical-device and real-client gates.** See [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).

## 1. Bedrock: Axolotl-PM is the primary candidate

### Why Axolotl-PM instead of the original PMMP?
The original `pmmp/PocketMine-MP` project announced end of support and was archived in July 2026. Its final release is not the right long-term dependency for a product that promises current Bedrock compatibility.

[Axolotl-PM](https://github.com/axolotl-pm/PocketMine-MP) is the maintained continuation/fork. Its current 5.51.1 release (2026-11-02) targets Bedrock 1.26.60 / v26.60. Axolotl-PM supports NetherNet, the WebRTC-based transport required by current Bedrock clients; RakNet is deprecated and removed for 1.26.60.

### Runtime requirements
Axolotl-PM requires PHP 8.1+ (64-bit) and several extensions. Current NetherNet specifically requires `axolotl-pm/ext-webrtc`, which uses libdatachannel.

The maintained [Axolotl-PM PHP-Binaries](https://github.com/axolotl-pm/PHP-Binaries) project publishes Android ARM64 PHP builds. Its PHP 8.4 build workflow includes the `ext-webrtc` extension and an Android static-link check. MineHost's build script now pins the Android ARM64 PHP 8.4 asset by SHA-256 and packages the static executable as `jniLibs/arm64-v8a/libphp.so`.

**Not yet verified:** the APK has not yet been confirmed to run this executable on a physical Android device; the PHP module check, PocketMine PHAR launch, NetherNet signalling, UDP ICE channel, and real client join still need device testing. Full dependency licence review is also required before release.

### NetherNet configuration
MineHost generates:
- `transport.name: nethernet`
- persistent `nethernet.key`
- a fixed UDP `port-range` sized from the configured player limit
- built-in HTTP signalling on `server-port`
- `ice-udp-mux: false` because Axolotl-PM documents that UDP mux cannot be used with TURN

NetherNet uses TCP HTTP(S) signalling (`GET /v1/join`, `POST /v1/join/{networkId}`) and UDP WebRTC gameplay (ICE/DTLS/SCTP). A successful `GET /v1/join` probe only proves signalling readiness; it does not prove the UDP gameplay path is reachable. A real client join is mandatory.

For public hosting, MineHost must terminate HTTPS at its own edge/reverse proxy and forward signalling through the phone's outbound tunnel. MineHost-operated TURN provides relay fallback when direct ICE connectivity fails. Direct ICE paths should be preferred when they work.

### Important compatibility note
Axolotl-PM is not a vanilla Bedrock server replacement. It has known gameplay-parity differences (for example, some vanilla world-generation, redstone, and mob-AI behaviour). The app and store listing must describe this honestly. If MineHost later needs a vanilla-like Bedrock experience, evaluate an additional server engine rather than claiming Axolotl-PM provides parity.

## 2. Other Bedrock candidates

### PowerNukkitX
- Java-based Bedrock server implementation.
- Potential alternative if its feature set or gameplay parity better matches a target use case.
- Requires a compatible JVM on Android ARM64, which is not yet validated.
- Must be tested for current NetherNet transport support before being considered.

### Official Bedrock Dedicated Server
- Check Mojang's current release artifacts and supported architectures.
- Do not assume an official ARM64 Android build exists.
- x86 emulation is not an acceptable primary path for a performance-first phone host unless measured evidence justifies it.

## 3. Java: Paper

- Paper is the initial Java server candidate.
- It requires a full compatible JVM; Android ART does not execute desktop Java server JARs.
- MineHost currently has no validated Android JVM distribution or install path.
- Feasibility checks include Bionic/dynamic-linker compatibility, native libraries, Java version, JIT, memory, filesystem, process signals, and sustained thermal performance.
- Pin server/JVM versions; do not follow a moving "latest" release without compatibility testing.
- Obtain explicit user EULA consent; never accept on behalf of the user.

## 4. Cross-play: Geyser and Floodgate

Geyser translates Bedrock client traffic to a Java server. Floodgate supports a Bedrock identity/authentication workflow with Geyser where appropriate. They are not independent server engines.

Target composition:
- Paper runs locally on Android.
- Geyser runs as a plugin or supported bridge connected to Paper.
- Geyser's Bedrock transport must be configured for NetherNet on current Bedrock clients (the current Geyser documentation describes NetherNet support for 26.50+).
- Floodgate is added only for the selected authentication mode and secured properly.

This is blocked until the Android JVM and Paper work on-device. Cross-play is not supported until real Java and Bedrock clients join the same phone-hosted world.

## 5. Runtime and packaging policy

- Android ART is not a desktop OpenJDK runtime.
- PHP/Java runtimes must target the Android ABI and compatible userspace/dynamic linker.
- Do not assume a downloaded ELF in app-writable storage can execute on modern Android.
- MineHost's PHP bootstrap packages the Android static executable via `jniLibs`; the app verifies it can run and checks for the `webrtc` PHP module before starting Axolotl-PM.
- Pin every runtime and engine version, source, ABI, licence, dependency set, and trusted digest/signature.
- Review all bundled PHP and native dependency licences before public distribution.

## 6. Engine support policy

A server engine may be labelled:
- **Candidate:** under research only.
- **Implemented:** integration code exists, but runtime/client proof is incomplete.
- **Device-verified:** launches and answers protocol probes on listed physical devices.
- **Client-verified:** real Minecraft clients join and play on LAN.
- **Externally verified:** real clients join over public internet through MineHost connectivity.
- **Supported:** tests pass for a documented version/device matrix with a maintained update path.

Never label a candidate or code-only integration as supported.
