package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAreaOption
import com.example.data.model.PaymentMethodOption
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.util.ShopCopy
import com.example.ui.util.formatBdt
import com.example.ui.viewmodel.CartPricingBreakdown

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartScreen(
  isBangla: Boolean,
  cartItems: List<CartItem>,
  pricing: CartPricingBreakdown,
  selectedDeliveryArea: String,
  appliedCouponCode: String,
  onSelectDeliveryArea: (String) -> Unit,
  onApplyCoupon: (String) -> Unit,
  onUpdateQty: (String, Long) -> Unit,
  onRemoveItem: (String) -> Unit,
  onProceedToCheckout: () -> Unit,
  onContinueShopping: () -> Unit
) {
  var couponInput by remember(appliedCouponCode) { mutableStateOf(appliedCouponCode) }

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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isBangla) "শপিং কার্ট (${pricing.totalItemsCount})" else "Shopping Cart (${pricing.totalItemsCount})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold
        )
        if (pricing.freeDeliveryApplied) {
          Surface(color = EmeraldAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
            Text(
              text = if (isBangla) "ফ্রি ডেলিভারি!" else "Free Delivery!",
              color = EmeraldAccent,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    if (cartItems.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            Icons.Rounded.ShoppingCartCheckout,
            contentDescription = null,
            tint = HotPinkPrimary,
            modifier = Modifier.size(64.dp)
          )
          Text(
            text = if (isBangla) "আপনার কার্ট এখনো খালি" else "Your shopping cart is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Button(
            onClick = onContinueShopping,
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary)
          ) {
            Text(if (isBangla) "কেনাকাটা শুরু করুন" else "Start Shopping")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(cartItems, key = { it.productId }) { item ->
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ProductThumbnail(
                imageUrl = item.imageUrl,
                contentDescription = item.productName,
                modifier = Modifier
                  .size(76.dp)
                  .clip(RoundedCornerShape(10.dp))
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = if (isBangla && item.productNameBn.isNotBlank()) item.productNameBn else item.productName,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                if (item.selectedColor.isNotBlank() || item.selectedSize.isNotBlank()) {
                  Text(
                    text = listOf(item.selectedColor, item.selectedSize).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Text(
                  text = formatBdt(item.unitPrice, isBangla),
                  style = MaterialTheme.typography.titleMedium,
                  color = HotPinkPrimary,
                  fontWeight = FontWeight.Bold
                )
              }
              Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { onRemoveItem(item.productId) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                      .size(30.dp)
                      .clickable { onUpdateQty(item.productId, item.quantity - 1) }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                  }
                  Text("${item.quantity}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                      .size(30.dp)
                      .clickable { onUpdateQty(item.productId, item.quantity + 1) }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = HotPinkPrimary, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }
        }

        // Delivery Area Selection
        item {
          Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = if (isBangla) "ডেলিভারি এলাকা নির্বাচন করুন" else "Select Delivery Area",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryAreaOption.entries.forEach { area ->
                  FilterChip(
                    selected = selectedDeliveryArea == area.code,
                    onClick = { onSelectDeliveryArea(area.code) },
                    label = { Text(area.displayLabel(isBangla)) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = HotPinkPrimary,
                      selectedLabelColor = Color.White
                    )
                  )
                }
              }
            }
          }
        }

        // Coupon Code
        item {
          Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = couponInput,
                onValueChange = { couponInput = it },
                label = { Text(if (isBangla) "কুপন কোড (যেমন SUBAHAN10)" else "Coupon Code (e.g. SUBAHAN10)") },
                leadingIcon = { Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = HotPinkPrimary) },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("coupon_input")
              )
              Button(
                onClick = { onApplyCoupon(couponInput) },
                colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary),
                modifier = Modifier.height(52.dp)
              ) {
                Text(if (isBangla) "প্রয়োগ" else "Apply")
              }
            }
          }
        }

        // Price Breakdown
        item {
          PricingSummaryCard(isBangla = isBangla, pricing = pricing)
        }
      }

      Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isBangla) "মোট পরিশোধযোগ্য" else "Total Payable",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = formatBdt(pricing.totalPayable, isBangla),
              style = MaterialTheme.typography.titleLarge,
              color = HotPinkPrimary,
              fontWeight = FontWeight.ExtraBold
            )
          }
          Button(
            onClick = onProceedToCheckout,
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .height(50.dp)
              .testTag("proceed_checkout_btn")
          ) {
            Text(ShopCopy.checkout(isBangla), fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun PricingSummaryCard(isBangla: Boolean, pricing: CartPricingBreakdown) {
  Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SummaryLine(if (isBangla) "পণ্যের মূল্য (সাবটোটাল)" else "Subtotal", formatBdt(pricing.subtotal, isBangla))
      SummaryLine(
        if (isBangla) "ডেলিভারি চার্জ" else "Delivery Charge",
        if (pricing.deliveryCharge == 0.0) (if (isBangla) "ফ্রি (৳০)" else "FREE (৳0)") else formatBdt(pricing.deliveryCharge, isBangla)
      )
      if (pricing.bundleDiscount > 0.0) {
        SummaryLine(
          if (isBangla) "বান্ডেল ছাড়" else "Bundle Offer Discount",
          "-${formatBdt(pricing.bundleDiscount, isBangla)}",
          valueColor = EmeraldAccent
        )
      }
      if (pricing.couponDiscount > 0.0) {
        SummaryLine(
          if (isBangla) "কুপন ছাড়" else "Coupon Discount",
          "-${formatBdt(pricing.couponDiscount, isBangla)}",
          valueColor = EmeraldAccent
        )
      }
      HorizontalDivider()
      SummaryLine(
        label = if (isBangla) "সর্বমোট (Total Payable)" else "Total Payable Amount",
        value = formatBdt(pricing.totalPayable, isBangla),
        isBold = true,
        valueColor = HotPinkPrimary
      )
    }
  }
}

