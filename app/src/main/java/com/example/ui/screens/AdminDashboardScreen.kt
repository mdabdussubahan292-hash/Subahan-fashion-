package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.OrderStatusOption
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.CrimsonRedDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.WarmGold
import com.example.ui.util.formatBdt
import com.example.ui.viewmodel.AdminDashboardMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  isBangla: Boolean,
  isBusy: Boolean,
  metrics: AdminDashboardMetrics,
  products: List<Product>,
  categories: List<Category>,
  orders: List<Order>,
  coupons: List<Coupon>,
  storeConfig: StoreConfig,
  productCosts: Map<String, Double>,
  onBack: () -> Unit,
  onSeed300Products: () -> Unit,
  onSaveProduct: (Product, Double?, () -> Unit) -> Unit,
  onDeleteProduct: (String) -> Unit,
  onBulkUpdateProducts: (List<String>, Double?, Long?, Long?) -> Unit,
  onCreateCategory: (String, String) -> Unit,
  onDeleteCategory: (String) -> Unit,
  onUpdateStoreConfig: (StoreConfig) -> Unit,
  onCreateCoupon: (String, String, Double, Double, Double) -> Unit,
  onDeleteCoupon: (String) -> Unit,
  onUpdateOrderStatus: (Order, String) -> Unit,
  onBroadcastPromo: (String, String, String, String) -> Unit,
  onImportCsv: (String) -> Unit,
  onBuildCsvExport: () -> String
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showNewProductDialog by remember { mutableStateOf(false) }
  var showBulkDialog by remember { mutableStateOf(false) }
  var showCsvImportDialog by remember { mutableStateOf(false) }
  var showBroadcastDialog by remember { mutableStateOf(false) }
  var adminProdQuery by remember { mutableStateOf("") }

  val filteredAdminProducts = remember(products, adminProdQuery) {
    val q = adminProdQuery.trim().lowercase()
    if (q.isBlank()) products
    else products.filter {
      it.name.lowercase().contains(q) ||
        it.nameBn.lowercase().contains(q) ||
        it.sku.lowercase().contains(q)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (isBangla) "সুবাহান ফ্যাশন — অ্যাডমিন" else "Subahan Fashion Admin",
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = "${metrics.totalProducts} products • ${metrics.pendingOrders} pending orders",
              style = MaterialTheme.typography.labelSmall,
              color = SunnyYellow
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = CrimsonRedDark,
          titleContentColor = Color.White,
          navigationIconContentColor = Color.White
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      val tabs = if (isBangla) listOf("ওভারভিউ", "পণ্যসমূহ", "অর্ডার", "সেটিংস")
      else listOf("Overview", "Products", "Orders", "Settings")

      TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = HotPinkPrimary) {
        tabs.forEachIndexed { idx, label ->
          Tab(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            text = { Text(label, fontWeight = FontWeight.Bold, maxLines = 1) },
            modifier = Modifier.testTag("admin_tab_$idx")
          )
        }
      }

      when (selectedTab) {
        0 -> AdminOverviewTab(
          isBangla = isBangla,
          isBusy = isBusy,
          metrics = metrics,
          lowStockList = products.filter { it.stockQuantity <= 5L },
          onSeed300 = onSeed300Products,
          onOpenBroadcast = { showBroadcastDialog = true },
          onExportCsv = {
            val csv = onBuildCsvExport()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/csv"
              putExtra(Intent.EXTRA_SUBJECT, "Subahan Fashion Catalog CSV")
              putExtra(Intent.EXTRA_TEXT, csv)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export Catalog CSV"))
          }
        )

        1 -> AdminProductsTab(
          isBangla = isBangla,
          query = adminProdQuery,
          onQueryChange = { adminProdQuery = it },
          products = filteredAdminProducts,
          onAddProduct = { showNewProductDialog = true },
          onBulkUpdate = { showBulkDialog = true },
          onImportCsv = { showCsvImportDialog = true },
          onEditProduct = { editingProduct = it },
          onTogglePublish = { prod -> onSaveProduct(prod.copy(isPublished = !prod.isPublished), null) {} },
          onDeleteProduct = { onDeleteProduct(it.id) }
        )

        2 -> AdminOrdersTab(
          isBangla = isBangla,
          orders = orders,
          onUpdateStatus = onUpdateOrderStatus
        )

        3 -> AdminSettingsTab(
          isBangla = isBangla,
          isBusy = isBusy,
          storeConfig = storeConfig,
          categories = categories,
          coupons = coupons,
          onSaveConfig = onUpdateStoreConfig,
          onCreateCategory = onCreateCategory,
          onDeleteCategory = onDeleteCategory,
          onCreateCoupon = onCreateCoupon,
          onDeleteCoupon = onDeleteCoupon
        )
      }
    }
  }

  if (showNewProductDialog || editingProduct != null) {
    ProductEditorDialog(
      isBangla = isBangla,
      initialProduct = editingProduct,
      initialCostPrice = editingProduct?.let { productCosts[it.id] },
      categories = categories,
      onDismiss = {
        showNewProductDialog = false
        editingProduct = null
      },
      onSave = { prod, cost ->
        onSaveProduct(prod, cost) {
          showNewProductDialog = false
          editingProduct = null
        }
      }
    )
  }

  if (showBulkDialog) {
    BulkUpdateDialog(
      isBangla = isBangla,
      productCount = filteredAdminProducts.size,
      onDismiss = { showBulkDialog = false },
      onApply = { multiplier, disc, stockDelta ->
        onBulkUpdateProducts(filteredAdminProducts.map { it.id }, multiplier, disc, stockDelta)
        showBulkDialog = false
      }
    )
  }

  if (showCsvImportDialog) {
    CsvImportDialog(
      isBangla = isBangla,
      onDismiss = { showCsvImportDialog = false },
      onImport = { csv ->
        onImportCsv(csv)
        showCsvImportDialog = false
      }
    )
  }

  if (showBroadcastDialog) {
    BroadcastNotificationDialog(
      isBangla = isBangla,
      onDismiss = { showBroadcastDialog = false },
      onSend = { tEn, tBn, mEn, mBn ->
        onBroadcastPromo(tEn, tBn, mEn, mBn)
        showBroadcastDialog = false
      }
    )
  }
}

