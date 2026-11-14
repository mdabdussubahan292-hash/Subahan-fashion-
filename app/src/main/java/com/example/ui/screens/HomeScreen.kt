package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.example.data.model.WishlistItem
import com.example.ui.components.CompactDealCard
import com.example.ui.components.ProductGridCard
import com.example.ui.theme.CrimsonRedDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.theme.SunnyYellow
import com.example.ui.util.ShopCopy
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
  isBangla: Boolean,
  isAdmin: Boolean,
  productsState: UiState<List<Product>>,
  categoriesState: UiState<List<Category>>,
  storeConfig: StoreConfig,
  wishlistItems: List<WishlistItem>,
  cartCount: Long,
  unreadNotificationsCount: Int,
  onToggleLanguage: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenCart: () -> Unit,
  onOpenAccount: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenAdmin: () -> Unit,
  onSelectCategory: (String) -> Unit,
  onOpenProduct: (Product) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (Product) -> Unit
) {
  val allProducts = (productsState as? UiState.Success)?.data.orEmpty().filter { it.isPublished }
  val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
  val wishlistedIds = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }

  // Live Flash Sale countdown ticker
  var remainingSeconds by remember { mutableIntStateOf(5 * 3600 + 42 * 60 + 18) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      remainingSeconds = if (remainingSeconds > 0) remainingSeconds - 1 else 6 * 3600
    }
  }
  val hrs = remainingSeconds / 3600
  val mins = (remainingSeconds % 3600) / 60
  val secs = remainingSeconds % 60

  val flashProducts = remember(allProducts) {
    allProducts.filter { it.isFlashSale }.ifEmpty { allProducts.take(10) }
  }
  val choiceProducts = remember(allProducts) {
    allProducts.filter { it.isChoiceDay }.ifEmpty { allProducts.drop(4).take(10) }
  }
  val bundleProducts = remember(allProducts) {
    allProducts.filter { it.isBundleEligible }.ifEmpty { allProducts.take(10) }
  }
  val trendProducts = remember(allProducts) {
    allProducts.filter { it.isFeatured }.ifEmpty { allProducts.take(10) }
  }
  val newArrivals = remember(allProducts) {
    allProducts.filter { it.isNewArrival }.ifEmpty { allProducts.take(10) }
  }
  val bestSelling = remember(allProducts) {
    allProducts.sortedByDescending { it.soldCount }.take(10)
  }
  val discountProducts = remember(allProducts) {
    allProducts.filter { it.discountPercent >= 15L }.sortedByDescending { it.discountPercent }.take(10)
  }
  val recommendedRows = remember(allProducts) {
    allProducts.take(40).chunked(2)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Branded Header + Search Bar
    Surface(
      color = HotPinkPrimary,
      shadowElevation = 4.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_subahan_logo),
              contentDescription = "Subahan Fashion Logo",
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp)),
              contentScale = ContentScale.Crop
            )
            Column {
              Text(
                text = ShopCopy.brandTitle(isBangla),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = if (isBangla) "বগুড়া • সারাদেশে হোম ডেলিভারি" else "Bogura • Nationwide Delivery",
                style = MaterialTheme.typography.labelSmall,
                color = SunnyYellow
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = Color.White.copy(alpha = 0.2f),
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier
                .clickable(onClick = onToggleLanguage)
                .testTag("language_toggle_btn")
            ) {
              Text(
                text = if (isBangla) "EN" else "বাং",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }

            if (isAdmin) {
              IconButton(
                onClick = onOpenAdmin,
                modifier = Modifier.testTag("header_admin_btn")
              ) {
                Icon(
                  imageVector = Icons.Rounded.AdminPanelSettings,
                  contentDescription = "Admin Dashboard",
                  tint = SunnyYellow
                )
              }
            }

            IconButton(
              onClick = onOpenNotifications,
              modifier = Modifier.testTag("header_notifications_btn")
            ) {
              BadgedBox(
                badge = {
                  if (unreadNotificationsCount > 0) {
                    Badge(containerColor = SunnyYellow, contentColor = Color.Black) {
                      Text("$unreadNotificationsCount")
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Rounded.Notifications,
                  contentDescription = "Notifications",
                  tint = Color.White
                )
              }
            }

            IconButton(
              onClick = onOpenCart,
              modifier = Modifier.testTag("header_cart_btn")
            ) {
              BadgedBox(
                badge = {
                  if (cartCount > 0L) {
                    Badge(containerColor = SunnyYellow, contentColor = Color.Black) {
                      Text("$cartCount")
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Rounded.ShoppingCart,
                  contentDescription = "Cart",
                  tint = Color.White
                )
              }
            }

            IconButton(
              onClick = onOpenAccount,
              modifier = Modifier.testTag("header_profile_btn")
            ) {
              Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "My Account",
                tint = Color.White
              )
            }
          }
        }

        // Search Input Trigger
        Surface(
          color = Color.White,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onOpenSearch)
            .testTag("home_search_bar")
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "Search",
              tint = HotPinkPrimary
            )
            Text(
              text = ShopCopy.searchHint(isBangla),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
            Surface(
              color = HotPinkPrimary,
              shape = RoundedCornerShape(16.dp)
            ) {
              Text(
                text = if (isBangla) "খুঁজুন" else "Search",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
        }
      }
    }

    // Main Storefront Feed
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Promotional Slider Banners
      item {
        PromoBannerSlider(
          isBangla = isBangla,
          storeConfig = storeConfig,
          onBannerClick = onOpenSearch
        )
      }

      // 2. Free Delivery / Special Offer Ribbon
      item {
        Surface(
          color = Color(0xFFFFF7D6),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(EmeraldAccent),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = ShopCopy.freeDeliveryBanner(isBangla, storeConfig.freeDeliveryMinOrder),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF3E2700),
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (isBangla)
                  "বগুড়া ৳${storeConfig.insideBoguraCharge.toInt()} • ঢাকা ৳${storeConfig.insideDhakaCharge.toInt()} • ক্যাশ অন ডেলিভারি"
                else
                  "Bogura ৳${storeConfig.insideBoguraCharge.toInt()} • Dhaka ৳${storeConfig.insideDhakaCharge.toInt()} • COD Available",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B4C00)
              )
            }
          }
        }
      }

      // 3. Category Icons Strip
      if (categories.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitleBar(
              title = if (isBangla) "জনপ্রিয় ক্যাটাগরি" else "Shop by Category",
              icon = Icons.Rounded.Category,
              accent = HotPinkPrimary
            )
            LazyRow(
              contentPadding = PaddingValues(horizontal = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              items(categories, key = { it.id }) { category ->
                CategoryChipBubble(
                  category = category,
                  isBangla = isBangla,
                  onClick = { onSelectCategory(category.id) }
                )
              }
            }
          }
        }
      }

      if (productsState is UiState.Loading) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = HotPinkPrimary)
          }
        }
      } else if (allProducts.isEmpty()) {
        item {
          EmptyCatalogNotice(isBangla = isBangla, isAdmin = isAdmin, onOpenAdmin = onOpenAdmin)
        }
      } else {
        // 4. Flash Sale Section
        item {
          Surface(
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Rounded.Bolt, contentDescription = null, tint = HotPinkPrimary)
                  Text(
                    text = ShopCopy.flashSale(isBangla),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = HotPinkPrimary
                  )
                }
                Surface(
                  color = Color(0xFF1A1D24),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = String.format("%02d : %02d : %02d", hrs, mins, secs),
                    style = MaterialTheme.typography.labelMedium,
                    color = SunnyYellow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
              LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(flashProducts, key = { "flash_${it.id}" }) { product ->
                  CompactDealCard(
                    product = product,
                    isBangla = isBangla,
                    onClick = { onOpenProduct(product) }
                  )
                }
              }
            }
          }
        }

        // 5. Choice Day / Special Offers
        item {
          HorizontalSectionBlock(
            title = ShopCopy.choiceDay(isBangla),
            subtitle = if (isBangla) "সীমিত সময়ের এক্সক্লুসিভ চয়েস ডিল" else "Curated Choice Day Specials",
            icon = Icons.Rounded.AutoAwesome,
            badgeBg = SunnyYellow,
            badgeText = Color(0xFF3E2700),
            products = choiceProducts,
            isBangla = isBangla,
            onOpenProduct = onOpenProduct
          )
        }

        // 6. Save More / Bundle Offers
        item {
          HorizontalSectionBlock(
            title = ShopCopy.saveMore(isBangla),
            subtitle = if (isBangla)
              "২টিতে ${storeConfig.buy2DiscountPercent}% ও ৩টিতে ${storeConfig.buy3DiscountPercent}% অতিরিক্ত ছাড়!"
            else
              "Buy 2 get ${storeConfig.buy2DiscountPercent}% off • Buy 3 get ${storeConfig.buy3DiscountPercent}% off!",
            icon = Icons.Rounded.CardGiftcard,
            badgeBg = EmeraldAccent,
            badgeText = Color.White,
            products = bundleProducts,
            isBangla = isBangla,
            onOpenProduct = onOpenProduct
          )
        }

        // 7. Fashion Trends
        item {
          HorizontalSectionBlock(
            title = ShopCopy.fashionTrends(isBangla),
            subtitle = if (isBangla) "ট্রেন্ডিং কালেকশন" else "Trending Styles",
            icon = Icons.Rounded.Checkroom,
            badgeBg = HotPinkPrimary,
            badgeText = Color.White,
            products = trendProducts,
            isBangla = isBangla,
            onOpenProduct = onOpenProduct
          )
        }

        // 8. New Arrivals
        item {
          HorizontalSectionBlock(
            title = ShopCopy.newArrivals(isBangla),
            subtitle = null,
            icon = Icons.Rounded.NewReleases,
            badgeBg = Color(0xFF2563EB),
            badgeText = Color.White,
            products = newArrivals,
            isBangla = isBangla,
            onOpenProduct = onOpenProduct
          )
        }

        // 9. Best Selling Products
        item {
          HorizontalSectionBlock(
            title = ShopCopy.bestSelling(isBangla),
            subtitle = null,
            icon = Icons.Rounded.LocalFireDepartment,
            badgeBg = CrimsonRedDark,
            badgeText = Color.White,
            products = bestSelling,
            isBangla = isBangla,
            onOpenProduct = onOpenProduct
          )
        }

        // 10. Discount Products
        item {
          HorizontalSectionBlock(
            title = ShopCopy.discountSection(isBangla),
            subtitle = null,
            icon = Icons.Rounded.LocalOffer,
            badgeBg = HotPinkPrimary,
            badgeText = Color.White,
            products = discountProducts,
            isBangla = isBangla,
            onOpenProduct = onOpenProduct
          )
        }

        // 11. Recommended Products 2-Column Grid
        item {
          SectionTitleBar(
            title = ShopCopy.recommended(isBangla) + " (${allProducts.size})",
            icon = Icons.Rounded.AutoAwesome,
            accent = HotPinkPrimary
          )
        }

        items(recommendedRows, key = { row -> row.joinToString("_") { it.id } }) { pair ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            pair.forEach { product ->
              ProductGridCard(
                product = product,
                isBangla = isBangla,
                isWishlisted = product.id in wishlistedIds,
                onClick = { onOpenProduct(product) },
                onAddToCart = { onAddToCart(product) },
                onToggleWishlist = { onToggleWishlist(product) },
                modifier = Modifier.weight(1f)
              )
            }
            if (pair.size == 1) {
              Spacer(Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PromoBannerSlider(
  isBangla: Boolean,
  storeConfig: StoreConfig,
  onBannerClick: () -> Unit
) {
  val banners = remember(isBangla, storeConfig) {
    listOf(
      Triple(
        R.drawable.img_banner_fashion_sale,
        if (isBangla) "মেগা ফ্যাশন সেল — ৫০% পর্যন্ত ছাড়!" else "Mega Fashion Sale — Up to 50% OFF!",
        if (isBangla) "প্রিমিয়াম মানিব্যাগ, ঘড়ি ও পারফিউম কালেকশন" else "Premium Wallets, Watches & Perfumes"
      ),
      Triple(
        R.drawable.img_banner_choice_day,
        if (isBangla) "চয়েস ডে বান্ডেল অফার" else "Choice Day Bundle Deals",
        if (isBangla) "৩টি পণ্য কিনলে ${storeConfig.buy3DiscountPercent}% অতিরিক্ত ছাড়!" else "Buy 3 & Save ${storeConfig.buy3DiscountPercent}% Extra!"
      ),
      Triple(
        R.drawable.img_banner_home_living,
        if (isBangla) "ঝাড়বাতি লাইট ও সেভিং বক্স" else "Chandelier Lights & Home Living",
        if (isBangla) "সারাদেশে নিরাপদ হোম ডেলিভারি" else "Safe Nationwide Home Delivery"
      )
    )
  }
  val pagerState = rememberPagerState(pageCount = { banners.size })

  LaunchedEffect(pagerState) {
    while (true) {
      delay(4000L)
      val next = (pagerState.currentPage + 1) % banners.size
      pagerState.animateScrollToPage(next)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 10.dp, start = 12.dp, end = 12.dp)
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxWidth()
        .height(168.dp)
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onBannerClick)
    ) { page ->
      val (imgRes, headline, subline) = banners[page]
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(id = imgRes),
          contentDescription = headline,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(16.dp)
            .fillMaxWidth(0.7f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(color = SunnyYellow, shape = RoundedCornerShape(6.dp)) {
            Text(
              text = if (isBangla) "সুবাহান ফ্যাশন" else "SUBAHAN FASHION",
              style = MaterialTheme.typography.labelSmall,
              color = Color.Black,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = subline,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
          )
        }
      }
    }

    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      repeat(banners.size) { idx ->
        Box(
          modifier = Modifier
            .size(if (pagerState.currentPage == idx) 18.dp else 7.dp, 7.dp)
            .clip(CircleShape)
            .background(
              if (pagerState.currentPage == idx) SunnyYellow else Color.White.copy(alpha = 0.6f)
            )
        )
      }
    }
  }
}

