# MineHost — Known Issues and Technical Limitations

**Last reconciled:** 2026-11-10. This list describes current source-level risks and unverified capabilities. See [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md) for release blockers and [MINEHOST_MASTER_ROADMAP.md](../MINEHOST_MASTER_ROADMAP.md) for the plan.

## Critical runtime blockers

### 1. Bedrock PHP runtime is not proven
The PocketMine adapter expects a PHP process executable. No Android PHP runtime artifact is present in the repository tree. `RuntimeManager` searches for `libphp.so` or a PHP executable in app-private storage, but file presence is not proof of launchability.

A shared object named `libphp.so` is not automatically a CLI executable. Validate ELF type, entry point/interpreter, dependencies, Android/Bionic compatibility and actual process launch on physical ARM64 devices. A downloaded binary in app-writable storage may be blocked by Android execution policy.

**Impact:** Bedrock hosting cannot be advertised as working until a real runtime is launched and a real client joins.

### 2. Java runtime is not proven
`JavaEngine` expects a Java launcher path represented as `libjava.so`; no complete ARM64 JVM distribution is present in the repository tree. A JVM shared library alone is not a complete Java runtime.

**Impact:** Paper hosting is blocked until a complete Android-compatible JVM, launcher and runtime library layout are validated on a device.

### 3. Android executable policy
Do not assume `chmod` / `setExecutable(true)` makes a binary in app-private writable storage executable on modern Android. Validate the chosen native packaging or launcher strategy on every supported API level. Desktop Linux binaries are not automatically compatible with Android's Bionic libc.

## Networking and independence blockers

### 4. Current provider is Playit-specific
The app currently binds `TunnelProvider` to `PlayitTunnelProvider`. This is an external managed tunnel provider and conflicts with the final MineHost independence goal if treated as mandatory.

**Impact:** The desired MineHost-owned network is not implemented.

**Required:** Build MineHost's own control plane, authenticated host agent and TCP/UDP relay data plane. Keep the provider interface modular. Playit may be used for temporary experiments only; it must not be the final production dependency.

### 5. Direct connectivity is not implemented
No complete direct public endpoint negotiation or protocol-specific NAT traversal is present. STUN/ICE concepts are not a universal drop-in for Minecraft TCP/UDP transports.

**Impact:** Direct-first route selection and automatic relay fallback remain roadmap items.

### 6. Bedrock protocol is version-dependent
The current readiness implementation contains RakNet-specific probing. That does not prove support for every modern Bedrock transport or server/client version.

**Required:** Publish a tested compatibility matrix for each chosen engine and client version; implement transport-specific probes and forwarding.

### 7. International connectivity is unverified
No real remote or international Minecraft client session is recorded. A hostname assigned by a tunnel agent does not prove the endpoint is reachable or playable.

## Lifecycle and reliability risks

### 8. Server lifecycle serialization is newly changed
Start and stop operations are now serialized using a mutex, and missing/dead process handles are treated as errors/recovered state. This source change has not yet passed Android CI or physical-device testing.

### 9. Process and service lifecycle
Foreground service, wake lock, thermal monitoring and stale-state reconciliation exist in source but are not physical-device verified. OEM process killers and memory pressure can still terminate hosting.

### 10. Live backup flush acknowledgement
Backup pauses world saving and issues save/flush commands, but the current implementation uses a fixed settle delay rather than a server-confirmed completion signal. Large worlds and engine-specific save behaviour need real validation.

### 11. Player and agent log parsing
Player tracking and Playit output parsing depend on expected line formats. Capture real output from the supported server and agent versions before treating these features as authoritative.

### 12. Tunnel credential protection
Review the storage of agent secrets and any future MineHost relay credentials. Use Android Keystore-backed encryption where applicable, short-lived scoped credentials, and log redaction. Never commit secrets.

## Release and repository hygiene

### 13. Project licence file
The inspected root tree did not contain a `LICENSE` file, although an earlier README claimed Apache-2.0. The owner must confirm the intended project licence and add the correct licence text before public distribution. Do not infer a licence from dependencies.

### 14. CI and build verification
An Android CI workflow has been added at `.github/workflows/android-ci.yml`. Its first run is pending. Until it passes, the current source changes remain uncompiled by CI.

### 15. Release signing
The Gradle release configuration no longer falls back to the debug key. A release build remains unsigned unless a valid owner-controlled production keystore is supplied. Keep the keystore and passwords outside Git.

### 16. Play Store and legal review
Review current Google Play target API, foreground-service declarations, downloaded executable-code policies, privacy disclosures, dependency licences, Mojang/Minecraft branding and distribution terms before release.

## What is not a bug but a product constraint

- Zero latency is impossible; distance, ISP routing, host upload speed, Wi-Fi/cellular radio quality and phone performance impose lower bounds.
- Relaying traffic costs money. A free product needs direct-first networking and a sustainable relay funding/capacity policy.
- Android cannot guarantee that every OEM will keep a long-running server alive under all battery/memory conditions.
- Console support for custom external servers can be restricted by platform policies and must be tested separately.
- A phone-hosted server is limited by that phone's thermal envelope, memory, storage and uplink.

Do not hide these constraints or promise unlimited capacity. MineHost should measure and explain them clearly.
