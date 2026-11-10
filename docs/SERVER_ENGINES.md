# MineHost — Server Engines & Compatibility

> **Status correction (2026-11-10):** No Minecraft server engine is yet proven to launch from MineHost on a physical Android device. The Bedrock implementation now targets Axolotl-PM, not the archived original PMMP upstream. The matching Android ARM64 PHP package is selected as a candidate but is not yet packaged or executable through MineHost. Paper/Java and Geyser/Floodgate remain blocked by the missing Android-compatible JVM. Treat all engine entries below as architecture/implementation targets, not verified supported runtimes. See [Decision 0001](decisions/0001-bedrock-engine-and-php-runtime.md).

## 1. Engine Targets

MineHost provides a modular server engine interface. Below is the detailed breakdown of supported engines, their installation requirements, runtime configurations, and known limitations.

---

## 2. Bedrock Edition Engines

### 2.1 Axolotl-PM (PocketMine-compatible fork, v5.x)
- **Engine ID:** `bedrock-pocketmine-v5` (kept stable for existing MineHost server records)
- **Architecture:** PHP 8.4 PM5 runtime candidate for Android ARM64; executable packaging and process launch are not yet verified.
- **Selected baseline:** Axolotl-PM 5.51.1 for Bedrock 1.26.60; matching PHP asset: `PHP-8.4-Android-arm64-PM5.tar.gz`.
- **Target Minecraft Client:** Bedrock 1.26.60 at the selected release baseline.
- **Default Port:** `19132` (UDP RakNet).
- **Binary Components:**
  - Axolotl-PM `PocketMine-MP.phar`: Core server archive.
  - Matching PHP 8.4 Android ARM64 runtime archive from Axolotl-PM PHP-Binaries.
  - A proven Android-compatible executable launch path (still unresolved); a JNI shared object is not interchangeable with a PHP CLI executable.
- **Resource Footprint:**
  - Idle RAM: ~180 MB.
  - Active (2-4 players): ~350 MB.
- **Advantages:** PocketMine's lightweight PHP architecture is a promising fit for mobile hosting. Actual MineHost performance and compatibility are not yet measured.
- **Limitations:**
  - PocketMine-derived software is not a vanilla Bedrock server: mob AI, redstone and other vanilla behaviours differ and may require plugins.
  - Plugin compatibility must be checked, especially plugins that use internal network APIs.
  - Android runtime packaging, PHP extension loading and process execution are not yet proven.
  - The original `pmmp/PocketMine-MP` upstream was archived in July 2026; Axolotl-PM is the selected maintained continuation.

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

