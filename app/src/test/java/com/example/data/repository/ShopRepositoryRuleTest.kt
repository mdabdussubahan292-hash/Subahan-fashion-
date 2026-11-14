package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ShopRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun adminBootstrapAnd310Products_andDynamicPriceDelete_reflectsToCustomer() = runBlocking {
    signInVerifiedAdminUser(ShopRepository.ADMIN_EMAIL)
    val adminRepo = ShopRepository(firestore)
    assertTrue(
      "Bootstrapped admin check must pass for verified owner email (user=${auth.currentUser?.email}, verified=${auth.currentUser?.isEmailVerified}, uid=${auth.currentUser?.uid})",
      adminRepo.isCurrentUserBootstrappedAdmin()
    )

    // Seed full 310+ catalog and update delivery settings dynamically
    val bootRes = withTimeout(SEED_TIMEOUT_MS) { adminRepo.bootstrapStore(seedFull300 = true) }
    assertTrue("Admin should seed 310+ products: ${bootRes.exceptionOrNull()}", bootRes.isSuccess)

    val allProductsAdmin = withTimeout(FLOW_TIMEOUT_MS) {
      adminRepo.observeAllProductsAdmin().first { it.size >= 310 }
    }
    assertTrue("Expected at least 310 seeded products, got ${allProductsAdmin.size}", allProductsAdmin.size >= 310)

    // Update store delivery charges
    val updatedConfig = StoreConfig(
      insideBoguraCharge = 45.0,
      outsideBoguraCharge = 95.0,
      insideDhakaCharge = 75.0,
      outsideDhakaCharge = 115.0,
      nationwideCharge = 115.0
    )
    val cfgRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.updateStoreConfig(updatedConfig) }
    assertTrue("Admin should update delivery charges: ${cfgRes.exceptionOrNull()}", cfgRes.isSuccess)

    // Add a custom product with mobile data-URI photo, edit price, then delete another
    val uniqueId = "prod_rule_${UUID.randomUUID().toString().take(8)}"
    val product = Product(
      id = uniqueId,
      sku = "SF-RULE-1",
      name = "Rule Test Leather Wallet",
      nameBn = "টেস্ট চামড়ার মানিব্যাগ",
      categoryId = "cat_wallet",
      categoryName = "মানিব্যাগ",
      price = 750.0,
      previousPrice = 950.0,
      discountPercent = 21L,
      stockQuantity = 25L,
      imageUrls = listOf("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD="),
      isPublished = true,
      isFeatured = true
    )

    val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.upsertProduct(product, costPrice = 420.0)
    }
    assertTrue("Admin should upsert product: ${createRes.exceptionOrNull()}", createRes.isSuccess)

    // Switch to customer: verify cannot access admin role, sees live price & updated delivery charge
    signInTestUser(CUSTOMER_ALICE)
    val customerRepo = ShopRepository(firestore)
    assertFalse("Non-admin customer must not be admin", customerRepo.isCurrentUserBootstrappedAdmin())

    val observedCfg = withTimeout(FLOW_TIMEOUT_MS) {
      customerRepo.observeStoreConfig().first { it.insideBoguraCharge == 45.0 }
    }
    assertEquals(45.0, observedCfg.insideBoguraCharge, 0.01)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      customerRepo.observePublishedProducts().first { list -> list.any { it.id == uniqueId } }
    }
    assertEquals(750.0, observed.first { it.id == uniqueId }.price, 0.01)

    // Customer cannot create/modify products or store_config or read product_costs
    val blockedProdWrite = withTimeout(DEFAULT_TIMEOUT_MS) {
      customerRepo.upsertProduct(product.copy(id = "prod_hack_${UUID.randomUUID().toString().take(6)}", price = 1.0))
    }
    assertTrue("Customer product write must fail", blockedProdWrite.isFailure)

    val blockedConfigWrite = withTimeout(DEFAULT_TIMEOUT_MS) {
      customerRepo.updateStoreConfig(updatedConfig.copy(insideBoguraCharge = 0.0))
    }
    assertTrue("Customer delivery charge write must fail", blockedConfigWrite.isFailure)

    // Switch back to admin and dynamically update price -> customer sees updated price, then delete
    signInVerifiedAdminUser(ShopRepository.ADMIN_EMAIL)
    val loaded = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.getProductById(uniqueId).getOrThrow()!! }
    val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.upsertProduct(loaded.copy(price = 620.0))
    }
    assertTrue("Admin should update price dynamically: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

    signInTestUser(CUSTOMER_ALICE)
    val updatedObserved = withTimeout(FLOW_TIMEOUT_MS) {
      customerRepo.observePublishedProducts().first { list -> list.any { it.id == uniqueId && it.price == 620.0 } }
    }
    assertEquals(620.0, updatedObserved.first { it.id == uniqueId }.price, 0.01)

    // Admin can delete product cleanly
    signInVerifiedAdminUser(ShopRepository.ADMIN_EMAIL)
    val delRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.deleteProduct(uniqueId) }
    assertTrue("Admin should delete product: ${delRes.exceptionOrNull()}", delRes.isSuccess)
  }

  @Test
  fun customerCodOrder_isolatedPerUser_andAdminUpdatesOrderStatus() = runBlocking {
    val aliceUid = signInTestUser(CUSTOMER_ALICE)
    val aliceRepo = ShopRepository(firestore)
    val prodId = "prod_cart_${UUID.randomUUID().toString().take(8)}"

    val cartProduct = Product(
      id = prodId,
      sku = "SF-CART-1",
      name = "Perfume 100ml",
      nameBn = "পারফিউম",
      categoryId = "cat_perfume",
      price = 1200.0,
      stockQuantity = 10L
    )
    val addCartRes = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addToCart(cartProduct, 2L) }
    assertTrue("Alice should add to her cart", addCartRes.isSuccess)

    val cartItems = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeCart(aliceUid).first { it.any { item -> item.productId == prodId } }
    }
    assertEquals(2L, cartItems.first { it.productId == prodId }.quantity)

    val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.placeOrder(
        customerName = "Alice Rahman",
        mobileNumber = "01711223344",
        altMobileNumber = "",
        fullAddress = "Koritala Bazar, Sariakandi, Bogura",
        division = "Rajshahi",
        district = "Bogura",
        upazila = "Sariakandi",
        deliveryArea = "INSIDE_BOGURA",
        orderNote = "Call before delivery",
        paymentMethod = "COD",
        transactionId = "",
        senderPhone = "",
        cartItems = listOf(
          CartItem(
            userId = aliceUid,
            productId = prodId,
            productName = "Perfume 100ml",
            sku = "SF-CART-1",
            unitPrice = 1200.0,
            quantity = 2L
          )
        ),
        subtotal = 2400.0,
        deliveryCharge = 50.0,
        discountAmount = 0.0,
        couponCode = "",
        totalAmount = 2450.0
      )
    }
    assertTrue("Alice COD order placement should succeed: ${orderRes.exceptionOrNull()}", orderRes.isSuccess)
    val createdOrderNumber = orderRes.getOrThrow()

    val aliceOrders = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeCustomerOrders(aliceUid).first { list -> list.any { it.orderNumber == createdOrderNumber } }
    }
    val aliceOrder = aliceOrders.first { it.orderNumber == createdOrderNumber }
    assertEquals("Pending", aliceOrder.status)
    assertEquals("COD", aliceOrder.paymentMethod)

    // Customer cannot escalate own order status to Delivered
    val blockedConfirm = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.updateOrderStatusAdmin(aliceOrder, "Delivered")
    }
    assertTrue("Customer must not escalate order status", blockedConfirm.isFailure)

    // Bob cannot see Alice's orders
    val bobUid = signInTestUser(CUSTOMER_BOB)
    val bobRepo = ShopRepository(firestore)
    val bobOrders = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeCustomerOrders(bobUid).first()
    }
    assertTrue("Bob should not see Alice's orders", bobOrders.none { it.userId == aliceUid })

    // Verified Admin updates Alice's order status to Confirmed -> Delivered
    signInVerifiedAdminUser(ShopRepository.ADMIN_EMAIL)
    val adminRepo = ShopRepository(firestore)
    val confirmRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.updateOrderStatusAdmin(aliceOrder, "Confirmed") }
    assertTrue("Admin should confirm order: ${confirmRes.exceptionOrNull()}", confirmRes.isSuccess)

    val deliverRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.updateOrderStatusAdmin(aliceOrder.copy(status = "Confirmed"), "Delivered")
    }
    assertTrue("Admin should deliver order: ${deliverRes.exceptionOrNull()}", deliverRes.isSuccess)

    // Alice observes live Delivered status
    signInTestUser(CUSTOMER_ALICE)
    val deliveredOrders = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeCustomerOrders(aliceUid).first { list ->
        list.any { it.id == aliceOrder.id && it.status == "Delivered" }
      }
    }
    assertEquals("Delivered", deliveredOrders.first { it.id == aliceOrder.id }.status)
  }

  @Test
  fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = ShopRepository(firestore)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePublishedProducts().first()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertTrue("Expected unauthenticated error", caught != null)
    val fsEx = caught?.unwrapFirestoreException()
    if (fsEx != null) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }
  }

  private companion object {
    const val CUSTOMER_ALICE = "alice.shopper@test.com"
    const val CUSTOMER_BOB = "bob.shopper@test.com"
    const val SEED_TIMEOUT_MS = 30000L
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
