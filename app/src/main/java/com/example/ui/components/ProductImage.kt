package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R

@Composable
fun ProductThumbnail(
  imageUrl: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop
) {
  val source = imageUrl?.trim().orEmpty()
  when {
    source.startsWith("res://") -> {
      val resId = when (source) {
        "res://img_banner_fashion_sale" -> R.drawable.img_banner_fashion_sale
        "res://img_banner_choice_day" -> R.drawable.img_banner_choice_day
        "res://img_banner_home_living" -> R.drawable.img_banner_home_living
        else -> R.drawable.img_storefront_hero
      }
      Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
      )
    }

    source.startsWith("data:image") -> {
      val bitmap = remember(source) {
        runCatching {
          val base64Part = source.substringAfter("base64,", "")
          if (base64Part.isBlank()) null
          else {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
          }
        }.getOrNull()
      }
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = contentDescription,
          modifier = modifier,
          contentScale = contentScale
        )
      } else {
        FallbackProductBox(modifier)
      }
    }

    source.startsWith("http://") || source.startsWith("https://") || source.startsWith("content://") -> {
      AsyncImage(
        model = source,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
      )
    }

    else -> {
      Image(
        painter = painterResource(id = R.drawable.img_storefront_hero),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
      )
    }
  }
}

@Composable
private fun FallbackProductBox(modifier: Modifier) {
  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Rounded.ShoppingBag,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(32.dp)
    )
  }
}
