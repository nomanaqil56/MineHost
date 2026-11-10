# MineHost — Networking Architecture & Global Connectivity

> **Target:** direct-first connections with MineHost-operated fallback infrastructure. The Android phone runs the actual Minecraft server. MineHost relays forward traffic only; they never host the world.
>
> **Status:** architecture target and partial local code. The MineHost coordinator, TCP reverse tunnel, TURN deployment, and real external-network tests are not complete.

See [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md) for delivery phases and acceptance gates.

---

## 1. Goals

- Host the actual game server on the user's Android device.
- Support Wi-Fi and cellular hosting.
- Prefer direct paths to minimize latency and relay bandwidth.
- Provide MineHost-operated public connectivity for CGNAT/restricted networks.
- Support Java TCP and current Bedrock NetherNet transport.
- Keep local hosting independent of MineHost cloud/control-plane availability.
- Never claim that a signalling endpoint or tunnel status proves a successful game session.

No network can guarantee zero lag. Host upload speed, phone CPU/thermal limits, physical distance, ISP routing, jitter and packet loss remain constraints.

## 2. Two protocol paths

Java and current Bedrock are not the same network problem.

### Java Edition
- Gameplay uses TCP (default port 25565).
- Direct LAN/public IPv6/reachable public IPv4 when available.
- Otherwise, the phone maintains an authenticated outbound TCP tunnel to a MineHost edge.
- The edge exposes a public TCP endpoint and forwards the byte stream through the tunnel to the local Java server.
- Use bounded buffers, backpressure, keepalives, reconnect handling and protocol-aware health checks.
- Do not route Java TCP through a UDP-only tunnel.

### Bedrock Edition — NetherNet
MineHost's current Bedrock candidate is Axolotl-PM. Bedrock 1.26.60 uses NetherNet, a WebRTC transport based on ICE, DTLS and SCTP DataChannels. Axolotl-PM requires `ext-webrtc` (libdatachannel bindings) for it.

NetherNet has two separate paths:
- **Signalling:** HTTP(S) over TCP. The built-in endpoint uses `GET /v1/join` as a capability probe and `POST /v1/join/{networkId}` for SDP exchange. The default signalling port follows `server-port` (usually 19132).
- **Gameplay:** WebRTC data channels over UDP. ICE gathers direct candidates and configured STUN/TURN candidates.

MineHost design:
1. Phone runs Axolotl-PM and its built-in HTTP signalling endpoint locally.
2. Phone maintains an authenticated outbound TCP reverse tunnel to a MineHost edge.
3. MineHost edge exposes a public hostname with valid HTTPS and forwards signalling requests through the tunnel to the phone.
4. Axolotl-PM uses MineHost-operated STUN/TURN ICE servers. Direct candidate pairs should be selected when possible; TURN relays gameplay only when direct UDP connectivity fails.
5. Keep signalling reachability and WebRTC gameplay reachability as separate health states.

**Important:** `GET /v1/join` returning 2xx proves only that NetherNet signalling is available. It does not prove that the UDP WebRTC channel can be established. A real Bedrock client join is required.

### Geyser cross-play
For a Java/Paper server with Geyser:
- Java players connect over TCP to Paper.
- Bedrock players connect to Geyser using the current NetherNet transport (Geyser documents NetherNet support for current Bedrock clients).
- MineHost must expose both the Java TCP endpoint and the Bedrock signalling + UDP gameplay path.
- Do not mark cross-play as supported until both client types join the same phone-hosted world.

---

## 3. Target logical architecture

```text
Player Client
   |                         MineHost Control Plane
   |                    registration / tickets / relay choice
   |                                  |
   +-------- direct path -------------+
   |
   +-------- TCP public edge -------- MineHost edge
   |                                  |    |                                  |  +-- HTTPS signalling proxy
   |                                  |        |
   |                                  |    outbound TCP tunnel
   |                                  |        |
   |                                  |    Android phone : local server
   |                                  |
   +-------- WebRTC ICE/DTLS ---------- MineHost TURN (fallback)
                                            |
                                       Android phone
                                            |
                                     Actual Minecraft process
```

