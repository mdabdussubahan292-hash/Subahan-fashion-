package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.ProductCost
import com.example.data.model.ProductReview
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShopRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(db.app)
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun isCurrentUserBootstrappedAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.isEmailVerified && user.email.equals(ADMIN_EMAIL, ignoreCase = true)
  }

  suspend fun checkIsAdmin(uid: String): Boolean {
    if (isCurrentUserBootstrappedAdmin()) return true
    val path = "admins/$uid"
    return try {
      val doc = db.collection("admins").document(uid).get().await()
      doc.exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  private fun <T> Flow<T>.catchFirestore(op: OperationType, path: String, uid: String? = null): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && (uid == null || auth.currentUser?.uid == uid)) {
        handleFirestoreError(exceptionToLog, op, path)
      }
      throw error
    }

  // --- PRODUCTS ---

  fun observePublishedProducts(): Flow<List<Product>> = flow {
    val uid = requireUserId()
    val path = "products"
    emitAll(
      db.collection(path)
        .whereEqualTo("isPublished", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  fun observeAllProductsAdmin(): Flow<List<Product>> = flow {
    val uid = requireUserId()
    val path = "products"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  suspend fun getProductById(productId: String): Result<Product?> {
    val path = "products/$productId"
    return try {
      requireUserId()
      val snap = db.collection("products").document(productId).get().await()
      Result.success(snap.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun upsertProduct(product: Product, costPrice: Double? = null): Result<String> {
    val id = product.id.ifBlank { "prod_${UUID.randomUUID().toString().take(8)}" }
    val docRef = db.collection("products").document(id)
    return try {
      requireUserId()
      val existingSnap = if (product.id.isNotBlank()) docRef.get().await() else null
      if (existingSnap == null || !existingSnap.exists()) {
        docRef.set(product.toCreateMap()).await()
      } else {
        val serverCreatedAt = existingSnap.getTimestamp("createdAt")
        docRef.set(product.copy(id = id, createdAt = serverCreatedAt, updatedAt = null)).await()
      }
      if (costPrice != null && costPrice >= 0.0) {
        db.collection("product_costs").document(id).set(
          mapOf(
            "productId" to id,
            "costPrice" to costPrice,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(productId: String): Result<Unit> {
    val docRef = db.collection("products").document(productId)
    return try {
      requireUserId()
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun bulkUpdateProducts(
    productIds: List<String>,
    priceMultiplier: Double? = null,
    discountPercentOverride: Long? = null,
    stockDelta: Long? = null
  ): Result<Unit> {
    return try {
      requireUserId()
      for (chunk in productIds.chunked(400)) {
        val batch = db.batch()
        for (pid in chunk) {
          val ref = db.collection("products").document(pid)
          val snap = ref.get().await()
          val prod = snap.toObject(Product::class.java) ?: continue
          val newPrice = if (priceMultiplier != null) {
            kotlin.math.max(1.0, kotlin.math.round(prod.price * priceMultiplier))
          } else prod.price
          val newDiscount = discountPercentOverride ?: prod.discountPercent
          val newStock = if (stockDelta != null) {
            kotlin.math.max(0L, prod.stockQuantity + stockDelta)
          } else prod.stockQuantity
          batch.update(
            ref,
            mapOf(
              "price" to newPrice,
              "discountPercent" to newDiscount,
              "stockQuantity" to newStock,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "products")
      Result.failure(e)
    }
  }

  fun observeProductCostsAdmin(): Flow<Map<String, Double>> = flow {
    val uid = requireUserId()
    val path = "product_costs"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(ProductCost::class.java).associate { it.productId to it.costPrice }
        }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  // --- REVIEWS ---

  fun observeReviews(productId: String): Flow<List<ProductReview>> = flow {
    val uid = requireUserId()
    val path = "products/$productId/reviews"
    emitAll(
      db.collection("products").document(productId).collection("reviews")
        .whereGreaterThanOrEqualTo("rating", 1)
        .snapshots()
        .map { snap ->
          snap.toObjects(ProductReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  suspend fun submitReview(productId: String, rating: Long, comment: String, userName: String): Result<Unit> {
    val uid = requireUserId()
    val reviewId = "rev_$uid"
    val docRef = db.collection("products").document(productId).collection("reviews").document(reviewId)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "rating" to rating.coerceIn(1L, 5L),
            "comment" to comment.trim(),
            "userName" to userName.ifBlank { "Customer" },
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "productId" to productId,
            "userId" to uid,
            "userName" to userName.ifBlank { "Customer" },
            "rating" to rating.coerceIn(1L, 5L),
            "comment" to comment.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  // --- CATEGORIES ---

  fun observeActiveCategories(): Flow<List<Category>> = flow {
    val uid = requireUserId()
    val path = "categories"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.sortOrder }
        }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  suspend fun createCategory(nameEn: String, nameBn: String, iconKey: String = "category", sortOrder: Long = 99L): Result<String> {
    val id = "cat_${UUID.randomUUID().toString().take(8)}"
    val docRef = db.collection("categories").document(id)
    return try {
      requireUserId()
      docRef.set(
        mapOf(
          "name" to nameEn.trim(),
          "nameBn" to nameBn.trim().ifBlank { nameEn.trim() },
          "iconKey" to iconKey,
          "sortOrder" to sortOrder,
          "isActive" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCategory(categoryId: String): Result<Unit> {
    val docRef = db.collection("categories").document(categoryId)
    return try {
      requireUserId()
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- STORE CONFIG ---

  fun observeStoreConfig(): Flow<StoreConfig> = flow {
    val uid = requireUserId()
    val path = "store_config/settings"
    emitAll(
      db.collection("store_config").document("settings")
        .snapshots()
        .map { snap ->
          snap.toObject(StoreConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: StoreConfig()
        }
        .catchFirestore(OperationType.GET, path, uid)
    )
  }

  suspend fun updateStoreConfig(config: StoreConfig): Result<Unit> {
    val docRef = db.collection("store_config").document("settings")
    return try {
      requireUserId()
      val existing = docRef.get().await()
      if (existing.exists()) {
        val serverCreatedAt = existing.getTimestamp("createdAt")
        docRef.set(config.copy(isPublicConfig = true, createdAt = serverCreatedAt, updatedAt = null)).await()
      } else {
        docRef.set(config.copy(isPublicConfig = true, createdAt = null, updatedAt = null)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- COUPONS ---

  fun observeActiveCoupons(): Flow<List<Coupon>> = flow {
    val uid = requireUserId()
    val path = "coupons"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap -> snap.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  suspend fun createCoupon(
    code: String,
    discountType: String,
    discountValue: Double,
    minOrderAmount: Double,
    maxDiscountAmount: Double
  ): Result<String> {
    val cleanCode = code.trim().uppercase().replace(" ", "")
    val id = "cp_${cleanCode.lowercase()}"
    val docRef = db.collection("coupons").document(id)
    return try {
      requireUserId()
      val existing = docRef.get().await()
      val baseMap = mapOf(
        "code" to cleanCode,
        "discountType" to discountType,
        "discountValue" to discountValue,
        "minOrderAmount" to minOrderAmount,
        "maxDiscountAmount" to maxDiscountAmount,
        "isActive" to true,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (existing.exists()) {
        docRef.update(baseMap).await()
      } else {
        docRef.set(baseMap + ("createdAt" to FieldValue.serverTimestamp())).await()
      }
      Result.success(id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCoupon(couponId: String): Result<Unit> {
    val docRef = db.collection("coupons").document(couponId)
    return try {
      requireUserId()
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- USER PROFILE ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catchFirestore(OperationType.GET, path, uid)
    )
  }

  fun observeAllCustomersAdmin(): Flow<List<UserProfile>> = flow {
    val uid = requireUserId()
    val path = "users"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  suspend fun ensureUserProfile(): Result<Unit> {
    val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(user.uid)
    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        docRef.set(
          mapOf(
            "userId" to user.uid,
            "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Subahan Customer"),
            "email" to (user.email ?: ""),
            "phone" to (user.phoneNumber ?: ""),
            "altPhone" to "",
            "defaultAddress" to "",
            "division" to "Rajshahi",
            "district" to "Bogura",
            "upazila" to "Sariakandi",
            "deliveryArea" to "INSIDE_BOGURA",
            "languagePref" to "bn",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snap = docRef.get().await()
      val payload = mapOf(
        "displayName" to profile.displayName.ifBlank { "Customer" },
        "email" to profile.email,
        "phone" to profile.phone,
        "altPhone" to profile.altPhone,
        "defaultAddress" to profile.defaultAddress,
        "division" to profile.division,
        "district" to profile.district,
        "upazila" to profile.upazila,
        "deliveryArea" to profile.deliveryArea,
        "languagePref" to profile.languagePref,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (snap.exists()) {
        docRef.update(payload).await()
      } else {
        docRef.set(
          payload + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  // --- CART ---

  fun observeCart(userId: String): Flow<List<CartItem>> = flow {
    val path = "users/$userId/cart"
    emitAll(
      db.collection("users").document(userId).collection("cart")
        .snapshots()
        .map { snap ->
          snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, userId)
    )
  }

  suspend fun addToCart(
    product: Product,
    quantity: Long = 1L,
    selectedColor: String = "",
    selectedSize: String = ""
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("cart").document(product.id)
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        val currentQty = snap.getLong("quantity") ?: 1L
        docRef.update(
          mapOf(
            "quantity" to (currentQty + quantity).coerceIn(1L, 500L),
            "selectedColor" to selectedColor.ifBlank { product.colors.firstOrNull() ?: "" },
            "selectedSize" to selectedSize.ifBlank { product.sizes.firstOrNull() ?: "" },
            "unitPrice" to product.price,
            "previousPrice" to product.previousPrice,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "productId" to product.id,
            "productName" to product.name,
            "productNameBn" to product.nameBn,
            "sku" to product.sku,
            "imageUrl" to (product.imageUrls.firstOrNull() ?: ""),
            "unitPrice" to product.price,
            "previousPrice" to product.previousPrice,
            "quantity" to quantity.coerceIn(1L, 500L),
            "selectedColor" to selectedColor.ifBlank { product.colors.firstOrNull() ?: "" },
            "selectedSize" to selectedSize.ifBlank { product.sizes.firstOrNull() ?: "" },
            "deliveryChargeOverride" to product.deliveryChargeOverride,
            "weightKg" to product.weightKg,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateCartQuantity(productId: String, newQuantity: Long): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("cart").document(productId)
    return try {
      if (newQuantity <= 0L) {
        docRef.delete().await()
      } else {
        docRef.update(
          mapOf(
            "quantity" to newQuantity.coerceIn(1L, 500L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun removeFromCart(productId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("cart").document(productId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- WISHLIST ---

  fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
    val path = "users/$userId/wishlist"
    emitAll(
      db.collection("users").document(userId).collection("wishlist")
        .snapshots()
        .map { snap -> snap.toObjects(WishlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catchFirestore(OperationType.LIST, path, userId)
    )
  }

  suspend fun toggleWishlist(product: Product, currentlyInWishlist: Boolean): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("wishlist").document(product.id)
    return try {
      if (currentlyInWishlist) {
        docRef.delete().await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "productId" to product.id,
            "productName" to product.name,
            "productNameBn" to product.nameBn,
            "price" to product.price,
            "previousPrice" to product.previousPrice,
            "imageUrl" to (product.imageUrls.firstOrNull() ?: ""),
            "categoryName" to product.categoryName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  // --- ORDERS ---

  fun observeCustomerOrders(userId: String): Flow<List<Order>> = flow {
    val path = "orders"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, userId)
    )
  }

  fun observeAllOrdersAdmin(): Flow<List<Order>> = flow {
    val uid = requireUserId()
    val path = "orders"
    emitAll(
      db.collection(path)
        .snapshots()
        .map { snap ->
          snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, uid)
    )
  }

  suspend fun placeOrder(
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
    cartItems: List<CartItem>,
    subtotal: Double,
    deliveryCharge: Double,
    discountAmount: Double,
    couponCode: String,
    totalAmount: Double
  ): Result<String> {
    val uid = requireUserId()
    require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
    val orderId = "ord_${UUID.randomUUID().toString().take(10)}"
    val orderNumber = "SF-${(100000..999999).random()}"
    val docRef = db.collection("orders").document(orderId)

    return try {
      val lineItems = cartItems.take(50).map { item ->
        mapOf(
          "productId" to item.productId,
          "name" to item.productName,
          "sku" to item.sku,
          "qty" to item.quantity,
          "unitPrice" to item.unitPrice,
          "color" to item.selectedColor,
          "size" to item.selectedSize,
          "imageUrl" to item.imageUrl
        )
      }
      val summary = cartItems.joinToString(", ") { "${it.productName} x${it.quantity}" }.take(950)

      val payload = mapOf(
        "orderNumber" to orderNumber,
        "userId" to uid,
        "customerName" to customerName.trim(),
        "mobileNumber" to mobileNumber.trim(),
        "altMobileNumber" to altMobileNumber.trim(),
        "fullAddress" to fullAddress.trim(),
        "division" to division.trim(),
        "district" to district.trim(),
        "upazila" to upazila.trim(),
        "deliveryArea" to deliveryArea,
        "orderNote" to orderNote.trim(),
        "paymentMethod" to paymentMethod,
        "transactionId" to transactionId.trim(),
        "senderPhone" to senderPhone.trim(),
        "itemsSummary" to summary,
        "items" to lineItems,
        "subtotal" to subtotal,
        "deliveryCharge" to deliveryCharge,
        "discountAmount" to discountAmount,
        "couponCode" to couponCode.trim().uppercase(),
        "totalAmount" to totalAmount,
        "totalCost" to 0.0,
        "status" to "Pending",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()

      // Clear customer cart in batch
      val clearBatch = db.batch()
      for (item in cartItems) {
        clearBatch.delete(db.collection("users").document(uid).collection("cart").document(item.productId))
      }
      clearBatch.commit().await()

      // Best-effort stock decrement & soldCount increment per item
      for (item in cartItems.take(50)) {
        runCatching {
          val prodRef = db.collection("products").document(item.productId)
          val prodSnap = prodRef.get().await()
          if (prodSnap.exists() && prodSnap.getBoolean("isPublished") == true) {
            val currentStock = prodSnap.getLong("stockQuantity") ?: 0L
            val currentSold = prodSnap.getLong("soldCount") ?: 0L
            val newStock = kotlin.math.max(0L, currentStock - item.quantity)
            val newSold = currentSold + item.quantity
            prodRef.update(
              mapOf(
                "stockQuantity" to newStock,
                "soldCount" to newSold,
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          }
        }
      }

      // Best-effort notification for customer
      val notifRef = db.collection("notifications").document("notif_${orderId}")
      runCatching {
        notifRef.set(
          mapOf(
            "userId" to uid,
            "title" to "Order Placed: $orderNumber",
            "titleBn" to "অর্ডার সম্পন্ন হয়েছে: $orderNumber",
            "message" to "Thank you! Your order of ৳${totalAmount.toLong()} is Pending confirmation.",
            "messageBn" to "ধন্যবাদ! আপনার ৳${totalAmount.toLong()} টাকার অর্ডারটি কনফার্মেশনের অপেক্ষায় আছে।",
            "type" to "ORDER_UPDATE",
            "targetId" to orderId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      Result.success(orderNumber)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun cancelCustomerOrder(orderId: String): Result<Unit> {
    val docRef = db.collection("orders").document(orderId)
    return try {
      requireUserId()
      docRef.update(
        mapOf(
          "status" to "Cancelled",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateOrderStatusAdmin(order: Order, newStatus: String): Result<Unit> {
    val docRef = db.collection("orders").document(order.id)
    return try {
      requireUserId()
      docRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      // Send notification to customer about status change
      val notifId = "notif_ord_${order.id}_${newStatus.lowercase()}_${UUID.randomUUID().toString().take(6)}"
      runCatching {
        db.collection("notifications").document(notifId).set(
          mapOf(
            "userId" to order.userId,
            "title" to "Order ${order.orderNumber} is $newStatus",
            "titleBn" to "অর্ডার ${order.orderNumber} এখন $newStatus",
            "message" to "Your Subahan Fashion order status has been updated to $newStatus.",
            "messageBn" to "আপনার সুবাহান ফ্যাশন অর্ডারের স্ট্যাটাস এখন: $newStatus।",
            "type" to "ORDER_UPDATE",
            "targetId" to order.id,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- NOTIFICATIONS ---

  fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
    val path = "notifications"
    emitAll(
      db.collection(path)
        .whereIn("userId", listOf(userId, "ALL"))
        .snapshots()
        .map { snap ->
          snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catchFirestore(OperationType.LIST, path, userId)
    )
  }

  suspend fun broadcastNotificationAdmin(
    titleEn: String,
    titleBn: String,
    messageEn: String,
    messageBn: String,
    type: String = "PROMO"
  ): Result<Unit> {
    val id = "notif_${UUID.randomUUID().toString().take(8)}"
    val docRef = db.collection("notifications").document(id)
    return try {
      requireUserId()
      docRef.set(
        mapOf(
          "userId" to "ALL",
          "title" to titleEn,
          "titleBn" to titleBn.ifBlank { titleEn },
          "message" to messageEn,
          "messageBn" to messageBn.ifBlank { messageEn },
          "type" to type,
          "targetId" to "",
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- BOOTSTRAP & CSV EXPORT / IMPORT ---

  suspend fun bootstrapStore(seedFull300: Boolean = false): Result<Unit> {
    return try {
      requireUserId()
      CatalogSeeder.ensureStoreBootstrapped(db, seedFull300)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "store_config/settings")
      Result.failure(e)
    }
  }

  suspend fun importProductsFromCsv(csvText: String): Result<Int> {
    return try {
      requireUserId()
      val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
      if (lines.size <= 1) return Result.success(0)
      val dataLines = if (lines.first().lowercase().contains("sku")) lines.drop(1) else lines
      var imported = 0
      val batch = db.batch()
      for ((idx, line) in dataLines.take(350).withIndex()) {
        val cols = line.split(",").map { it.trim() }
        if (cols.size < 4) continue
        val sku = cols[0].ifBlank { "CSV-${1000 + idx}" }
        val name = cols[1].ifBlank { "Product $sku" }
        val nameBn = cols.getOrNull(2)?.ifBlank { name } ?: name
        val catId = cols.getOrNull(3)?.ifBlank { "cat_others" } ?: "cat_others"
        val price = cols.getOrNull(4)?.toDoubleOrNull() ?: 500.0
        val stock = cols.getOrNull(5)?.toLongOrNull() ?: 20L
        val prodId = "prod_${sku.lowercase().replace(Regex("[^a-z0-9_]"), "_")}"
        val prodMap = Product(
          sku = sku,
          name = name,
          nameBn = nameBn,
          categoryId = catId,
          categoryName = catId,
          price = price,
          previousPrice = price * 1.2,
          discountPercent = 17L,
          stockQuantity = stock,
          isPublished = true,
          isNewArrival = true
        ).toCreateMap()
        batch.set(db.collection("products").document(prodId), prodMap)
        imported++
      }
      if (imported > 0) batch.commit().await()
      Result.success(imported)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "products")
      Result.failure(e)
    }
  }

  companion object {
    const val ADMIN_EMAIL = "mdabdussubahan292@gmail.com"
  }
}
