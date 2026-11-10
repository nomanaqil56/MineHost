# MineHost Networking Architecture

> **Target architecture, not a claim of current functionality.** The MineHost-owned global network described here is not implemented yet. Current code contains an experimental Playit provider; it must not be mistaken for MineHost-owned connectivity.

## 1. Goals

- Host the actual Minecraft server on the user's Android phone.
- Support Wi-Fi and mobile data.
- Allow guests to join with their normal Minecraft client; guests do not need MineHost.
- Prefer LAN or direct public paths to minimize additional latency.
- Use MineHost-operated relays only when direct reachability is unavailable.
- Avoid permanent dependence on a third-party managed tunnel provider.
- Continue local hosting if MineHost's control plane is temporarily unavailable.
- Report server health and network health separately.

Zero latency is impossible. Physical distance, ISP routing, congestion, radio quality, packet loss, host upload capacity, phone CPU, thermal throttling, and Minecraft tick time all affect the experience.

## 2. Separate control plane and data plane

### Control plane

A small MineHost-operated service handles:
- Host/device registration and authentication.
- Session negotiation and connection metadata.
- Relay allocation and endpoint assignment.
- Health/telemetry ingestion and abuse controls.

Gameplay packets must not be proxied through application-level control-plane APIs. A control-plane outage must not stop a locally running server.

### Data plane

The gameplay path is one of:

1. **LAN direct:** guest and host share a reachable local network. No MineHost relay.
2. **Public direct:** host is reachable through public IPv6 or an explicitly configured public route. No relay.
3. **NAT traversal:** attempt direct UDP connectivity only for transports that support the required behavior and have been verified.
4. **MineHost relay:** route through a MineHost-operated regional relay if direct reachability fails.

Do not route traffic through a relay just because one is available. Measure both paths and prefer the healthy lower-latency route.

## 3. Protocol-specific handling

Java and Bedrock are not interchangeable network workloads.

### Java Edition

- Minecraft gameplay uses TCP.
- Implement a TCP reverse tunnel / stream proxy for relayed access.
- Direct IPv6 or explicit port mapping can avoid relay.
- Do not assume UDP hole punching or a generic WebRTC data channel solves Java TCP traversal.
- Preserve byte-stream ordering and avoid unnecessary buffering.

### Bedrock Edition

- Bedrock transport is version-dependent and is undergoing a transition from legacy RakNet to NetherNet.
- The exact selected server engine and Minecraft client versions determine the supported transport.
- Implement protocol-aware TCP signaling and UDP gameplay forwarding as required by that version.
- Preserve datagram boundaries and source/destination semantics where required.
- Never use a TCP connect as proof of UDP Bedrock reachability.
- Do not claim a Bedrock engine is compatible until its current protocol support is verified with real clients.

The transport implementation must be selected from current official Minecraft/server-engine documentation at implementation time. Do not hard-code assumptions from older RakNet-only deployments.

## 4. Direct connectivity and relay fallback

For UDP-capable paths, investigate STUN and ICE-style candidate discovery and connectivity checks. A relay fallback can be based on TURN concepts or a carefully audited protocol-specific relay.

Important constraints:
- ICE is a framework, not a magic adapter for every Minecraft transport.
- TCP traversal is different from UDP traversal and is not reliably solved by generic UDP hole punching.
- Some carrier-grade NATs and symmetric NATs prevent direct paths.
- A successful control tunnel does not prove that the public Minecraft gameplay endpoint is reachable.
- Every path must be tested with a real client.

For Java, build and test a TCP forwarding path. For Bedrock, build and test the required transport-specific forwarding path. Share common lifecycle, authentication, metrics, and provider abstractions where appropriate, not an incorrect one-size-fits-all packet implementation.

## 5. MineHost-owned relay fleet

Initial development should use one self-operated relay node. Candidate future test regions include Mumbai, Singapore, Frankfurt, and US East; deploy based on actual user geography, provider quality, and measured paths.

