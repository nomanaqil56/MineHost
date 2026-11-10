# Device Testing Guide

Do these on a real ARM64 Android 9+ phone. Record pass/fail honestly; nothing here has been run yet.

## 0. Build
1. CI run 42763475505 passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and APK content assertions. For a local reproduction, run `./gradlew testDebugUnitTest lintDebug assembleDebug` (Android SDK + network required).
3. Install `app/build/outputs/apk/debug/app-debug.apk`.

## 1. Existing-data upgrade
Install the previous build, create a server, then install this build over it. Expect: app opens, server still listed (Room migration 1 to 2).

## 2. Runtime availability (known blocker)
Open Diagnostics on an ARM64 device. Confirm the packaged PHP CLI is detected and `php -v` succeeds. Java should remain unavailable until a complete Android-compatible JVM is supplied. A successful PHP version check alone is not a successful Minecraft server test.

## 3. Bedrock local join (PHP now packaged; device test still required)
1. Create a Bedrock server, install the pinned Axolotl-PM PHAR, and start it. Expect STARTING, then ONLINE only after a real RakNet pong. Capture console output and any native process-launch error.
2. Another device on the same Wi-Fi adds the address shown under "Same Wi-Fi". Join, place a block, stop the server, start it again: the block must persist.
3. Kill the app from recents while running; reopen. The server must show CRASHED, not ONLINE.

## 4. Tunnel (needs a Playit account)
Start the server, open the claim link shown, confirm the public address appears, join from mobile data. Toggle airplane mode on the host and confirm RECONNECTING, then recovery or an honest ERROR.

## 5. Backups
Backup while running (the app pauses saving), stop, restore, verify the world. Corrupt a copy of the zip and confirm restore is refused.

## 6. Background
Lock the screen for 30 minutes with the server running; check it is still reachable and note battery and temperature.
