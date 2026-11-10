# MineHost — Networking Architecture

**Status:** Partly implemented. This document distinguishes current source from the target architecture. The target is MineHost-operated connectivity; the current app still contains a Playit-specific provider and has no MineHost relay service.

See [MINEHOST_MASTER_ROADMAP.md](../MINEHOST_MASTER_ROADMAP.md) for product principles and release gates.

## 1. Product requirement

The Minecraft server process and world stay on the user's Android device. MineHost's network infrastructure only coordinates connections and forwards packets when a direct path is unavailable. It must never become a hidden cloud game-server host.

Required user experience:
- Host over Wi-Fi or mobile data.
- Same-LAN play works without MineHost cloud services.
- Remote players use ordinary compatible Minecraft clients; they do not need MineHost installed.
- Prefer the shortest viable direct path.
- Fall back to a MineHost-operated relay when direct connectivity is impossible.
- Show truthful local, tunnel and remote reachability states.
- A control-plane outage must not stop a running local server.

## 2. Target connection priority

1. **LAN:** Connect directly to the host's local address. No relay or account required.
2. **Reachable public address:** Use direct IPv6 or public IPv4 only when reachable and safely configured.
3. **NAT traversal:** Attempt direct UDP traversal where the actual protocol and network support it. STUN/ICE concepts are useful, but generic WebRTC ICE is not a drop-in Minecraft transport.
4. **MineHost relay:** Allocate a MineHost-operated relay only if a direct path cannot be established.

Do not force all players through a central relay. A relay adds an extra network leg, processing/queueing and bandwidth cost. It can improve a poor route, but it cannot remove physical distance or a weak host uplink.

## 3. Transport adapters

Java and Bedrock must not be treated as one generic tunnel.

### Java Edition
- Gameplay uses TCP.
- Requires a TCP reverse tunnel/proxy or a separately proven TCP traversal method.
- Preserve ordered byte-stream semantics.
- Measure handshake and end-to-end latency; avoid excessive buffering.
- UDP hole punching alone does not solve Java TCP reachability.

### Bedrock Edition
- Transport is version-dependent. Bedrock 26.60+ uses NetherNet; older versions may use RakNet depending on client/server support.
- NetherNet uses HTTP signaling (GET /v1/join, POST /v1/join/{networkId} with SDP) and WebRTC DTLS/SCTP data channels. Gameplay uses UDP; the signaling endpoint uses TCP/HTTP(S).
- NetherNet server identity assertions and the operator key must be implemented and protected. iOS clients require HTTPS signaling with a trusted certificate.
- The Mojang onboarding guide documents full ICE with trickle ICE disabled and no client STUN/TURN URLs by default. Do not assume generic STUN/TURN or a UDP-only tunnel solves CGNAT. Test the actual client ICE candidates and session flow.
- A MineHost relay must provide a reachable advertised ICE candidate and forward the signaling and gameplay paths independently. The implementation below is the proposed proof-of-concept architecture; it is not yet implemented or client-verified.
- Probe the real signaling and data-channel protocol, not just whether TCP/UDP port 19132 is open. Keep RakNet and NetherNet probes separate.
- A RakNet readiness probe only proves compatibility with a RakNet-speaking endpoint; it must not be treated as a universal current Bedrock probe.

### Java + Bedrock cross-play
Geyser introduces a Bedrock-facing transport and a Java TCP connection to the backend server. Both legs need correct forwarding and real-client testing. A generated Geyser config is not evidence that cross-play works.

## 4. Target architecture

```text
Java / Bedrock client
        |
        +---- LAN / direct public path ----------------------+
        |                                                    |
        +---- direct NAT traversal (where supported) -------+
        |                                                    |
        +---- MineHost regional relay (fallback)             |
                                                             |
                                                     Android host agent
                                                             |
                                                     Local server port
                                                             |
                                                 Minecraft process on phone

MineHost control plane:
  host registration, short-lived credentials, session negotiation,
  relay discovery/allocation, health and abuse controls.

MineHost data plane:
  self-operated TCP/UDP forwarding nodes. No Minecraft simulation,
  world storage or game-server hosting.
```

## 5. Wi-Fi and cellular

Use Android `ConnectivityManager` and `NetworkCallback` to observe available networks and changes.

