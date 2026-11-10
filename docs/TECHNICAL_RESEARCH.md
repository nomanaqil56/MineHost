# MineHost — Technical Research and Feasibility

**Reviewed:** 2026-11-10  
**Purpose:** Evidence-based evaluation of Android server runtimes, current Bedrock transport, cross-play and independent connectivity. This replaces earlier assumptions that PocketMine-MP has a ready-made Android ARM64 runtime and that Bedrock always uses RakNet/UDP.

## 1. Executive summary

MineHost's product model is feasible in principle: run the actual game server on the Android device, keep worlds local, and operate a MineHost control plane/relay only to make the phone reachable. However, the current repository does not yet prove the critical Android runtime or networking paths.

The most important new finding is that the Bedrock networking model changed in late October 2026. Bedrock 26.60 clients use NetherNet rather than RakNet. NetherNet is WebRTC data-channel transport with HTTP signaling and ICE. A generic UDP tunnel or RakNet ping is not sufficient for current Bedrock compatibility.

The original PocketMine-MP project and its PHP-Binaries repository are archived. VanillaMine is a continuation and supports Bedrock 1.26.60, but its current published PHP binaries are listed for x86-64 Linux and Windows x64, not ARM. Altay is another current Bedrock candidate; its published release page advertises 1.26.60, but ARM64 Android runtime and NetherNet compatibility still need verification.

**Recommendation:** Do not select or advertise a Bedrock engine yet. Run a focused runtime + NetherNet proof-of-concept before changing the engine downloader. For Java, investigate a mobile OpenJDK 17/21 distribution, but prove it can run headless Paper/Allay as a server process rather than assuming a Minecraft client runtime is automatically suitable.

## 2. Current Bedrock transport — critical update

### Mojang transport migration

Mojang's Bedrock server guidance states that a networking change began rolling out on 2026-10-29. Bedrock 26.60+ requires NetherNet; server hosts must support TCP signaling and UDP gameplay on the configured port. The official BDS guide says official binaries are available for 64-bit Windows and Ubuntu Linux and are not available for ARM-based devices.

