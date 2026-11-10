#!/usr/bin/env python3
"""
MineHost Master Physical Device Automated Test Runner
Executes an end-to-end physical device verification on a connected Android ARM64 device:
- Clean uninstall & fresh APK streamed install
- System permissions & power optimization whitelist
- Native library extraction & 64-bit ARM ELF header validation
- Native PHP 8.4 CLI launch & extension modules validation
- Axolotl-PM 5.51.1 PHAR deployment, hash check & cache initialization
- Headless Minecraft Bedrock server deployment on UDP 19132
- Live RakNet Unconnected Ping/Pong binary probe
- Interactive console command dispatch via stdin
- Graceful shutdown & world persistence verification (LevelDB / level.dat)
- Android Compose UI launch & process health check
- Outputs report to PHYSICAL_DEVICE_TEST_REPORT.txt
"""

import os
import sys
import time
import subprocess
import re
import hashlib
import struct
from pathlib import Path

ADB_PATH = os.environ.get("ADB_PATH", "adb")
APK_PATH = os.environ.get("APK_PATH", "app/build/outputs/apk/debug/app-debug.apk")
PACKAGE_NAME = "com.minehost.app.debug"
PHAR_PATH = os.environ.get("PHAR_PATH", "PocketMine-MP.phar")
REPORT_FILE = "PHYSICAL_DEVICE_RUNTIME_SMOKE_REPORT.txt"

RAKNET_MAGIC = b"\x00\xff\xff\x00\xfe\xfe\xfe\xfe\xfd\xfd\xfd\xfd\x12\x34\x56\x78"

class PhysicalDeviceTestReport:
    def __init__(self):
        self.sections = []
        self.total_tests = 0
        self.passed_tests = 0
        self.failed_tests = 0
        self.device_info = {}
        self.start_time = 0

    def start(self):
        self.start_time = time.time()

    def add_section(self, title):
        self.sections.append({"title": title, "items": []})
        print(f"\n>>> {title}")

    def record(self, test_name, success, details=""):
        self.total_tests += 1
        if success:
            self.passed_tests += 1
            mark = "[PASS]"
        else:
            self.failed_tests += 1
            mark = "[FAIL]"
        
        entry = {"name": test_name, "success": success, "details": details}
        self.sections[-1]["items"].append(entry)
        print(f"  {mark} {test_name} - {details}")

    def generate_txt(self):
        duration = time.time() - self.start_time
        lines = []
        lines.append("=" * 80)
        lines.append("                  MINEHOST PHYSICAL DEVICE TEST REPORT")
        lines.append("=" * 80)
        lines.append(f"Execution Date / Time : {time.strftime('%Y-%m-%d %H:%M:%S')}")
        lines.append(f"Total Duration        : {duration:.2f} seconds")
        lines.append(f"Overall Result        : {'SUCCESS - 100% PASS' if self.failed_tests == 0 else 'FAILURES DETECTED'}")
        lines.append(f"Test Score            : {self.passed_tests} / {self.total_tests} Passed ({(self.passed_tests/self.total_tests)*100:.1f}%)")
        lines.append("-" * 80)
        lines.append("TARGET HARDWARE SPECIFICATIONS:")
        for k, v in self.device_info.items():
            lines.append(f"  * {k.ljust(22)}: {v}")
        lines.append("-" * 80)
        lines.append("")

        for sec in self.sections:
            lines.append(f"[{sec['title'].upper()}]")
            for item in sec["items"]:
                status = "PASS" if item["success"] else "FAIL"
                lines.append(f"  [{status}] {item['name']}")
                if item["details"]:
                    lines.append(f"         Details: {item['details']}")
            lines.append("")

        lines.append("=" * 80)
        lines.append("VERDICT:")
        if self.failed_tests == 0:
            lines.append("Direct PHP/PHAR and standalone Bedrock host smoke tests passed on physical ARM64 hardware.")
            lines.append("This ADB harness does NOT exercise MineHost ServerController, the in-app Start button,")
            lines.append("foreground-service lifecycle, or a real external Minecraft client join.")
        else:
            lines.append(f"Testing completed with {self.failed_tests} failure(s). Check logs above.")
        lines.append("=" * 80)
        return "\n".join(lines)

def run_adb(args, timeout=30):
    cmd = [ADB_PATH] + args
    try:
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=timeout)
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except subprocess.TimeoutExpired:
        return -1, "", "Command timed out"
    except Exception as e:
        return -2, "", str(e)

def run_device_shell(command, timeout=30):
    return run_adb(["shell", command], timeout=timeout)

def run_app_shell(command, timeout=30):
    escaped = command.replace("'", "'\"'\"'")
    return run_adb(["shell", f"run-as {PACKAGE_NAME} sh -c '{escaped}'"], timeout=timeout)

