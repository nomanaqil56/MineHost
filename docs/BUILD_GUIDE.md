# MineHost — Build & Installation Guide

## 1. Prerequisites

To build MineHost from source, ensure you have:
- **Operating System:** Windows 10/11, macOS, or Linux.
- **JDK:** OpenJDK 17 or 21 (Temurin, Zulu, or Microsoft OpenJDK).
- **Android SDK:**
  - Android SDK Platform 34 (Android 14).
  - Android SDK Build-Tools 34.0.0.
  - CMake 3.22+ and NDK 26.x (optional for custom C++ modules).
- **Git:** Git 2.30+ installed and configured.

---

## 2. Building the Debug APK

### Option A: Local Build with Gradle Wrapper
```bash
# Clone the repository
git clone https://github.com/nomanaqil56/MineHost.git
cd MineHost

# On Linux / macOS
chmod +x gradlew
./gradlew assembleDebug

# On Windows PowerShell
.\gradlew.bat assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Option B: Automated Build via GitHub Actions CI
Every commit pushed to `nomanaqil56/MineHost` triggers `.github/workflows/build-apk.yml`.
1. Open [https://github.com/nomanaqil56/MineHost/actions](https://github.com/nomanaqil56/MineHost/actions).
2. Select the latest build run.
3. Download the `minehost-debug-apk` artifact.

---

## 3. Physical-Device Installation & Testing Guide

### Step 1: Install APK onto Android Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*Alternatively, transfer the APK file to your device via USB or download it directly from GitHub, then tap to install (enable "Install unknown apps" if prompted).*

### Step 2: Grant Required Permissions
1. Open **MineHost**.
2. Grant **Notification Permission** (required on Android 13+ to display the ongoing server status and stop controls).
3. In **Diagnostics & Setup** screen, tap **Disable Battery Optimization** to ensure Android does not kill the server when the screen locks.

### Step 3: Install Server Engine Components
1. In MineHost, navigate to **Diagnostics & Setup** (top-right gear icon).
2. Under **Server Engines & Runtimes**, tap **Download** next to **Bedrock Engine (PocketMine-MP)**.
3. The app will download and verify `PocketMine-MP.phar` directly into your private app files.

### Step 4: Create and Start Your Bedrock Server
1. Return to the **Dashboard** and tap the **+** (Create Server) button.
2. Enter a server name (e.g. *Survival World*).
3. Select **Bedrock Edition** (Default Port: `19132`).
4. Keep **Worldwide Public Tunnel** checked (or uncheck for local Wi-Fi only).
5. Tap **Create Server**.
6. On the Server Details screen, tap **START SERVER**.
7. The notification will show the server status.

### Step 5: Connect Second Minecraft Device (Local Wi-Fi)
1. Connect a second device (phone, tablet, or PC) to the same Wi-Fi network.
2. Open **Minecraft: Bedrock Edition**.
3. Go to **Play -> Servers -> Add Server**.
4. Server Name: `MineHost Local`
5. Server Address: Enter the local IP address of your Android phone (e.g. `192.168.1.50`).
6. Port: `19132`.
7. Tap **Save** and **Join Server**.
8. Build something, break blocks, and chat!

### Step 6: Worldwide Multiplayer (Playit.gg)
1. If Worldwide Tunnel is enabled, MineHost will initialize the Playit agent.
2. If first time, tap **Claim Agent on Playit.gg** to link your free agent.
3. Once assigned, your public address (e.g. `xyz.gl.joinmc.link:38491`) appears on screen.
4. Friends can join from another Wi-Fi or cellular network from anywhere in the world!

### Step 7: Stop & Persistence Verification
1. Tap **STOP SERVER** in MineHost.
2. Wait for graceful shutdown.
3. Tap **START SERVER** again.
4. Rejoin from Minecraft to confirm world blocks and inventory are preserved!
