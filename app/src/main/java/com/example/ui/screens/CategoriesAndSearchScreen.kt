package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.WishlistItem
import com.example.ui.components.ProductGridCard
import com.example.ui.theme.HotPinkPrimary
import com.example.ui.util.ShopCopy
import com.example.ui.viewmodel.ProductSortOption
import com.example.ui.viewmodel.UiState

@Composable
fun CategoriesAndSearchScreen(
  isBangla: Boolean,
  categoriesState: UiState<List<Category>>,
  filteredProducts: List<Product>,
  wishlistItems: List<WishlistItem>,
  searchQuery: String,
  selectedCategoryId: String?,
  sortOption: ProductSortOption,
  onlyInStock: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onSelectCategory: (String?) -> Unit,
  onSortChange: (ProductSortOption) -> Unit,
  onToggleInStock: (Boolean) -> Unit,
  onOpenProduct: (Product) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (Product) -> Unit
) {
  val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
  val wishlistedIds = remember(wishlistItems) { wishlistItems.map { it.productId }.toSet() }
  val productRows = remember(filteredProducts) { filteredProducts.chunked(2) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Search Header
    Surface(color = Color.White, shadowElevation = 2.dp) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text(ShopCopy.searchHint(isBangla)) },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = HotPinkPrimary) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HotPinkPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("catalog_search_input")
        )

        // Category filter pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedCategoryId == null,
              onClick = { onSelectCategory(null) },
              label = { Text(if (isBangla) "সব পণ্য" else "All Products") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HotPinkPrimary,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("cat_chip_all")
            )
          }
          items(categories, key = { it.id }) { cat ->
            FilterChip(
              selected = selectedCategoryId == cat.id,
              onClick = { onSelectCategory(if (selectedCategoryId == cat.id) null else cat.id) },
              leadingIcon = {
                Icon(
                  imageVector = categoryIconFor(cat.iconKey),
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
              },
              label = { Text(cat.displayName(isBangla)) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HotPinkPrimary,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
              ),
              modifier = Modifier.testTag("cat_chip_${cat.id}")
            )
          }
        }

        // Sort & In-Stock Filter Row
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          item {
            Icon(
              Icons.Rounded.FilterList,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
          items(ProductSortOption.entries) { option ->
            FilterChip(
              selected = sortOption == option,
              onClick = { onSortChange(option) },
              label = { Text(if (isBangla) option.labelBn else option.labelEn) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
              )
            )
          }
          item {
            FilterChip(
              selected = onlyInStock,
              onClick = { onToggleInStock(!onlyInStock) },
              label = { Text(if (isBangla) "শুধু স্টকে আছে" else "In Stock Only") }
            )
          }
        }
      }
    }

    // Category Directory Strip when no search/category active
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (selectedCategoryId == null && searchQuery.isBlank() && categories.isNotEmpty()) {
        item {
          Text(
            text = if (isBangla) "সকল ক্যাটাগরি (${categories.size})" else "All Categories (${categories.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        items(categories.chunked(3), key = { row -> "dir_" + row.joinToString("_") { it.id } }) { trio ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            trio.forEach { cat ->
              Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onSelectCategory(cat.id) }
              ) {
                Column(
                  modifier = Modifier.padding(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = categoryIconFor(cat.iconKey),
                    contentDescription = cat.displayName(isBangla),
                    tint = HotPinkPrimary,
                    modifier = Modifier.size(26.dp)
                  )
                  Text(
                    text = cat.displayName(isBangla),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
            repeat(3 - trio.size) {
              Spacer(Modifier.weight(1f))
            }
          }
        }
      }

      item {
        Text(
          text = if (isBangla) "${filteredProducts.size}টি পণ্য পাওয়া গেছে" else "${filteredProducts.size} products found",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold
        )
      }

      if (filteredProducts.isEmpty()) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Rounded.Inventory2,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp)
            )
            Text(
              text = if (isBangla) "কোনো পণ্য পাওয়া যায়নি" else "No matching products found",
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
      } else {
        items(productRows, key = { row -> row.joinToString("_") { it.id } }) { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
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
