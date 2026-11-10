# MineHost — Local World Storage and Backup Management

**Product rule:** the user's worlds remain on the Android host device. MineHost does not require cloud world storage. The user must be able to export their world/server data and move it to another host.

This document describes the current storage design and safeguards. Real engine-specific world backup/restore still requires physical-device testing.

## 1. Storage and privacy

- Server metadata, logs and world files are stored under app-private `context.filesDir/minehost/`.
- Cache directories are not used for durable world data.
- Import/export should use Android Storage Access Framework pickers (OpenDocument/CreateDocument) rather than broad storage permissions.
- MineHost should not upload world contents to its control plane or relay infrastructure.
- App uninstall can remove app-private data; users must be clearly told to export or back up important worlds before uninstalling.

## 2. Expected directory layout

```text
/data/data/com.minehost.app/files/minehost/
  servers/
    <server-id>/
      worlds/
      plugins/
      logs/
      backups/
      config/
  engines/
    bedrock/
    java/
  runtimes/
  tunnels/
  runtime_manifest.json
```

Engine adapters own the exact engine-specific paths. Bedrock LevelDB and Java Anvil worlds are not interchangeable. Never assume the same world layout or import compatibility.

## 3. Backup creation

### Offline backup
1. Verify the server process is stopped.
2. Resolve the exact world path from the engine adapter.
3. Check available storage.
4. Create an archive in a temporary location.
5. Generate a content checksum/manifest.
6. Verify the archive can be read.
7. Atomically move it into the backup directory and persist metadata.

### Online backup
The current source attempts to pause world saving, issue a save/flush command, wait a fixed interval, archive the world, then resume saving in a `finally` block.

**Known limitation:** the implementation currently uses a fixed settle delay; it does not have a confirmed server-side completion acknowledgement for every supported engine. A fixed wait is best-effort, not a guarantee that every large world has fully flushed.

Before production:
- Implement engine-specific save/flush acknowledgement where possible.
- Fail or clearly label a backup as best-effort if a save command cannot be sent or acknowledged.
- Ensure save-on is always restored.
- Test with large, active real worlds and abrupt process termination.

## 4. Backup integrity and restore

The code includes checksum verification, safe archive extraction, path validation and a temporary-directory swap with rollback safeguards. These mechanisms have pure-logic tests, but a real Bedrock LevelDB and Java Anvil world restore has not yet been verified on-device.

Required restore sequence:
1. Refuse restore while the server is STARTING, ONLINE or STOPPING.
2. Verify archive checksum against stored metadata.
3. Reject path traversal, absolute paths, symlinks or unreasonable archive expansion.
4. Extract into a fresh temporary directory.
5. Validate expected engine-specific world structure.
6. Create a safety snapshot of the current world; abort if that fails.
7. Swap using same-filesystem atomic rename where supported.
8. Roll back if the swap fails.
9. Update metadata only after the replacement is confirmed.
10. Start the server and verify it can load the restored world.

Never delete the live world before a validated replacement and rollback path exist.

## 5. Import and export

Planned formats and behaviour:
- Bedrock: support formats actually accepted by the selected Bedrock engine; validate `.mcworld`/ZIP structure and database directory.
- Java: ZIP export/import with a valid Java world structure, including expected region and metadata files.
- Export the complete server directory where practical, including configuration and plugins, while excluding transient PID/lock files and credentials.
- Clearly warn when moving worlds across editions, engines or Minecraft versions may be unsupported.
- Provide a share/save picker and verify the exported file after writing.

## 6. Acceptance tests

- [ ] Create world on a physical device.
- [ ] Place blocks and change inventory with a real client.
- [ ] Stop and restart server; verify persistence.
- [ ] Create offline backup and verify archive checksum.
- [ ] Create online backup on a large active world; verify engine save completion.
- [ ] Corrupt a backup and confirm restore refuses it without damaging the current world.
- [ ] Restore a valid backup and verify the engine loads it.
- [ ] Test insufficient storage and interrupted archive creation.
- [ ] Export world/server files and import on a fresh test installation.
- [ ] Verify no world content is sent to MineHost control-plane or relay services.
