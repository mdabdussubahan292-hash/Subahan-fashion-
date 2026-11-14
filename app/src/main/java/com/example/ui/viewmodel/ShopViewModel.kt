package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.DeliveryAreaOption
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.data.preferences.LanguagePreferences
import com.example.data.repository.ShopRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class ProductSortOption(val labelEn: String, val labelBn: String) {
  NEWEST("Newest", "নতুনতম"),
  BEST_SELLING("Best Selling", "বেশি বিক্রিত"),
  LOWEST_PRICE("Lowest Price", "কম দাম"),
  HIGHEST_PRICE("Highest Price", "বেশি দাম"),
  DISCOUNT("Biggest Discount", "সর্বোচ্চ ছাড়")
}

data class CartPricingBreakdown(
  val subtotal: Double = 0.0,
  val deliveryCharge: Double = 0.0,
  val bundleDiscount: Double = 0.0,
  val couponDiscount: Double = 0.0,
  val totalDiscount: Double = 0.0,
  val totalPayable: Double = 0.0,
  val totalItemsCount: Long = 0L,
  val freeDeliveryApplied: Boolean = false
)

data class AdminDashboardMetrics(
  val totalProducts: Int = 0,
  val totalOrders: Int = 0,
  val pendingOrders: Int = 0,
  val confirmedOrders: Int = 0,
  val deliveredOrders: Int = 0,
  val cancelledOrders: Int = 0,
  val totalSales: Double = 0.0,
  val todaysSales: Double = 0.0,
  val monthlySales: Double = 0.0,
  val totalCustomers: Int = 0,
  val lowStockProducts: Int = 0,
  val estimatedProfit: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(
  private val repository: ShopRepository,
  private val languagePrefs: LanguagePreferences,
  val currentUserId: String
) : ViewModel() {

  val isBangla: StateFlow<Boolean> = languagePrefs.isBanglaFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

  private val _isAdmin = MutableStateFlow(repository.isCurrentUserBootstrappedAdmin())
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  // Search & Filters
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategoryId = MutableStateFlow<String?>(null)
  val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

  private val _sortOption = MutableStateFlow(ProductSortOption.NEWEST)
  val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

  private val _onlyInStock = MutableStateFlow(false)
  val onlyInStock: StateFlow<Boolean> = _onlyInStock.asStateFlow()

  private val _selectedDeliveryArea = MutableStateFlow(DeliveryAreaOption.INSIDE_BOGURA.code)
  val selectedDeliveryArea: StateFlow<String> = _selectedDeliveryArea.asStateFlow()

  private val _appliedCouponCode = MutableStateFlow("")
  val appliedCouponCode: StateFlow<String> = _appliedCouponCode.asStateFlow()

  // Safe Flow Wrapper
  private fun <T> Flow<T>.toUiState(tag: String, fallbackMsg: String): Flow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "$tag stream error", rootError)
        emit(UiState.Error(rootError.message ?: fallbackMsg))
      }

  val productsState: StateFlow<UiState<List<Product>>> = _isAdmin
    .flatMapLatest { admin ->
      if (admin) repository.observeAllProductsAdmin() else repository.observePublishedProducts()
    }
    .toUiState("Products", "Failed to load products")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val categoriesState: StateFlow<UiState<List<Category>>> = repository.observeActiveCategories()
    .toUiState("Categories", "Failed to load categories")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val storeConfig: StateFlow<StoreConfig> = repository.observeStoreConfig()
    .catch { error ->
      val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
      if (fsErr == null && error is CancellationException) throw error
      Log.w(TAG, "StoreConfig stream fallback", fsErr ?: error)
      emit(StoreConfig())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StoreConfig())

  val coupons: StateFlow<List<Coupon>> = repository.observeActiveCoupons()
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  private val rawCartItems: Flow<List<CartItem>> = repository.observeCart(currentUserId)
    .catch { emit(emptyList()) }

  val cartItems: StateFlow<List<CartItem>> = combine(rawCartItems, productsState) { items, prodState ->
    val liveProducts = (prodState as? UiState.Success)?.data?.associateBy { it.id }.orEmpty()
    items.map { item ->
      val live = liveProducts[item.productId]
      if (live != null) {
        item.copy(
          unitPrice = live.price,
          previousPrice = live.previousPrice,
          deliveryChargeOverride = live.deliveryChargeOverride,
          weightKg = live.weightKg
        )
      } else {
        item
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val wishlistItems: StateFlow<List<WishlistItem>> = repository.observeWishlist(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val customerOrders: StateFlow<UiState<List<Order>>> = repository.observeCustomerOrders(currentUserId)
    .toUiState("Orders", "Failed to load orders")
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val notifications: StateFlow<List<NotificationItem>> = repository.observeNotifications(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
    .catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  // Admin-only streams
  val adminAllOrders: StateFlow<List<Order>> = _isAdmin
    .flatMapLatest { admin -> if (admin) repository.observeAllOrdersAdmin() else flowOf(emptyList()) }
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminCustomers: StateFlow<List<UserProfile>> = _isAdmin
    .flatMapLatest { admin -> if (admin) repository.observeAllCustomersAdmin() else flowOf(emptyList()) }
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminProductCosts: StateFlow<Map<String, Double>> = _isAdmin
    .flatMapLatest { admin -> if (admin) repository.observeProductCostsAdmin() else flowOf(emptyMap()) }
    .catch { emit(emptyMap()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

  val filteredProducts: StateFlow<List<Product>> = combine(
    productsState,
    _searchQuery,
    _selectedCategoryId,
    _sortOption,
    _onlyInStock
  ) { state, query, catId, sort, inStockOnly ->
    val all = (state as? UiState.Success)?.data.orEmpty().filter { it.isPublished }
    val q = query.trim().lowercase()
    val filtered = all.filter { prod ->
      val matchesQuery = q.isBlank() ||
        prod.name.lowercase().contains(q) ||
        prod.nameBn.lowercase().contains(q) ||
        prod.sku.lowercase().contains(q) ||
        prod.categoryName.lowercase().contains(q)
      val matchesCat = catId == null || prod.categoryId == catId
      val matchesStock = !inStockOnly || prod.stockQuantity > 0L
      matchesQuery && matchesCat && matchesStock
    }
    when (sort) {
      ProductSortOption.NEWEST -> filtered.sortedByDescending { it.isNewArrival }
      ProductSortOption.BEST_SELLING -> filtered.sortedByDescending { it.soldCount }
      ProductSortOption.LOWEST_PRICE -> filtered.sortedBy { it.price }
      ProductSortOption.HIGHEST_PRICE -> filtered.sortedByDescending { it.price }
      ProductSortOption.DISCOUNT -> filtered.sortedByDescending { it.discountPercent }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val cartPricing: StateFlow<CartPricingBreakdown> = combine(
    cartItems,
    storeConfig,
    _selectedDeliveryArea,
    _appliedCouponCode,
    coupons
  ) { items, config, areaCode, couponCode, couponList ->
    calculatePricing(items, config, areaCode, couponCode, couponList)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CartPricingBreakdown())

  val adminMetrics: StateFlow<AdminDashboardMetrics> = combine(
    productsState,
    adminAllOrders,
    adminCustomers,
    adminProductCosts
  ) { prodState, orders, customers, costs ->
    val prods = (prodState as? UiState.Success)?.data.orEmpty()
    val nowCal = Calendar.getInstance()
    val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)
    val todayYear = nowCal.get(Calendar.YEAR)
    val todayMonth = nowCal.get(Calendar.MONTH)

    var totalSales = 0.0
    var todaySales = 0.0
    var monthlySales = 0.0
    var estProfit = 0.0

    for (ord in orders) {
      if (ord.status != "Cancelled" && ord.status != "Returned") {
        totalSales += ord.totalAmount
        val ordCal = Calendar.getInstance().apply {
          timeInMillis = ord.createdAt?.toDate()?.time ?: 0L
        }
        if (ord.createdAt != null) {
          if (ordCal.get(Calendar.YEAR) == todayYear && ordCal.get(Calendar.DAY_OF_YEAR) == todayDay) {
            todaySales += ord.totalAmount
          }
          if (ordCal.get(Calendar.YEAR) == todayYear && ordCal.get(Calendar.MONTH) == todayMonth) {
            monthlySales += ord.totalAmount
          }
        }
        for (line in ord.items) {
          val unitCost = costs[line.productId] ?: (line.unitPrice * 0.6)
          estProfit += (line.unitPrice - unitCost) * line.qty
        }
      }
    }

    AdminDashboardMetrics(
      totalProducts = prods.size,
      totalOrders = orders.size,
      pendingOrders = orders.count { it.status == "Pending" },
      confirmedOrders = orders.count { it.status == "Confirmed" },
      deliveredOrders = orders.count { it.status == "Delivered" },
      cancelledOrders = orders.count { it.status == "Cancelled" },
      totalSales = totalSales,
      todaysSales = todaySales,
      monthlySales = monthlySales,
      totalCustomers = customers.size,
      lowStockProducts = prods.count { it.stockQuantity <= 5L },
      estimatedProfit = estProfit
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminDashboardMetrics())

  init {
    viewModelScope.launch {
      repository.ensureUserProfile()
      val adminStatus = repository.checkIsAdmin(currentUserId)
      _isAdmin.value = adminStatus
      if (adminStatus) {
        repository.bootstrapStore(seedFull300 = false)
      }
    }
  }

  private fun calculatePricing(
    items: List<CartItem>,
    config: StoreConfig,
    areaCode: String,
    couponCode: String,
    couponList: List<Coupon>
  ): CartPricingBreakdown {
    if (items.isEmpty()) return CartPricingBreakdown()
    val subtotal = items.sumOf { it.unitPrice * it.quantity }
    val totalCount = items.sumOf { it.quantity }
    val totalWeight = items.sumOf { it.weightKg * it.quantity }

    // Base delivery by zone
    val zoneCharge = when (areaCode) {
      DeliveryAreaOption.INSIDE_BOGURA.code -> config.insideBoguraCharge
      DeliveryAreaOption.OUTSIDE_BOGURA.code -> config.outsideBoguraCharge
      DeliveryAreaOption.INSIDE_DHAKA.code -> config.insideDhakaCharge
      DeliveryAreaOption.OUTSIDE_DHAKA.code -> config.outsideDhakaCharge
      else -> config.nationwideCharge
    }

    // Special delivery override if all items specify an override (e.g., 0 for Free Delivery item)
    val overrides = items.mapNotNull { if (it.deliveryChargeOverride >= 0.0) it.deliveryChargeOverride else null }
    val baseDelivery = if (overrides.size == items.size && overrides.isNotEmpty()) {
      overrides.maxOrNull() ?: zoneCharge
    } else {
      val extraWeightCost = if (totalWeight > 1.0) ceil(totalWeight - 1.0) * config.perKgExtraCharge else 0.0
      val extraQtyCost = if (totalCount > 2L) (totalCount - 2L) * config.perExtraItemCharge else 0.0
      zoneCharge + extraWeightCost + extraQtyCost
    }

    val freeDeliveryApplied = config.freeDeliveryActive && subtotal >= config.freeDeliveryMinOrder
    val finalDelivery = if (freeDeliveryApplied) 0.0 else baseDelivery

    // Bundle discounts (Buy 2 / Buy 3)
    val bundlePct = when {
      totalCount >= 3L -> config.buy3DiscountPercent
      totalCount == 2L -> config.buy2DiscountPercent
      else -> 0L
    }
    val bundleDiscount = (subtotal * (bundlePct / 100.0)).roundToLong().toDouble()

    // Coupon discount
    val cleanCode = couponCode.trim().uppercase()
    val matchedCoupon = couponList.firstOrNull { it.code.equals(cleanCode, ignoreCase = true) && it.isActive }
    val couponDiscount = if (matchedCoupon != null && subtotal >= matchedCoupon.minOrderAmount) {
      val raw = if (matchedCoupon.discountType == "PERCENT") {
        subtotal * (matchedCoupon.discountValue / 100.0)
      } else {
        matchedCoupon.discountValue
      }
      min(raw, matchedCoupon.maxDiscountAmount).roundToLong().toDouble()
    } else 0.0

    val totalDiscount = min(subtotal, bundleDiscount + couponDiscount)
    val totalPayable = max(0.0, subtotal + finalDelivery - totalDiscount)

    return CartPricingBreakdown(
      subtotal = subtotal,
      deliveryCharge = finalDelivery,
      bundleDiscount = bundleDiscount,
      couponDiscount = couponDiscount,
      totalDiscount = totalDiscount,
      totalPayable = totalPayable,
      totalItemsCount = totalCount,
      freeDeliveryApplied = freeDeliveryApplied
    )
  }

  fun toggleLanguage() {
    viewModelScope.launch {
      languagePrefs.setBangla(!isBangla.value)
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectCategory(categoryId: String?) {
    _selectedCategoryId.value = categoryId
  }

  fun setSortOption(option: ProductSortOption) {
    _sortOption.value = option
  }

  fun setOnlyInStock(onlyInStock: Boolean) {
    _onlyInStock.value = onlyInStock
  }

  fun setDeliveryArea(areaCode: String) {
    _selectedDeliveryArea.value = areaCode
  }

  fun applyCoupon(code: String) {
    val clean = code.trim().uppercase()
    val found = coupons.value.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.isActive }
    if (found != null) {
      _appliedCouponCode.value = found.code
      showToast(if (isBangla.value) "কুপন ${found.code} প্রয়োগ করা হয়েছে!" else "Coupon ${found.code} applied!")
    } else {
      _appliedCouponCode.value = ""
      showToast(if (isBangla.value) "সঠিক কুপন কোড দিন" else "Invalid or expired coupon code")
    }
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  private fun showToast(msg: String) {
    _toastMessage.value = msg
  }

  // --- CUSTOMER ACTIONS ---

  fun addToCart(product: Product, qty: Long = 1L, color: String = "", size: String = "") {
    if (product.stockQuantity <= 0L) {
      showToast(if (isBangla.value) "দুঃখিত, পণ্যটি স্টকে নেই" else "Product is out of stock")
      return
    }
    viewModelScope.launch {
      val res = repository.addToCart(product, qty, color, size)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "কার্টে যোগ হয়েছে" else "Added to cart")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to add to cart")
      }
    }
  }

  fun updateCartQuantity(productId: String, newQty: Long) {
    viewModelScope.launch {
      repository.updateCartQuantity(productId, newQty)
    }
  }

  fun removeFromCart(productId: String) {
    viewModelScope.launch {
      repository.removeFromCart(productId)
    }
  }

  fun toggleWishlist(product: Product) {
    val currentlyIn = wishlistItems.value.any { it.productId == product.id }
    viewModelScope.launch {
      val res = repository.toggleWishlist(product, currentlyIn)
      if (res.isSuccess) {
        showToast(
          if (currentlyIn) {
            if (isBangla.value) "উইশলিস্ট থেকে সরানো হয়েছে" else "Removed from wishlist"
          } else {
            if (isBangla.value) "উইশলিস্টে সংরক্ষিত হয়েছে" else "Saved to wishlist"
          }
        )
      }
    }
  }

  fun placeOrder(
    customerName: String,
    mobileNumber: String,
    altMobileNumber: String,
    fullAddress: String,
    division: String,
    district: String,
    upazila: String,
    deliveryArea: String,
    orderNote: String,
    paymentMethod: String,
    transactionId: String,
    senderPhone: String,
    onSuccess: (String) -> Unit
  ) {
    val items = cartItems.value
    if (items.isEmpty()) {
      showToast(if (isBangla.value) "কার্ট খালি" else "Cart is empty")
      return
    }
    val pricing = cartPricing.value
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.placeOrder(
        customerName = customerName,
        mobileNumber = mobileNumber,
        altMobileNumber = altMobileNumber,
        fullAddress = fullAddress,
        division = division,
        district = district,
        upazila = upazila,
        deliveryArea = deliveryArea,
        orderNote = orderNote,
        paymentMethod = paymentMethod,
        transactionId = transactionId,
        senderPhone = senderPhone,
        cartItems = items,
        subtotal = pricing.subtotal,
        deliveryCharge = pricing.deliveryCharge,
        discountAmount = pricing.totalDiscount,
        couponCode = _appliedCouponCode.value,
        totalAmount = pricing.totalPayable
      )
      _isBusy.value = false
      if (res.isSuccess) {
        _appliedCouponCode.value = ""
        val orderNum = res.getOrThrow()
        showToast(if (isBangla.value) "অর্ডার সম্পন্ন: $orderNum" else "Order placed: $orderNum")
        onSuccess(orderNum)
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Order failed")
      }
    }
  }

  fun cancelOrder(orderId: String) {
    viewModelScope.launch {
      val res = repository.cancelCustomerOrder(orderId)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "অর্ডার বাতিল হয়েছে" else "Order cancelled")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Cannot cancel order")
      }
    }
  }

  fun saveProfile(profile: UserProfile) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.saveUserProfile(profile)
      _isBusy.value = false
      if (res.isSuccess) {
        showToast(if (isBangla.value) "প্রোফাইল সংরক্ষিত হয়েছে" else "Profile saved")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile")
      }
    }
  }

  fun observeProductReviews(productId: String): Flow<List<ProductReview>> =
    repository.observeReviews(productId).catch { emit(emptyList()) }

  fun submitReview(productId: String, rating: Long, comment: String) {
    val name = userProfile.value?.displayName?.ifBlank { "Customer" } ?: "Customer"
    viewModelScope.launch {
      val res = repository.submitReview(productId, rating, comment, name)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "রিভিউ জমা হয়েছে!" else "Review submitted!")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to submit review")
      }
    }
  }

  // --- ADMIN ACTIONS ---

  fun seedFull300Catalog() {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.bootstrapStore(seedFull300 = true)
      _isBusy.value = false
      if (res.isSuccess) {
        showToast(if (isBangla.value) "৩১০+ পণ্যের ক্যাটালগ সফলভাবে তৈরি হয়েছে!" else "310+ products seeded into catalog!")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Catalog seed failed")
      }
    }
  }

  fun saveProductAdmin(product: Product, costPrice: Double? = null, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.upsertProduct(product, costPrice)
      _isBusy.value = false
      if (res.isSuccess) {
        showToast(if (isBangla.value) "পণ্য সংরক্ষিত হয়েছে" else "Product saved")
        onDone()
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Failed to save product")
      }
    }
  }

  fun deleteProductAdmin(productId: String) {
    viewModelScope.launch {
      val res = repository.deleteProduct(productId)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "পণ্য মুছে ফেলা হয়েছে" else "Product deleted")
      }
    }
  }

  fun bulkUpdateProductsAdmin(
    productIds: List<String>,
    priceMultiplier: Double? = null,
    discountPercentOverride: Long? = null,
    stockDelta: Long? = null
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.bulkUpdateProducts(productIds, priceMultiplier, discountPercentOverride, stockDelta)
      _isBusy.value = false
      if (res.isSuccess) {
        showToast(if (isBangla.value) "বাল্ক আপডেট সম্পন্ন হয়েছে" else "Bulk update completed")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Bulk update failed")
      }
    }
  }

  fun createCategoryAdmin(nameEn: String, nameBn: String) {
    viewModelScope.launch {
      val res = repository.createCategory(nameEn, nameBn)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "নতুন ক্যাটাগরি যোগ হয়েছে" else "Category added")
      }
    }
  }

  fun deleteCategoryAdmin(categoryId: String) {
    viewModelScope.launch {
      repository.deleteCategory(categoryId)
    }
  }

  fun updateStoreConfigAdmin(config: StoreConfig) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateStoreConfig(config)
      _isBusy.value = false
      if (res.isSuccess) {
        showToast(if (isBangla.value) "সেটিংস আপডেট হয়েছে" else "Settings updated")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "Settings update failed")
      }
    }
  }

  fun createCouponAdmin(code: String, type: String, value: Double, minOrder: Double, maxDisc: Double) {
    viewModelScope.launch {
      val res = repository.createCoupon(code, type, value, minOrder, maxDisc)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "কুপন তৈরি হয়েছে" else "Coupon created")
      }
    }
  }

  fun deleteCouponAdmin(couponId: String) {
    viewModelScope.launch {
      repository.deleteCoupon(couponId)
    }
  }

  fun updateOrderStatusAdmin(order: Order, newStatus: String) {
    viewModelScope.launch {
      val res = repository.updateOrderStatusAdmin(order, newStatus)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "অর্ডার স্ট্যাটাস: $newStatus" else "Order status updated to $newStatus")
      }
    }
  }

  fun broadcastPromoAdmin(titleEn: String, titleBn: String, msgEn: String, msgBn: String) {
    viewModelScope.launch {
      val res = repository.broadcastNotificationAdmin(titleEn, titleBn, msgEn, msgBn)
      if (res.isSuccess) {
        showToast(if (isBangla.value) "নোটিফিকেশন পাঠানো হয়েছে" else "Notification broadcast sent!")
      }
    }
  }

  fun importCsvProductsAdmin(csvText: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.importProductsFromCsv(csvText)
      _isBusy.value = false
      if (res.isSuccess) {
        val count = res.getOrDefault(0)
        showToast(if (isBangla.value) "$count টি পণ্য ইম্পোর্ট হয়েছে" else "Imported $count products from CSV")
      } else {
        showToast(res.exceptionOrNull()?.localizedMessage ?: "CSV import failed")
      }
    }
  }

  fun buildProductsCsvExport(): String {
    val products = (productsState.value as? UiState.Success)?.data.orEmpty()
    val sb = StringBuilder("sku,name,nameBn,categoryId,price,stockQuantity,discountPercent\n")
    for (p in products) {
      val safeName = p.name.replace(",", " ")
      val safeNameBn = p.nameBn.replace(",", " ")
      sb.append("${p.sku},$safeName,$safeNameBn,${p.categoryId},${p.price},${p.stockQuantity},${p.discountPercent}\n")
    }
    return sb.toString()
  }

  companion object {
    private const val TAG = "ShopViewModel"
  }
}
