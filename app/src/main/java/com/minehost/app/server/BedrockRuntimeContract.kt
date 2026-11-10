package com.minehost.app.server

/**
 * Immutable compatibility and integrity contract for the Axolotl-PM 5.51.1 runtime.
 * Values are pinned in source so mutable release metadata cannot silently change what is trusted.
 */
object BedrockRuntimeContract {
    const val PHAR_RELEASE_TAG = "5.51.1"
    const val PHAR_ASSET = "PocketMine-MP.phar"
    const val PHAR_SHA256 = "bcfbd0deff349c2b25b1688299a7d54f1fc8a080d2f95e19884f8e341319021a"
    const val PHAR_DOWNLOAD_URL =
        "https://github.com/axolotl-pm/PocketMine-MP/releases/download/5.51.1/PocketMine-MP.phar"
    const val PHAR_PUBLISHED_DIGEST = "sha256:$PHAR_SHA256"

    /**
     * The pinned Android CLI has PocketMine extensions compiled in. The upstream
     * php.ini attempts to load external modules not shipped with the static binary.
     * Use -n consistently for validation and server startup.
     */
    const val PHP_NO_INI_FLAG = "-n"

    fun serverArguments(pharPath: String, serverDir: String): List<String> = listOf(
        PHP_NO_INI_FLAG,
        pharPath,
        "--no-wizard",
        "--enable-ansi=false",
        "--data=$serverDir"
    )

    val REQUIRED_PHP_MODULES: Set<String> = setOf(
        "curl", "gmp", "igbinary", "leveldb", "mbstring", "openssl", "phar",
        "sockets", "yaml", "zip", "zlib", "chunkutils2", "crypto", "encoding",
        "morton", "pmmpthread", "webrtc"
    )

    fun validateReleaseMetadata(tag: String, assetName: String, assetUrl: String, publishedDigest: String?): String? {
        if (tag != PHAR_RELEASE_TAG) return "expected release $PHAR_RELEASE_TAG, received $tag"
        if (assetName != PHAR_ASSET) return "unexpected asset $assetName"
        if (assetUrl != PHAR_DOWNLOAD_URL) return "release asset URL does not match the pinned GitHub release URL"
        if (publishedDigest != PHAR_PUBLISHED_DIGEST) return "published SHA-256 digest is missing or differs from the pinned digest"
        return null
    }

    /** Parses php -m output, ignoring section headings and blank lines. */
    fun missingRequiredModules(moduleOutput: String): Set<String> {
        val installed = moduleOutput.lineSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && !it.startsWith("[") }
            .toSet()
        return REQUIRED_PHP_MODULES - installed
    }
}