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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.OrderStatusOption
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.WarmGold
import com.example.ui.util.formatBdt
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrdersScreen(
  isBangla: Boolean,
  ordersState: UiState<List<Order>>,
  onCancelOrder: (String) -> Unit,
  onExploreStore: () -> Unit
) {
  val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Surface(color = Color.White, shadowElevation = 2.dp) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null, tint = HotPinkPrimary)
        Text(
          text = if (isBangla) "আমার অর্ডারসমূহ" else "My Orders",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }

    when (ordersState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = HotPinkPrimary)
        }
      }

      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(ordersState.message, color = MaterialTheme.colorScheme.error)
        }
      }

      is UiState.Success -> {
        val orders = ordersState.data
        if (orders.isEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.ShoppingBag,
              contentDescription = null,
              tint = HotPinkPrimary.copy(alpha = 0.45f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = if (isBangla) "এখনও কোনো অর্ডার নেই" else "No orders placed yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Button(
              onClick = onExploreStore,
              colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
              modifier = Modifier.testTag("orders_explore_btn")
            ) {
              Text(if (isBangla) "শপিং শুরু করুন" else "Start Shopping")
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(orders, key = { it.id }) { order ->
              CustomerOrderCard(
                order = order,
                isBangla = isBangla,
                dateFmt = dateFmt,
                onCancelOrder = { onCancelOrder(order.id) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CustomerOrderCard(
  order: Order,
  isBangla: Boolean,
  dateFmt: SimpleDateFormat,
  onCancelOrder: () -> Unit
) {
  val statusOption = remember(order.status) {
    OrderStatusOption.entries.firstOrNull { it.code.equals(order.status, ignoreCase = true) }
  }
  val statusColor = when (order.status) {
    "Delivered" -> EmeraldAccent
    "Confirmed", "Processing", "Shipped" -> HotPinkPrimary
    "Cancelled", "Returned" -> Color(0xFFDC2626)
    else -> WarmGold
  }
  val dateStr = remember(order.createdAt) {
    order.createdAt?.toDate()?.let { dateFmt.format(it) } ?: ""
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = order.orderNumber.ifBlank { order.id.take(10) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
          )
          if (dateStr.isNotBlank()) {
            Text(
              text = dateStr,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
          Text(
            text = statusOption?.displayLabel(isBangla) ?: order.status,
            color = statusColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      HorizontalDivider()

      if (order.items.isNotEmpty()) {
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "${item.qty}× ${item.name}" +
                listOf(item.color, item.size).filter { it.isNotBlank() }.let {
                  if (it.isNotEmpty()) " (${it.joinToString("/")})" else ""
                },
              style = MaterialTheme.typography.bodySmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = formatBdt(item.unitPrice * item.qty, isBangla),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      } else {
        Text(text = order.itemsSummary, style = MaterialTheme.typography.bodySmall)
      }

      Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "${order.customerName} • ${order.mobileNumber}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "${order.fullAddress}, ${order.upazila}, ${order.district}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = (if (isBangla) "পেমেন্ট: " else "Payment: ") + order.paymentMethod +
              if (order.transactionId.isNotBlank()) " (TrxID: ${order.transactionId})" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isBangla) "ডেলিভারি: ${formatBdt(order.deliveryCharge, true)} | ছাড়: ${formatBdt(order.discountAmount, true)}"
            else "Delivery: ${formatBdt(order.deliveryCharge, false)} | Disc: ${formatBdt(order.discountAmount, false)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = (if (isBangla) "মোট: " else "Total: ") + formatBdt(order.totalAmount, isBangla),
            style = MaterialTheme.typography.titleMedium,
            color = HotPinkPrimary,
            fontWeight = FontWeight.ExtraBold
          )
        }

        if (order.status == "Pending") {
          OutlinedButton(
            onClick = onCancelOrder,
            border = BorderStroke(1.dp, Color(0xFFDC2626)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
            modifier = Modifier.testTag("cancel_order_${order.id}")
          ) {
            Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isBangla) "বাতিল" else "Cancel", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
  isBangla: Boolean,
  isAdmin: Boolean,
  isBusy: Boolean,
  userProfile: UserProfile?,
  wishlistItems: List<WishlistItem>,
  allProducts: List<Product>,
  storeConfig: StoreConfig,
  onToggleLanguage: () -> Unit,
  onSaveProfile: (UserProfile) -> Unit,
  onOpenProduct: (Product) -> Unit,
  onAddToCart: (Product) -> Unit,
  onOpenAdmin: () -> Unit,
  onOpenNotifications: () -> Unit,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf(userProfile?.displayName ?: "") }
  var phone by remember { mutableStateOf(userProfile?.phone ?: "") }
  var altPhone by remember { mutableStateOf(userProfile?.altPhone ?: "") }
  var address by remember { mutableStateOf(userProfile?.defaultAddress ?: "") }
  var division by remember { mutableStateOf(userProfile?.division?.ifBlank { "Rajshahi" } ?: "Rajshahi") }
  var district by remember { mutableStateOf(userProfile?.district?.ifBlank { "Bogura" } ?: "Bogura") }
  var upazila by remember { mutableStateOf(userProfile?.upazila?.ifBlank { "Sariakandi" } ?: "Sariakandi") }

  var policyDialogTitle by remember { mutableStateOf<String?>(null) }
  var policyDialogBody by remember { mutableStateOf("") }

  LaunchedEffect(userProfile) {
    if (userProfile != null) {
      if (name.isBlank()) name = userProfile.displayName
      if (phone.isBlank()) phone = userProfile.phone
      if (altPhone.isBlank()) altPhone = userProfile.altPhone
      if (address.isBlank()) address = userProfile.defaultAddress
      if (userProfile.division.isNotBlank()) division = userProfile.division
      if (userProfile.district.isNotBlank()) district = userProfile.district
      if (userProfile.upazila.isNotBlank()) upazila = userProfile.upazila
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Profile Header Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = HotPinkPrimary),
        shape = RoundedCornerShape(18.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
              }
              Column {
                Text(
                  text = userProfile?.displayName?.ifBlank { "Valued Customer" } ?: "Valued Customer",
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = userProfile?.email.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.88f)
                )
              }
            }

            Surface(
              color = SunnyYellow,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .clickable(onClick = onToggleLanguage)
                .testTag("account_lang_toggle")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Text(
                  text = if (isBangla) "বাংলা" else "English",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.Black,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          if (isAdmin) {
            Button(
              onClick = onOpenAdmin,
              colors = ButtonDefaults.buttonColors(containerColor = SunnyYellow, contentColor = Color.Black),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_admin_dashboard_btn")
            ) {
              Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(
                text = if (isBangla) "অ্যাডমিন ড্যাশবোর্ড খুলুন (Owner Panel)" else "Open Admin Dashboard (Owner Panel)",
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }
      }
    }

    // Notifications shortcut
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onOpenNotifications)
          .testTag("account_notifications_card")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(Icons.Rounded.Notifications, contentDescription = null, tint = HotPinkPrimary)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isBangla) "অফার ও অর্ডার নোটিফিকেশন" else "Offers & Order Notifications",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (isBangla) "নতুন অফার ও অর্ডারের আপডেট দেখুন" else "View flash sales, promos & order status alerts",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Wishlist Section
    if (wishlistItems.isNotEmpty()) {
      item {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HotPinkPrimary)
              Text(
                text = if (isBangla) "আমার উইশলিস্ট (${wishlistItems.size})" else "My Wishlist (${wishlistItems.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(wishlistItems, key = { it.productId }) { wish ->
                val matchedProd = allProducts.firstOrNull { it.id == wish.productId }
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .width(140.dp)
                    .clickable { if (matchedProd != null) onOpenProduct(matchedProd) }
                ) {
                  Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProductThumbnail(
                      imageUrl = wish.imageUrl,
                      contentDescription = wish.productName,
                      modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Text(
                      text = if (isBangla && wish.productNameBn.isNotBlank()) wish.productNameBn else wish.productName,
                      style = MaterialTheme.typography.labelMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = formatBdt(wish.price, isBangla),
                      style = MaterialTheme.typography.labelLarge,
                      color = HotPinkPrimary,
                      fontWeight = FontWeight.Bold
                    )
                    if (matchedProd != null) {
                      OutlinedButton(
                        onClick = { onAddToCart(matchedProd) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                      ) {
                        Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isBangla) "কার্টে নিন" else "Cart", style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Delivery Profile Form
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = if (isBangla) "ব্যক্তিগত তথ্য ও ডেলিভারি ঠিকানা" else "Profile & Default Address",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (isBangla) "আপনার নাম" else "Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text(if (isBangla) "মোবাইল" else "Mobile") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = altPhone,
              onValueChange = { altPhone = it },
              label = { Text(if (isBangla) "বিকল্প মোবাইল" else "Alt Mobile") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(if (isBangla) "পূর্ণ ঠিকানা" else "Full Address") },
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = division,
              onValueChange = { division = it },
              label = { Text(if (isBangla) "বিভাগ" else "Division") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = district,
              onValueChange = { district = it },
              label = { Text(if (isBangla) "জেলা" else "District") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = upazila,
              onValueChange = { upazila = it },
              label = { Text(if (isBangla) "উপজেলা" else "Upazila") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
          Button(
            onClick = {
              val updated = (userProfile ?: UserProfile()).copy(
                displayName = name.trim(),
                phone = phone.trim(),
                altPhone = altPhone.trim(),
                defaultAddress = address.trim(),
                division = division.trim(),
                district = district.trim(),
                upazila = upazila.trim()
              )
              onSaveProfile(updated)
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("save_profile_btn")
          ) {
            Icon(Icons.Rounded.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isBangla) "তথ্য সংরক্ষণ করুন" else "Save Profile", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Business Information & Policies
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = if (isBangla) "প্রতিষ্ঠানের তথ্য ও নীতিমালা" else "Business Info & Policies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = HotPinkPrimary, modifier = Modifier.size(18.dp))
            Text(storeConfig.location, style = MaterialTheme.typography.bodySmall)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                runCatching {
                  context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${storeConfig.contactPhone}")))
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(if (isBangla) "কল করুন" else "Call Us", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = {
                runCatching {
                  val num = storeConfig.whatsappNumber.replace("+", "").replace(" ", "")
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num")))
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = {
                runCatching {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeConfig.facebookUrl)))
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Facebook", style = MaterialTheme.typography.labelMedium)
            }
          }

          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PolicyChip(if (isBangla) "আমাদের সম্পর্কে" else "About Us") {
              policyDialogTitle = if (isBangla) "আমাদের সম্পর্কে (About Us)" else "About Subahan Fashion"
              policyDialogBody = storeConfig.aboutUs
            }
            PolicyChip(if (isBangla) "ডেলিভারি নীতিমালা" else "Delivery Policy") {
              policyDialogTitle = if (isBangla) "ডেলিভারি নীতিমালা" else "Delivery Policy"
              policyDialogBody = storeConfig.deliveryPolicy
            }
            PolicyChip(if (isBangla) "রিটার্ন ও রিফান্ড" else "Return & Refund") {
              policyDialogTitle = if (isBangla) "রিটার্ন ও রিফান্ড নীতিমালা" else "Return & Refund Policy"
              policyDialogBody = storeConfig.returnPolicy
            }
            PolicyChip(if (isBangla) "প্রাইভেসি পলিসি" else "Privacy Policy") {
              policyDialogTitle = if (isBangla) "প্রাইভেসি পলিসি" else "Privacy Policy"
              policyDialogBody = storeConfig.privacyPolicy
            }
          }
        }
      }
    }

    // Sign Out
    item {
      OutlinedButton(
        onClick = onSignOut,
        border = BorderStroke(1.dp, Color(0xFFDC2626)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("sign_out_btn")
      ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (isBangla) "সাইন আউট করুন" else "Sign Out", fontWeight = FontWeight.Bold)
      }
    }
  }

  if (policyDialogTitle != null) {
    AlertDialog(
      onDismissRequest = { policyDialogTitle = null },
      icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = HotPinkPrimary) },
      title = { Text(policyDialogTitle.orEmpty(), fontWeight = FontWeight.Bold) },
      text = { Text(policyDialogBody, style = MaterialTheme.typography.bodyMedium) },
      confirmButton = {
        TextButton(onClick = { policyDialogTitle = null }) {
          Text(if (isBangla) "বন্ধ করুন" else "Close")
        }
      }
    )
  }
}

@Composable
private fun PolicyChip(label: String, onClick: () -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  isBangla: Boolean,
  notifications: List<NotificationItem>,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (isBangla) "নোটিফিকেশন (${notifications.size})" else "Notifications (${notifications.size})",
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_btn")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = HotPinkPrimary,
          titleContentColor = Color.White,
          navigationIconContentColor = Color.White
        )
      )
    }
  ) { innerPadding ->
    if (notifications.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        Text(if (isBangla) "কোনো নোটিফিকেশন নেই" else "No notifications yet")
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(notifications, key = { it.id }) { note ->
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = if (note.type.startsWith("ORDER")) Icons.Rounded.CheckCircle else Icons.Rounded.Notifications,
                contentDescription = null,
                tint = HotPinkPrimary
              )
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = if (isBangla && note.titleBn.isNotBlank()) note.titleBn else note.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (isBangla && note.messageBn.isNotBlank()) note.messageBn else note.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}
