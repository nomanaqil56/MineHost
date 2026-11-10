# MineHost — Networking Architecture & Global Connectivity

> **Target architecture:** direct-first connectivity with MineHost-operated relays as fallback. The Android phone always runs the actual Minecraft server. A relay forwards traffic only; it must never host or simulate the Minecraft world.
>
> **Status:** architecture target, not implemented or externally verified. The current Playit adapter is experimental and is not the intended permanent dependency.

See [MINEHOST_MASTER_ROADMAP.md](MINEHOST_MASTER_ROADMAP.md) for delivery phases and acceptance gates.

---

## 1. Goals and non-goals

### Goals
- Host the actual Minecraft server on the user's Android device.
- Support local Wi-Fi and mobile-data hosting.
- Allow players on external networks to connect without router administration when possible.
- Prefer direct paths to minimize additional latency and relay bandwidth.
- Operate our own coordinator and relay services for CGNAT/restricted-network fallback.
- Support Java TCP and the current transport required by the selected Bedrock engine/client versions.
- Report measured connectivity honestly.

### Non-goals
- A promise of zero latency or universal direct connectivity.
- Hosting Minecraft worlds in MineHost's cloud.
- Routing every session through a central proxy by default.
- Assuming one generic tunnel implementation works for all Minecraft protocols.
- Depending permanently on Playit.gg or another managed tunnel provider.

---

## 2. Target logical architecture

```text
                         MineHost Control Plane
                  registration / tickets / relay selection
                                  |
                                  | control only
                                  |
Player Minecraft Client ---- Connection Negotiation
           |                      |
           |             +--------+---------+
           |             |                  |
           +-------- Direct Path       MineHost Relay
                       preferred        fallback
                           |                  |
                           +--------+---------+
                                    |
                         Android Host / MineHost
                                    |
                           Local server port
                                    |
                       Actual Minecraft process
                         +----------+----------+
                         |                     |
                      Bedrock                 Java
                   selected engine           Paper
```

The control plane must not proxy gameplay packets. It issues short-lived, scoped connection tickets and helps peers select a path. The data plane carries game traffic only when a relay is required.

---

## 3. Connection preference order

1. **LAN direct:** same local network; no public relay or coordinator needed after discovery.
2. **Direct IPv6:** use a globally reachable IPv6 address when firewall policy and client reachability permit it.
3. **Direct public IPv4:** use only when the host really has an externally reachable mapping.
4. **Protocol-compatible NAT traversal:** attempt direct UDP traversal where both endpoints and protocol support it. Do not assume symmetric NAT or carrier networks will permit it.
5. **MineHost-operated relay:** use a regional self-hosted relay when direct access fails.

Java gameplay is TCP. Bedrock networking is version-dependent and must follow the selected engine's current transport implementation. A UDP hole-punching design is not a universal answer for Java TCP. Do not tunnel all Minecraft traffic through a generic WebRTC data channel without proving protocol compatibility and performance.

---

## 4. Java and Bedrock are separate transport adapters

### Java Edition
- Gameplay connection: TCP.
- Default port: 25565 (configurable).
- LAN direct path.
- Direct public TCP only when genuinely reachable.
- For CGNAT/restricted networks, use a MineHost-authenticated outbound TCP tunnel to a MineHost relay, which forwards to the local Java port.
- Preserve byte-stream semantics and avoid excessive buffering/Nagle-related latency.
- Probe using Java Server List Ping and a real client join.

### Bedrock Edition
- Do not hard-code "RakNet forever". The correct transport depends on the Bedrock client version and selected server implementation.
- Validate the selected engine's support for the currently required Bedrock transport and its TCP/UDP signalling/gameplay requirements.
- Preserve datagram boundaries and source/destination/session semantics required by that transport.
- Use a protocol-aware UDP relay path when relaying is required.
- Probe using the selected engine's real supported status/query protocol and confirm with a real client join.

**Release gate:** publish a supported Bedrock version/transport matrix. If the engine does not support the current client transport, show it as unsupported rather than pretending an old RakNet tunnel is sufficient.

---

## 5. MineHost-owned control plane

The control plane is a small, self-operated service (Go is a candidate, not a fixed requirement) responsible for:
- Host registration and revocable credentials.
- Server/session registration without storing world data.
- Short-lived connection tickets.
- Peer candidate exchange and NAT-traversal coordination.
- Relay discovery and allocation.
- Endpoint lifecycle/expiry.
- Relay health and capacity data.
- Abuse prevention, quotas and rate limits.

Requirements:
- No game packet proxying through the control plane.
- Authenticated host-to-control-plane communication.
- Short-lived scoped credentials, rotation and revocation.
- Minimal telemetry and documented retention.
- Redundant deployment when service maturity justifies it.
- Existing local hosting continues during a control-plane outage.

---

## 6. MineHost-operated data plane

MineHost operates relay software on infrastructure it controls. Initially this may run on rented VPS or colocated servers; that is still self-operated software, but it is not physical infrastructure ownership.

Relay requirements:
- Protocol-specific TCP and UDP forwarding.
- Authenticated encrypted host tunnel.
- Connection/session authorization.
- Bounded queues and backpressure.
- Low-latency forwarding without unnecessary application-layer transformations.
- Per-host and per-session bandwidth limits.
- Capacity metrics and overload protection.
- Abuse controls and DDoS response plan.
- Regional deployment only when traffic and measured routing justify it.

