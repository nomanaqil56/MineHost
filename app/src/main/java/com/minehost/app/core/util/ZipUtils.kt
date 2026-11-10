package com.minehost.app.core.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    /**
     * Compresses a folder into a ZIP archive, calculating its SHA256 checksum during compression.
     */
    @Throws(IOException::class)
    fun zipDirectory(
        sourceDir: File,
        targetZipFile: File,
        onProgress: ((Float) -> Unit)? = null,
        additionalFiles: Map<String, File> = emptyMap()
    ): String {
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw IllegalArgumentException("Source must be an existing directory: ${sourceDir.absolutePath}")
        }
        targetZipFile.parentFile?.mkdirs()

        val allFiles = sourceDir.walkTopDown().filter { it.isFile }.toList()
        val validatedAdditionalFiles = additionalFiles.map { (entryName, file) ->
            val normalizedName = entryName.replace('\\', '/')
            require(
                normalizedName.isNotBlank() &&
                    !normalizedName.startsWith("/") &&
                    normalizedName.split('/').none { it.isBlank() || it == "." || it == ".." }
            ) { "Invalid additional ZIP entry name: $entryName" }
            require(file.isFile) { "Additional ZIP source must be an existing file: ${file.absolutePath}" }
            normalizedName to file
        }
        val existingNames = allFiles.map { it.relativeTo(sourceDir).path.replace('\\', '/') }.toSet()
        require(validatedAdditionalFiles.none { it.first in existingNames }) {
            "An additional ZIP entry duplicates a file already in the source directory."
        }
        val totalBytes = allFiles.sumOf { it.length() } + validatedAdditionalFiles.sumOf { it.second.length() }
        var bytesProcessed = 0L

        val digest = MessageDigest.getInstance("SHA-256")
        val tempZipFile = File(targetZipFile.parentFile, ".${targetZipFile.name}.${UUID.randomUUID()}.tmp")

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile))).use { zos ->
            val buffer = ByteArray(8192)

            for (file in allFiles) {
                val relativePath = file.relativeTo(sourceDir).path.replace('\\', '/')
                val entry = ZipEntry(relativePath)
                zos.putNextEntry(entry)

                BufferedInputStream(FileInputStream(file)).use { bis ->
                    var count: Int
                    while (bis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        bytesProcessed += count
                        if (totalBytes > 0) {
                            onProgress?.invoke(bytesProcessed.toFloat() / totalBytes)
                        }
                    }
                }
                zos.closeEntry()
            }

            // Append explicitly selected server metadata (for example, NetherNet identity keys)
            // after the world tree. Entry names are validated above and never derived from untrusted input.
            for ((entryName, file) in validatedAdditionalFiles) {
                zos.putNextEntry(ZipEntry(entryName))
                BufferedInputStream(FileInputStream(file)).use { bis ->
                    var count: Int
                    while (bis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        bytesProcessed += count
                        if (totalBytes > 0) {
                            onProgress?.invoke(bytesProcessed.toFloat() / totalBytes)
                        }
                    }
                }
                zos.closeEntry()
            }
            }

            if (targetZipFile.exists()) {
                throw IOException("Refusing to overwrite an existing ZIP archive: ${targetZipFile.absolutePath}")
            }
            if (!tempZipFile.renameTo(targetZipFile)) {
                throw IOException("Could not atomically publish ZIP archive: ${targetZipFile.absolutePath}")
            }
        } catch (e: Exception) {
            tempZipFile.delete()
            throw e
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Recomputes the content checksum of an archive created by [zipDirectory]: the SHA-256 of all
     * file entry bytes, in archive order. Throws if the archive is unreadable or corrupt, so it
     * doubles as an integrity check.
     */
    @Throws(IOException::class)
    fun computeContentChecksum(zipFile: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            val buffer = ByteArray(8192)
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    var len: Int
                    while (zis.read(buffer).also { len = it } != -1) {
                        digest.update(buffer, 0, len)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Unzips an archive into a target directory safely, guarding against Zip Slip path traversal.
     */
    @Throws(IOException::class)
    fun unzip(
        zipFile: File,
        destDir: File,
        onProgress: ((Float) -> Unit)? = null
    ) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val canonicalDestDirPath = destDir.canonicalPath

        val totalLength = zipFile.length()
        var bytesRead = 0L

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            val buffer = ByteArray(8192)
            var entry: ZipEntry? = zis.nextEntry

            while (entry != null) {
                val newFile = File(destDir, entry.name)
                val canonicalNewFilePath = newFile.canonicalPath

                // Zip Slip vulnerability check
                if (!canonicalNewFilePath.startsWith(canonicalDestDirPath + File.separator) &&
                    canonicalNewFilePath != canonicalDestDirPath
                ) {
                    throw SecurityException("Zip entry attempted path traversal: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(newFile)).use { bos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } != -1) {
                            bos.write(buffer, 0, len)
                            bytesRead += len
                            if (totalLength > 0) {
                                onProgress?.invoke((bytesRead.toFloat() / totalLength).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}

