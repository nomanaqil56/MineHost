# MineHost — Networking Architecture & Worldwide Connectivity

## 1. Networking Overview

MineHost's networking subsystem provides low-latency, worldwide multiplayer connectivity without requiring router access, UPnP, or public IPv4 addresses.

```mermaid
graph LR
    subgraph ClientLayer ["Player Devices"]
        BedrockClient["Bedrock Client\n(Android / iOS / Win)"]
        JavaClient["Java Client\n(PC / Mac / Linux)"]
    end

    subgraph InternetLayer ["Public Internet"]
        AnycastDNS["Anycast Edge\n(*.joinmc.link / *.ply.gg)"]
    end

    subgraph RelayLayer ["Tunnel Infrastructure"]
        PlayitRelay["Playit.gg Anycast Node\n(UDP + TCP Forwarder)"]
    end

    subgraph HostDevice ["Android Host Device (MineHost)"]
        TunnelAgent["Playit Agent\n(libplayit.so)"]
        LocalLoopback["Local Ports\n(UDP 19132 / TCP 25565)"]
        ServerProcess["Minecraft Server Process\n(PM5 / PNX / Paper)"]
    end

    BedrockClient -->|UDP 19132| AnycastDNS
    JavaClient -->|TCP 25565| AnycastDNS
    AnycastDNS --> PlayitRelay
    PlayitRelay <==|Encrypted Control & Data Tunnel|=> TunnelAgent
    TunnelAgent --> LocalLoopback
    LocalLoopback --> ServerProcess
```

---

## 2. Protocol Requirements

### 2.1 Minecraft Bedrock Edition (UDP / RakNet)
- Protocol: RakNet over UDP.
- Default local port: `19132`.
- Packet characteristics: Connectionless UDP datagrams carrying RakNet reliability headers, ACK/NACK frames, and split packets.
- Tunnel requirement: Tunnel agent must buffer datagram boundaries accurately without dropping MTU payload sizes (~1400 bytes).

### 2.2 Minecraft Java Edition (TCP)
- Protocol: TCP streaming protocol.
- Default local port: `25565`.
- Packet characteristics: Connection-oriented byte stream with VarInt frame length prefixing.
- Tunnel requirement: TCP socket proxying with low buffering latency to prevent tick lag.

---

## 3. Tunnel Provider Abstraction (`TunnelProvider`)

The networking subsystem defines a unified interface for multiple tunnel backends:

```kotlin
interface TunnelProvider {
    val id: String
    val name: String
    val status: StateFlow<TunnelStatus>
    val publicEndpoint: StateFlow<PublicEndpoint?>

    suspend fun initialize(context: Context): Result<Unit>
    suspend fun start(localPort: Int, protocol: NetworkProtocol): Result<PublicEndpoint>
    suspend fun stop(): Result<Unit>
    suspend fun reconnect(): Result<PublicEndpoint>
    fun getDiagnosticLogs(): List<String>
}
```

### 3.1 Playit.gg Implementation
- **Binary:** Native Rust agent compiled for `aarch64-linux-android`, loaded as `libplayit.so`.
- **Claim Protocol:**
  - Upon first launch without a claim token, the agent generates an auth keypair and outputs a unique claim URL (`https://playit.gg/claim/...`).
  - MineHost intercepts this URL via stdout parser, presents a direct clickable button in the UI or auto-claims via the Playit REST API when user provides an API token.
- **Tunnels Configured:**
  - Bedrock: `udp/19132` -> Public endpoint (e.g. `stone-golem.gl.joinmc.link:38491`).
  - Java: `tcp/25565` -> Public endpoint (e.g. `stone-golem.gl.joinmc.link:25565`).

### 3.2 Custom Self-Hosted Relay (Rathole / FRP)
- Optional alternative provider for advanced users hosting their own VPS relay.
- Uses token authentication, encrypting traffic with Noise protocol / TLS.

---

## 4. Connection State & Diagnostics

MineHost maintains distinct statuses for the server and the networking tunnel:

| Server State | Tunnel State | UI Display | User Action |
| :--- | :--- | :--- | :--- |
| `ONLINE` | `CONNECTED` | **Active & Reachable Worldwide** (Green badge) | Share public address & port |
| `ONLINE` | `CONNECTING` | **Local Server Online, Tunnel Connecting...** (Yellow badge) | Wait for endpoint assignment |
| `ONLINE` | `DISCONNECTED` | **Local Only / Offline Relay** (Orange badge) | Check internet, press Reconnect Tunnel |
| `STOPPED` | `DISCONNECTED` | **Server Offline** (Grey badge) | Press Start Server |
| `ONLINE` | `ERROR` | **Tunnel Error: <reason>** (Red badge) | Inspect networking diagnostics |

### Health Check & Auto-Reconnection
- `ConnectionMonitor` continuously pings the local server port via UDP/TCP to confirm engine health.
- Measures round-trip ping time to the tunnel edge relay every 30 seconds.
- Automatically triggers exponential backoff reconnection if the device switches networks (e.g. Wi-Fi to 5G cellular).



## Update: current tunnel behaviour (uncompiled, untested on a device)

`TunnelStatus` now has DISCONNECTED, INSTALLING, AUTHENTICATING (claim required), STARTING, CONNECTED, RECONNECTING, ERROR and STOPPING. CONNECTED is shown only after the agent reports an endpoint; the app then probes the local server and the public endpoint with the real protocol (RakNet ping for Bedrock, Server List Ping for Java) and shows "Unreachable" if the probe fails. Restarts use backoff 5/10/20/40/60 s, at most 5 attempts. Agent output formats are assumed, not captured. LAN reachability does not imply internet reachability.