Candidate building blocks may include established open-source tunnel, crypto, NAT traversal, and proxy libraries. Their use does not make MineHost dependent on a third-party managed service. Review licences and maintainability before selection. Do not invent a custom transport protocol unless existing components demonstrably cannot meet requirements.

---

## 7. Relay selection and latency

Select by measured path quality, not geography alone. A useful first-order estimate is:

`estimated_rtt = player_to_relay_rtt + relay_to_host_rtt + relay_processing_overhead`

Also consider:
- packet loss and jitter on both legs;
- relay CPU, memory, queue depth and current bandwidth;
- host upload capacity;
- route stability and regional peering;
- protocol support.

A relay cannot remove physical distance or compensate for a weak host upload connection. A geographically close relay may still have poor peering. Record p50 and p95 results.

Initial candidate test regions (not a commitment to deploy all immediately): Mumbai, Singapore, Frankfurt, and US East. Start with one controlled relay and expand only from measured demand.

---

## 8. Wi-Fi and mobile data

Use Android connectivity APIs to observe Wi-Fi, cellular, Ethernet and network capability changes.

Policy:
- Support Wi-Fi and mobile data.
- Prefer the healthier route based on measured reachability, latency, jitter, packet loss and upload capacity.
- Respect user data-cost preference.
- Keep LAN connectivity available where possible.
- When the selected network changes, re-establish the tunnel and update the public endpoint only after it is verified.
- Explain that existing player sessions may disconnect because IP/NAT mappings change.
- Do not claim seamless session migration until it is proven.

Do not automatically bond Wi-Fi and cellular. Multipath aggregation requires compatible transports at both ends and can increase complexity, battery use and cost.

---

## 9. Health model and diagnostics

Track independent facts; do not collapse them into one green "online" state.

- Local server process: stopped / starting / ready / crashed.
- Local protocol health: reachable / unreachable / unknown.
- Host internet: available / unavailable.
- Direct path: available / failed / not attempted.
- Coordinator: reachable / unavailable.
- Relay agent: stopped / starting / authenticated / running / failed.
- Relay endpoint: assigned / expired / unknown.
- Remote protocol probe: reachable / unreachable / unknown.
- Network type: Wi-Fi / cellular / Ethernet / none.
- Measured RTT, jitter, loss, throughput, and probe timestamp.

A tunnel process being alive or an endpoint being assigned is not proof that a player can join. The public endpoint must pass a protocol-aware remote probe. Do not use TCP connect as the only health check for a UDP gameplay endpoint.

Tunnel failure must not stop the local Minecraft server. Reconnection must use bounded exponential backoff with jitter and a maximum retry policy.

---

## 10. Security and abuse controls

- Encrypt and authenticate the host-to-relay channel.
- Use short-lived relay tickets bound to host, port, protocol, and expiry.
- Prevent one host from claiming another host's endpoint.
- Rate-limit allocation, control-plane calls and probes.
- Bound packet sizes, queues, connections and bandwidth.
- Avoid logging player payloads or credentials.
- Redact secrets from logs and diagnostic exports.
- Support credential revocation.
- Define an abuse reporting and relay-blocking process.
- Plan for volumetric attacks; a small VPS alone cannot guarantee DDoS protection.

---

## 11. Rollout plan

### N1 — Local LAN
- Verify local IPv4/IPv6 discovery and connection.
- Join from a second device on the same Wi-Fi.
- Test server restart and world persistence.

### N2 — First self-hosted public relay
- Deploy one relay and coordinator.
- Implement authenticated outbound host tunnel.
- Test Java TCP and selected Bedrock transport independently.
- Join from a genuinely external network behind CGNAT.
- Measure end-to-end RTT, jitter, loss, bandwidth and relay cost.

### N3 — Direct-first routing
- Add IPv6/direct candidates.
- Add supported NAT traversal.
- Select direct path when proven reachable.
- Fall back to MineHost relay.

### N4 — Network changes
- Detect Wi-Fi/cellular transitions.
- Rebuild tunnel and endpoint.
- Test player reconnection behaviour.

### N5 — Regional scale
- Add relay regions from measured traffic.
- Add capacity-aware selection, redundancy, alerts and operational runbooks.

---

## 12. Acceptance tests

A networking feature is not complete until tested with real Minecraft clients.

- [ ] Two devices on the same Wi-Fi join via LAN.
- [ ] Host on Wi-Fi behind CGNAT is reachable through MineHost relay from another ISP.
- [ ] Host on mobile data behind carrier CGNAT is reachable through MineHost relay.
- [ ] Java TCP is tested independently from Bedrock.
- [ ] Bedrock current transport is tested against declared supported clients.
- [ ] Relay outage leaves local server running.
- [ ] Host network change triggers endpoint recovery and clear player messaging.
- [ ] Public endpoint passes a protocol-aware remote probe.
- [ ] p50/p95 RTT, jitter, loss and throughput are recorded.
- [ ] Relay bandwidth and cost per active server-hour are recorded.
- [ ] No Playit or other managed tunnel account is required for the MineHost-owned relay acceptance test.

**No worldwide-connectivity claim is valid until these external-network tests pass.**
