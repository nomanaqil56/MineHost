# MineHost — Physical Device Testing

> Nothing in this guide should be reported as passed until it has been run on a real device and the evidence recorded. The current source audit has no successful real Minecraft server/client session.

## 0. Build and install

1. Use JDK 17 and Android SDK 36.
2. Run:
   §bash
   ./gradlew --no-daemon testDebugUnitTest assembleDebug
   §
3. Install the debug APK on an ARM64 Android phone.
4. Record device model, Android version/API, RAM, free storage, and build commit.

## 1. App foundation

- Open the app and navigate through dashboard, server creation, settings, diagnostics, worlds, and backups.
- Create and edit a server profile.
- Force-stop and reopen the app; verify Room data persists.
- Upgrade from the previous supported app version and verify migrations.
- Check logs for crashes, leaked secrets, or misleading online statuses.

## 2. Runtime guard (current expected result)

The current branch has no verified PHP or JVM packaged. Therefore:
- Diagnostics must show the runtime as missing.
- Bedrock install must fail with an actionable missing-PHP message.
- Java install must fail with an actionable missing-JVM message.
- Neither engine may be marked installed or online.
- Cross-play must remain unavailable.
- The app must not download and execute arbitrary native binaries from writable app storage.

This is a correctness test, not a successful hosting test.

## 3. Engine acceptance (blocked until engine/runtime selected)

When an engine passes the feasibility spike:
1. Install the pinned engine/runtime through the approved distribution path.
2. Start a real server and wait for protocol-specific readiness.
3. Join from a second real Minecraft client on the same LAN.
4. Place/break blocks, chat, disconnect, and reconnect.
5. Stop gracefully, restart, and verify world/player data persistence.
6. Force-kill the app/process and verify honest CRASHED recovery without world corruption.
7. Record server logs, engine version, client version, TPS/MSPT, memory, CPU, battery, and temperature.

## 4. Connectivity acceptance (blocked until MineHost relay exists)

- Test same-LAN direct join.
- Test public IPv6/direct join where available.
- Test cellular host behind CGNAT.
- Test MineHost-operated relay fallback with a real client.
- Test from an independent ISP and at least one other country before claiming international support.
- Record median/p95 RTT, jitter, packet loss, throughput, relay region, reconnect behavior, and host upload speed.
- Verify Wi-Fi to mobile-data handover; expect existing TCP sessions may need reconnecting.

Do not use a Playit account as proof of MineHost-owned connectivity.

## 5. Background and thermal acceptance

Only after a real server works:
- Run with screen off for at least 30 minutes, then several hours.
- Test while charging and on battery.
- Test Wi-Fi and mobile data.
- Record CPU, memory, thermal status, battery drain, server TPS/MSPT and disconnects.
- Test Doze, OEM battery optimization, low-memory pressure, incoming calls, app update and reboot.
- Verify graceful stop, crash recovery, and no excessive wake-lock retention.

## 6. Backup acceptance

- Create a backup of a real server world.
- Restore to a separate test profile.
- Verify world opens and player data remains intact.
- Corrupt an archive and verify restore is refused.
- Verify failed restore rolls back to the previous world.
- Test both Bedrock and Java world formats once those engines are supported.
