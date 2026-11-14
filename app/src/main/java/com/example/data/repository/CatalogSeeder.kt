package com.example.data.repository

import com.example.data.model.Product
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToLong

object CatalogSeeder {

  data class CategorySeed(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val iconKey: String,
    val sortOrder: Long
  )

  val defaultCategories = listOf(
    CategorySeed("cat_wallet", "Wallets", "মানিব্যাগ", "wallet", 1),
    CategorySeed("cat_saving_box", "Saving Box", "সেভিং বক্স", "savings", 2),
    CategorySeed("cat_chandelier", "Chandelier Lights", "ঝাড়বাতি লাইট", "light", 3),
    CategorySeed("cat_watch", "Watches", "ঘড়ি", "watch", 4),
    CategorySeed("cat_toys", "Toys", "খেলনা", "toys", 5),
    CategorySeed("cat_perfume", "Perfumes", "পারফিউম", "spa", 6),
    CategorySeed("cat_cosmetic", "Cosmetics", "কসমেটিক", "face", 7),
    CategorySeed("cat_clothing", "Clothing", "কাপড়", "checkroom", 8),
    CategorySeed("cat_bracelet", "Men's Bracelets & Chains", "ছেলেদের ব্রেসলেট ও মালা", "diamond", 9),
    CategorySeed("cat_fashion_acc", "Fashion Accessories", "ফ্যাশন এক্সেসরিজ", "sunglasses", 10),
    CategorySeed("cat_electronics", "Electronics", "ইলেকট্রনিক্স", "devices", 11),
    CategorySeed("cat_home_living", "Home & Living", "হোম ও লিভিং", "home", 12),
    CategorySeed("cat_new_products", "New Products", "নতুন পণ্য", "new_releases", 13),
    CategorySeed("cat_others", "Other Products", "অন্যান্য পণ্য", "category", 14)
  )

  private data class ProductTemplate(
    val catId: String,
    val titleEn: String,
    val titleBn: String,
    val basePrice: Double,
    val costRatio: Double,
    val colors: List<String>,
    val sizes: List<String>,
    val weightKg: Double,
    val imageResKey: String
  )