Relay selection should consider:
- Player-to-relay RTT.
- Relay-to-host tunnel RTT.
- Jitter and packet loss on both legs.
- Relay CPU, bandwidth, and active-session capacity.
- Route stability and cost.

Geographic distance alone is not a sufficient selection algorithm. A relay cannot remove the physical distance between the host phone and the player. It can only provide a better route or solve reachability.

Production relay requirements:
- Authenticated host tunnels.
- Encryption in transit.
- TCP and/or UDP forwarding as required by the engine/version.
- Per-host and per-session quotas.
- Abuse prevention and rate limiting.
- Capacity metrics and health checks.
- Redundant control-plane coordination.
- Incident and emergency shutdown procedures.
- Transparent fair-use policy and bandwidth cost monitoring.

MineHost may initially rent VPS/colocation capacity while operating its own relay software. This is compatible with product independence. The project cannot eliminate dependence on upstream internet transit, power, DNS registrars, or data-centre connectivity.

## 6. Wi-Fi and mobile data

Use Android ConnectivityManager and NetworkCallback to observe available networks and network changes.

- Support Wi-Fi and cellular data.
- Measure reachability, RTT, jitter, packet loss, and upload capacity.
- Prefer Wi-Fi by default when healthy, while allowing user preference and a better measured mobile path.
- Warn before cellular data use and optionally enforce user-configured data limits.
- On network handover, establish a new tunnel, verify it, and update the endpoint.
- Existing TCP sessions generally cannot migrate across a changed IP/NAT mapping; clients may need to reconnect.
- Do not claim seamless handover unless the actual protocol and implementation preserve sessions.
- Do not claim Wi-Fi + mobile data bonding unless a supported multipath transport has been implemented and tested.

## 7. Connection state model

Keep independent facts for:
- Minecraft server process state.
- Local protocol readiness.
- Local LAN reachability.
- Control-plane registration.
- Tunnel-agent process state.
- Tunnel authentication state.
- Relay allocation state.
- Public endpoint reachability.
- Guest/player session state.

A tunnel agent printing an address is not sufficient proof that the Minecraft server is reachable. A local port being open is not sufficient proof of Bedrock UDP readiness. Show connected/reachable only after protocol-appropriate checks.

## 8. Metrics

Collect and display separate network and server metrics:
- RTT median and p95.
- Jitter.
- Packet loss.
- Upload/download throughput.
- Tunnel reconnect count and uptime.
- Server TPS and MSPT.
- CPU and memory usage.
- Thermal status and throttling.
- Battery and storage pressure.

Never infer a remote player's ping from the relay's ping alone. Use real session-level measurements where possible and label estimates clearly.

## 9. Development sequence

1. Verify local LAN connectivity for one real engine and real client.
2. Validate Wi-Fi and mobile-data network detection and route recovery.
3. Build one self-operated relay proof of concept.
4. Prove external Java TCP or Bedrock transport connectivity behind CGNAT, using a real client.
5. Measure direct versus relayed latency, packet loss, and throughput.
6. Build MineHost control-plane registration and authenticated relay allocation.
7. Add direct-connect/NAT traversal only for protocols where it is proven useful.
8. Add regional relays based on measured need and sustainable operating cost.
9. Perform international and sustained-load testing.

## 10. Current status

- Local network discovery/diagnostic groundwork: source exists; physical-device validation pending.
- Playit provider: experimental third-party adapter; real external join not verified.
- MineHost control plane: not implemented.
- MineHost-owned relay: not implemented.
- Direct public connectivity/NAT traversal: not implemented.
- International connectivity: not verified.
- Version-aware Bedrock transport: research and engine compatibility validation required.

See [MineHost Master Roadmap](MINEHOST_MASTER_ROADMAP.md) and [Production Readiness](PRODUCTION_READINESS.md).
