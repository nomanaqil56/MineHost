# MineHost — Physical Device Acceptance Test Plan

> **No test in this plan is considered passed until a real device/client/network result is recorded.** This document is a procedure, not evidence that the tests have been run.

## Test report template

For every run, record:
- Date/time and MineHost commit SHA.
- Phone manufacturer/model, SoC, RAM, Android version, ABI.
- App build variant and APK SHA-256.
- Server engine/runtime version and artifact SHA-256.
- Minecraft client edition/version/device.
- Host network (Wi-Fi/cellular/ISP), IPv4/IPv6, CGNAT status where known.
- Client network and country/region.
- p50/p95 RTT, jitter, packet loss, upload throughput.
- Server TPS/MSPT, memory, CPU and thermal state.
- Steps, expected result, actual result, logs and outcome.

## 0. Build and install

1. Run `./gradlew assembleDebug`.
2. Run `./gradlew testDebugUnitTest`.
3. Install the generated APK on a supported ARM64 phone.
4. Record the build SHA and device details.
5. Confirm app startup, Room migration, permissions, diagnostics and navigation.

## 1. Runtime installation gate

This is currently a known blocker.

1. Confirm the app reports whether the exact PHP/Java runtime is installed.
2. Confirm a missing runtime produces an actionable error.
3. Do not label an engine installed because only its PHAR/JAR exists.
4. Once a candidate runtime is available, verify its ABI, dynamic dependencies, version, licence and trusted checksum/signature.
5. Run its version/self-test command on-device and record output.

## 2. Bedrock local acceptance (after a valid engine/runtime exists)

1. Install the pinned Bedrock engine and compatible runtime.
2. Create a new server and start it.
3. Require a real protocol response before displaying ONLINE.
4. Join from a second Bedrock client on the same Wi-Fi.
5. Generate/load a world, place and break blocks, chat, and disconnect/reconnect.
6. Stop gracefully, restart, and confirm the changes persist.
7. Test backup, corrupt-archive rejection and safe restore on a disposable world.
8. Repeat at least three times and preserve logs.

## 3. Java local acceptance (after a valid JVM exists)

1. Install the pinned Java runtime and Paper build.
2. Obtain explicit user EULA consent; never auto-accept.
3. Start and require a real Java Server List Ping response.
4. Join from a separate Java client on the same LAN.
5. Test commands, plugins (if enabled), save, graceful stop and restart.
6. Record memory, CPU, TPS/MSPT and thermal behaviour.

## 4. Cross-play acceptance (after Java works)

1. Install compatible Paper, Geyser and (if needed) Floodgate versions.
2. Verify the authentication configuration and key handling.
3. Join the same world from one Java client and one Bedrock client.
4. Test movement, chat, inventory, blocks and disconnect/reconnect.
5. Publish only the tested client/server version matrix.

## 5. MineHost-owned public connectivity

Do not use Playit as evidence for the independent architecture.

1. Deploy the MineHost coordinator and one MineHost-operated relay.
2. Connect the host phone through Wi-Fi behind CGNAT.
3. Connect a Minecraft client from a genuinely different ISP.
4. Verify the public endpoint using a protocol-aware probe and a real client join.
5. Repeat with host on cellular data behind carrier CGNAT.
6. Test Java TCP and Bedrock transport separately.
7. Measure direct and relayed RTT, jitter, loss, throughput and relay resource use.
8. Stop the relay and verify the local server stays running.
9. Restore the relay and verify bounded reconnect behaviour.

## 6. Wi-Fi/cellular transition

1. Start a validated server and remote session.
2. Switch host from Wi-Fi to cellular and record what happens.
3. Confirm the app detects the network change.
4. Confirm the tunnel/endpoint is re-established before showing remote connectivity as healthy.
5. Confirm players receive an honest reconnect/disconnect experience; do not claim existing TCP sessions migrate seamlessly.
6. Switch back and repeat.

## 7. Background and thermal endurance

1. Start a validated server while charging on stable Wi-Fi.
2. Lock the screen for at least 30 minutes, then extend to several hours.
3. Record server process state, external reachability, battery and thermal status.
4. Repeat unplugged and on cellular to measure trade-offs.
5. Test low-memory conditions and OEM battery-management settings.
6. Confirm any process death is detected and not displayed as ONLINE.
7. Confirm wake locks and listeners are released after stop/crash.

## 8. Release regression

Before every release candidate:
- run CI build and unit tests;
- install over the previous app version and verify database migration;
- verify backup/export/import and rollback;
- verify release signing and update continuity;
- scan logs for secrets;
- confirm current engine and Bedrock transport compatibility;
- re-run LAN and external-network smoke tests.