Sources:
- [Mojang: Set up a Bedrock Dedicated Server](https://help.minecraft.net/hc/en-us/articles/49914677714957)
- [Mojang: NetherNet HTTP Signaling Partner Onboarding Guide](https://mojang.github.io/bedrock-protocol-docs/guides/nether-net-onboarding-guide/)

### NetherNet characteristics

The Mojang onboarding guide describes:
- WebRTC peer connections with DTLS/SCTP data channels.
- HTTP signaling: `GET /v1/join` capability/metadata and `POST /v1/join/{networkId}` with SDP offer; response is SDP answer.
- ICE candidate gathering and connectivity checks.
- Full ICE (trickle ICE disabled): candidates are gathered before the SDP exchange.
- Client-side RTC configuration in the guide has no STUN/TURN URLs configured; the client gathers host candidates only.
- Identity assertions in SDP: client identity validation and a long-lived server/operator identity with a public-key fingerprint, which the client uses for trust-on-first-use where applicable.
- iOS clients require HTTPS signaling with a trusted certificate; plain HTTP is insufficient for iOS.

The guide warns that configuring STUN/TURN can increase connection setup time because all ICE gathering completes before the SDP is sent. This does **not** mean NAT traversal is solved for every mobile network. MineHost must test how real clients behave behind CGNAT and restrictive NATs.

Source: [Mojang NetherNet onboarding guide](https://mojang.github.io/bedrock-protocol-docs/guides/nether-net-onboarding-guide/).

### Implication for MineHost networking

The existing Bedrock RakNet UDP readiness probe is not a universal current readiness check. For 26.60+:
- The engine must expose the NetherNet HTTP signaling endpoint.
- MineHost must validate a correct capability response and a successful WebRTC session, not just an open TCP/UDP port.
- The server needs both TCP signaling and UDP gameplay paths.
- A public relay for NetherNet may need to proxy signaling/SDP and provide a reachable ICE candidate or terminate/forward the WebRTC data channel. A generic TCP reverse proxy or UDP-only tunnel is not enough.
- The relay must preserve the protocol's security/identity requirements and handle the client's actual ICE behaviour.
- Older RakNet-only clients can be supported only if the chosen engine and current client ecosystem still allow that compatibility path.

Do not design NetherNet fallback by assuming that a conventional TURN server will work: the official client configuration documented by Mojang does not configure TURN URLs. Prove the actual client/host path experimentally.

## 3. Bedrock engine candidates

### PocketMine-MP (original upstream)
- The original PMMP project was archived in July 2026; its final upstream release is reported as 5.44.3 for Bedrock 1.26.30.
- The `pmmp/PHP-Binaries` repository is archived (2026-08-08).
- This makes the original upstream a poor long-term dependency for a new product unless using a maintained fork.

Sources:
- [PocketMine-MP documentation](https://doc.pmmp.io/en/rtfd/)
- [Archived PMMP PHP-Binaries repository](https://github.com/pmmp/PHP-Binaries)

### VanillaMine — strongest current PHP candidate to investigate
- Describes itself as the continuation of PocketMine-MP 5.
- Current website advertises VanillaMine 5.48.4 for Bedrock 1.26.60, PHP 8.4, LGPL-3.0, released 2026-10-31.
- Includes vanilla gameplay improvements and NetherNet support via a `pm-nethernet` helper.
- Current install docs list published PHP builds for Linux x86-64 and Windows x64; they explicitly say ARM builds are not yet available.
- NetherNet requires TCP signaling and UDP gameplay on the server port; iOS additionally needs HTTPS signaling.
- `pm-nethernet` has published Linux ARM64 binaries, but Android/Bionic compatibility and an Android/arm64 build must be tested.

Sources:
- [VanillaMine](https://vanillamine.com/)
- [VanillaMine install requirements](https://docs.vanillamine.com/server/install)
- [VanillaMine network documentation](https://docs.vanillamine.com/server/network)
- [pm-nethernet Go package](https://pkg.go.dev/gitlab.com/VanillaPocketMine/nethernet)

**Assessment:** Excellent protocol/gameplay candidate, but not a drop-in Android engine today. Requires a proven ARM64 PHP 8.4 build with required extensions and a working Android `pm-nethernet` helper. Do not download the x86-64 build onto Android.

### Altay — second current PHP candidate
- Official downloads page advertises Altay 5.45.1 for Bedrock 1.26.60 (2026-10-31).
- Provides its own PHP binaries because the server needs non-standard extensions.
- The public download information reviewed does not establish a production-ready Android ARM64 package or confirm the exact NetherNet/Android path.

Sources:
- [Altay downloads](https://getaltay.org/downloads)
- [Altay project](https://getaltay.org/)

**Assessment:** Evaluate its build system, licence, protocol implementation and ARM64 support. Do not assume the Linux/Windows binaries run on Android.

### Allay — Java-based Bedrock candidate
- Allay is an active Java 21 Bedrock server project with a shaded JAR, multi-version goal and plugin API.
- It could share the Android Java runtime infrastructure needed by Paper.
- Current public README confirms Java 21, but does not by itself prove NetherNet 26.60 support or Android device performance.

Source: [Allay GitHub](https://github.com/AllayMC/Allay)

**Assessment:** Evaluate as a possible Bedrock engine only after testing current client transport and feature parity. Do not treat its “fast” positioning as an independently verified MineHost performance result.

### Official Bedrock Dedicated Server
Mojang currently documents Windows x64 and Ubuntu Linux x64 builds, not ARM64. It is not a native Android ARM64 option. Heavy x86 emulation is not an acceptable default for MineHost's performance-first product.

Source: [Mojang Bedrock server setup](https://help.minecraft.net/hc/en-us/articles/49914677714957).

## 4. Android PHP runtime candidates

### AndroidPHP community build
A small public project provides an ARM64 PHP binary for PocketMine and documents running it inside Termux. This is useful as a feasibility lead, but it is not a production-ready MineHost runtime:
- Requires Termux in its documented installation.
- Does not prove a self-contained APK integration.
- Needs supply-chain, source/build provenance, extensions, licence and reproducibility review.
- The app must handle Android executable restrictions correctly.

Source: [AndroidPHP](https://github.com/ItzxDwi/AndroidPHP).

### Build our own ARM64 PHP runtime
Preferred long-term path if VanillaMine/Altay is selected:
- Pin PHP source, extensions and build toolchain.
- Build for Android/Bionic ARM64 or a deliberately chosen compatible userspace.
- Include the exact server-required extensions (for VanillaMine, its own docs describe a custom PHP build and extensions such as threading, LevelDB and chunk utilities).
- Sign/verify artifacts and publish a reproducible build manifest.
- Test launch, extension list, PHAR execution, thread creation, LevelDB world creation and sustained load on physical Android.

The archived PMMP PHP-Binaries scripts are useful historical reference but should not be the only long-term build source. Prefer the maintained engine's own supported build scripts or a MineHost-maintained reproducible fork.

### Android executable restrictions
Android 10+ restricts execution from writable app-data directories. Setting the executable bit alone is not sufficient evidence. Possible strategies include:
- Package a compatible Android PIE executable in the APK's native library directory (the file's ELF type and entry point must be correct; a shared library is not automatically a CLI program).
- Use the Android dynamic linker to launch a Bionic-compatible executable from app storage, after device validation.
- Use a PRoot-based Linux userspace for Linux-targeted binaries, accepting and benchmarking the overhead.

A useful reference implementation is [DroidNative's documented execution model](https://github.com/DroidNative/DroidNativeApp/blob/main/docs/architecture.md). MineHost must choose and validate one strategy; do not mix incompatible Bionic, musl and glibc binaries.

## 5. Java runtime candidates

### PojavLauncher mobile OpenJDK
PojavLauncher documents mobile OpenJDK ports for ARM64, including Java 17 and Java 21. This is a promising candidate source for experimentation, not proof that its runtime can run headless server processes without modification.

Source: [PojavLauncher OpenJDK branch](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk).

### Required validation
- Run a simple headless Java program from the packaged runtime.
- Run Paper/Allay JAR with no desktop/graphics dependencies.
- Verify native libraries, modules, TLS, DNS, file I/O, thread scheduling and signal handling.
- Measure memory/GC/tick performance on physical Android.
- Verify runtime licence and redistribution conditions.
- Confirm the exact `bin/java` launcher and library layout; `libjava.so` alone is not a JVM distribution.

## 6. Java + Bedrock cross-play

Geyser supports NetherNet:
- Geyser build 1237+ supports NetherNet.
- Geyser docs instruct setting `bedrock.transport: nethernet`.
- Geyser build 1276 (2026-10-28) advertises Bedrock 26.40–26.63 and Java 26.3 support.
- Geyser requires TCP signaling and UDP gameplay on its Bedrock port; the Bedrock port cannot collide with the Paper Java port.
- Floodgate remains an optional authentication bridge depending on desired account model.

Sources:
- [Geyser downloads](https://geysermc.org/download/)
- [Geyser NetherNet migration guide](https://geysermc.org/blog/preparing-for-nethernet/)
- [Geyser setup](https://geysermc.org/wiki/geyser/setup/)

Cross-play still requires a working Java runtime, Java server, Geyser, correct port allocation, secure authentication and real Java/Bedrock client tests. A config generator is not a working bridge.

## 7. MineHost-owned connectivity design

### Java
Java gameplay uses TCP. Use a MineHost-owned authenticated TCP reverse tunnel/proxy when direct inbound reachability is unavailable. Direct TCP NAT traversal is not guaranteed.

### Bedrock 26.60+
NetherNet is WebRTC data-channel transport. The public MineHost endpoint must support:
- HTTPS/HTTP capability and SDP signaling endpoints.
- Per-session signaling and peer lifecycle.
- Server identity keypair and valid identity assertions.
- A reachable ICE candidate for the client.
- A transport path from the relay to the Android host.
- TCP signaling and UDP/WebRTC gameplay.
- HTTPS certificates for iOS clients.

A conventional UDP port forward is not sufficient when the host is behind CGNAT. A MineHost relay may need to act as a WebRTC endpoint or implement a protocol-aware UDP/ICE forwarding mechanism. The exact architecture must be proven with the Mojang client before implementation is considered complete.

### Direct-first strategy
- LAN first.
- Public IPv6/direct endpoint when available.
- Attempt protocol-compatible direct connectivity.
- MineHost relay fallback when direct connectivity fails.
- Do not force all traffic through a relay.
- Select relay by measured end-to-end route quality, not geographic distance alone.

### Sustainability
Relays incur bandwidth and egress costs. The free product should maximize direct paths, apply transparent fair-use limits to relayed traffic, and have a sustainable funding plan. Do not promise unlimited free relaying.

## 8. Android background and device constraints

- Use a correctly declared foreground service and comply with current Android/Play policy for the selected service type.
- Keep a partial wake lock only while hosting needs it; release it on every terminal path.
- Monitor thermal state, memory pressure, storage and network changes.
- Provide screen-off/OEM battery guidance; no app can guarantee every vendor keeps a process alive.
- Test charging and battery operation; avoid unsafe thermal/battery behaviour.
- Provide graceful shutdown and world-save protection.

## 9. Recommended proof-of-concept sequence

1. **Runtime feasibility:** launch an Android ARM64 PHP or Java runtime from MineHost without requiring a separate Termux install.
2. **Local server:** run one chosen Bedrock engine on a physical device and join from another device on the same Wi-Fi.
3. **Current transport:** prove Bedrock 26.60+ NetherNet signaling and gameplay locally.
4. **CGNAT:** test from cellular and a remote ISP; record the actual ICE candidates and connection outcome.
5. **MineHost relay:** build a minimal self-operated signaling/data-plane prototype and prove a remote Bedrock client can join a phone behind CGNAT.
6. **Java:** prove Android JVM + Paper with a real Java client.
7. **Cross-play:** prove Geyser 26.60+ + Floodgate with real clients.
8. **Performance:** compare direct vs relay RTT/jitter/loss and server TPS/MSPT under sustained load.

## 10. Decision log

| Decision | Current position |
|---|---|
| Game process runs on Android | Fixed product requirement |
| Local worlds remain on device | Fixed product requirement |
| Wi-Fi and mobile data | Both required |
| Direct-first networking | Target architecture |
| MineHost-operated relay/control plane | Target architecture; not implemented |
| Playit | Optional temporary test adapter only; not final dependency |
| PocketMine-MP upstream | Do not use as long-term upstream; archived |
| VanillaMine | Leading PHP Bedrock candidate, but ARM64 runtime blocker |
| Altay | Secondary PHP Bedrock candidate; ARM64/NetherNet need validation |
| Allay | Java 21 Bedrock candidate; NetherNet/Android need validation |
| Paper | Java candidate; Android JVM not verified |
| Geyser/Floodgate | Cross-play candidate; underlying runtime/transport must pass |
| PRoot | Optional fallback, not default performance path |