- Support both Wi-Fi and cellular.
- Choose using validated internet, measured quality, upload capacity, packet loss, jitter, policy and data cost—not a hard-coded Wi-Fi preference.
- Distinguish LAN availability from public internet availability.
- Detect CGNAT and IPv6 conditions where possible.
- On network change, rebind/recreate the host tunnel, verify the new route, update the endpoint and report whether clients must reconnect.
- Do not claim seamless TCP session migration. A change of network often changes NAT mappings and source addresses.
- Do not claim Wi-Fi/cellular bandwidth bonding unless a multipath transport is actually implemented and tested.
- Warn users before high mobile-data usage.

## 6. MineHost relay and control plane (not yet implemented)

The production system should be operated by MineHost.

### Control plane
- Host registration and device identity.
- Short-lived scoped credentials.
- Connection/session negotiation.
- Relay discovery and allocation.
- Endpoint lifecycle and health reporting.
- Rate limits, abuse detection and relay quotas.
- Redundant instances and minimal durable state.
- Must not carry the Minecraft gameplay data path unless explicitly designed for that purpose.

### Data plane
- MineHost-operated TCP and UDP relays.
- Authenticated and encrypted host-to-relay channel.
- Protocol-correct forwarding.
- Backpressure, bounded buffers and resource limits.
- Per-session accounting and abuse controls.
- Health/capacity telemetry.
- Regional nodes added only after measured demand and cost justification.

### Route selection

Select by measured end-to-end quality, not geography alone. Candidate inputs:
- Client-to-relay and relay-to-host RTT.
- End-to-end protocol probe.
- Jitter and packet loss.
- Relay queue depth, CPU and bandwidth.
- Host upload capacity and network type.
- Regional/ISP routing.

A relay close to the player may still be a poor route to the host. Test candidate routes and use measured results.

## 7. Current repository implementation

Present in source:
- `TunnelProvider` abstraction.
- `TunnelManager` and tunnel health model.
- `PlayitTunnelProvider` as a third-party provider adapter.
- `LocalNetwork` discovery.
- `ConnectionMonitor` with TCP Java and UDP Bedrock-style probes.
- Reconnect policy and Playit output parser tests.

Not implemented:
- MineHost control-plane service.
- MineHost relay server or Android relay client.
- Direct public path negotiation.
- Production NAT traversal.
- Region selection or route optimization.
- Independent public address allocation.
- Real international gameplay tests.

The Playit adapter is currently a development/compatibility integration only. It must not be described as MineHost-owned infrastructure or treated as a mandatory final dependency. Its Android executable path and live claim/join flow are unverified.

## 8. Observability

Track separate measurements:
- Local server protocol readiness.
- Tunnel agent process health.
- Authentication/claim state.
- Endpoint assignment.
- Remote protocol reachability.
- End-to-end RTT (median and p95), jitter and packet loss.
- Upload/download throughput and data volume.
- Reconnect attempts and time-to-recovery.
- Server TPS/MSPT separately from network metrics.

A public hostname being assigned does not prove that a player can join. A local port responding does not prove public reachability. A TCP connect does not prove Bedrock UDP gameplay works.

## 9. Network acceptance tests

- [ ] Same-LAN Bedrock client join.
- [ ] Same-LAN Java client join.
- [ ] Direct public IPv6 path where available.
- [ ] Direct public IPv4 path where available.
- [ ] Wi-Fi behind CGNAT.
- [ ] Cellular behind carrier CGNAT.
- [ ] MineHost relay TCP path for Java.
- [ ] MineHost relay Bedrock path for each supported transport/version.
- [ ] Geyser dual-leg path, if cross-play is advertised.
- [ ] Host Wi-Fi-to-cellular network change and recovery.
- [ ] Relay outage and failover.
- [ ] Control-plane outage while local server and LAN play continue.
- [ ] International real-client play from at least two regions.
- [ ] Record p50/p95 RTT, jitter, packet loss, reconnect time, throughput and relay cost.

**No zero-lag or universal-uptime promise:** the network is bounded by distance, ISP routing, radio conditions, host upload speed, relay capacity and the phone's processing/thermal limits.


## 10. Concrete NetherNet relay design (proposed, not implemented)

The current protocol makes a simple port-forwarding model possible, provided the selected engine exposes the right configuration and the client path is tested.

### Bedrock-only server

