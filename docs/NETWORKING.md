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
- A MineHost relay may need to proxy the HTTP SDP exchange and provide a reachable ICE candidate or terminate/forward WebRTC traffic over the phone tunnel. This architecture is a proof-of-concept blocker, not an already solved feature.
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
