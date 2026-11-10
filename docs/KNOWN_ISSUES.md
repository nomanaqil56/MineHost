# MineHost — Known Issues and Release Blockers

This document is a concise index of unresolved issues. The detailed source audit and evidence are maintained in [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md); implementation priorities are in [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md).

## Critical blockers

1. **No proven Android PHP runtime.** PocketMine-MP cannot start until a compatible ARM64 PHP runtime is available and proven on a physical Android device. A normal desktop Linux binary or an executable downloaded into app-private storage is not an acceptable assumption.
2. **No proven Android JVM.** PaperMC and Geyser cannot run until an Android-compatible ARM64 JVM strategy is implemented and validated.
3. **No real server acceptance test.** No current audit evidence demonstrates a real Bedrock or Java server starting and accepting a real Minecraft client on physical hardware.
4. **No MineHost-owned public networking.** Legacy Playit provider source remains unbound. MineHost control plane, direct-connect negotiation, and MineHost-operated TCP/UDP relays are not implemented.
5. **No verified international join.** Public endpoint parsing or a tunnel process running does not prove a remote Minecraft client can join and play.
6. **Cross-play is unavailable.** Geyser/Floodgate execution is blocked by the Java runtime and must remain disabled until real cross-play tests pass.
7. **Release readiness is unproven.** Current release signing, target SDK, foreground-service policy, privacy, licensing, security, and Play distribution checks still require a complete release audit.

## Platform and reliability risks

- Android OEM battery management and low-memory killing can terminate a server process.
- Sustained server workloads can cause thermal throttling and battery wear.
- Cellular networks commonly use CGNAT and may block inbound traffic.
- Network changes can invalidate tunnel mappings and active client sessions.
- Java TCP and Bedrock transports require different forwarding and reachability checks.
- A relay improves reachability, not physical distance; it cannot guarantee low ping worldwide.
- Relay bandwidth and transit costs must be measured before promising unlimited free relaying.

## Policy and supply-chain risks

- Android restricts execution of dynamically downloaded native binaries; runtime distribution must be designed around current Android platform and Google Play rules.
- Every bundled/downloaded runtime and server component needs version pinning, checksum/signature verification, licence review, and attribution notices.
- Minecraft branding and distribution must follow current Mojang/Microsoft guidelines.
- Do not silently accept the Minecraft EULA or redistribute Minecraft game assets.

## Known-good claims policy

Do not describe a feature as working based only on a UI screen, configuration file, interface, or mocked test. Use the verification levels in the master roadmap and keep test evidence current.