  private val templates = listOf(
    ProductTemplate("cat_wallet", "Genuine Cowhide Leather Wallet", "আসল গরুর চামড়ার মানিব্যাগ", 650.0, 0.58, listOf("Dark Brown", "Jet Black", "Tan"), listOf("Standard"), 0.25, "res://img_storefront_hero"),
    ProductTemplate("cat_wallet", "RFID Blocking Executive Bifold Wallet", "আরএফআইডি প্রিমিয়াম বাইফোল্ড মানিব্যাগ", 790.0, 0.55, listOf("Black", "Coffee"), listOf("Standard"), 0.25, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_wallet", "Slim Carbon Cardholder Wallet", "স্লিম কার্বন কার্ডহোল্ডার মানিব্যাগ", 420.0, 0.52, listOf("Black", "Navy"), listOf("Slim"), 0.15, "res://img_storefront_hero"),
    ProductTemplate("cat_saving_box", "Digital Coin Counter Saving Box", "ডিজিটাল কয়েন কাউন্টার সেভিং বক্স", 850.0, 0.60, listOf("Gold", "Rose Pink", "Blue"), listOf("Medium", "Large"), 0.6, "res://img_banner_home_living"),
    ProductTemplate("cat_saving_box", "Royal Antique Treasure Saving Box", "রয়্যাল অ্যান্টিক ডিজাইন সেভিং বক্স", 1150.0, 0.58, listOf("Antique Gold", "Bronze"), listOf("Large"), 0.9, "res://img_banner_home_living"),
    ProductTemplate("cat_saving_box", "Kids Electronic ATM Password Bank", "বাচ্চাদের ইলেকট্রনিক পাসওয়ার্ড সেভিং বক্স", 1290.0, 0.62, listOf("Pink", "Yellow", "Black"), listOf("Standard"), 0.85, "res://img_banner_choice_day"),
    ProductTemplate("cat_chandelier", "Modern K9 Crystal Ceiling Chandelier", "আধুনিক কে৯ ক্রিস্টাল ঝাড়বাতি লাইট", 4850.0, 0.65, listOf("Warm Gold", "Cool Chrome"), listOf("16 inch", "24 inch"), 3.2, "res://img_banner_home_living"),
    ProductTemplate("cat_chandelier", "Golden Ring LED Hanging Jharbati", "গোল্ডেন রিং এলইডি ঝাড়বাতি লাইট", 3490.0, 0.62, listOf("Gold", "Rose Gold"), listOf("2-Ring", "3-Ring"), 2.5, "res://img_banner_home_living"),
    ProductTemplate("cat_chandelier", "Turkish Mosaic Hanging Pendant Light", "তুর্কি মোজাইক ঝুলন্ত লাইট", 2150.0, 0.60, listOf("Amber", "Multicolor"), listOf("Single", "Triple"), 1.8, "res://img_banner_home_living"),
    ProductTemplate("cat_watch", "Royal Chronograph Stainless Watch", "রয়্যাল ক্রনোগ্রাফ স্টেইনলেস ঘড়ি", 1650.0, 0.55, listOf("Gold", "Silver", "Black"), listOf("40mm", "42mm"), 0.35, "res://img_storefront_hero"),
    ProductTemplate("cat_watch", "Classic Roman Dial Leather Watch", "ক্লাসিক রোমান ডায়াল চামড়ার ঘড়ি", 1250.0, 0.56, listOf("Rose Gold", "Brown"), listOf("40mm"), 0.3, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_watch", "AMOLED Fitness Smart Watch Pro", "অ্যামোলেড ফিটনেস স্মার্ট ঘড়ি প্রো", 2290.0, 0.64, listOf("Black", "Pink", "Gold"), listOf("44mm"), 0.3, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_toys", "Remote Control Off-Road Monster Car", "রিমোট কন্ট্রোল মনস্টার কার খেলনা", 1350.0, 0.60, listOf("Red", "Yellow", "Blue"), listOf("1:16 Scale"), 0.9, "res://img_banner_choice_day"),
    ProductTemplate("cat_toys", "Magnetic 3D Building Blocks Set", "ম্যাগনেটিক থ্রিডি বিল্ডিং ব্লক খেলনা", 980.0, 0.58, listOf("Multicolor"), listOf("48 Pcs", "72 Pcs"), 0.7, "res://img_banner_choice_day"),
    ProductTemplate("cat_toys", "Dancing Musical Robot Toy", "ড্যান্সিং মিউজিক্যাল রোবট খেলনা", 750.0, 0.56, listOf("White", "Blue"), listOf("Standard"), 0.5, "res://img_banner_choice_day"),
    ProductTemplate("cat_perfume", "Subahan Oud Al-Malaki EDP 100ml", "সুবাহান উদ আল-মালাকি পারফিউম ১০০মি.লি.", 1490.0, 0.50, listOf("Golden Oud"), listOf("50ml", "100ml"), 0.45, "res://img_storefront_hero"),
    ProductTemplate("cat_perfume", "Flora Velvet Rose Women Perfume", "ফ্লোরা ভেলভেট রোজ লেডিস পারফিউম", 1190.0, 0.52, listOf("Rose Pink"), listOf("100ml"), 0.45, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_perfume", "Noir Intense Men's Long Lasting Attar", "নয়ার ইনটেন্স দীর্ঘস্থায়ী আতর ও পারফিউম", 550.0, 0.48, listOf("Amber", "Musk"), listOf("12ml", "30ml"), 0.2, "res://img_banner_choice_day"),
    ProductTemplate("cat_cosmetic", "24K Gold Radiance Glow Serum", "২৪কে গোল্ড গ্লো ফেস সিরাম", 680.0, 0.54, listOf("Gold"), listOf("30ml"), 0.25, "res://img_banner_choice_day"),
    ProductTemplate("cat_cosmetic", "Velvet Matte Lipstick Combo Set", "ভেলভেট ম্যাট লিপস্টিক কম্বো", 590.0, 0.52, listOf("Crimson", "Rose", "Nude"), listOf("4 Pcs Set"), 0.25, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_cosmetic", "Hydra Compact Powder & Primer Kit", "হাইড্রা কমপ্যাক্ট পাউডার কিট", 740.0, 0.55, listOf("Ivory", "Natural"), listOf("Standard"), 0.3, "res://img_banner_choice_day"),
    ProductTemplate("cat_clothing", "Premium Egyptian Cotton Panjabi", "প্রিমিয়াম ইজিপশিয়ান কটন পাঞ্জাবি", 1850.0, 0.60, listOf("Maroon", "Black", "Cream"), listOf("M", "L", "XL", "XXL"), 0.5, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_clothing", "Luxury Embroidered Three-Piece Suit", "লাক্সারি এমব্রয়ডারি থ্রি-পিস", 2450.0, 0.62, listOf("Hot Pink", "Emerald", "Gold"), listOf("Free Size"), 0.7, "res://img_banner_choice_day"),
    ProductTemplate("cat_clothing", "Export Polo Collar T-Shirt", "এক্সপোর্ট কোয়ালিটি পোলো টি-শার্ট", 580.0, 0.55, listOf("Navy", "White", "Red"), listOf("M", "L", "XL"), 0.3, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_bracelet", "Natural Lava Stone & Gold Bead Bracelet", "ন্যাচারাল লাভা স্টোন ছেলেদের ব্রেসলেট", 390.0, 0.45, listOf("Black & Gold", "Turquoise"), listOf("Adjustable"), 0.15, "res://img_storefront_hero"),
    ProductTemplate("cat_bracelet", "316L Stainless Steel Cuban Chain", "স্টেইনলেস স্টিল কিউবান মালা ও চেইন", 650.0, 0.50, listOf("Silver", "Gold Plated"), listOf("20 inch", "22 inch"), 0.2, "res://img_storefront_hero"),
    ProductTemplate("cat_bracelet", "Tiger Eye Double Strand Men's Bracelet", "টাইগার আই স্টোন ব্রেসলেট ও মালা সেট", 490.0, 0.46, listOf("Tiger Brown", "Obsidian"), listOf("Free Size"), 0.15, "res://img_banner_choice_day"),
    ProductTemplate("cat_fashion_acc", "UV400 Polarized Tortoise Sunglasses", "ইউভি৪০০ পোলারাইজড সানগ্লাস", 550.0, 0.50, listOf("Tortoise", "Black"), listOf("Standard"), 0.2, "res://img_banner_fashion_sale"),
    ProductTemplate("cat_fashion_acc", "Reversible Formal Leather Belt", "রিভার্সিবল ফরমাল চামড়ার বেল্ট", 690.0, 0.55, listOf("Black/Brown"), listOf("38-44"), 0.35, "res://img_storefront_hero"),
    ProductTemplate("cat_electronics", "ANC Wireless Bluetooth Earbuds Pro", "নয়েজ ক্যান্সেলিং ওয়্যারলেস ইয়ারবাডস", 1450.0, 0.62, listOf("White", "Black"), listOf("Pro"), 0.25, "res://img_banner_choice_day"),
    ProductTemplate("cat_electronics", "20000mAh Fast Charge Power Bank", "২০০০০এমএএইচ ফাস্ট চার্জ পাওয়ার ব্যাংক", 1590.0, 0.65, listOf("Black", "White"), listOf("22.5W"), 0.45, "res://img_banner_choice_day"),
    ProductTemplate("cat_home_living", "Nordic Ceramic Vase & Candle Decor", "নর্ডিক সিরামিক ফুলদানি ও ডেকোর", 890.0, 0.56, listOf("Amber Gold", "Cream"), listOf("Set of 2"), 0.9, "res://img_banner_home_living"),
    ProductTemplate("cat_new_products", "Eid & Festive Gift Hamper Box", "উৎসবের লাক্সারি গিফট হ্যাম্পার বক্স", 1990.0, 0.58, listOf("Pink & Gold"), listOf("4-in-1 Box"), 1.0, "res://img_banner_choice_day"),
    ProductTemplate("cat_others", "Portable Mini Neck Fan & Lamp", "পোর্টেবল মিনি নেক ফ্যান ও ল্যাম্প", 620.0, 0.55, listOf("Pink", "White"), listOf("USB-C"), 0.35, "res://img_banner_home_living")
  )

  private val editionsEn = listOf(
    "Signature", "Deluxe", "Royal", "Classic", "Elite", "Studio", "Heritage", "Premier", "Urban", "Golden"
  )
  private val editionsBn = listOf(
    "সিগনেচার", "ডিলাক্স", "রয়্যাল", "ক্লাসিক", "এলিট", "স্টুডিও", "হেরিটেজ", "প্রিমিয়ার", "আর্বান", "গোল্ডেন"
  )

  suspend fun ensureStoreBootstrapped(db: FirebaseFirestore, seedFull300: Boolean = false) {
    val configRef = db.collection("store_config").document("settings")
    val configSnap = configRef.get().await()
    if (!configSnap.exists()) {
      configRef.set(
        mapOf(
          "isPublicConfig" to true,
          "insideBoguraCharge" to 50.0,
          "outsideBoguraCharge" to 100.0,
          "insideDhakaCharge" to 80.0,
          "outsideDhakaCharge" to 120.0,
          "nationwideCharge" to 120.0,
          "perKgExtraCharge" to 20.0,
          "perExtraItemCharge" to 10.0,
          "freeDeliveryMinOrder" to 2500.0,
          "freeDeliveryActive" to true,
          "codEnabled" to true,
          "bkashEnabled" to true,
          "bkashNumber" to "01700-000000",
          "nagadEnabled" to true,
          "nagadNumber" to "01700-000000",
          "rocketEnabled" to true,
          "rocketNumber" to "01700-000000",
          "bankEnabled" to true,
          "bankDetails" to "Islami Bank Bangladesh PLC\nAccount: Subahan Fashion\nA/C: 2050-00000000\nBogura Branch",
          "businessName" to "Subahan Fashion",
          "businessNameBn" to "সুবাহান ফ্যাশন",
          "location" to "কড়িতলা বাজার, সারিয়াকান্দি, বগুড়া, বাংলাদেশ",
          "contactPhone" to "+880 1700-000000",
          "facebookUrl" to "https://facebook.com/subahanfashion",
          "whatsappNumber" to "+8801700000000",
          "aboutUs" to "Subahan Fashion (সুবাহান ফ্যাশন) is a trusted online shopping destination in Bangladesh, headquartered at Koritala Bazar, Sariakandi, Bogura.",
          "privacyPolicy" to "Customer phone numbers and addresses are used solely for parcel delivery and order verification.",
          "returnPolicy" to "7-day return or replacement guarantee on damaged or wrong items checked upon delivery.",
          "deliveryPolicy" to "Inside Bogura: 24-48 hrs | Inside Dhaka: 48 hrs | Nationwide: 2-4 days.",
          "flashSaleTitle" to "ফ্ল্যাশ সেল — ৫০% পর্যন্ত ছাড়!",
          "flashSaleActive" to true,
          "flashSaleDurationHours" to 24L,
          "buy2DiscountPercent" to 5L,
          "buy3DiscountPercent" to 10L,
          "newCustomerDiscountPercent" to 5L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }

    // Seed any missing categories out of the 14 defaults
    val existingCatIds = db.collection("categories").get().await().documents.map { it.id }.toSet()
    val missingCategories = defaultCategories.filter { it.id !in existingCatIds }
    if (missingCategories.isNotEmpty()) {
      val catBatch = db.batch()
      for (cat in missingCategories) {
        val ref = db.collection("categories").document(cat.id)
        catBatch.set(
          ref,
          mapOf(
            "name" to cat.nameEn,
            "nameBn" to cat.nameBn,
            "iconKey" to cat.iconKey,
            "sortOrder" to cat.sortOrder,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      catBatch.commit().await()
    }

    // Seed any missing starter coupons
    val existingCouponIds = db.collection("coupons").get().await().documents.map { it.id }.toSet()
    val coupons = listOf(
      Triple("SUBAHAN10", "PERCENT", 10.0),
      Triple("BOGURA50", "FIXED", 50.0),
      Triple("WELCOME100", "FIXED", 100.0)
    ).filter { (code, _, _) -> "cp_${code.lowercase()}" !in existingCouponIds }
    if (coupons.isNotEmpty()) {
      val couponBatch = db.batch()
      for ((code, type, value) in coupons) {
        couponBatch.set(
          db.collection("coupons").document("cp_${code.lowercase()}"),
          mapOf(
            "code" to code,
            "discountType" to type,
            "discountValue" to value,
            "minOrderAmount" to if (value >= 100.0) 1000.0 else 300.0,
            "maxDiscountAmount" to 500.0,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      couponBatch.commit().await()
    }

    // Seed welcome promo notification if absent
    val promoRef = db.collection("notifications").document("notif_welcome_promo")
    if (!promoRef.get().await().exists()) {
      promoRef.set(
        mapOf(
          "userId" to "ALL",
          "title" to "Choice Day & Free Delivery!",
          "titleBn" to "চয়েস ডে ও ফ্রি ডেলিভারি!",
          "message" to "Enjoy free delivery on orders above ৳2,500 and up to 10% extra bundle savings!",
          "messageBn" to "২,৫০০ টাকার বেশি অর্ডারে ফ্রি ডেলিভারি এবং ৩টি পণ্যে ১০% পর্যন্ত বান্ডেল ছাড়!",
          "type" to "PROMO",
          "targetId" to "",
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }

    // Seed products: 42 initial curated products on first setup, or full 310 products when requested
    val existingProducts = db.collection("products").limit(1).get().await()
    if (existingProducts.isEmpty || seedFull300) {
      val count = if (seedFull300) 310 else 42
      seedProductsBatch(db, count)
    }
  }

  suspend fun seedProductsBatch(db: FirebaseFirestore, totalCount: Int = 310) {
    val categoryLookup = defaultCategories.associateBy { it.id }
    val existingProductIds = db.collection("products").get().await().documents.map { it.id }.toSet()
    val operations = mutableListOf<Pair<String, Pair<Map<String, Any>, Double>>>()

    for (i in 0 until totalCount) {
      val tpl = templates[i % templates.size]
      val editionIdx = (i / templates.size) % editionsEn.size
      val sku = "SF-${1001 + i}"
      val prodId = "prod_${1001 + i}"
      if (prodId in existingProductIds) continue

      val cat = categoryLookup[tpl.catId]
      val discountPct = when {
        i % 5 == 0 -> 35L
        i % 3 == 0 -> 25L
        i % 2 == 0 -> 15L
        else -> 10L
      }
      val priceStep = (editionIdx * 35).toDouble()
      val prevPrice = ((tpl.basePrice + priceStep) / (1.0 - discountPct / 100.0)).roundToLong().toDouble()
      val sellingPrice = tpl.basePrice + priceStep
      val costPrice = (sellingPrice * tpl.costRatio).roundToLong().toDouble()

      val editionPrefixEn = if (editionIdx == 0) "" else "${editionsEn[editionIdx]} "
      val editionPrefixBn = if (editionIdx == 0) "" else "${editionsBn[editionIdx]} "

      val prodMap = Product(
        sku = sku,
        name = "$editionPrefixEn${tpl.titleEn}",
        nameBn = "$editionPrefixBn${tpl.titleBn}",
        description = "Authentic ${tpl.titleEn} curated by Subahan Fashion Bogura with nationwide cash-on-delivery and 7-day inspection guarantee.",
        details = "SKU: $sku • Category: ${cat?.nameBn ?: tpl.catId} • Weight: ${tpl.weightKg} kg • Fast Delivery across Bangladesh.",
        categoryId = tpl.catId,
        categoryName = cat?.nameBn ?: tpl.catId,
        price = sellingPrice,
        previousPrice = prevPrice,
        discountPercent = discountPct,
        stockQuantity = if (i % 19 == 0) 3L else (15L + (i % 60)),
        colors = tpl.colors,
        sizes = tpl.sizes,
        imageUrls = listOf(tpl.imageResKey),
        videoUrl = "",
        deliveryChargeOverride = if (i % 11 == 0) 0.0 else -1.0,
        weightKg = tpl.weightKg,
        isPublished = true,
        isFeatured = i % 4 == 0,
        isBestSelling = i % 3 == 0,
        isNewArrival = i % 4 == 1 || i >= totalCount - 20,
        isFlashSale = i % 5 == 0,
        isChoiceDay = i % 6 == 0,
        isBundleEligible = i % 2 == 0,
        soldCount = 12L + ((i * 7) % 180),
        ratingAvg = 4.5 + ((i % 5) * 0.1),
        reviewCount = 4L + (i % 45)
      ).toCreateMap()

      operations.add(prodId to (prodMap to costPrice))
    }

    if (operations.isEmpty()) return

    // Commit in chunks of 200 products (400 writes per batch including product_costs)
    for (chunk in operations.chunked(200)) {
      val batch = db.batch()
      for ((prodId, pair) in chunk) {
        val (prodMap, costPrice) = pair
        batch.set(db.collection("products").document(prodId), prodMap)
        batch.set(
          db.collection("product_costs").document(prodId),
          mapOf(
            "productId" to prodId,
            "costPrice" to costPrice,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
    }
  }
}
