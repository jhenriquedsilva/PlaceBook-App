package com.raywenderlich.placebook.util

import android.content.Context
import java.io.File

/**
 * Feature: deleting a bookmark
 * Deletes a unique file
 * Called by deleteImage in Bookmark
 */
object FileUtils {
    fun deleteFile(context: Context, filename: String) {
        val dir = context.filesDir
        // Creates the file
        val file = File(dir, filename)
        file.delete()
    }
}