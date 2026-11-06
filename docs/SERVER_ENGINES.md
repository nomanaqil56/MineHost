# MineHost — Supported Server Engines & Compatibility

## 1. Supported Server Engines

MineHost provides a modular server engine interface. Below is the detailed breakdown of supported engines, their installation requirements, runtime configurations, and known limitations.

---

## 2. Bedrock Edition Engines

### 2.1 PocketMine-MP (v5.x)
- **Engine ID:** `bedrock-pocketmine-v5`
- **Architecture:** PHP 8.2+ (ZTS - Zend Thread Safety) compiled for `aarch64-linux-android`.
- **Target Minecraft Client:** Bedrock 1.20.x - 1.21.x (updated via RakLib & protocol updates).
- **Default Port:** `19132` (UDP RakNet).
- **Binary Components:**
  - `PocketMine-MP.phar`: Core server archive.
  - Native PHP binary (`libphp.so` in `nativeLibraryDir`).
  - `start.sh` or direct executable invocation via ProcessBuilder.
- **Resource Footprint:**
  - Idle RAM: ~180 MB.
  - Active (2-4 players): ~350 MB.
- **Advantages:** Extremely low memory and CPU usage; instantaneous startup; low thermal impact on mobile devices.
- **Limitations:**
  - Does not implement vanilla mob AI by default (requires plugins like PureEntitiesX).
  - Nether/The End dimensions require plugins or custom world generators.
  - Redstone mechanics are limited compared to Bedrock Dedicated Server.

### 2.2 PowerNukkitX (PNX)
- **Engine ID:** `bedrock-powernukkitx`
- **Architecture:** Java 21 on Android ARM64 JVM.
- **Target Minecraft Client:** Bedrock 1.20.x - 1.21.x.
- **Default Port:** `19132` (UDP RakNet).
- **Binary Components:**
  - `powernukkitx.jar`.
  - Android JRE 21 runtime.
- **Resource Footprint:**
  - Idle RAM: ~650 MB.
  - Active (2-4 players): ~1.2 GB.
- **Advantages:**
  - Extensive vanilla feature parity: mob AI, complete redstone simulation, water physics, multi-dimensions (Overworld, Nether, End).
  - Modern Java plugin API with reflection and events.
- **Limitations:** Higher RAM and battery consumption than PocketMine-MP. Requires devices with at least 6GB physical RAM.

---

## 3. Java Edition Engines

### 3.1 PaperMC
- **Engine ID:** `java-paper`
- **Architecture:** Java 17/21 OpenJDK.
- **Target Minecraft Client:** Java Edition (versions 1.20.4, 1.21+).
- **Default Port:** `25565` (TCP).
- **Binary Components:**
  - `paper-<version>.jar`.
  - Android JRE 17/21.
- **Startup Parameters:**
  - `-Xms256M -Xmx1536M -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -jar paper.jar --nogui`
- **Resource Footprint:**
  - Idle RAM: ~900 MB.
  - Active (2-4 players): ~1.8 GB.
- **Advantages:** Industry-standard optimization, anti-X-ray, async chunk loading, Bukkit/Spigot/Paper plugin support.
- **Limitations:** Requires substantial memory; not recommended for low-end devices with <= 4GB RAM.

---

## 4. Cross-Play Integration (Geyser & Floodgate)

### 4.1 Geyser-Standalone Bridge
- **Engine ID:** `crossplay-geyser-standalone`
- **Target Setup:** Runs alongside PaperMC.
- **Ports:**
  - Bedrock Facing: `19132` (UDP).
  - Java Facing: `127.0.0.1:25565` (TCP loopback).
- **Authentication Modes:**
  - `online`: Bedrock players log in with their Java-licensed Microsoft account.
  - `floodgate`: Bedrock players can join using their Bedrock Xbox credentials via automatic public/private key verification without purchasing a separate Java account.
- **Configuration:** Auto-generated `config.yml` synchronizing server MOTD, player limits, and loopback forwarding.

