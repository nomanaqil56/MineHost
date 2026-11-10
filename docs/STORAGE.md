# MineHost — Storage & Backup Management

This document describes the current app-private storage implementation. It is not a claim that every backup path has passed physical-device testing.

## 1. Storage principles

- Minecraft server processes and authoritative world data stay on the Android host.
- Server/world files remain in app-private storage unless the user explicitly exports them.
- Room stores metadata and indexes; it is not the authoritative world store.
- Cloud app backup excludes the `minehost/servers/` directory. Device-to-device transfer rules may include MineHost data, depending on Android/OEM behaviour.
- The user can explicitly export a backup; exported archives are not encrypted by the current implementation.

## 2. Current directory layout

The app-private root is `files/minehost/`:

```text
files/minehost/
  servers/<server-id>/
    server.properties
    pocketmine.yml
    nethernet.key                 # created by Axolotl-PM after first start
    worlds/<world-name>/          # authoritative world data
    plugins/                      # engine/plugin data when applicable
    backups/
      <backup-id>.zip
      <backup-id>.json            # sidecar manifest, not inside the ZIP
  engines/
    bedrock/PocketMine-MP.phar    # Axolotl-PM PHAR asset filename
    java/<paper-jar>              # only after a compatible JVM is implemented
  tunnels/                        # experimental provider artifacts; not production network
  runtime_manifest.json
```

The PHP executable is packaged in the APK under `jniLibs/arm64-v8a/libphp.so` and extracted to Android's native library directory. It is not downloaded to writable app storage at runtime. The current PHP bootstrap pins an Axolotl-PM PHP 8.4 Android ARM64 build containing ext-webrtc. The Java runtime is not yet validated.

Server logs and metadata are also indexed in Room. Do not assume the file tree above contains every database-managed record.

## 3. Backup archive contents

A backup ZIP contains:
- the selected world directory tree;
- optionally `__minehost/nethernet.key` if the server identity key exists.

The NetherNet key is a private server identity credential. It is included so restore/migration can preserve the identity clients have pinned. **Treat any exported backup containing this key as sensitive; do not post it publicly or send it to untrusted people.** The current archive format is not password-encrypted.

The sidecar `<backup-id>.json` manifest records:
- backup ID;
- server ID and name;
- world name;
- creation timestamp;
- ZIP size;
- content SHA-256 checksum;
- optional note;
- whether the NetherNet identity key was included.

The SHA-256 is calculated over uncompressed file contents in ZIP entry order (including the identity key), not over the compressed ZIP byte stream.

## 4. Backup creation safety

When the target server is stopped, the manager snapshots the world directly.

When the target server is online, MineHost:
1. sends `save-off`;
2. sends `save-all` for Axolotl-PM or `save-all flush` for Paper;
3. sends `list` as a console barrier;
4. waits for the server's `players online` output, which is processed after the save command;
5. creates the ZIP while autosaving is paused;
6. sends `save-on` in a `finally` block.

If commands cannot be sent or the console barrier is not observed within 15 seconds, backup creation aborts instead of silently copying a potentially mid-write world. This is stronger than a fixed delay but still requires real-device testing against supported engine versions.

The ZIP is written to a unique temporary file and renamed into place only after compression completes. The manifest is also written to a temporary file and renamed after successful serialization. Failed creation cleans up partial artifacts.

## 5. Restore safety

Restore is refused while the target server process is active.

The restore workflow:
1. verifies the recorded content checksum before changing the live world;
2. checks free storage using a conservative heuristic (currently twice the compressed archive size; this is not a guarantee for highly compressible worlds);
3. extracts to a temporary directory with ZIP path-traversal checks;
4. stages the optional NetherNet identity key outside the world directory;
5. creates a pre-restore safety snapshot of the current world;
6. uses the world-swap helper with rollback;
7. commits the restored identity key only as part of the guarded restore sequence and restores the previous key if the world swap fails.

The pre-restore safety archive is retained locally for recovery and is not automatically uploaded anywhere.

## 6. Export/import

- Explicit export uses Android's Storage Access Framework (SAF).
- Import validates archive structure/checksum before replacing world data.
- Do not assume a backup is portable across arbitrary engine versions; world formats and plugin data may be version-specific.
- A world export that includes `nethernet.key` also carries the private server identity. Users should be warned before sharing/exporting such an archive.
- Cross-device restore should be tested on a second physical device before being advertised as supported.

## 7. Remaining storage work

- Add optional password-based encryption for exported backups containing server identity keys.
- Test large worlds, interrupted backup, low-storage, process death, corrupt ZIPs and rollback on physical devices.
- Verify file permissions for `nethernet.key` after restore.
- Add a visible warning when an export includes the private identity key.
- Verify migration between Axolotl-PM releases and between Bedrock/Java world formats.
