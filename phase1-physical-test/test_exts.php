<?php
$required = [
    'curl', 'gmp', 'igbinary', 'leveldb', 'mbstring', 'openssl',
    'phar', 'sockets', 'yaml', 'zip', 'zlib', 'chunkutils2',
    'crypto', 'encoding', 'morton', 'pmmpthread', 'webrtc'
];

echo "================================================================================" . PHP_EOL;
echo "MINEHOST PHP 8.4 RUNTIME & EXTENSION ACCEPTANCE AUDIT" . PHP_EOL;
echo "================================================================================" . PHP_EOL;
echo "PHP Version:        " . PHP_VERSION . PHP_EOL;
echo "SAPI Name:          " . PHP_SAPI . PHP_EOL;
echo "ZTS (Thread Safe):  " . (PHP_ZTS ? "ENABLED (1)" : "DISABLED (0)") . PHP_EOL;
echo "PHP Binary OS:      " . PHP_OS . PHP_EOL;
echo "Integer Size:       " . (PHP_INT_SIZE * 8) . "-bit" . PHP_EOL;
echo "Configuration File: " . (php_ini_loaded_file() ?: "None (running with -n)") . PHP_EOL;
echo "================================================================================" . PHP_EOL . PHP_EOL;

echo "--- 17 REQUIRED AXOLOTL-PM EXTENSIONS AUDIT ---" . PHP_EOL;
$passed = 0;
$failed = 0;
$details = [];

foreach ($required as $ext) {
    $isLoaded = extension_loaded($ext);
    $version = phpversion($ext) ?: 'compiled-in';
    if ($isLoaded) {
        $passed++;
        $status = "PASS";
    } else {
        $failed++;
        $status = "FAIL";
    }
    printf(" [%-4s] %-14s | Version: %-15s | Status: %s\n", $status, $ext, $version, $isLoaded ? "LOADED" : "MISSING");
}

echo PHP_EOL;
echo "Audit Summary: $passed / " . count($required) . " required extensions present." . PHP_EOL;
echo "Audit Verdict: " . ($failed === 0 ? "100% PASS - ALL 17 EXTENSIONS VERIFIED" : "FAIL - $failed EXTENSIONS MISSING") . PHP_EOL;

