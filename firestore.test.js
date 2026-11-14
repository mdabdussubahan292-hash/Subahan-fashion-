const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const nowDate = () => new Date();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_subahan";
const SPOOF_UID = "spoof_admin";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "mdabdussubahan292@gmail.com",
    email_verified: true,
  });
}

function spoofAdminContext() {
  return testEnv.authenticatedContext(SPOOF_UID, {
    email: "mdabdussubahan292@gmail.com",
    email_verified: false,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read products, orders, users, or cart", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cart").get());
});

test("Email spoofing test: unverified admin email is rejected for admin operations", async () => {
  const spoofDb = spoofAdminContext().firestore();
  await assertFails(
    spoofDb.collection("products").doc("prod_1").set({
      sku: "SF-001",
      name: "Leather Wallet",
      categoryId: "cat_wallet",
      price: 550,
      stockQuantity: 10,
      isPublished: true,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );
  await assertFails(spoofDb.collection("orders").get());
});

test("Verified bootstrapped admin: can create/update products, categories, store_config, and list all orders", async () => {
  const db = adminContext().firestore();
  await assertSucceeds(
    db.collection("categories").doc("cat_wallet").set({
      name: "Wallets",
      nameBn: "মানিব্যাগ",
      iconKey: "wallet",
      sortOrder: 1,
      isActive: true,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  await assertSucceeds(
    db.collection("products").doc("prod_1").set({
      sku: "SF-1001",
      name: "Premium Leather Wallet",
      nameBn: "প্রিমিয়াম চামড়ার মানিব্যাগ",
      categoryId: "cat_wallet",
      categoryName: "মানিব্যাগ",
      price: 650,
      previousPrice: 850,
      discountPercent: 24,
      stockQuantity: 45,
      colors: ["Brown", "Black"],
      sizes: ["Standard"],
      imageUrls: [],
      isPublished: true,
      isFeatured: true,
      soldCount: 12,
      ratingAvg: 4.8,
      reviewCount: 5,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  await assertSucceeds(
    db.collection("store_config").doc("settings").set({
      isPublicConfig: true,
      insideBoguraCharge: 50,
      outsideBoguraCharge: 100,
      insideDhakaCharge: 80,
      outsideDhakaCharge: 120,
      nationwideCharge: 120,
      codEnabled: true,
      bkashEnabled: true,
      bkashNumber: "01700000000",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  await assertSucceeds(db.collection("orders").get());
});

test("Customer cannot create product, read product_costs, or inject ghost fields on user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Cannot write products
  await assertFails(
    aliceDb.collection("products").doc("prod_fake").set({
      sku: "FAKE-1",
      name: "Fake",
      categoryId: "cat_1",
      price: 100,
      stockQuantity: 1,
      isPublished: true,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Shadow Update / ghost field test on user profile
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      isAdmin: true,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Valid user profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      email: "alice@example.com",
      phone: "01711223344",
      defaultAddress: "Bogura Sadar",
      languagePref: "bn",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Cannot read admin-only product_costs
  await assertFails(aliceDb.collection("product_costs").doc("prod_1").get());
});

test("PII Isolation: Alice cannot read Bob's user profile, cart, wishlist, or orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      phone: "01811223344",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    });
    await db.collection("users").doc(BOB_UID).collection("cart").doc("prod_1").set({
      userId: BOB_UID,
      productId: "prod_1",
      productName: "Watch",
      unitPrice: 1200,
      quantity: 1,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    });
    await db.collection("orders").doc("order_bob_1").set({
      orderNumber: "SF-0001",
      userId: BOB_UID,
      customerName: "Bob",
      mobileNumber: "01811223344",
      fullAddress: "Dhaka",
      deliveryArea: "INSIDE_DHAKA",
      paymentMethod: "COD",
      items: [{ productId: "prod_1", name: "Watch", qty: 1, unitPrice: 1200 }],
      subtotal: 1200,
      deliveryCharge: 80,
      discountAmount: 0,
      totalAmount: 1280,
      status: "Pending",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("cart").get());
  await assertFails(aliceDb.collection("orders").doc("order_bob_1").get());
  await assertFails(aliceDb.collection("orders").get()); // unfiltered list fails for customer
});

test("Customer orders: can create Pending order, query own orders, cancel Pending order, cannot spoof price or status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Cannot create already-Delivered order
  await assertFails(
    aliceDb.collection("orders").doc("ord_bad").set({
      orderNumber: "SF-100",
      userId: ALICE_UID,
      customerName: "Alice",
      mobileNumber: "01711223344",
      fullAddress: "Bogura",
      deliveryArea: "INSIDE_BOGURA",
      paymentMethod: "COD",
      items: [{ productId: "prod_1", name: "Wallet", qty: 1, unitPrice: 650 }],
      subtotal: 650,
      deliveryCharge: 50,
      discountAmount: 0,
      totalAmount: 700,
      status: "Delivered",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Valid Pending order succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_alice_1").set({
      orderNumber: "SF-101",
      userId: ALICE_UID,
      customerName: "Alice",
      mobileNumber: "01711223344",
      fullAddress: "Sariakandi, Bogura",
      deliveryArea: "INSIDE_BOGURA",
      paymentMethod: "BKASH",
      transactionId: "TRX998877",
      items: [{ productId: "prod_1", name: "Wallet", qty: 1, unitPrice: 650 }],
      subtotal: 650,
      deliveryCharge: 50,
      discountAmount: 0,
      totalAmount: 700,
      status: "Pending",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Query own orders succeeds
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );

  // Customer cannot mutate totalAmount
  await assertFails(
    aliceDb.collection("orders").doc("ord_alice_1").update({
      totalAmount: 1,
      updatedAt: nowDate(),
    })
  );

  // Customer can cancel own Pending order
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_alice_1").update({
      status: "Cancelled",
      updatedAt: nowDate(),
    })
  );
});

test("Customer cart, published products query, and notifications alignment", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("products").doc("prod_pub").set({
      sku: "SF-1002",
      name: "Crystal Light",
      categoryId: "cat_light",
      price: 2500,
      stockQuantity: 5,
      isPublished: true,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    });
    await db.collection("products").doc("prod_hidden").set({
      sku: "SF-1003",
      name: "Draft Product",
      categoryId: "cat_light",
      price: 1500,
      stockQuantity: 5,
      isPublished: false,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    });
    await db.collection("notifications").doc("notif_1").set({
      userId: "ALL",
      title: "Flash Sale!",
      message: "50% off today",
      type: "PROMO",
      isRead: false,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("products").where("isPublished", "==", true).get());
  await assertFails(aliceDb.collection("products").doc("prod_hidden").get());
  await assertSucceeds(aliceDb.collection("notifications").where("userId", "in", [ALICE_UID, "ALL"]).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_pub").set({
      userId: ALICE_UID,
      productId: "prod_pub",
      productName: "Crystal Light",
      unitPrice: 2500,
      quantity: 2,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );
});
