import os
import subprocess
import sys

JAVA_HOME = r"D:\Coding\Projects\Personal Projects\MineHost\.tools\jdk-17"
SDK_ROOT = r"D:\Coding\Projects\Personal Projects\MineHost\.tools\android-sdk"
SDKMANAGER = os.path.join(SDK_ROOT, "cmdline-tools", "latest", "bin", "sdkmanager.bat")

env = os.environ.copy()
env["JAVA_HOME"] = JAVA_HOME
env["ANDROID_HOME"] = SDK_ROOT

print(f"Using JAVA_HOME={JAVA_HOME}")
print(f"Using SDK_ROOT={SDK_ROOT}")

# Pre-accept licenses by creating the license files if needed or using --licenses
licenses_dir = os.path.join(SDK_ROOT, "licenses")
os.makedirs(licenses_dir, exist_ok=True)

# Standard Android SDK license hashes:
known_licenses = {
    "android-sdk-license": (
        "24333f8a63068bbb0a49de3689bf03f508d0e447\n"
        "d56f5185479d641e49dd6834241900552f03b7c7\n"
        "89338d0d9b181a9d948489fde1402973f0aa707e\n"
    ),
    "android-sdk-preview-license": "84831b9409646a256e301b10b6f38f9d404e9b9d\n",
    "intel-android-extra-license": "d975f751698a77b662f1254ddbeed3901e176f5a\n",
}

for name, content in known_licenses.items():
    lic_path = os.path.join(licenses_dir, name)
    if not os.path.exists(lic_path):
        with open(lic_path, "w") as f:
            f.write(content)
print("Known license hashes written.")

# Also run sdkmanager --licenses with 'y' input just to be 100% sure
try:
    p = subprocess.Popen(
        [SDKMANAGER, f"--sdk_root={SDK_ROOT}", "--licenses"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        env=env
    )
    # Send 'y\n' several times to accept any remaining licenses
    out, _ = p.communicate(input="y\ny\ny\ny\ny\ny\ny\ny\ny\n", timeout=30)
    print("License agreement output:")
    for line in out.splitlines()[-10:]:
        print(line)
except Exception as e:
    print("Notice during --licenses:", e)

# Now install the required packages: platforms;android-34 and build-tools;34.0.0
packages = ["platforms;android-34", "build-tools;34.0.0", "platform-tools"]
print(f"Installing packages: {packages}...")
cmd = [SDKMANAGER, f"--sdk_root={SDK_ROOT}"] + packages
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=env)

for line in proc.stdout:
    sys.stdout.write(line)
    sys.stdout.flush()

proc.wait()
if proc.returncode == 0:
    print("\nAndroid SDK components installed successfully!")
else:
    print(f"\nsdkmanager exited with code {proc.returncode}")
    sys.exit(proc.returncode)

