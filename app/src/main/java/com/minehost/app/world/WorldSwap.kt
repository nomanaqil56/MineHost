package com.minehost.app.world

import java.io.File

/**
 * Replaces a world directory with an already-verified replacement without ever leaving the user
 * without a world.
 *
 * Steps: move the current world aside, move the replacement into place, and only then delete the old
 * world. If any step fails the previous world is put back. Pure java.io so it is unit-testable.
 */
object WorldSwap {

    sealed class Result {
        object Success : Result()
        /** [message] explains what failed; the previous world is intact in every failure case. */
        data class Failed(val message: String) : Result()
    }

    /**
     * @param worldDir current world location (may not exist)
     * @param replacementDir fully extracted and validated replacement, on the same filesystem
     * @param asideDir scratch location used to hold the old world during the swap
     * @param deleteDir deletes a directory tree (injected so callers/tests can log or fail it)
     */
    fun replace(
        worldDir: File,
        replacementDir: File,
        asideDir: File,
        deleteDir: (File) -> Unit = { it.deleteRecursively() }
    ): Result {
        val hadOldWorld = worldDir.exists()
        if (hadOldWorld && !worldDir.renameTo(asideDir)) {
            deleteDir(replacementDir)
            return Result.Failed("Could not move the current world aside. Restore cancelled; nothing was changed.")
        }
        if (!replacementDir.renameTo(worldDir)) {
            val restored = !hadOldWorld || asideDir.renameTo(worldDir)
            deleteDir(replacementDir)
            return Result.Failed(
                if (restored) {
                    "Could not move the restored world into place. Your previous world was put back."
                } else {
                    "Could not move the restored world into place AND could not put the previous world back. " +
                        "Your previous world is preserved at: ${asideDir.absolutePath}"
                }
            )
        }
        if (hadOldWorld) deleteDir(asideDir)
        return Result.Success
    }
}
