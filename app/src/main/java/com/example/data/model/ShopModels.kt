package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val sku: String = "",
  val name: String = "",
  val nameBn: String = "",
  val description: String = "",
  val details: String = "",
  val categoryId: String = "",
  val categoryName: String = "",
  val price: Double = 0.0,
  val previousPrice: Double = 0.0,
  val discountPercent: Long = 0L,
  val stockQuantity: Long = 0L,
  val colors: List<String> = emptyList(),
  val sizes: List<String> = emptyList(),
  val imageUrls: List<String> = emptyList(),
  val videoUrl: String = "",
  val deliveryChargeOverride: Double = -1.0,
  val weightKg: Double = 0.5,
  @get:PropertyName("isPublished") @set:PropertyName("isPublished") var isPublished: Boolean = true,
  @get:PropertyName("isFeatured") @set:PropertyName("isFeatured") var isFeatured: Boolean = false,
  @get:PropertyName("isBestSelling") @set:PropertyName("isBestSelling") var isBestSelling: Boolean = false,
  @get:PropertyName("isNewArrival") @set:PropertyName("isNewArrival") var isNewArrival: Boolean = false,
  @get:PropertyName("isFlashSale") @set:PropertyName("isFlashSale") var isFlashSale: Boolean = false,
  @get:PropertyName("isChoiceDay") @set:PropertyName("isChoiceDay") var isChoiceDay: Boolean = false,
  @get:PropertyName("isBundleEligible") @set:PropertyName("isBundleEligible") var isBundleEligible: Boolean = false,
  val soldCount: Long = 0L,
  val ratingAvg: Double = 4.8,
  val reviewCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun displayName(isBangla: Boolean): String =
    if (isBangla && nameBn.isNotBlank()) nameBn else name

  fun toCreateMap(): Map<String, Any> = buildMap {
    put("sku", sku)
    put("name", name)
    put("nameBn", nameBn)
    put("description", description)
    put("details", details)
    put("categoryId", categoryId)
    put("categoryName", categoryName)
    put("price", price)
    put("previousPrice", previousPrice)
    put("discountPercent", discountPercent)
    put("stockQuantity", stockQuantity)
    put("colors", colors.take(15))
    put("sizes", sizes.take(15))
    put("imageUrls", imageUrls.take(6))
    put("videoUrl", videoUrl)
    put("deliveryChargeOverride", deliveryChargeOverride)
    put("weightKg", weightKg)
    put("isPublished", isPublished)
    put("isFeatured", isFeatured)
    put("isBestSelling", isBestSelling)
    put("isNewArrival", isNewArrival)
    put("isFlashSale", isFlashSale)
    put("isChoiceDay", isChoiceDay)
    put("isBundleEligible", isBundleEligible)
    put("soldCount", soldCount)
    put("ratingAvg", ratingAvg)
    put("reviewCount", reviewCount)
    put("createdAt", createdAt ?: FieldValue.serverTimestamp())
    put("updatedAt", FieldValue.serverTimestamp())
  }
}