@Composable
private fun SummaryLine(
  label: String,
  value: String,
  isBold: Boolean = false,
  valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(
      text = label,
      style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
    )
    Text(
      text = value,
      style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
      fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
      color = valueColor
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(
  isBangla: Boolean,
  isBusy: Boolean,
  userProfile: UserProfile?,
  storeConfig: StoreConfig,
  pricing: CartPricingBreakdown,
  selectedDeliveryArea: String,
  onSelectDeliveryArea: (String) -> Unit,
  onBack: () -> Unit,
  onPlaceOrder: (
    customerName: String,
    mobile: String,
    altMobile: String,
    address: String,
    division: String,
    district: String,
    upazila: String,
    area: String,
    note: String,
    paymentMethod: String,
    trxId: String,
    senderPhone: String
  ) -> Unit
) {
  BackHandler(onBack = onBack)

  var customerName by remember { mutableStateOf(userProfile?.displayName ?: "") }
  var mobile by remember { mutableStateOf(userProfile?.phone ?: "") }
  var altMobile by remember { mutableStateOf(userProfile?.altPhone ?: "") }
  var fullAddress by remember { mutableStateOf(userProfile?.defaultAddress ?: "") }
  var division by remember { mutableStateOf(userProfile?.division?.ifBlank { "Rajshahi" } ?: "Rajshahi") }
  var district by remember { mutableStateOf(userProfile?.district?.ifBlank { "Bogura" } ?: "Bogura") }
  var upazila by remember { mutableStateOf(userProfile?.upazila?.ifBlank { "Sariakandi" } ?: "Sariakandi") }
  var orderNote by remember { mutableStateOf("") }

  LaunchedEffect(userProfile) {
    if (customerName.isBlank() && !userProfile?.displayName.isNullOrBlank()) customerName = userProfile!!.displayName
    if (mobile.isBlank() && !userProfile?.phone.isNullOrBlank()) mobile = userProfile!!.phone
    if (fullAddress.isBlank() && !userProfile?.defaultAddress.isNullOrBlank()) fullAddress = userProfile!!.defaultAddress
  }

  val enabledPayments = remember(storeConfig) {
    buildList {
      if (storeConfig.codEnabled) add(PaymentMethodOption.COD)
      if (storeConfig.bkashEnabled) add(PaymentMethodOption.BKASH)
      if (storeConfig.nagadEnabled) add(PaymentMethodOption.NAGAD)
      if (storeConfig.rocketEnabled) add(PaymentMethodOption.ROCKET)
      if (storeConfig.bankEnabled) add(PaymentMethodOption.BANK)
    }.ifEmpty { listOf(PaymentMethodOption.COD) }
  }
  var selectedPayment by remember(enabledPayments) { mutableStateOf(enabledPayments.first()) }
  var trxId by remember { mutableStateOf("") }
  var senderPhone by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (isBangla) "চেকআউট ফর্ম" else "Checkout", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(padding),
      contentPadding = PaddingValues(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = if (isBangla) "ডেলিভারি তথ্য" else "Customer & Delivery Details",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
              value = customerName,
              onValueChange = { customerName = it },
              label = { Text(if (isBangla) "গ্রাহকের নাম *" else "Customer Name *") },
              modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
            )
            OutlinedTextField(
              value = mobile,
              onValueChange = { mobile = it },
              label = { Text(if (isBangla) "মোবাইল নম্বর * (যেমন 017...)" else "Mobile Number *") },
              modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
            )
            OutlinedTextField(
              value = altMobile,
              onValueChange = { altMobile = it },
              label = { Text(if (isBangla) "বিকল্প মোবাইল নম্বর (ঐচ্ছিক)" else "Alternative Mobile (Optional)") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = fullAddress,
              onValueChange = { fullAddress = it },
              label = { Text(if (isBangla) "সম্পূর্ণ ঠিকানা *" else "Full Address *") },
              minLines = 2,
              modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = division,
                onValueChange = { division = it },
                label = { Text(if (isBangla) "বিভাগ" else "Division") },
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = district,
                onValueChange = { district = it },
                label = { Text(if (isBangla) "জেলা" else "District") },
                modifier = Modifier.weight(1f)
              )
            }
            OutlinedTextField(
              value = upazila,
              onValueChange = { upazila = it },
              label = { Text(if (isBangla) "উপজেলা / থানা" else "Upazila / Thana") },
              modifier = Modifier.fillMaxWidth()
            )

            Text(
              text = if (isBangla) "ডেলিভারি জোন" else "Delivery Area",
              style = MaterialTheme.typography.labelLarge
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              DeliveryAreaOption.entries.forEach { area ->
                FilterChip(
                  selected = selectedDeliveryArea == area.code,
                  onClick = { onSelectDeliveryArea(area.code) },
                  label = { Text(area.displayLabel(isBangla)) }
                )
              }
            }

            OutlinedTextField(
              value = orderNote,
              onValueChange = { orderNote = it },
              label = { Text(if (isBangla) "অর্ডার নোট (ঐচ্ছিক)" else "Order Note (Optional)") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      // Payment Methods
      item {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.Payments, contentDescription = null, tint = HotPinkPrimary)
              Text(
                text = if (isBangla) "পেমেন্ট মেথড নির্বাচন করুন" else "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              enabledPayments.forEach { pm ->
                FilterChip(
                  selected = selectedPayment == pm,
                  onClick = { selectedPayment = pm },
                  label = { Text(pm.displayLabel(isBangla)) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HotPinkPrimary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }

            if (selectedPayment != PaymentMethodOption.COD) {
              val instruction = when (selectedPayment) {
                PaymentMethodOption.BKASH -> "bKash Send Money / Payment: ${storeConfig.bkashNumber}"
                PaymentMethodOption.NAGAD -> "Nagad Send Money: ${storeConfig.nagadNumber}"
                PaymentMethodOption.ROCKET -> "Rocket Send Money: ${storeConfig.rocketNumber}"
                PaymentMethodOption.BANK -> storeConfig.bankDetails
                PaymentMethodOption.COD -> ""
              }
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = instruction,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(10.dp)
                )
              }
              OutlinedTextField(
                value = senderPhone,
                onValueChange = { senderPhone = it },
                label = { Text(if (isBangla) "যে নম্বর থেকে টাকা পাঠিয়েছেন" else "Sender Account / Phone") },
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = trxId,
                onValueChange = { trxId = it },
                label = { Text(if (isBangla) "ট্রানজেকশন আইডি (TrxID) *" else "Transaction ID (TrxID) *") },
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }

      item {
        PricingSummaryCard(isBangla = isBangla, pricing = pricing)
      }

      if (validationError != null) {
        item {
          Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
            Text(
              text = validationError ?: "",
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp)
            )
          }
        }
      }

      item {
        Button(
          onClick = {
            when {
              customerName.trim().length < 2 -> validationError = if (isBangla) "সঠিক নাম লিখুন" else "Enter customer name"
              mobile.trim().length < 8 -> validationError = if (isBangla) "সঠিক মোবাইল নম্বর দিন" else "Enter valid mobile number"
              fullAddress.trim().length < 4 -> validationError = if (isBangla) "সম্পূর্ণ ঠিকানা লিখুন" else "Enter full delivery address"
              selectedPayment != PaymentMethodOption.COD && trxId.trim().isBlank() ->
                validationError = if (isBangla) "পেমেন্ট TrxID প্রদান করুন" else "Please enter Transaction ID"
              else -> {
                validationError = null
                onPlaceOrder(
                  customerName,
                  mobile,
                  altMobile,
                  fullAddress,
                  division,
                  district,
                  upazila,
                  selectedDeliveryArea,
                  orderNote,
                  selectedPayment.code,
                  trxId,
                  senderPhone
                )
              }
            }
          },
          enabled = !isBusy,
          colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("place_order_submit_btn")
        ) {
          if (isBusy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
          } else {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
              text = "${ShopCopy.placeOrder(isBangla)} (${formatBdt(pricing.totalPayable, isBangla)})",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
