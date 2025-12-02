package com.pedro.streamer.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File

object PathUtils {

    fun getRecordPath(): File {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Streamer")
        return folder
    }

    fun updateGallery(context: Context, path: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    fun toast(context: Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

