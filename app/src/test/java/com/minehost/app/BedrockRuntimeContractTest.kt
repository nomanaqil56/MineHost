package com.minehost.app

import com.minehost.app.server.BedrockRuntimeContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class BedrockRuntimeContractTest {
    @Test
    fun acceptsOnlyExactPinnedAxolotlReleaseMetadata() {
        assertNull(BedrockRuntimeContract.validateReleaseMetadata(
            tag = BedrockRuntimeContract.PHAR_RELEASE_TAG,
            assetName = BedrockRuntimeContract.PHAR_ASSET,
            assetUrl = BedrockRuntimeContract.PHAR_DOWNLOAD_URL,
            publishedDigest = BedrockRuntimeContract.PHAR_PUBLISHED_DIGEST
        ))
    }

    @Test
    fun rejectsMutableOrUnexpectedReleaseMetadata() {
        assertNotNull(BedrockRuntimeContract.validateReleaseMetadata("5.51.2", BedrockRuntimeContract.PHAR_ASSET, BedrockRuntimeContract.PHAR_DOWNLOAD_URL, BedrockRuntimeContract.PHAR_PUBLISHED_DIGEST))
        assertNotNull(BedrockRuntimeContract.validateReleaseMetadata(BedrockRuntimeContract.PHAR_RELEASE_TAG, BedrockRuntimeContract.PHAR_ASSET, "https://attacker.invalid/server.phar", BedrockRuntimeContract.PHAR_PUBLISHED_DIGEST))
        assertNotNull(BedrockRuntimeContract.validateReleaseMetadata(BedrockRuntimeContract.PHAR_RELEASE_TAG, BedrockRuntimeContract.PHAR_ASSET, BedrockRuntimeContract.PHAR_DOWNLOAD_URL, null))
        assertNotNull(BedrockRuntimeContract.validateReleaseMetadata(BedrockRuntimeContract.PHAR_RELEASE_TAG, BedrockRuntimeContract.PHAR_ASSET, BedrockRuntimeContract.PHAR_DOWNLOAD_URL, "sha256:" + "0".repeat(64)))
    }

    @Test
    fun phpModuleCheckFindsMissingRequiredExtensions() {
        val output = """
            [PHP Modules]
            curl
            gmp
            openssl
            phar
            sockets
            yaml
            [Zend Modules]
        """.trimIndent()
        val missing = BedrockRuntimeContract.missingRequiredModules(output)
        assertEquals(setOf("igbinary", "leveldb", "mbstring", "zip", "zlib", "chunkutils2", "crypto", "encoding", "morton", "pmmpthread", "webrtc"), missing)
    }

    @Test
    fun phpModuleCheckAcceptsCompleteRequiredModuleList() {
        val output = (listOf("[PHP Modules]") + BedrockRuntimeContract.REQUIRED_PHP_MODULES).joinToString("\n")
        assertEquals(emptySet<String>(), BedrockRuntimeContract.missingRequiredModules(output))
    }
}