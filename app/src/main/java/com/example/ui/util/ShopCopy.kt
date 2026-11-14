package com.example.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

fun formatBdt(amount: Double, isBangla: Boolean = false): String {
  val rounded = amount.roundToLong()
  val formatted = NumberFormat.getNumberInstance(Locale.US).format(rounded)
  return if (isBangla) "৳${toBanglaDigits(formatted)}" else "৳$formatted"
}

fun toBanglaDigits(input: String): String {
  val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
  val sb = StringBuilder(input.length)
  for (c in input) {
    if (c in '0'..'9') sb.append(bn[c - '0']) else sb.append(c)
  }
  return sb.toString()
}

object ShopCopy {
  fun brandTitle(bn: Boolean) = if (bn) "সুবাহান ফ্যাশন" else "Subahan Fashion"
  fun searchHint(bn: Boolean) = if (bn) "পণ্য, ক্যাটাগরি বা কোড খুঁজুন..." else "Search products, categories, SKU..."
  fun navHome(bn: Boolean) = if (bn) "হোম" else "Home"
  fun navCategories(bn: Boolean) = if (bn) "ক্যাটাগরি" else "Categories"
  fun navCart(bn: Boolean) = if (bn) "কার্ট" else "Cart"
  fun navOrders(bn: Boolean) = if (bn) "অর্ডার" else "Orders"
  fun navAccount(bn: Boolean) = if (bn) "অ্যাকাউন্ট" else "My Account"
  fun addToCart(bn: Boolean) = if (bn) "কার্টে যোগ করুন" else "Add to Cart"
  fun buyNow(bn: Boolean) = if (bn) "এখনই কিনুন" else "Buy Now"
  fun outOfStock(bn: Boolean) = if (bn) "স্টক শেষ" else "Out of Stock"
  fun inStock(bn: Boolean) = if (bn) "স্টকে আছে" else "In Stock"
  fun flashSale(bn: Boolean) = if (bn) "ফ্ল্যাশ সেল" else "Flash Sale"
  fun choiceDay(bn: Boolean) = if (bn) "চয়েস ডে অফার" else "Choice Day Deals"
  fun saveMore(bn: Boolean) = if (bn) "সেভ মোর ও বান্ডেল" else "Save More & Bundles"
  fun fashionTrends(bn: Boolean) = if (bn) "ফ্যাশন ট্রেন্ডস" else "Fashion Trends"
  fun newArrivals(bn: Boolean) = if (bn) "নতুন পণ্য" else "New Arrivals"
  fun bestSelling(bn: Boolean) = if (bn) "সেরা বিক্রিত পণ্য" else "Best Selling"
  fun discountSection(bn: Boolean) = if (bn) "বিশেষ ছাড়" else "Special Discounts"
  fun recommended(bn: Boolean) = if (bn) "আপনার জন্য সুপারিশকৃত" else "Recommended For You"
  fun freeDeliveryBanner(bn: Boolean, minOrder: Double) =
    if (bn) "${formatBdt(minOrder, true)}+ অর্ডারে সারাদেশে ফ্রি ডেলিভারি!"
    else "Free Nationwide Delivery on orders ${formatBdt(minOrder, false)}+!"
  fun checkout(bn: Boolean) = if (bn) "অর্ডার কনফার্ম করুন" else "Proceed to Checkout"
  fun placeOrder(bn: Boolean) = if (bn) "অর্ডার প্লেস করুন" else "Place Order"
}
