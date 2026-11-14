package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreConfig
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.theme.SunnyYellow
import com.example.ui.util.ShopCopy
import com.example.ui.util.formatBdt
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
  product: Product,
  isBangla: Boolean,
  isWishlisted: Boolean,
  storeConfig: StoreConfig,
  reviewsFlow: Flow<List<ProductReview>>,
  onBack: () -> Unit,
  onToggleWishlist: () -> Unit,
  onAddToCart: (Long, String, String) -> Unit,
  onBuyNow: (Long, String, String) -> Unit,
  onSubmitReview: (Long, String) -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val reviews by reviewsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
  val images = remember(product.imageUrls) {
    product.imageUrls.ifEmpty { listOf("res://img_storefront_hero") }
  }
  val pagerState = rememberPagerState(pageCount = { images.size })

  var selectedColor by remember(product.id) { mutableStateOf(product.colors.firstOrNull() ?: "") }
  var selectedSize by remember(product.id) { mutableStateOf(product.sizes.firstOrNull() ?: "") }
  var quantity by remember(product.id) { mutableLongStateOf(1L) }

  var reviewRating by remember { mutableLongStateOf(5L) }
  var reviewComment by remember { mutableStateOf("") }

  Scaffold(
    bottomBar = {
      Surface(
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier.navigationBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { onAddToCart(quantity, selectedColor, selectedSize) },
            enabled = product.stockQuantity > 0L,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, HotPinkPrimary),
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("detail_add_to_cart_btn")
          ) {
            Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, tint = HotPinkPrimary)
            Spacer(Modifier.width(6.dp))
            Text(
              text = ShopCopy.addToCart(isBangla),
              color = HotPinkPrimary,
              fontWeight = FontWeight.Bold
            )
          }
          Button(
            onClick = { onBuyNow(quantity, selectedColor, selectedSize) },
            enabled = product.stockQuantity > 0L,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("detail_buy_now_btn")
          ) {
            Icon(Icons.Rounded.FlashOn, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(
              text = ShopCopy.buyNow(isBangla),
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(padding),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Image Gallery Header
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(Color.White)
        ) {
          HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { idx ->
            ProductThumbnail(
              imageUrl = images[idx],
              contentDescription = product.displayName(isBangla),
              modifier = Modifier.fillMaxSize()
            )
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.5f)
            ) {
              IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
              }
            }
            Surface(
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.92f)
            ) {
              IconButton(onClick = onToggleWishlist) {
                Icon(
                  imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                  contentDescription = "Wishlist",
                  tint = if (isWishlisted) HotPinkPrimary else Color.DarkGray
                )
              }
            }
          }

          if (images.size > 1) {
            Surface(
              color = Color.Black.copy(alpha = 0.55f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
            ) {
              Text(
                text = "${pagerState.currentPage + 1}/${images.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // Price, Title, Badges
      item {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = formatBdt(product.price, isBangla),
                style = MaterialTheme.typography.displaySmall,
                color = HotPinkPrimary,
                fontWeight = FontWeight.ExtraBold
              )
              if (product.previousPrice > product.price) {
                Text(
                  text = formatBdt(product.previousPrice, isBangla),
                  style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = TextDecoration.LineThrough
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (product.discountPercent > 0L) {
                Surface(color = SunnyYellow, shape = RoundedCornerShape(8.dp)) {
                  Text(
                    text = "-${product.discountPercent}% OFF",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }

            Text(
              text = product.displayName(isBangla),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                  text = "${String.format("%.1f", product.ratingAvg)} (${product.reviewCount + reviews.size})",
                  style = MaterialTheme.typography.bodySmall
                )
              }
              Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = "SKU: ${product.sku}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = if (product.stockQuantity > 0L) "${ShopCopy.inStock(isBangla)} (${product.stockQuantity})"
                else ShopCopy.outOfStock(isBangla),
                style = MaterialTheme.typography.labelMedium,
                color = if (product.stockQuantity > 0L) EmeraldAccent else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Variants & Quantity
      item {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (product.colors.isNotEmpty()) {
              Text(
                text = if (isBangla) "কালার নির্বাচন করুন" else "Select Color",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                product.colors.forEach { color ->
                  FilterChip(
                    selected = selectedColor == color,
                    onClick = { selectedColor = color },
                    label = { Text(color) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = HotPinkPrimary,
                      selectedLabelColor = Color.White
                    )
                  )
                }
              }
            }

            if (product.sizes.isNotEmpty()) {
              Text(
                text = if (isBangla) "সাইজ নির্বাচন করুন" else "Select Size",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                product.sizes.forEach { size ->
                  FilterChip(
                    selected = selectedSize == size,
                    onClick = { selectedSize = size },
                    label = { Text(size) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = HotPinkPrimary,
                      selectedLabelColor = Color.White
                    )
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = if (isBangla) "পরিমাণ (Quantity)" else "Quantity",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                  modifier = Modifier
                    .size(48.dp)
                    .clickable { if (quantity > 1L) quantity-- }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
                  }
                }
                Text(
                  text = "$quantity",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier
                    .size(48.dp)
                    .clickable { if (quantity < product.stockQuantity) quantity++ }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase quantity", tint = HotPinkPrimary)
                  }
                }
              }
            }
          }
        }
      }

      // Delivery & Guarantee Info
      item {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = EmeraldAccent)
              Column {
                Text(
                  text = if (product.deliveryChargeOverride == 0.0) {
                    if (isBangla) "এই পণ্যে ফ্রি ডেলিভারি!" else "Free Delivery on this item!"
                  } else if (product.deliveryChargeOverride > 0.0) {
                    if (isBangla) "বিশেষ ডেলিভারি চার্জ: ${formatBdt(product.deliveryChargeOverride, true)}"
                    else "Special Delivery Charge: ${formatBdt(product.deliveryChargeOverride, false)}"
                  } else {
                    if (isBangla)
                      "বগুড়া ${formatBdt(storeConfig.insideBoguraCharge, true)} • ঢাকা ${formatBdt(storeConfig.insideDhakaCharge, true)} • সারাদেশ ${formatBdt(storeConfig.nationwideCharge, true)}"
                    else
                      "Bogura ${formatBdt(storeConfig.insideBoguraCharge, false)} • Dhaka ${formatBdt(storeConfig.insideDhakaCharge, false)} • Nationwide ${formatBdt(storeConfig.nationwideCharge, false)}"
                  },
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = storeConfig.deliveryPolicy,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Verified, contentDescription = null, tint = HotPinkPrimary)
              Text(
                text = storeConfig.returnPolicy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Description & Details
      item {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = if (isBangla) "পণ্যের বিবরণ" else "Product Description & Details",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(text = product.description, style = MaterialTheme.typography.bodyMedium)
            if (product.details.isNotBlank()) {
              Text(
                text = product.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (product.videoUrl.isNotBlank()) {
              OutlinedButton(
                onClick = {
                  runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(product.videoUrl)))
                  }
                },
                modifier = Modifier.testTag("product_video_btn")
              ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = HotPinkPrimary)
                Spacer(Modifier.width(6.dp))
                Text(if (isBangla) "পণ্যের ভিডিও দেখুন" else "Watch Product Video")
              }
            }
          }
        }
      }

      // Customer Reviews & Rating Form
      item {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = if (isBangla) "কাস্টমার রিভিউ (${reviews.size})" else "Customer Reviews (${reviews.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              (1L..5L).forEach { star ->
                IconButton(onClick = { reviewRating = star }, modifier = Modifier.size(48.dp)) {
                  Icon(
                    imageVector = if (star <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "$star stars",
                    tint = Color(0xFFFFA000)
                  )
                }
              }
              Spacer(Modifier.width(8.dp))
              Text("$reviewRating / 5", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedTextField(
              value = reviewComment,
              onValueChange = { reviewComment = it },
              label = { Text(if (isBangla) "আপনার মতামত লিখুন" else "Write your review") },
              modifier = Modifier.fillMaxWidth()
            )
            Button(
              onClick = {
                if (reviewComment.isNotBlank()) {
                  onSubmitReview(reviewRating, reviewComment)
                  reviewComment = ""
                }
              },
              enabled = reviewComment.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary)
            ) {
              Text(if (isBangla) "রিভিউ জমা দিন" else "Submit Review")
            }
          }
        }
      }

      items(reviews, key = { it.id }) { rev ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(rev.userName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Row {
                repeat(rev.rating.toInt()) {
                  Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
                }
              }
            }
            Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}
