import os
import sys
import zipfile
import urllib.request
import shutil
from pathlib import Path

TOOLS_DIR = Path(".tools").resolve()
JDK_DIR = TOOLS_DIR / "jdk-17"
SDK_DIR = TOOLS_DIR / "android-sdk"

def download_file(url, target_path, desc):
    print(f"Downloading {desc}...", flush=True)
    target_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = target_path.with_suffix(".tmp")
    
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    with urllib.request.urlopen(req) as response, open(temp_path, "wb") as out_file:
        shutil.copyfileobj(response, out_file)
        
    if target_path.exists():
        target_path.unlink()
    temp_path.rename(target_path)
    print(f"Downloaded {desc} ({target_path.stat().st_size // (1024*1024)} MB)", flush=True)

def setup_jdk():
    if (JDK_DIR / "bin" / "java.exe").exists():
        print(f"JDK 17 already installed at {JDK_DIR}", flush=True)
        return
        
    jdk_zip = TOOLS_DIR / "jdk17.zip"
    jdk_url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.10_7.zip"
    download_file(jdk_url, jdk_zip, "OpenJDK 17")
    
    print("Extracting OpenJDK 17...", flush=True)
    with zipfile.ZipFile(jdk_zip, "r") as zf:
        zf.extractall(TOOLS_DIR)
        
    extracted_dirs = [d for d in TOOLS_DIR.iterdir() if d.is_dir() and "jdk-17" in d.name and d != JDK_DIR]
    if extracted_dirs:
        if JDK_DIR.exists():
            shutil.rmtree(JDK_DIR)
        extracted_dirs[0].rename(JDK_DIR)
        
    if jdk_zip.exists():
        jdk_zip.unlink()
    print(f"OpenJDK 17 ready at {JDK_DIR}", flush=True)

def setup_android_sdk():
    cmdline_dir = SDK_DIR / "cmdline-tools" / "latest"
    if (cmdline_dir / "bin" / "sdkmanager.bat").exists():
        print(f"Android Command-Line Tools already installed at {cmdline_dir}", flush=True)
    else:
        sdk_zip = TOOLS_DIR / "commandlinetools.zip"
        sdk_url = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
        download_file(sdk_url, sdk_zip, "Android Command-Line Tools")
        
        print("Extracting Android SDK Command-Line Tools...", flush=True)
        temp_extract = TOOLS_DIR / "cmdline_temp"
        with zipfile.ZipFile(sdk_zip, "r") as zf:
            zf.extractall(temp_extract)
            
        cmdline_dir.parent.mkdir(parents=True, exist_ok=True)
        if cmdline_dir.exists():
            shutil.rmtree(cmdline_dir)
        (temp_extract / "cmdline-tools").rename(cmdline_dir)
        shutil.rmtree(temp_extract, ignore_errors=True)
        if sdk_zip.exists():
            sdk_zip.unlink()
        print(f"Android Command-Line Tools ready at {cmdline_dir}", flush=True)

    sdk_path_str = str(SDK_DIR).replace("\\", "\\\\")
    with open("local.properties", "w") as f:
        f.write(f"sdk.dir={sdk_path_str}\n")
    print("Created local.properties", flush=True)

if __name__ == "__main__":
    setup_jdk()
    setup_android_sdk()
    print("Setup completed successfully!", flush=True)
