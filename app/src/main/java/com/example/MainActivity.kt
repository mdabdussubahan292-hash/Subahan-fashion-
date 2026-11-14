package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.preferences.LanguagePreferences
import com.example.data.repository.ShopRepository
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CategoriesAndSearchScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.screens.signOutUser
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.theme.SubahanFashionTheme
import com.example.ui.theme.SunnyYellow
import com.example.ui.util.ShopCopy
import com.example.ui.viewmodel.ShopViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private enum class MainTab {
  HOME,
  CATEGORIES,
  CART,
  ORDERS,
  ACCOUNT
}

private sealed interface SubScreen {
  data class ProductDetail(val productId: String) : SubScreen
  data object Checkout : SubScreen
  data object Notifications : SubScreen
  data object Admin : SubScreen
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SubahanFashionTheme {
        SubahanRootApp()
      }
    }
  }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
private fun SubahanRootApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val currentUser by remember { Firebase.auth.authStateFlow() }
    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = scope
      )
    }
  }

  val signedInUser = currentUser
  if (signedInUser == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(signedInUser.uid) {
      val appContext = context.applicationContext
      val shopViewModel: ShopViewModel = viewModel(
        key = signedInUser.uid,
        factory = viewModelFactory {
          initializer {
            ShopViewModel(
              repository = ShopRepository(appContext),
              languagePrefs = LanguagePreferences(appContext),
              currentUserId = signedInUser.uid
            )
          }
        }
      )
      SubahanStorefrontShell(
        viewModel = shopViewModel,
        onSignOut = {
          signOutUser(credentialManager = credentialManager, scope = scope)
        }
      )
    }
  }
}

