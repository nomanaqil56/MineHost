# Device Testing Guide

Do these on a real ARM64 Android 9+ phone. Record pass/fail honestly; nothing here has been run yet.

## 0. Build
1. `./gradlew assembleDebug` (needs Android SDK + network). Fix any compile errors first: the latest edits were never compiled.
2. `./gradlew testDebugUnitTest`.
3. Install `app/build/outputs/apk/debug/app-debug.apk`.

## 1. Existing-data upgrade
Install the previous build, create a server, then install this build over it. Expect: app opens, server still listed (Room migration 1 to 2).

## 2. Runtime availability (known blocker)
Open Diagnostics. PHP and Java rows will show missing until a runtime is bundled in `jniLibs`. Starting a server must show a clear error, never ONLINE.

## 3. Bedrock local join (needs a PHP build)
1. Create a Bedrock server, start it. Expect STARTING, then ONLINE only after a real RakNet pong.
2. Another device on the same Wi-Fi adds the address shown under "Same Wi-Fi". Join, place a block, stop the server, start it again: the block must persist.
3. Kill the app from recents while running; reopen. The server must show CRASHED, not ONLINE.

## 4. Tunnel (needs a Playit account)
Start the server, open the claim link shown, confirm the public address appears, join from mobile data. Toggle airplane mode on the host and confirm RECONNECTING, then recovery or an honest ERROR.

## 5. Backups
Backup while running (the app pauses saving), stop, restore, verify the world. Corrupt a copy of the zip and confirm restore is refused.

## 6. Background
Lock the screen for 30 minutes with the server running; check it is still reachable and note battery and temperature.
