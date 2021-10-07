package com.raywenderlich.placebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

object ImageUtils {

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String) {

        val stream = ByteArrayOutputStream()

        // The quality argument is ignored by the PNG format
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val bytes = stream.toByteArray()

        saveBytesToFile(context, bytes, filename)
    }

    private fun saveBytesToFile(context: Context, bytes: ByteArray, filename: String) {
        val outputStream: FileOutputStream

        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBitmapFromFile(context: Context, filename: String): Bitmap? {
        val filePath = File(context.filesDir, filename).absolutePath
        return BitmapFactory.decodeFile(filePath)
    }

    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val filename = "PlaceBook_" + timestamp + "_"
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename,".jpg",filesDir)
    }

    fun decodeFileToSize(
        filePath: String,
        width: Int,
        height: Int,
    ): Bitmap {
        val options = BitmapFactory.Options()

        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
// 2
        options.inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight, width, height)
// 3
        options.inJustDecodeBounds = false
// 4
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @Throws(IOException::class)
    fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
        val path = selectedImage.path
        val ei: ExifInterface = when {
            Build.VERSION.SDK_INT > 23 && input != null -> ExifInterface(input)
            path != null -> ExifInterface(path)
            else -> null
        } ?: return img

        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {

            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90.0f) ?: img
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180.0f) ?: img
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270.0f) ?: img
            else -> img

        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width,
            img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}