The control plane handles registration, short-lived credentials, rendezvous and relay selection. It must not proxy gameplay packets. The data plane consists of the TCP reverse tunnel/edge and the TURN relay for Bedrock WebRTC.

---

## 4. MineHost control plane

A small self-operated service (Go is a candidate) handles:
- Host/device registration and revocable credentials.
- Server/session registration (no world storage).
- Short-lived connection tickets.
- Relay/TURN allocation and region selection.
- Public hostname/endpoint lifecycle.
- Relay health, capacity and abuse controls.
- Minimal telemetry with documented retention.

Requirements:
- No game packet proxying through the control plane.
- Authenticated communication and short-lived scoped credentials.
- Rotation and revocation.
- Redundant deployment as service maturity grows.
- Existing local server remains running during control-plane outages.

---

## 5. MineHost data plane

### A. TCP edge and reverse tunnel
Required for Java TCP and Bedrock HTTP(S) signalling.

- Phone opens an outbound authenticated encrypted connection to a MineHost edge.
- Edge allocates a public host/port and routes incoming TCP streams to the phone's local service.
- For Bedrock, edge terminates TLS with a valid certificate and forwards HTTP signalling. Configure Axolotl-PM's trusted reverse-proxy settings only for the local tunnel/proxy address and a specific forwarded-IP header.
- For Java, edge forwards the Minecraft TCP byte stream without modifying the protocol.
- Tunnel must support concurrent connections, backpressure, idle timeouts, health checks, reconnect and endpoint updates.
- A tunnel failure must not stop the local Minecraft process.

### B. TURN for Bedrock WebRTC
Operate our own TURN infrastructure (Coturn or another reviewed implementation) instead of depending on public/managed TURN services.

- Axolotl-PM's `ice-servers` configuration points to MineHost-controlled TURN endpoints.
- Use time-limited authenticated credentials; never embed a permanent TURN password in the APK or public config.
- Prefer direct ICE candidate pairs when available. TURN is fallback for symmetric NAT, CGNAT or restrictive networks.
- TURN relays gameplay packets and consumes bandwidth; implement per-host quotas, capacity limits, abuse controls and regional monitoring.
- Axolotl-PM documents that `ice-udp-mux: true` cannot be used with TURN. MineHost currently generates `ice-udp-mux: false` and a fixed UDP port range sized to max players.
- Do not add unreachable ICE servers: candidate gathering can stall until timeout.

The MineHost edge and TURN service are separate components: TCP signalling is not a substitute for UDP WebRTC relaying.

---

## 6. Direct-first route policy

1. Same-LAN direct connection.
2. Direct public IPv6 where reachable and firewall rules permit it.
3. Direct public IPv4 where the host is genuinely reachable.
4. WebRTC ICE direct candidate pair for NetherNet when possible.
5. MineHost TURN relay for Bedrock WebRTC when direct ICE fails.
6. MineHost TCP reverse tunnel/edge for Java and Bedrock signalling when the phone is not publicly reachable.

Do not assume a single generic UDP hole-punching library solves Java TCP or all NAT types. NetherNet already uses WebRTC ICE; use its protocol implementation rather than creating a parallel NAT traversal layer for Bedrock.

### Route selection
For TURN region choice, measure:
- host-to-TURN RTT, jitter and loss;
- player-to-TURN path where measurable;
- relay capacity and queue depth;
- host upload and device thermal state.

A first-order estimate is `player_to_turn_rtt + turn_to_host_rtt + relay_processing_overhead`. Geography alone is not enough. A relay cannot remove physical distance or fix a slow host upload connection.

Start with one self-managed region, measure real traffic, then add regions. Candidate test locations: Mumbai, Singapore, Frankfurt and US East.

---

## 7. Wi-Fi and mobile data

Use Android connectivity APIs to observe Wi-Fi, cellular, Ethernet and capability changes.