For a current NetherNet engine such as VanillaMine:
- Allocate a MineHost public Bedrock port `P`.
- Configure the server's UDP gameplay port to local port `P`.
- Configure its signaling TCP listener to a local port `S` (which may equal `P` when the engine supports same-port TCP/UDP).
- Add the MineHost relay public IP address(es) to the engine's advertised-address list so its SDP includes reachable relay candidates.
- Relay public UDP `P` to the host's local UDP `P`.
- Relay public TCP `P` to the host's local signaling TCP `S`; use HTTPS on the public endpoint for iOS clients.

Do not assume the engine's advertised candidate port or signaling URL is correct merely because configuration was written. Inspect the SDP/server metadata and verify with a real 26.60+ client.

### Java + Geyser cross-play

Geyser's current NetherNet configuration uses TCP signaling and UDP gameplay. Its HTTPS guidance documents a reverse-proxy pattern in which:
- Players connect to public Bedrock port `P` over TCP (HTTPS signaling) and UDP (gameplay).
- The proxy forwards TCP signaling to a separate internal Geyser signaling port `S`.
- UDP gameplay continues to the Geyser WebRTC port `P`.
- Geyser trusts the proxy and reads the real client address from `X-Forwarded-For`.

MineHost should implement the same mapping across its relay tunnel:
- Public TCP `P` -> local TCP `S` (TLS terminates at the MineHost relay).
- Public UDP `P` -> local UDP `P`.
- The relay injects a trustworthy `X-Forwarded-For` value; Geyser trusts only the authenticated local host-agent/proxy address.
- Paper Java TCP uses a separate public TCP port `J` -> local Paper TCP `J`.

This gives a cross-play server two public endpoints: Java TCP `J`, and Bedrock TCP+UDP `P`. Do not reuse the Paper port for Geyser.

Sources: [Geyser setup](https://geysermc.org/wiki/geyser/setup/), [Geyser NetherNet migration](https://geysermc.org/blog/preparing-for-nethernet/), [Geyser HTTPS for NetherNet](https://github.com/GeyserMC/GeyserWebsite/blob/master/wiki/geyser/nethernet-https.mdx).

### MineHost host-agent and relay transport

Proposed first implementation:
- A Go Android ARM64 host agent maintains an outbound authenticated QUIC connection to a MineHost relay. This works through common CGNAT configurations because the phone initiates the connection; it still needs real carrier testing.
- Java TCP and Bedrock signaling TCP use QUIC bidirectional streams.
- Bedrock gameplay UDP uses QUIC DATAGRAM frames. The host agent maps each player/session to a distinct local UDP socket so the local NetherNet endpoint can maintain per-peer flows.
- The relay forwards packets only; it never simulates Minecraft or stores worlds.
- Use TLS 1.3/QUIC authentication, short-lived scoped host credentials, bounded buffers, datagram size limits/fragmentation, rate limits and per-session bandwidth accounting. Do not implement custom cryptography.
- `quic-go` v0.65.0 (Go 1.26.0) is a candidate library. Android cross-compilation, NAT behaviour, MTU and performance must be proven before adoption.
- The separate `pm-nethernet` helper is the engine's WebRTC transport; it is not the MineHost relay agent.

### Regional routing

Start with one self-operated relay and measure it. Later, a host may maintain tunnels to multiple MineHost regions:
- Bedrock engines that support multiple advertised addresses can publish several relay candidates and let ICE select a working path.
- Java TCP needs a regional entrypoint or GeoDNS selection; DNS location is only a heuristic.
- Reserve a consistent public port per server across the relays that advertise it, or explicitly rewrite/advertise each candidate port. Never assume port equality without checking the SDP.
- Add regions only when measurements show a better end-to-end route and the relay cost is sustainable.

### HTTPS, domain and independence boundary

iOS NetherNet signaling requires HTTPS with a trusted certificate and a domain name. MineHost can terminate HTTPS at its own relay and forward plain HTTP over the authenticated host tunnel. Certificate issuance still relies on a public certificate authority (for example, Let's Encrypt), and a domain requires a registrar; these are unavoidable public-internet trust dependencies, not managed Minecraft tunnel services. Do not distribute a wildcard private key to every host. Prefer per-host certificates or terminate TLS centrally at MineHost relays.

### Failure behaviour

- If the MineHost control plane is down, local server and LAN play continue.
- If a relay tunnel drops, the host agent reconnects; existing player sessions may need to reconnect.
- If all relays are down, a reachable direct path may still work; otherwise remote play is unavailable but the local server remains running.
- If UDP is blocked, Bedrock relay service may be unavailable or degraded. Do not silently tunnel latency-sensitive UDP over TCP and claim equivalent performance.
