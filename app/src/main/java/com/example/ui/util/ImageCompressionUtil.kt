package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressionUtil {
  fun uriToCompressedDataUri(context: Context, uri: Uri, maxDim: Int = 600, quality: Int = 72): String? {
    return runCatching {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val original = BitmapFactory.decodeStream(stream) ?: return null
        val scale = max(original.width, original.height).toFloat() / maxDim.toFloat()
        val resized = if (scale > 1f) {
          val w = (original.width / scale).toInt().coerceAtLeast(1)
          val h = (original.height / scale).toInt().coerceAtLeast(1)
          Bitmap.createScaledBitmap(original, w, h, true)
        } else {
          original
        }
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
      }
    }.getOrNull()
  }
}