@Composable
private fun CategoryChipBubble(
  category: Category,
  isBangla: Boolean,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(74.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(52.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = categoryIconFor(category.iconKey),
          contentDescription = category.displayName(isBangla),
          tint = HotPinkPrimary,
          modifier = Modifier.size(26.dp)
        )
      }
    }
    Text(
      text = category.displayName(isBangla),
      style = MaterialTheme.typography.labelSmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
  }
}

fun categoryIconFor(key: String): ImageVector = when (key) {
  "wallet" -> Icons.Rounded.Wallet
  "savings" -> Icons.Rounded.Savings
  "light" -> Icons.Rounded.Lightbulb
  "watch" -> Icons.Rounded.Watch
  "toys" -> Icons.Rounded.Toys
  "spa" -> Icons.Rounded.Spa
  "face" -> Icons.Rounded.Face
  "checkroom" -> Icons.Rounded.Checkroom
  "diamond" -> Icons.Rounded.Diamond
  "sunglasses" -> Icons.Rounded.AutoAwesome
  "devices" -> Icons.Rounded.Devices
  "home" -> Icons.Rounded.Home
  "new_releases" -> Icons.Rounded.NewReleases
  else -> Icons.Rounded.Category
}

@Composable
private fun SectionTitleBar(
  title: String,
  icon: ImageVector,
  accent: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold
    )
  }
}