@Composable
private fun AdminOverviewTab(
  isBangla: Boolean,
  isBusy: Boolean,
  metrics: AdminDashboardMetrics,
  lowStockList: List<Product>,
  onSeed300: () -> Unit,
  onOpenBroadcast: () -> Unit,
  onExportCsv: () -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox(if (isBangla) "মোট বিক্রি" else "Total Sales", formatBdt(metrics.totalSales, isBangla), HotPinkPrimary, Modifier.weight(1f))
        MetricBox(if (isBangla) "সম্ভাব্য লাভ" else "Est. Profit", formatBdt(metrics.estimatedProfit, isBangla), EmeraldAccent, Modifier.weight(1f))
      }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox(if (isBangla) "আজকের বিক্রি" else "Today's Sales", formatBdt(metrics.todaysSales, isBangla), WarmGold, Modifier.weight(1f))
        MetricBox(if (isBangla) "মাসিক বিক্রি" else "Monthly Sales", formatBdt(metrics.monthlySales, isBangla), HotPinkPrimary, Modifier.weight(1f))
      }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox(if (isBangla) "মোট পণ্য" else "Total Products", "${metrics.totalProducts}", Color(0xFF2563EB), Modifier.weight(1f))
        MetricBox(if (isBangla) "মোট কাস্টমার" else "Customers", "${metrics.totalCustomers}", Color(0xFF7C3AED), Modifier.weight(1f))
        MetricBox(if (isBangla) "লো স্টক" else "Low Stock", "${metrics.lowStockProducts}", Color(0xFFDC2626), Modifier.weight(1f))
      }
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox("Pending", "${metrics.pendingOrders}", WarmGold, Modifier.weight(1f))
        MetricBox("Confirmed", "${metrics.confirmedOrders}", HotPinkPrimary, Modifier.weight(1f))
        MetricBox("Delivered", "${metrics.deliveredOrders}", EmeraldAccent, Modifier.weight(1f))
        MetricBox("Cancelled", "${metrics.cancelledOrders}", Color(0xFFDC2626), Modifier.weight(1f))
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(if (isBangla) "দ্রুত অ্যাডমিন অ্যাকশন" else "Quick Store Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Button(
            onClick = onSeed300,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().testTag("admin_seed_catalog_btn")
          ) {
            Icon(Icons.Rounded.Inventory, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isBangla) "৩১০+ পণ্যের ক্যাটালগ তৈরি করুন" else "Seed Full 310+ Product Catalog")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenBroadcast, modifier = Modifier.weight(1f)) {
              Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text(if (isBangla) "প্রমো অ্যালার্ট" else "Promo Alert")
            }
            OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f)) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("Export CSV")
            }
          }
        }
      }
    }

    if (lowStockList.isNotEmpty()) {
      item {
        Text(
          text = if (isBangla) "লো স্টক সতর্কতা (${lowStockList.size})" else "Low Stock Alerts (${lowStockList.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
      items(lowStockList.take(25), key = { it.id }) { prod ->
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(prod.displayName(isBangla), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Surface(color = Color(0xFFDC2626).copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
              Text(
                text = "Stock: ${prod.stockQuantity}",
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricBox(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
  }
}

@Composable
private fun AdminProductsTab(
  isBangla: Boolean,
  query: String,
  onQueryChange: (String) -> Unit,
  products: List<Product>,
  onAddProduct: () -> Unit,
  onBulkUpdate: () -> Unit,
  onImportCsv: () -> Unit,
  onEditProduct: (Product) -> Unit,
  onTogglePublish: (Product) -> Unit,
  onDeleteProduct: (Product) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Surface(color = Color.White, shadowElevation = 1.dp) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          placeholder = { Text(if (isBangla) "নাম বা SKU দিয়ে খুঁজুন..." else "Search by Name or SKU...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("admin_product_search")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = onAddProduct,
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.weight(1f).testTag("admin_add_product_btn")
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isBangla) "নতুন পণ্য" else "Add", style = MaterialTheme.typography.labelMedium)
          }
          OutlinedButton(onClick = onBulkUpdate, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (isBangla) "বাল্ক" else "Bulk", style = MaterialTheme.typography.labelMedium)
          }
          OutlinedButton(onClick = onImportCsv, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("CSV", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(products, key = { it.id }) { prod ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            ProductThumbnail(
              imageUrl = prod.imageUrls.firstOrNull(),
              contentDescription = prod.name,
              modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(prod.displayName(isBangla), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text("${prod.sku} • ${prod.categoryName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = "${formatBdt(prod.price, isBangla)}  •  Stock: ${prod.stockQuantity}",
                style = MaterialTheme.typography.labelMedium,
                color = HotPinkPrimary,
                fontWeight = FontWeight.Bold
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Row {
                IconButton(onClick = { onEditProduct(prod) }) {
                  Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = HotPinkPrimary)
                }
                IconButton(onClick = { onDeleteProduct(prod) }) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626))
                }
              }
              Surface(
                color = if (prod.isPublished) EmeraldAccent.copy(alpha = 0.14f) else Color.Gray.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp),
                onClick = { onTogglePublish(prod) }
              ) {
                Text(
                  text = if (prod.isPublished) "Published" else "Hidden",
                  color = if (prod.isPublished) EmeraldAccent else Color.DarkGray,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOrdersTab(
  isBangla: Boolean,
  orders: List<Order>,
  onUpdateStatus: (Order, String) -> Unit
) {
  var statusFilter by remember { mutableStateOf("ALL") }
  val filteredOrders = remember(orders, statusFilter) {
    if (statusFilter == "ALL") orders else orders.filter { it.status == statusFilter }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    FlowRow(
      modifier = Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      FilterChip(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }, label = { Text("All (${orders.size})") })
      OrderStatusOption.entries.forEach { st ->
        FilterChip(
          selected = statusFilter == st.code,
          onClick = { statusFilter = st.code },
          label = { Text(st.displayLabel(isBangla)) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HotPinkPrimary, selectedLabelColor = Color.White)
        )
      }
    }

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(filteredOrders, key = { it.id }) { order ->
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(order.orderNumber, fontWeight = FontWeight.ExtraBold)
              Text(formatBdt(order.totalAmount, isBangla), color = HotPinkPrimary, fontWeight = FontWeight.ExtraBold)
            }
            Text("${order.customerName} • ${order.mobileNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("${order.fullAddress}, ${order.upazila}, ${order.district} (${order.deliveryArea})", style = MaterialTheme.typography.bodySmall)
            Text(order.itemsSummary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Payment: ${order.paymentMethod}" + if (order.transactionId.isNotBlank()) " • TrxID: ${order.transactionId}" else "", style = MaterialTheme.typography.labelSmall)
            HorizontalDivider()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OrderStatusOption.entries.forEach { opt ->
                FilterChip(
                  selected = order.status == opt.code,
                  onClick = { onUpdateStatus(order, opt.code) },
                  label = { Text(opt.displayLabel(isBangla), style = MaterialTheme.typography.labelSmall) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HotPinkPrimary, selectedLabelColor = Color.White)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminSettingsTab(
  isBangla: Boolean,
  isBusy: Boolean,
  storeConfig: StoreConfig,
  categories: List<Category>,
  coupons: List<Coupon>,
  onSaveConfig: (StoreConfig) -> Unit,
  onCreateCategory: (String, String) -> Unit,
  onDeleteCategory: (String) -> Unit,
  onCreateCoupon: (String, String, Double, Double, Double) -> Unit,
  onDeleteCoupon: (String) -> Unit
) {
  var insideBogura by remember(storeConfig) { mutableStateOf(storeConfig.insideBoguraCharge.toLong().toString()) }
  var outsideBogura by remember(storeConfig) { mutableStateOf(storeConfig.outsideBoguraCharge.toLong().toString()) }
  var insideDhaka by remember(storeConfig) { mutableStateOf(storeConfig.insideDhakaCharge.toLong().toString()) }
  var outsideDhaka by remember(storeConfig) { mutableStateOf(storeConfig.outsideDhakaCharge.toLong().toString()) }
  var nationwide by remember(storeConfig) { mutableStateOf(storeConfig.nationwideCharge.toLong().toString()) }
  var perKgExtra by remember(storeConfig) { mutableStateOf(storeConfig.perKgExtraCharge.toLong().toString()) }
  var perItemExtra by remember(storeConfig) { mutableStateOf(storeConfig.perExtraItemCharge.toLong().toString()) }
  var freeMin by remember(storeConfig) { mutableStateOf(storeConfig.freeDeliveryMinOrder.toLong().toString()) }
  var freeActive by remember(storeConfig) { mutableStateOf(storeConfig.freeDeliveryActive) }
  var flashSaleActive by remember(storeConfig) { mutableStateOf(storeConfig.flashSaleActive) }

  var codEnabled by remember(storeConfig) { mutableStateOf(storeConfig.codEnabled) }
  var bkashEnabled by remember(storeConfig) { mutableStateOf(storeConfig.bkashEnabled) }
  var bkashNumber by remember(storeConfig) { mutableStateOf(storeConfig.bkashNumber) }
  var nagadEnabled by remember(storeConfig) { mutableStateOf(storeConfig.nagadEnabled) }
  var nagadNumber by remember(storeConfig) { mutableStateOf(storeConfig.nagadNumber) }
  var rocketEnabled by remember(storeConfig) { mutableStateOf(storeConfig.rocketEnabled) }
  var rocketNumber by remember(storeConfig) { mutableStateOf(storeConfig.rocketNumber) }
  var bankEnabled by remember(storeConfig) { mutableStateOf(storeConfig.bankEnabled) }
  var bankDetails by remember(storeConfig) { mutableStateOf(storeConfig.bankDetails) }

  var buy2Pct by remember(storeConfig) { mutableStateOf(storeConfig.buy2DiscountPercent.toString()) }
  var buy3Pct by remember(storeConfig) { mutableStateOf(storeConfig.buy3DiscountPercent.toString()) }
  var phone by remember(storeConfig) { mutableStateOf(storeConfig.contactPhone) }

  var newCatEn by remember { mutableStateOf("") }
  var newCatBn by remember { mutableStateOf("") }
  var newCouponCode by remember { mutableStateOf("") }
  var newCouponVal by remember { mutableStateOf("10") }

  LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(if (isBangla) "ডেলিভারি ও পেমেন্ট সেটিংস" else "Delivery & Payment Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = insideBogura, onValueChange = { insideBogura = it }, label = { Text("Inside Bogura ৳") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = outsideBogura, onValueChange = { outsideBogura = it }, label = { Text("Outside Bogura ৳") }, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = insideDhaka, onValueChange = { insideDhaka = it }, label = { Text("Inside Dhaka ৳") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = outsideDhaka, onValueChange = { outsideDhaka = it }, label = { Text("Outside Dhaka ৳") }, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = nationwide, onValueChange = { nationwide = it }, label = { Text("Nationwide ৳") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = freeMin, onValueChange = { freeMin = it }, label = { Text("Free Deliv Min ৳") }, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = perKgExtra, onValueChange = { perKgExtra = it }, label = { Text("Per Kg Extra ৳") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = perItemExtra, onValueChange = { perItemExtra = it }, label = { Text("Per Item Extra ৳") }, modifier = Modifier.weight(1f))
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Free Delivery Active")
            Switch(checked = freeActive, onCheckedChange = { freeActive = it })
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Flash Sale Active")
            Switch(checked = flashSaleActive, onCheckedChange = { flashSaleActive = it })
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = buy2Pct, onValueChange = { buy2Pct = it }, label = { Text("Buy 2 Bundle %") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = buy3Pct, onValueChange = { buy3Pct = it }, label = { Text("Buy 3 Bundle %") }, modifier = Modifier.weight(1f))
          }
          HorizontalDivider()
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cash on Delivery (COD)"); Switch(checked = codEnabled, onCheckedChange = { codEnabled = it })
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("bKash"); Switch(checked = bkashEnabled, onCheckedChange = { bkashEnabled = it })
          }
          OutlinedTextField(value = bkashNumber, onValueChange = { bkashNumber = it }, label = { Text("bKash Number") }, modifier = Modifier.fillMaxWidth())
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Nagad"); Switch(checked = nagadEnabled, onCheckedChange = { nagadEnabled = it })
          }
          OutlinedTextField(value = nagadNumber, onValueChange = { nagadNumber = it }, label = { Text("Nagad Number") }, modifier = Modifier.fillMaxWidth())
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Rocket"); Switch(checked = rocketEnabled, onCheckedChange = { rocketEnabled = it })
          }
          OutlinedTextField(value = rocketNumber, onValueChange = { rocketNumber = it }, label = { Text("Rocket Number") }, modifier = Modifier.fillMaxWidth())
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Bank Transfer"); Switch(checked = bankEnabled, onCheckedChange = { bankEnabled = it })
          }
          OutlinedTextField(value = bankDetails, onValueChange = { bankDetails = it }, label = { Text("Bank Details") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Business Hotline") }, modifier = Modifier.fillMaxWidth())

          Button(
            onClick = {
              onSaveConfig(
                storeConfig.copy(
                  insideBoguraCharge = insideBogura.toDoubleOrNull() ?: 50.0,
                  outsideBoguraCharge = outsideBogura.toDoubleOrNull() ?: 100.0,
                  insideDhakaCharge = insideDhaka.toDoubleOrNull() ?: 80.0,
                  outsideDhakaCharge = outsideDhaka.toDoubleOrNull() ?: 120.0,
                  nationwideCharge = nationwide.toDoubleOrNull() ?: 120.0,
                  perKgExtraCharge = perKgExtra.toDoubleOrNull() ?: 20.0,
                  perExtraItemCharge = perItemExtra.toDoubleOrNull() ?: 10.0,
                  freeDeliveryMinOrder = freeMin.toDoubleOrNull() ?: 2500.0,
                  freeDeliveryActive = freeActive,
                  flashSaleActive = flashSaleActive,
                  buy2DiscountPercent = buy2Pct.toLongOrNull() ?: 5L,
                  buy3DiscountPercent = buy3Pct.toLongOrNull() ?: 10L,
                  codEnabled = codEnabled,
                  bkashEnabled = bkashEnabled,
                  bkashNumber = bkashNumber.trim(),
                  nagadEnabled = nagadEnabled,
                  nagadNumber = nagadNumber.trim(),
                  rocketEnabled = rocketEnabled,
                  rocketNumber = rocketNumber.trim(),
                  bankEnabled = bankEnabled,
                  bankDetails = bankDetails.trim(),
                  contactPhone = phone.trim()
                )
              )
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = HotPinkPrimary, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_config_btn")
          ) {
            Icon(Icons.Rounded.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isBangla) "সেটিংস সংরক্ষণ করুন" else "Save Store Settings", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Categories CRUD
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(if (isBangla) "ক্যাটাগরি পরিচালনা" else "Categories (${categories.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = newCatEn, onValueChange = { newCatEn = it }, label = { Text("Name EN") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = newCatBn, onValueChange = { newCatBn = it }, label = { Text("নাম বাংলা") }, modifier = Modifier.weight(1f))
          }
          OutlinedButton(
            onClick = {
              if (newCatEn.isNotBlank()) {
                onCreateCategory(newCatEn.trim(), newCatBn.trim().ifBlank { newCatEn.trim() })
                newCatEn = ""; newCatBn = ""
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) { Text(if (isBangla) "+ নতুন ক্যাটাগরি যোগ করুন" else "+ Add Category") }
          categories.forEach { cat ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("${cat.nameBn} (${cat.name})", style = MaterialTheme.typography.bodySmall)
              IconButton(onClick = { onDeleteCategory(cat.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete category", tint = Color(0xFFDC2626))
              }
            }
          }
        }
      }
    }

    // Coupons CRUD
    item {
      Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(if (isBangla) "কুপন কোড পরিচালনা" else "Coupon Codes (${coupons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = newCouponCode, onValueChange = { newCouponCode = it }, label = { Text("Code (e.g. EID15)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = newCouponVal, onValueChange = { newCouponVal = it }, label = { Text("Discount %") }, modifier = Modifier.weight(1f))
          }
          OutlinedButton(
            onClick = {
              val pct = newCouponVal.toDoubleOrNull() ?: 10.0
              if (newCouponCode.isNotBlank()) {
                onCreateCoupon(newCouponCode.trim().uppercase(), "PERCENT", pct, 500.0, 500.0)
                newCouponCode = ""
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) { Text(if (isBangla) "+ নতুন কুপন তৈরি করুন" else "+ Create Coupon") }
          coupons.forEach { c ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("${c.code} (${c.discountValue}% / min ৳${c.minOrderAmount.toLong()})", style = MaterialTheme.typography.bodySmall)
              IconButton(onClick = { onDeleteCoupon(c.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete coupon", tint = Color(0xFFDC2626))
              }
            }
          }
        }
      }
    }
  }
}
