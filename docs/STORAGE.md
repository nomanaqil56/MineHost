# MineHost — Local World Storage & Backup Management

## 1. Storage Architecture & Privacy

MineHost strictly adheres to the principle of **local data ownership**:
- **Zero Cloud Storage:** World files, player inventories, and logs remain on the host's physical device.
- **Durable Storage Path:** Located in `context.filesDir/minehost/`, immune to cache clearing or temp cleaning by the OS.
- **Scoped Storage Access Framework (SAF):** User world import/export operations use Android's `ActivityResultContracts.OpenDocument` and `CreateDocument` to interact safely with external storage or SD cards without requiring broad `MANAGE_EXTERNAL_STORAGE` permissions on Android 11+.

---

## 2. Directory Layout

```
/data/data/com.minehost.app/files/minehost/
│
├── servers/
│   └── <server_uuid>/
│       ├── server.json             # Application metadata (name, port, engine, settings)
│       ├── server.properties       # Standard Minecraft engine properties
│       │
│       ├── worlds/                 # World files
│       │   └── world/              # Active world save
│       │       ├── level.dat       # NBT world header
│       │       ├── db/             # Bedrock LevelDB database OR
│       │       └── region/         # Java Anvil .mca region files
│       │
│       ├── plugins/                # Plugins / Addons
│       ├── logs/                   # Server execution and console logs
│       └── backups/                # Local compressed backup archives (.zip)
│
├── engines/
│   ├── bedrock/
│   │   └── pmmp/PocketMine-MP.phar
│   └── java/
│       └── paper/paper-1.20.4.jar
│
└── backups_global/                 # Restorable multi-server archive index
```

---

## 3. World Backup & Snapshot Management

### 3.1 Consistent Snapshot Protocol
Copying active world files (LevelDB `.ldb` or Anvil `.mca`) while the server process is writing risks corrupting chunk index tables. MineHost enforces the following backup protocol:

1. **Online Backup:**
   - Dispatches `save-off` (or engine equivalent) to the server console.
   - Dispatches `save-all flush` to ensure all cached chunks are committed to disk.
   - Waits for console confirmation (`Save completed` or `Flushed world data`).
   - Recursively reads and compresses the world directory into a timestamped `.zip` archive.
   - Dispatches `save-on` to resume normal autosaves.
2. **Offline Backup:**
   - Automatically executed before engine upgrades, world imports, or world restorations.
   - Directly packages the quiescent world folder.

### 3.2 Backup Metadata & Recovery
Each backup archive contains a companion `backup_manifest.json`:
```json
{
  "backupId": "backup_20261106_143000",
  "serverId": "srv_a1b2c3d4",
  "serverName": "Survival Realm",
  "edition": "BEDROCK",
  "engine": "bedrock-pocketmine-v5",
  "minecraftVersion": "1.20.80",
  "worldName": "world",
  "createdAt": "2026-11-06T14:30:00Z",
  "sizeBytes": 48291040,
  "fileCount": 184,
  "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

### 3.3 Restoration Safety Guard
Before restoring any backup archive over an active server:
1. Validates the integrity and checksum of the backup archive.
2. Verifies available free storage (requires at least 2.5x the unpacked backup size).
3. Automatically generates a pre-restore recovery snapshot of the existing world (`pre_restore_<timestamp>.zip`).
4. Replaces the target world folder cleanly and updates database metadata.

---

## 4. World Import & Export

- **Import Formats:**
  - Bedrock: `.mcworld` (ZIP archive containing `level.dat` and `db/`), standard ZIP archives.
  - Java: ZIP archives containing `level.dat` and `region/`.
- **Validation Engine:** `WorldValidation.kt` checks for:
  - Magic bytes and valid GZIP/NBT header in `level.dat`.
  - Format compatibility (warns if a Java world is imported into a Bedrock server).
  - Storage quota verification prior to extraction.
- **Exporting:** Packages the full world structure into standard `.mcworld` or `.zip` and prompts the user with the Android System Share Sheet or file picker.

