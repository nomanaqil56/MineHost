package com.minehost.app.core.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
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
        onProgress: ((Float) -> Unit)? = null
    ): String {
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw IllegalArgumentException("Source must be an existing directory: ${sourceDir.absolutePath}")
        }
        targetZipFile.parentFile?.mkdirs()

        val allFiles = sourceDir.walkTopDown().filter { it.isFile }.toList()
        val totalBytes = allFiles.sumOf { it.length() }
        var bytesProcessed = 0L

        val digest = MessageDigest.getInstance("SHA-256")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
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

