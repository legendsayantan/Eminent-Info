package com.legendsayantan.eminentinfo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author legendsayantan
 */
class Images(val context: Context) {
    fun getProfilePic(ID: String): Bitmap? {
        return try {
            val fileInputStream: FileInputStream = context.openFileInput(ID.replace("/", "_"))
            val bitmap: Bitmap? = BitmapFactory.decodeStream(fileInputStream)
            fileInputStream.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun saveProfilePic(ID: String, bitmap: Bitmap): Boolean {
        //save to storage
        return try {
            val fileOutputStream: FileOutputStream =
                context.openFileOutput(ID.replace("/", "_"), Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun loadFromUrl(url: String): Bitmap? {
        return try {
            val inputStream = java.net.URL(url).openStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}