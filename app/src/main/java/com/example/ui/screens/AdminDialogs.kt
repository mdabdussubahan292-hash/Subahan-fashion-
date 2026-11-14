package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.util.ImageCompressionUtil
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorDialog(
  isBangla: Boolean,
  initialProduct: Product?,
  initialCostPrice: Double?,
  categories: List<Category>,
  onDismiss: () -> Unit,
  onSave: (Product, Double?) -> Unit
) {
  val context = LocalContext.current

  var sku by remember { mutableStateOf(initialProduct?.sku ?: "SF-${System.currentTimeMillis() % 100000}") }
  var name by remember { mutableStateOf(initialProduct?.name ?: "") }
  var nameBn by remember { mutableStateOf(initialProduct?.nameBn ?: "") }
  var selectedCatId by remember {
    mutableStateOf(initialProduct?.categoryId?.ifBlank { null } ?: categories.firstOrNull()?.id ?: "cat_fashion_acc")
  }
  var priceStr by remember { mutableStateOf(initialProduct?.price?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
  var prevPriceStr by remember { mutableStateOf(initialProduct?.previousPrice?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
  var stockStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "20") }
  var costStr by remember { mutableStateOf(initialCostPrice?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
  var colorsStr by remember { mutableStateOf(initialProduct?.colors?.joinToString(", ") ?: "Black, Pink") }
  var sizesStr by remember { mutableStateOf(initialProduct?.sizes?.joinToString(", ") ?: "Free Size") }
  var weightStr by remember { mutableStateOf(initialProduct?.weightKg?.toString() ?: "0.4") }
  var deliveryOverrideStr by remember {
    mutableStateOf(initialProduct?.deliveryChargeOverride?.takeIf { it >= 0 }?.toLong()?.toString() ?: "")
  }
  var description by remember { mutableStateOf(initialProduct?.description ?: "") }
  var details by remember { mutableStateOf(initialProduct?.details ?: "") }
  var videoUrl by remember { mutableStateOf(initialProduct?.videoUrl ?: "") }
  var customImgUrl by remember { mutableStateOf("") }

  val images = remember {
    mutableStateListOf<String>().apply {
      addAll(initialProduct?.imageUrls.orEmpty())
    }
  }

  var isPublished by remember { mutableStateOf(initialProduct?.isPublished ?: true) }
  var isFeatured by remember { mutableStateOf(initialProduct?.isFeatured ?: false) }
  var isBestSelling by remember { mutableStateOf(initialProduct?.isBestSelling ?: false) }
  var isNewArrival by remember { mutableStateOf(initialProduct?.isNewArrival ?: true) }
  var isFlashSale by remember { mutableStateOf(initialProduct?.isFlashSale ?: false) }
  var isChoiceDay by remember { mutableStateOf(initialProduct?.isChoiceDay ?: false) }
  var isBundleEligible by remember { mutableStateOf(initialProduct?.isBundleEligible ?: false) }

  // Android zero-permission Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null && images.size < 6) {
      val dataUri = ImageCompressionUtil.uriToCompressedDataUri(context, uri)
      if (dataUri != null) {
        images.add(dataUri)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialProduct == null) {
          if (isBangla) "নতুন পণ্য যোগ করুন" else "Add New Product"
        } else {
          if (isBangla) "পণ্য আপডেট করুন" else "Edit Product"
        },
        fontWeight = FontWeight.ExtraBold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Photo picker + gallery preview
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.fillMaxWidth().testTag("admin_pick_product_photo_btn")
        ) {
          Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(if (isBangla) "গ্যালারি থেকে ছবি আপলোড (${images.size}/6)" else "Upload Photo from Mobile (${images.size}/6)")
        }

        if (images.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(images) { idx, img ->
              Box(modifier = Modifier.size(72.dp)) {
                ProductThumbnail(
                  imageUrl = img,
                  contentDescription = "Product Photo",
                  modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(8.dp))
                )
                IconButton(
                  onClick = { images.removeAt(idx) },
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                ) {
                  Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = CircleShape,
                    modifier = Modifier.size(22.dp)
                  ) {
                    Icon(
                      Icons.Rounded.Close,
                      contentDescription = "Remove photo",
                      tint = Color.White,
                      modifier = Modifier.padding(3.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = customImgUrl,
            onValueChange = { customImgUrl = it },
            label = { Text("Image URL (optional)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          TextButton(
            onClick = {
              if (customImgUrl.isNotBlank() && images.size < 6) {
                images.add(customImgUrl.trim())
                customImgUrl = ""
              }
            }
          ) { Text("+Add") }
        }

        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU / Product ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("admin_prod_name_input"))
        OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("পণ্যের নাম (বাংলা)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text(if (isBangla) "ক্যাটাগরি" else "Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCatId == cat.id,
              onClick = { selectedCatId = cat.id },
              label = { Text(cat.displayName(isBangla), style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HotPinkPrimary, selectedLabelColor = Color.White)
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Price (৳)") }, singleLine = true, modifier = Modifier.weight(1f).testTag("admin_prod_price_input"))
          OutlinedTextField(value = prevPriceStr, onValueChange = { prevPriceStr = it }, label = { Text("Old Price (৳)") }, singleLine = true, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text("Stock Qty") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("Buy Cost (৳)") }, singleLine = true, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text("Weight (kg)") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = deliveryOverrideStr, onValueChange = { deliveryOverrideStr = it }, label = { Text("Special Deliv (৳)") }, singleLine = true, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(value = colorsStr, onValueChange = { colorsStr = it }, label = { Text("Colors (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sizesStr, onValueChange = { sizesStr = it }, label = { Text("Sizes (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Specs / Details") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Video URL (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        AdminToggleRow(if (isBangla) "প্রকাশিত (Published)" else "Published", isPublished) { isPublished = it }
        AdminToggleRow(if (isBangla) "ফ্ল্যাশ সেল (Flash Sale)" else "Flash Sale", isFlashSale) { isFlashSale = it }
        AdminToggleRow(if (isBangla) "চয়েস ডে (Choice Day)" else "Choice Day", isChoiceDay) { isChoiceDay = it }
        AdminToggleRow(if (isBangla) "ফিচার্ড (Featured)" else "Featured", isFeatured) { isFeatured = it }
        AdminToggleRow(if (isBangla) "বেস্ট সেলিং (Best Selling)" else "Best Selling", isBestSelling) { isBestSelling = it }
        AdminToggleRow(if (isBangla) "নতুন আগমন (New Arrival)" else "New Arrival", isNewArrival) { isNewArrival = it }
        AdminToggleRow(if (isBangla) "বান্ডেল অফার (Bundle Offer)" else "Bundle Eligible", isBundleEligible) { isBundleEligible = it }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val price = priceStr.toDoubleOrNull() ?: 0.0
          val prev = prevPriceStr.toDoubleOrNull() ?: 0.0
          val discountPct = if (prev > price && prev > 0.0) {
            (((prev - price) / prev) * 100.0).roundToLong().coerceIn(0L, 95L)
          } else 0L
          val catName = categories.firstOrNull { it.id == selectedCatId }?.displayName(false) ?: "Fashion"
          val prod = (initialProduct ?: Product()).copy(
            sku = sku.trim().ifBlank { "SF-ITEM" },
            name = name.trim().ifBlank { "Subahan Item" },
            nameBn = nameBn.trim(),
            categoryId = selectedCatId,
            categoryName = catName,
            price = price,
            previousPrice = prev,
            discountPercent = discountPct,
            stockQuantity = stockStr.toLongOrNull() ?: 0L,
            colors = colorsStr.split(",").map { it.trim() }.filter { it.isNotBlank() },
            sizes = sizesStr.split(",").map { it.trim() }.filter { it.isNotBlank() },
            imageUrls = images.toList().ifEmpty { listOf("res://img_storefront_hero") },
            videoUrl = videoUrl.trim(),
            weightKg = weightStr.toDoubleOrNull() ?: 0.5,
            deliveryChargeOverride = deliveryOverrideStr.toDoubleOrNull() ?: -1.0,
            description = description.trim(),
            details = details.trim(),
            isPublished = isPublished,
            isFeatured = isFeatured,
            isBestSelling = isBestSelling,
            isNewArrival = isNewArrival,
            isFlashSale = isFlashSale,
            isChoiceDay = isChoiceDay,
            isBundleEligible = isBundleEligible
          )
          onSave(prod, costStr.toDoubleOrNull())
        },
        enabled = name.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) > 0.0,
        colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
        modifier = Modifier.testTag("admin_save_product_btn")
      ) {
        Text(if (isBangla) "সংরক্ষণ করুন" else "Save Product")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
    }
  )
}

@Composable
private fun AdminToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun BulkUpdateDialog(
  isBangla: Boolean,
  productCount: Int,
  onDismiss: () -> Unit,
  onApply: (priceMultiplier: Double?, discountOverride: Long?, stockDelta: Long?) -> Unit
) {
  var pricePctChange by remember { mutableStateOf("") }
  var discountOverride by remember { mutableStateOf("") }
  var stockDelta by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isBangla) "বাল্ক আপডেট ($productCount টি পণ্য)" else "Bulk Update ($productCount Products)",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = if (isBangla) "যে ফিল্ড পরিবর্তন করতে চান সেটি পূরণ করুন:" else "Fill only the fields you want to adjust:",
          style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = pricePctChange,
          onValueChange = { pricePctChange = it },
          label = { Text(if (isBangla) "মূল্য পরিবর্তন % (যেমন 10 বা -10)" else "Price Change % (e.g. 10 or -10)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = discountOverride,
          onValueChange = { discountOverride = it },
          label = { Text(if (isBangla) "ডিসকাউন্ট % সেট করুন" else "Set Discount % (0-95)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = stockDelta,
          onValueChange = { stockDelta = it },
          label = { Text(if (isBangla) "স্টক যোগ/বিয়োগ (যেমন +20)" else "Stock Delta (e.g. +20 or -5)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val pct = pricePctChange.toDoubleOrNull()
          val multiplier = pct?.let { (1.0 + it / 100.0).coerceAtLeast(0.1) }
          val disc = discountOverride.toLongOrNull()
          val delta = stockDelta.toLongOrNull()
          onApply(multiplier, disc, delta)
        },
        colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White)
      ) {
        Text(if (isBangla) "আপডেট করুন" else "Apply Bulk Update")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
    }
  )
}

@Composable
fun CsvImportDialog(
  isBangla: Boolean,
  onDismiss: () -> Unit,
  onImport: (String) -> Unit
) {
  var csvText by remember {
    mutableStateOf("sku,name,nameBn,categoryId,price,stockQuantity\nSF-NEW-101,Royal Perfume 50ml,রয়্যাল পারফিউম,cat_perfume,950,30")
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isBangla) "CSV থেকে পণ্য ইম্পোর্ট" else "Import Products via CSV", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Format: sku,name,nameBn,categoryId,price,stockQuantity", style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(
          value = csvText,
          onValueChange = { csvText = it },
          minLines = 5,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onImport(csvText) },
        colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White)
      ) {
        Text(if (isBangla) "ইম্পোর্ট করুন" else "Import CSV")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
    }
  )
}

@Composable
fun BroadcastNotificationDialog(
  isBangla: Boolean,
  onDismiss: () -> Unit,
  onSend: (titleEn: String, titleBn: String, msgEn: String, msgBn: String) -> Unit
) {
  var titleEn by remember { mutableStateOf("Special Flash Offer!") }
  var titleBn by remember { mutableStateOf("বিশেষ ফ্ল্যাশ অফার!") }
  var msgEn by remember { mutableStateOf("Enjoy up to 30% OFF across Subahan Fashion today.") }
  var msgBn by remember { mutableStateOf("সুবাহান ফ্যাশনে আজ পাচ্ছেন ৩০% পর্যন্ত বিশেষ ছাড়!") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isBangla) "কাস্টমার নোটিফিকেশন পাঠান" else "Broadcast Promo Alert", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title (EN)") }, singleLine = true)
        OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text("Title (BN)") }, singleLine = true)
        OutlinedTextField(value = msgEn, onValueChange = { msgEn = it }, label = { Text("Message (EN)") }, minLines = 2)
        OutlinedTextField(value = msgBn, onValueChange = { msgBn = it }, label = { Text("Message (BN)") }, minLines = 2)
      }
    },
    confirmButton = {
      Button(
        onClick = { onSend(titleEn, titleBn, msgEn, msgBn) },
        colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White)
      ) {
        Text(if (isBangla) "পাঠান" else "Send Broadcast")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(if (isBangla) "বাতিল" else "Cancel") }
    }
  )
}