@Composable
private fun SubahanStorefrontShell(
  viewModel: ShopViewModel,
  onSignOut: () -> Unit
) {
  val isBangla by viewModel.isBangla.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

  val productsState by viewModel.productsState.collectAsStateWithLifecycle()
  val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
  val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
  val coupons by viewModel.coupons.collectAsStateWithLifecycle()
  val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
  val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
  val customerOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
  val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
  val onlyInStock by viewModel.onlyInStock.collectAsStateWithLifecycle()
  val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
  val cartPricing by viewModel.cartPricing.collectAsStateWithLifecycle()
  val selectedDeliveryArea by viewModel.selectedDeliveryArea.collectAsStateWithLifecycle()
  val appliedCouponCode by viewModel.appliedCouponCode.collectAsStateWithLifecycle()

  val adminMetrics by viewModel.adminMetrics.collectAsStateWithLifecycle()
  val adminOrders by viewModel.adminAllOrders.collectAsStateWithLifecycle()
  val adminProductCosts by viewModel.adminProductCosts.collectAsStateWithLifecycle()

  var activeTab by remember { mutableStateOf(MainTab.HOME) }
  var activeSubScreen by remember { mutableStateOf<SubScreen?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(toastMessage) {
    val msg = toastMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  val allProducts = (productsState as? UiState.Success)?.data.orEmpty()
  val allCategories = (categoriesState as? UiState.Success)?.data.orEmpty()
  val cartBadgeCount = cartPricing.totalItemsCount

  when (val sub = activeSubScreen) {
    is SubScreen.ProductDetail -> {
      val product = allProducts.firstOrNull { it.id == sub.productId }
      if (product == null) {
        LaunchedEffect(sub.productId) { activeSubScreen = null }
      } else {
        ProductDetailScreen(
          product = product,
          isBangla = isBangla,
          isWishlisted = wishlistItems.any { it.productId == product.id },
          storeConfig = storeConfig,
          reviewsFlow = viewModel.observeProductReviews(product.id),
          onBack = { activeSubScreen = null },
          onToggleWishlist = { viewModel.toggleWishlist(product) },
          onAddToCart = { qty, color, size -> viewModel.addToCart(product, qty, color, size) },
          onBuyNow = { qty, color, size ->
            viewModel.addToCart(product, qty, color, size)
            activeSubScreen = SubScreen.Checkout
          },
          onSubmitReview = { rating, comment -> viewModel.submitReview(product.id, rating, comment) }
        )
      }
    }

    is SubScreen.Checkout -> {
      CheckoutScreen(
        isBangla = isBangla,
        isBusy = isBusy,
        userProfile = userProfile,
        storeConfig = storeConfig,
        pricing = cartPricing,
        selectedDeliveryArea = selectedDeliveryArea,
        onSelectDeliveryArea = viewModel::setDeliveryArea,
        onBack = { activeSubScreen = null },
        onPlaceOrder = { name, mobile, altMobile, address, div, dist, upz, area, note, payMethod, trx, sender ->
          viewModel.placeOrder(
            customerName = name,
            mobileNumber = mobile,
            altMobileNumber = altMobile,
            fullAddress = address,
            division = div,
            district = dist,
            upazila = upz,
            deliveryArea = area,
            orderNote = note,
            paymentMethod = payMethod,
            transactionId = trx,
            senderPhone = sender,
            onSuccess = {
              activeSubScreen = null
              activeTab = MainTab.ORDERS
            }
          )
        }
      )
    }

    is SubScreen.Notifications -> {
      NotificationsScreen(
        isBangla = isBangla,
        notifications = notifications,
        onBack = { activeSubScreen = null }
      )
    }

    is SubScreen.Admin -> {
      if (!isAdmin) {
        LaunchedEffect(isAdmin) { activeSubScreen = null }
      } else {
        AdminDashboardScreen(
          isBangla = isBangla,
          isBusy = isBusy,
          metrics = adminMetrics,
          products = allProducts,
          categories = allCategories,
          orders = adminOrders,
          coupons = coupons,
          storeConfig = storeConfig,
          productCosts = adminProductCosts,
          onBack = { activeSubScreen = null },
          onSeed300Products = viewModel::seedFull300Catalog,
          onSaveProduct = viewModel::saveProductAdmin,
          onDeleteProduct = viewModel::deleteProductAdmin,
          onBulkUpdateProducts = viewModel::bulkUpdateProductsAdmin,
          onCreateCategory = viewModel::createCategoryAdmin,
          onDeleteCategory = viewModel::deleteCategoryAdmin,
          onUpdateStoreConfig = viewModel::updateStoreConfigAdmin,
          onCreateCoupon = viewModel::createCouponAdmin,
          onDeleteCoupon = viewModel::deleteCouponAdmin,
          onUpdateOrderStatus = viewModel::updateOrderStatusAdmin,
          onBroadcastPromo = viewModel::broadcastPromoAdmin,
          onImportCsv = viewModel::importCsvProductsAdmin,
          onBuildCsvExport = viewModel::buildProductsCsvExport
        )
      }
    }

    null -> {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp
        Scaffold(
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          bottomBar = {
            if (!useRail) {
              NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
                MainTab.entries.forEach { tab ->
                  val label = when (tab) {
                    MainTab.HOME -> ShopCopy.navHome(isBangla)
                    MainTab.CATEGORIES -> ShopCopy.navCategories(isBangla)
                    MainTab.CART -> ShopCopy.navCart(isBangla)
                    MainTab.ORDERS -> ShopCopy.navOrders(isBangla)
                    MainTab.ACCOUNT -> ShopCopy.navAccount(isBangla)
                  }
                  val icon = when (tab) {
                    MainTab.HOME -> Icons.Rounded.Home
                    MainTab.CATEGORIES -> Icons.Rounded.Category
                    MainTab.CART -> Icons.Rounded.ShoppingCart
                    MainTab.ORDERS -> Icons.AutoMirrored.Rounded.ReceiptLong
                    MainTab.ACCOUNT -> Icons.Rounded.Person
                  }
                  NavigationBarItem(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    icon = {
                      if (tab == MainTab.CART && cartBadgeCount > 0L) {
                        BadgedBox(
                          badge = {
                            Badge(containerColor = SunnyYellow, contentColor = Color.Black) {
                              Text("$cartBadgeCount")
                            }
                          }
                        ) {
                          Icon(icon, contentDescription = label)
                        }
                      } else {
                        Icon(icon, contentDescription = label)
                      }
                    },
                    label = {
                      Text(label, maxLines = 1, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal)
                    },
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = HotPinkPrimary,
                      selectedTextColor = HotPinkPrimary,
                      indicatorColor = HotPinkPrimary.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                  )
                }
              }
            }
          }
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .windowInsetsPadding(WindowInsets.statusBars)
          ) {
            if (useRail) {
              NavigationRail(containerColor = Color.White) {
                MainTab.entries.forEach { tab ->
                  val label = when (tab) {
                    MainTab.HOME -> ShopCopy.navHome(isBangla)
                    MainTab.CATEGORIES -> ShopCopy.navCategories(isBangla)
                    MainTab.CART -> ShopCopy.navCart(isBangla)
                    MainTab.ORDERS -> ShopCopy.navOrders(isBangla)
                    MainTab.ACCOUNT -> ShopCopy.navAccount(isBangla)
                  }
                  val icon = when (tab) {
                    MainTab.HOME -> Icons.Rounded.Home
                    MainTab.CATEGORIES -> Icons.Rounded.Category
                    MainTab.CART -> Icons.Rounded.ShoppingCart
                    MainTab.ORDERS -> Icons.AutoMirrored.Rounded.ReceiptLong
                    MainTab.ACCOUNT -> Icons.Rounded.Person
                  }
                  NavigationRailItem(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label) }
                  )
                }
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
              contentAlignment = Alignment.TopCenter
            ) {
              Box(modifier = Modifier.widthIn(max = 1040.dp).fillMaxSize()) {
              when (activeTab) {
                MainTab.HOME -> HomeScreen(
                  isBangla = isBangla,
                  isAdmin = isAdmin,
                  productsState = productsState,
                  categoriesState = categoriesState,
                  storeConfig = storeConfig,
                  wishlistItems = wishlistItems,
                  cartCount = cartBadgeCount,
                  unreadNotificationsCount = notifications.count { !it.isRead },
                  onToggleLanguage = viewModel::toggleLanguage,
                  onOpenSearch = { activeTab = MainTab.CATEGORIES },
                  onOpenCart = { activeTab = MainTab.CART },
                  onOpenAccount = { activeTab = MainTab.ACCOUNT },
                  onOpenNotifications = { activeSubScreen = SubScreen.Notifications },
                  onOpenAdmin = { activeSubScreen = SubScreen.Admin },
                  onSelectCategory = { catId ->
                    viewModel.selectCategory(catId)
                    activeTab = MainTab.CATEGORIES
                  },
                  onOpenProduct = { prod -> activeSubScreen = SubScreen.ProductDetail(prod.id) },
                  onAddToCart = { prod -> viewModel.addToCart(prod) },
                  onToggleWishlist = { prod -> viewModel.toggleWishlist(prod) }
                )

                MainTab.CATEGORIES -> CategoriesAndSearchScreen(
                  isBangla = isBangla,
                  categoriesState = categoriesState,
                  filteredProducts = filteredProducts,
                  wishlistItems = wishlistItems,
                  searchQuery = searchQuery,
                  selectedCategoryId = selectedCategoryId,
                  sortOption = sortOption,
                  onlyInStock = onlyInStock,
                  onSearchQueryChange = viewModel::setSearchQuery,
                  onSelectCategory = viewModel::selectCategory,
                  onSortChange = viewModel::setSortOption,
                  onToggleInStock = viewModel::setOnlyInStock,
                  onOpenProduct = { prod -> activeSubScreen = SubScreen.ProductDetail(prod.id) },
                  onAddToCart = { prod -> viewModel.addToCart(prod) },
                  onToggleWishlist = { prod -> viewModel.toggleWishlist(prod) }
                )

                MainTab.CART -> CartScreen(
                  isBangla = isBangla,
                  cartItems = cartItems,
                  pricing = cartPricing,
                  selectedDeliveryArea = selectedDeliveryArea,
                  appliedCouponCode = appliedCouponCode,
                  onSelectDeliveryArea = viewModel::setDeliveryArea,
                  onApplyCoupon = viewModel::applyCoupon,
                  onUpdateQty = viewModel::updateCartQuantity,
                  onRemoveItem = viewModel::removeFromCart,
                  onProceedToCheckout = { activeSubScreen = SubScreen.Checkout },
                  onContinueShopping = { activeTab = MainTab.HOME }
                )

                MainTab.ORDERS -> OrdersScreen(
                  isBangla = isBangla,
                  ordersState = customerOrders,
                  onCancelOrder = viewModel::cancelOrder,
                  onExploreStore = { activeTab = MainTab.HOME }
                )

                MainTab.ACCOUNT -> AccountScreen(
                  isBangla = isBangla,
                  isAdmin = isAdmin,
                  isBusy = isBusy,
                  userProfile = userProfile,
                  wishlistItems = wishlistItems,
                  allProducts = allProducts,
                  storeConfig = storeConfig,
                  onToggleLanguage = viewModel::toggleLanguage,
                  onSaveProfile = viewModel::saveProfile,
                  onOpenProduct = { prod -> activeSubScreen = SubScreen.ProductDetail(prod.id) },
                  onAddToCart = { prod -> viewModel.addToCart(prod) },
                  onOpenAdmin = { if (isAdmin) activeSubScreen = SubScreen.Admin },
                  onOpenNotifications = { activeSubScreen = SubScreen.Notifications },
                  onSignOut = onSignOut
                )
              }
              }
            }
          }
        }
      }
    }
  }
}
