package com.minehost.app.core.util

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.IOException

object FileUtils {

    /**
     * Returns the available free bytes on the storage partition hosting the provided directory.
     */
    fun getAvailableStorageBytes(directory: File): Long {
        return try {
            val stat = StatFs(directory.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Recursively calculates the total size in bytes of a directory or file.
     */
    fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        if (directory.isFile) return directory.length()

        var totalSize = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }
        return totalSize
    }

    /**
     * Recursively copies a source directory to a destination directory.
     */
    @Throws(IOException::class)
    fun copyDirectory(sourceDir: File, targetDir: File) {
        if (!sourceDir.exists()) return
        if (!targetDir.exists()) targetDir.mkdirs()

        sourceDir.listFiles()?.forEach { file ->
            val destFile = File(targetDir, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile)
            } else {
                file.copyTo(destFile, overwrite = true)
            }
        }
    }

    /**
     * Recursively deletes a directory and all its contents safely.
     */
    fun deleteDirectoryRecursively(directory: File): Boolean {
        if (!directory.exists()) return true
        return directory.deleteRecursively()
    }
}
