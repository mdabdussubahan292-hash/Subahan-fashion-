package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.theme.SunnyYellow
import com.example.ui.util.formatBdt

@Composable
fun ProductGridCard(
  product: Product,
  isBangla: Boolean,
  isWishlisted: Boolean,
  onClick: () -> Unit,
  onAddToCart: () -> Unit,
  onToggleWishlist: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("product_card_${product.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(156.dp)
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrls.firstOrNull(),
          contentDescription = product.displayName(isBangla),
          modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        )

        // Top-left discount & flash badges
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (product.discountPercent > 0L) {
            Surface(
              color = HotPinkPrimary,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "-${product.discountPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
          if (product.isFlashSale) {
            Surface(
              color = SunnyYellow,
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Rounded.Bolt,
                  contentDescription = null,
                  tint = Color(0xFF3E2700),
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = if (isBangla) "ফ্ল্যাশ" else "Flash",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF3E2700),
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // Top-right wishlist button
        Surface(
          shape = CircleShape,
          color = Color.White.copy(alpha = 0.9f),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(34.dp)
        ) {
          IconButton(
            onClick = onToggleWishlist,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("wishlist_btn_${product.id}")
          ) {
            Icon(
              imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = if (isWishlisted) "Remove from wishlist" else "Save to wishlist",
              tint = if (isWishlisted) HotPinkPrimary else Color(0xFF5E6573),
              modifier = Modifier.size(18.dp)
            )
          }
        }

        if (product.deliveryChargeOverride == 0.0) {
          Surface(
            color = EmeraldAccent,
            shape = RoundedCornerShape(topEnd = 8.dp),
            modifier = Modifier.align(Alignment.BottomStart)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
              )
              Spacer(Modifier.width(3.dp))
              Text(
                text = if (isBangla) "ফ্রি ডেলিভারি" else "Free Delivery",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
              )
            }
          }
        }
      }

      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = product.displayName(isBangla),
          style = MaterialTheme.typography.titleSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          minLines = 2
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFFA000),
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = String.format("%.1f", product.ratingAvg),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = if (isBangla) "• ${product.soldCount} বিক্রি" else "• ${product.soldCount} sold",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = formatBdt(product.price, isBangla),
              style = MaterialTheme.typography.titleMedium,
              color = HotPinkPrimary,
              fontWeight = FontWeight.ExtraBold
            )
            if (product.previousPrice > product.price) {
              Text(
                text = formatBdt(product.previousPrice, isBangla),
                style = MaterialTheme.typography.labelSmall.copy(
                  textDecoration = TextDecoration.LineThrough
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            color = if (product.stockQuantity > 0) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .clickable(enabled = product.stockQuantity > 0, onClick = onAddToCart)
              .testTag("quick_add_cart_${product.id}")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.AddShoppingCart,
                contentDescription = "Add to cart",
                tint = if (product.stockQuantity > 0) HotPinkPrimary else Color.Gray,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CompactDealCard(
  product: Product,
  isBangla: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .width(142.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
  ) {
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(112.dp)
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrls.firstOrNull(),
          contentDescription = product.displayName(isBangla),
          modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
        )
        if (product.discountPercent > 0L) {
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(HotPinkPrimary)
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = "-${product.discountPercent}%",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      Column(modifier = Modifier.padding(8.dp)) {
        Text(
          text = product.displayName(isBangla),
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = formatBdt(product.price, isBangla),
          style = MaterialTheme.typography.titleSmall,
          color = HotPinkPrimary,
          fontWeight = FontWeight.ExtraBold
        )
        if (product.previousPrice > product.price) {
          Text(
            text = formatBdt(product.previousPrice, isBangla),
            style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