@Composable
private fun HorizontalSectionBlock(
  title: String,
  subtitle: String?,
  icon: ImageVector,
  badgeBg: Color,
  badgeText: Color,
  products: List<Product>,
  isBangla: Boolean,
  onOpenProduct: (Product) -> Unit
) {
  if (products.isEmpty()) return
  Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(color = badgeBg, shape = CircleShape, modifier = Modifier.size(26.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = badgeText, modifier = Modifier.size(16.dp))
              }
            }
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold
            )
          }
          if (subtitle != null) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }
      LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(products, key = { "${title}_${it.id}" }) { product ->
          CompactDealCard(
            product = product,
            isBangla = isBangla,
            onClick = { onOpenProduct(product) }
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyCatalogNotice(
  isBangla: Boolean,
  isAdmin: Boolean,
  onOpenAdmin: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Rounded.Category,
        contentDescription = null,
        tint = HotPinkPrimary,
        modifier = Modifier.size(44.dp)
      )
      Text(
        text = if (isBangla) "ক্যাটালগ লোড হচ্ছে বা খালি" else "Store catalog is ready for setup",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      if (isAdmin) {
        Surface(
          color = HotPinkPrimary,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.clickable(onClick = onOpenAdmin)
        ) {
          Text(
            text = if (isBangla) "অ্যাডমিন প্যানেল থেকে ৩০০+ পণ্য যোগ করুন" else "Open Admin Panel to Seed 300+ Products",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
          )
        }
      }
    }
  }
}