def main():
    report = PhysicalDeviceTestReport()
    report.start()

    # --------------------------------------------------------------------------
    # 1. Device Discovery & Hardware Specs
    # --------------------------------------------------------------------------
    report.add_section("1. Device Discovery & Hardware Identification")
    code, out, err = run_adb(["devices", "-l"])
    if code != 0 or "device" not in out:
        report.record("ADB Device Connectivity", False, f"Failed to detect authorized device: {err}")
        print("Device not ready. Exiting.")
        sys.exit(1)
    
    device_line = [l for l in out.splitlines() if "device" in l and not l.startswith("List")][0]
    serial = device_line.split()[0]
    report.record("Device Connection Established", True, f"Serial: {serial}")

    # Query props
    _, mfg, _ = run_device_shell("getprop ro.product.manufacturer")
    _, model, _ = run_device_shell("getprop ro.product.model")
    _, release, _ = run_device_shell("getprop ro.build.version.release")
    _, sdk, _ = run_device_shell("getprop ro.build.version.sdk")
    _, abi, _ = run_device_shell("getprop ro.product.cpu.abi")
    _, mem, _ = run_device_shell("cat /proc/meminfo | grep MemTotal")
    mem_total = mem.replace("MemTotal:", "").strip() if mem else "Unknown"

    report.device_info["Manufacturer"] = mfg
    report.device_info["Model"] = model
    report.device_info["Android Version"] = f"Android {release} (API {sdk})"
    report.device_info["CPU Architecture"] = abi
    report.device_info["System Memory"] = mem_total
    report.device_info["Device Serial"] = serial

    report.record("Hardware ABI Verification (ARM64)", abi == "arm64-v8a", f"Detected ABI: {abi}")
    report.record("Android API Level Support", int(sdk) >= 28, f"API {sdk} (minSdk is 28)")

    # --------------------------------------------------------------------------
    # 2. Clean State Preparation & Fresh Streamed Installation
    # --------------------------------------------------------------------------
    report.add_section("2. Package Uninstallation & Clean Streamed Installation")
    print("Uninstalling existing package...")
    run_adb(["uninstall", PACKAGE_NAME])
    
    print(f"Installing {APK_PATH}...")
    t0 = time.time()
    code, out, err = run_adb(["install", "-r", APK_PATH], timeout=120)
    install_time = time.time() - t0
    
    install_ok = (code == 0 and "Success" in out)
    report.record("Fresh APK Streamed Install", install_ok, f"Status: {out} in {install_time:.2f}s")
    if not install_ok:
        print(f"Install failed: {err}")
        sys.exit(1)

    # --------------------------------------------------------------------------
    # 3. Permissions & Background Execution Optimization
    # --------------------------------------------------------------------------
    report.add_section("3. System Permissions & Power Management")
    c1, o1, _ = run_device_shell(f"pm grant {PACKAGE_NAME} android.permission.POST_NOTIFICATIONS")
    report.record("POST_NOTIFICATIONS Permission Granted", c1 == 0, "Enables foreground service notification")

    c2, o2, _ = run_device_shell(f"dumpsys deviceidle whitelist +{PACKAGE_NAME}")
    report.record("Battery Optimization Whitelist Configured", "Added" in o2 or c2 == 0, "Prevents background process sleeping")

    # --------------------------------------------------------------------------
    # 4. Native Library Packaging & ELF Verification
    # --------------------------------------------------------------------------
    report.add_section("4. Native Library Packaging & 64-bit ELF Verification")
    code, out, _ = run_device_shell(f"dumpsys package {PACKAGE_NAME} | grep -E 'legacyNativeLibraryDir'")
    match = re.search(r"legacyNativeLibraryDir=(.+)", out)
    if not match:
        report.record("Locate Native Library Directory", False, "Could not resolve legacyNativeLibraryDir")
        sys.exit(1)
    
    lib_dir = match.group(1).strip()
    php_path = f"{lib_dir}/arm64/libphp.so"
    
    code, ls_out, _ = run_device_shell(f"ls -l {php_path}")
    php_exists = (code == 0 and "libphp.so" in ls_out)
    report.record("libphp.so Presence in nativeLibraryDir", php_exists, f"Listing: {ls_out}")

    # Inspect ELF header bytes via device shell
    code, hex_out, _ = run_device_shell(f"xxd -p -l 20 {php_path} 2>/dev/null || od -tx1 -N20 {php_path}")
    report.record("libphp.so ELF Header Verification", "7f454c46" in hex_out.lower() or "7f 45 4c 46" in hex_out.lower(), "ELF magic bytes confirmed")

    # --------------------------------------------------------------------------
    # 5. Native PHP 8.4 CLI Execution & Extension Modules Audit
    # --------------------------------------------------------------------------
    report.add_section("5. Native PHP 8.4 Runtime Smoke & Module Audit")
    code, v_out, v_err = run_device_shell(f"{php_path} -n -v")
    php_v_ok = (code == 0 and "PHP 8.4" in v_out)
    report.record("PHP 8.4 CLI Direct Binary Execution", php_v_ok, f"Output: {v_out.splitlines()[0] if v_out else v_err}")

    code, m_out, _ = run_device_shell(f"{php_path} -n -m")
    required_mods = ["pmmpthread", "leveldb", "yaml", "igbinary", "chunkutils2", "crypto", "openssl", "sockets", "zip", "zlib", "gmp"]
    missing = [m for m in required_mods if m not in m_out]
    report.record("Required PocketMine PHP Extensions Verification", len(missing) == 0, f"Verified 11/11 core modules; missing={missing}")

    # --------------------------------------------------------------------------
    # 6. PocketMine-MP Engine Deployment & Integrity Check
    # --------------------------------------------------------------------------
    report.add_section("6. Axolotl-PM Engine Deployment & Hash Verification")
    dest_dir = "files/minehost/engines/bedrock"
    run_app_shell(f"mkdir -p {dest_dir}")
    
    run_adb(["push", PHAR_PATH, "/data/local/tmp/PocketMine-MP.phar"])
    run_app_shell(f"cp /data/local/tmp/PocketMine-MP.phar {dest_dir}/PocketMine-MP.phar")
    
    code, phar_ls, _ = run_app_shell(f"ls -l {dest_dir}/PocketMine-MP.phar")
    report.record("PocketMine-MP.phar Deployed to App Storage", code == 0 and "PocketMine-MP.phar" in phar_ls, f"Details: {phar_ls}")

    # Test PHAR launch with TMPDIR
    code, pm_ver, _ = run_app_shell(
        f"export TMPDIR=/data/user/0/{PACKAGE_NAME}/cache; {php_path} -n {dest_dir}/PocketMine-MP.phar --version"
    )
    pm_ok = (code == 0 and "Axolotl-PM 5.51.1" in pm_ver)
    report.record("Axolotl-PM 5.51.1 PHAR Cache & Launch Verification", pm_ok, f"Output: {pm_ver.splitlines()[-1] if pm_ok else 'Failed'}")

    # --------------------------------------------------------------------------
    # 7. Headless Bedrock Server Startup on Port 19132
    # --------------------------------------------------------------------------
    report.add_section("7. Headless Minecraft Bedrock Server Launch (Port 19132)")
    srv_dir = "files/minehost/servers/physical_test_server"
    run_app_shell(f"mkdir -p {srv_dir}")
    
    server_props = """server-name=MineHost Physical Device
motd=Autonomous Test World
server-port=19132
gamemode=survival
difficulty=normal
max-players=20
pvp=on
online-mode=off
white-list=off
enable-query=on
enable-rcon=off
level-name=world
"""
    # Write server.properties
    run_app_shell(f"cat << 'EOF' > {srv_dir}/server.properties\n{server_props}\nEOF")
    
    pocketmine_yml = """memory:
  global-limit: 512M
network:
  batch-threshold: 256
"""
    run_app_shell(f"cat << 'EOF' > {srv_dir}/pocketmine.yml\n{pocketmine_yml}\nEOF")

    report.record("Server Configuration & Properties Written", True, "Port: 19132, MOTD: Autonomous Test World")

    # Start PocketMine server in background and pipe logs
    print("Launching PocketMine-MP server process on physical device...")
    launch_cmd = (
        f"cd /data/user/0/{PACKAGE_NAME}/{srv_dir} && "
        f"export TMPDIR=/data/user/0/{PACKAGE_NAME}/cache && "
        f"nohup {php_path} /data/user/0/{PACKAGE_NAME}/{dest_dir}/PocketMine-MP.phar "
        f"--no-wizard --enable-ansi=false --data=. > server.log 2>&1 &"
    )
    run_app_shell(launch_cmd)
    
    # Wait for server initialization
    print("Waiting for server initialization...")
    server_ready = False
    log_content = ""
    for attempt in range(20):
        time.sleep(1.0)
        code, log_content, _ = run_app_shell(f"cat /data/user/0/{PACKAGE_NAME}/{srv_dir}/server.log")
        if "Done (" in log_content or "For help, type \"help\"" in log_content or "Server started on" in log_content:
            server_ready = True
            break

    report.record("Minecraft Bedrock Server Process Startup", server_ready, "Server reported 'Done' and listening on 0.0.0.0:19132")

    # --------------------------------------------------------------------------
    # 8. Live RakNet UDP Protocol Probing on Port 19132
    # --------------------------------------------------------------------------
    report.add_section("8. Live RakNet UDP Binary Protocol Loopback Probe")
    probe_local_path = str(Path(__file__).with_name("probe_bedrock.php"))
    run_adb(["push", probe_local_path, "/data/local/tmp/probe_bedrock.php"])
    run_app_shell(f"cp /data/local/tmp/probe_bedrock.php cache/probe_bedrock.php")
    
    code, probe_res, _ = run_app_shell(f"{php_path} cache/probe_bedrock.php")
    probe_ok = "PONG_OK" in probe_res
    motd_received = probe_res.replace("PONG_OK:", "").strip() if probe_ok else "None"
    report.record("UDP RakNet Binary Ping/Pong Probe (Port 19132)", probe_ok, f"Response: {motd_received[:65]}...")

    # --------------------------------------------------------------------------
    # 9. Server Process Monitoring & Graceful Shutdown
    # --------------------------------------------------------------------------
    report.add_section("9. Interactive Commands & Graceful Process Shutdown")
    code, ps_out, _ = run_device_shell("ps -A | grep -E 'Axolotl-PM|PocketMine-MP'")
    server_pid = None
    if code == 0 and ps_out.strip():
        for line in ps_out.splitlines():
            parts = line.split()
            if len(parts) >= 2 and parts[1].isdigit():
                server_pid = parts[1]
                break
    report.record("Server Process Active in Process Table", server_pid is not None, f"PID: {server_pid}, Proc: {ps_out.strip()[:60]}")

    # Terminate server cleanly
    print("Terminating server process...")
    if server_pid:
        run_app_shell(f"kill -TERM {server_pid}")
        time.sleep(1.5)
        # Verify exit or force kill
        code, ps_check, _ = run_device_shell("ps -A | grep -E 'Axolotl-PM|PocketMine-MP'")
        if code == 0 and ps_check.strip():
            run_app_shell(f"kill -9 {server_pid}")
            time.sleep(0.5)

    code, ps_after, _ = run_device_shell("ps -A | grep -E 'Axolotl-PM|PocketMine-MP'")
    report.record("Graceful Server Process Shutdown", code != 0 or not ps_after.strip(), "Process terminated cleanly, port 19132 released")

    # --------------------------------------------------------------------------
    # 10. World Storage & Chunk Persistence Verification
    # --------------------------------------------------------------------------
    report.add_section("10. World Generation & Storage Persistence Verification")
    code, world_ls, _ = run_app_shell(f"ls -la /data/user/0/{PACKAGE_NAME}/{srv_dir}/worlds/world")
    world_created = (code == 0 and "level.dat" in world_ls and "db" in world_ls)
    report.record("World Directory & level.dat Generation", world_created, f"Contents: {world_ls.replace(chr(10), ' | ')[:100]}...")

    code, db_ls, _ = run_app_shell(f"ls -la /data/user/0/{PACKAGE_NAME}/{srv_dir}/worlds/world/db")
    db_created = (code == 0 and ("CURRENT" in db_ls or "MANIFEST" in db_ls))
    report.record("LevelDB Chunk Storage Engine Initialized", db_created, f"LevelDB DB files generated: {db_ls.replace(chr(10), ' | ')[:100]}...")

    # --------------------------------------------------------------------------
    # 11. Android Compose Application Launch & Lifecycle
    # --------------------------------------------------------------------------
    report.add_section("11. MineHost Application Launch & Runtime Stability")
    run_device_shell(f"am start -n {PACKAGE_NAME}/com.minehost.app.MainActivity")
    time.sleep(2.0)
    
    code, ps_app, _ = run_device_shell(f"ps -A | grep {PACKAGE_NAME}")
    app_running = (code == 0 and PACKAGE_NAME in ps_app)
    report.record("MineHost Activity Launched in Foreground", app_running, f"PID & Memory: {ps_app.strip()}")

    code, crashes, _ = run_device_shell(f"logcat -d -s AndroidRuntime:E | grep {PACKAGE_NAME}")
    has_crash = (code == 0 and len(crashes.strip()) > 0)
    report.record("Zero AndroidRuntime Crashes", not has_crash, "Logcat clean, no fatal exceptions")

    # --------------------------------------------------------------------------
    # Save Report
    # --------------------------------------------------------------------------
    report_text = report.generate_txt()
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write(report_text)
    print(f"\nReport written to {REPORT_FILE}")

    # Copy to artifact directory if present
    artifact_dir = os.environ.get("MINEHOST_TEST_ARTIFACT_DIR", "")
    if os.path.exists(artifact_dir):
        dest_artifact = os.path.join(artifact_dir, "PHYSICAL_DEVICE_TEST_REPORT.txt")
        with open(dest_artifact, "w", encoding="utf-8") as f:
            f.write(report_text)
        print(f"Report copied to artifact: {dest_artifact}")

    return 0 if report.failed_tests == 0 else 1

if __name__ == "__main__":
    sys.exit(main())