@IgnoreExtraProperties
data class ProductCost(
  @DocumentId val id: String = "",
  val productId: String = "",
  val costPrice: Double = 0.0,
  val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductReview(
  @DocumentId val id: String = "",
  val productId: String = "",
  val userId: String = "",
  val userName: String = "",
  val rating: Long = 5L,
  val comment: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
  @DocumentId val id: String = "",
  val name: String = "",
  val nameBn: String = "",
  val iconKey: String = "category",
  val sortOrder: Long = 0L,
  @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun displayName(isBangla: Boolean): String =
    if (isBangla && nameBn.isNotBlank()) nameBn else name
}

@IgnoreExtraProperties
data class StoreConfig(
  @DocumentId val id: String = "settings",
  @get:PropertyName("isPublicConfig") @set:PropertyName("isPublicConfig") var isPublicConfig: Boolean = true,
  val insideBoguraCharge: Double = 50.0,
  val outsideBoguraCharge: Double = 100.0,
  val insideDhakaCharge: Double = 80.0,
  val outsideDhakaCharge: Double = 120.0,
  val nationwideCharge: Double = 120.0,
  val perKgExtraCharge: Double = 20.0,
  val perExtraItemCharge: Double = 10.0,
  val freeDeliveryMinOrder: Double = 2500.0,
  val freeDeliveryActive: Boolean = true,
  val codEnabled: Boolean = true,
  val bkashEnabled: Boolean = true,
  val bkashNumber: String = "01700-000000",
  val nagadEnabled: Boolean = true,
  val nagadNumber: String = "01700-000000",
  val rocketEnabled: Boolean = true,
  val rocketNumber: String = "01700-000000",
  val bankEnabled: Boolean = true,
  val bankDetails: String = "Islami Bank Bangladesh PLC\nSubahan Fashion\nA/C: 2050-00000000\nBogura Branch",
  val businessName: String = "Subahan Fashion",
  val businessNameBn: String = "সুবাহান ফ্যাশন",
  val location: String = "কড়িতলা বাজার, সারিয়াকান্দি, বগুড়া, বাংলাদেশ",
  val contactPhone: String = "+880 1700-000000",
  val facebookUrl: String = "https://facebook.com/subahanfashion",
  val whatsappNumber: String = "+8801700000000",
  val aboutUs: String = "Subahan Fashion (সুবাহান ফ্যাশন) is a trusted online shopping destination in Bangladesh, headquartered at Koritala Bazar, Sariakandi, Bogura. We deliver authentic fashion, accessories, home lighting, cosmetics, and lifestyle essentials nationwide.",
  val privacyPolicy: String = "We protect your name, mobile number, and delivery address strictly for order fulfillment and customer support. We never sell or share customer data with third parties.",
  val returnPolicy: String = "7-day easy return and exchange for damaged, defective, or wrong items. Please inspect the parcel in front of the delivery rider.",
  val deliveryPolicy: String = "Inside Bogura: 24-48 hours. Inside Dhaka: 48 hours. Nationwide Bangladesh: 2-4 business days via Steadfast / Pathao / RedX.",
  val flashSaleTitle: String = "ফ্ল্যাশ সেল — ৫০% পর্যন্ত ছাড়!",
  val flashSaleActive: Boolean = true,
  val flashSaleDurationHours: Long = 24L,
  val buy2DiscountPercent: Long = 5L,
  val buy3DiscountPercent: Long = 10L,
  val newCustomerDiscountPercent: Long = 5L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Coupon(
  @DocumentId val id: String = "",
  val code: String = "",
  val discountType: String = "PERCENT",
  val discountValue: Double = 0.0,
  val minOrderAmount: Double = 0.0,
  val maxDiscountAmount: Double = 500.0,
  @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val phone: String = "",
  val altPhone: String = "",
  val defaultAddress: String = "",
  val division: String = "Rajshahi",
  val district: String = "Bogura",
  val upazila: String = "Sariakandi",
  val deliveryArea: String = "INSIDE_BOGURA",
  val languagePref: String = "bn",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CartItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productId: String = "",
  val productName: String = "",
  val productNameBn: String = "",
  val sku: String = "",
  val imageUrl: String = "",
  val unitPrice: Double = 0.0,
  val previousPrice: Double = 0.0,
  val quantity: Long = 1L,
  val selectedColor: String = "",
  val selectedSize: String = "",
  val deliveryChargeOverride: Double = -1.0,
  val weightKg: Double = 0.5,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WishlistItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productId: String = "",
  val productName: String = "",
  val productNameBn: String = "",
  val price: Double = 0.0,
  val previousPrice: Double = 0.0,
  val imageUrl: String = "",
  val categoryName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderLineItem(
  val productId: String = "",
  val name: String = "",
  val sku: String = "",
  val qty: Long = 1L,
  val unitPrice: Double = 0.0,
  val color: String = "",
  val size: String = "",
  val imageUrl: String = ""
)

@IgnoreExtraProperties
data class Order(
  @DocumentId val id: String = "",
  val orderNumber: String = "",
  val userId: String = "",
  val customerName: String = "",
  val mobileNumber: String = "",
  val altMobileNumber: String = "",
  val fullAddress: String = "",
  val division: String = "",
  val district: String = "",
  val upazila: String = "",
  val deliveryArea: String = "INSIDE_BOGURA",
  val orderNote: String = "",
  val paymentMethod: String = "COD",
  val transactionId: String = "",
  val senderPhone: String = "",
  val itemsSummary: String = "",
  val items: List<OrderLineItem> = emptyList(),
  val subtotal: Double = 0.0,
  val deliveryCharge: Double = 0.0,
  val discountAmount: Double = 0.0,
  val couponCode: String = "",
  val totalAmount: Double = 0.0,
  val totalCost: Double = 0.0,
  val status: String = "Pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
  @DocumentId val id: String = "",
  val userId: String = "ALL",
  val title: String = "",
  val titleBn: String = "",
  val message: String = "",
  val messageBn: String = "",
  val type: String = "PROMO",
  val targetId: String = "",
  @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class DeliveryAreaOption(
  val code: String,
  val labelEn: String,
  val labelBn: String
) {
  INSIDE_BOGURA("INSIDE_BOGURA", "Inside Bogura", "বগুড়ার ভেতরে"),
  OUTSIDE_BOGURA("OUTSIDE_BOGURA", "Outside Bogura", "বগুড়ার বাইরে"),
  INSIDE_DHAKA("INSIDE_DHAKA", "Inside Dhaka", "ঢাকার ভেতরে"),
  OUTSIDE_DHAKA("OUTSIDE_DHAKA", "Outside Dhaka", "ঢাকার বাইরে"),
  NATIONWIDE("NATIONWIDE", "Nationwide Delivery", "সারাদেশে ডেলিভারি");

  fun displayLabel(isBangla: Boolean): String = if (isBangla) labelBn else labelEn
}

enum class PaymentMethodOption(
  val code: String,
  val labelEn: String,
  val labelBn: String
) {
  COD("COD", "Cash on Delivery", "ক্যাশ অন ডেলিভারি"),
  BKASH("BKASH", "bKash", "বিকাশ"),
  NAGAD("NAGAD", "Nagad", "নগদ"),
  ROCKET("ROCKET", "Rocket", "রকেট"),
  BANK("BANK", "Bank Payment", "ব্যাংক পেমেন্ট");

  fun displayLabel(isBangla: Boolean): String = if (isBangla) labelBn else labelEn
}

enum class OrderStatusOption(
  val code: String,
  val labelBn: String
) {
  PENDING("Pending", "পেন্ডিং"),
  CONFIRMED("Confirmed", "কনফার্মড"),
  PROCESSING("Processing", "প্রসেসিং"),
  SHIPPED("Shipped", "শিপড"),
  DELIVERED("Delivered", "ডেলিভারড"),
  CANCELLED("Cancelled", "বাতিল"),
  RETURNED("Returned", "রিটার্নড");

  fun displayLabel(isBangla: Boolean): String = if (isBangla) labelBn else code
}