Policy:
- Support Wi-Fi and mobile data.
- Prefer the healthier path based on measured reachability, RTT, jitter, loss and upload capacity.
- Respect the user's data-cost preference.
- Keep LAN hosting available even if internet access is unavailable.
- On a network change, re-establish the tunnel and verify the new endpoint before reporting remote connectivity as healthy.
- Existing TCP and WebRTC sessions may disconnect when the host's public address/NAT mapping changes. Tell players to reconnect; do not claim seamless migration unless proven.
- Do not automatically bond Wi-Fi and cellular. Multipath aggregation requires compatible transports at both ends and may increase battery use and complexity.

---

## 8. Health model and diagnostics

Track independent facts; do not collapse them into one green "online" state.

- Local server process: stopped / starting / ready / crashed.
- Local engine readiness: protocol response / timeout / unknown.
- NetherNet signalling: HTTP(S) /v1/join reachable or not.
- WebRTC gameplay: direct ICE / TURN relay / failed / unknown (known only after actual ICE/client telemetry).
- Java TCP endpoint: reachable / unreachable.
- Host internet: available / unavailable.
- TCP tunnel: stopped / connecting / authenticated / forwarding / failed.
- TURN allocation: not configured / active / failed (where telemetry is available).
- Coordinator: reachable / unavailable.
- Network type: Wi-Fi / cellular / Ethernet / none.
- RTT, jitter, loss, throughput, timestamp and measurement type.

A successful NetherNet signalling probe is not gameplay ping. Label it "signalling RTT". Actual game RTT should be measured by a real client or a protocol-level gameplay echo/telemetry mechanism.

Tunnel failure must not stop the local server. Reconnection uses bounded exponential backoff with jitter and a maximum retry policy.

---

## 9. Security and abuse controls

- Encrypt and authenticate the host-to-edge tunnel.
- Use short-lived relay and TURN credentials bound to host/session/expiry.
- Prevent one host from claiming another host's endpoint.
- Rate-limit allocation, control-plane calls, signalling and probes.
- Bound queues, packet sizes, connections and bandwidth.
- Do not log player payloads or credentials.
- Support credential revocation and rotation.
- Restrict forwarded headers to trusted local proxy addresses.
- Define abuse reporting, account suspension and relay-blocking processes.
- Plan for volumetric attacks; a small VPS alone cannot guarantee DDoS protection.

---

## 10. Rollout and acceptance tests

### N1 — Local Bedrock/Java
- [ ] Axolotl-PM NetherNet signalling answers locally.
- [ ] Bedrock client joins on same Wi-Fi using current client version.
- [ ] Java server answers Server List Ping and accepts a real client.
- [ ] Cross-play tested only after Paper/Geyser work.

### N2 — MineHost TCP edge
- [ ] Phone establishes authenticated outbound tunnel.
- [ ] Public HTTPS signalling endpoint proxies to local Axolotl-PM.
- [ ] Java TCP endpoint forwards to local Paper port.
- [ ] Test host behind CGNAT from a different ISP.
- [ ] Relay outage leaves local server running.

### N3 — MineHost TURN
- [ ] Deploy self-managed TURN with ephemeral credentials.
- [ ] Configure Axolotl-PM `ice-servers` using MineHost TURN only.
- [ ] Verify direct ICE is preferred where it succeeds.
- [ ] Verify TURN fallback from mobile carrier CGNAT.
- [ ] Record gameplay join success, RTT, jitter, loss, bandwidth and TURN cost.

### N4 — Wi-Fi/cellular transitions
- [ ] Detect network changes.
- [ ] Re-establish TCP tunnel and update public endpoint.
- [ ] Re-gather ICE candidates/reconnect sessions.
- [ ] Verify truthful diagnostics and player reconnection behaviour.

### N5 — Regional scale
- [ ] Add TURN/edge regions based on real user distribution and measured paths.
- [ ] Capacity planning, quotas, abuse mitigation, DDoS response and runbooks.
- [ ] Redundant coordinator, monitoring, alerting and restore drills.

**No worldwide-connectivity claim is valid until real clients join from external networks through MineHost-operated infrastructure.**
