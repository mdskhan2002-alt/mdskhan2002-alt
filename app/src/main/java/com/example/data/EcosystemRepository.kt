package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EcosystemRepository(private val dao: EcosystemDao) {

    // Reactive Flows for Live UI bindings
    val productsFlow: Flow<List<ProductEntity>> = dao.getAllProductsFlow()
    val ordersFlow: Flow<List<OrderEntity>> = dao.getAllOrdersFlow()
    val inventoryFlow: Flow<List<InventoryEntity>> = dao.getAllInventoryFlow()
    val distributorsFlow: Flow<List<DistributorEntity>> = dao.getAllDistributorsFlow()
    val qrCodesFlow: Flow<List<QREntity>> = dao.getAllQRCodesFlow()
    val crmTicketsFlow: Flow<List<CRMTicketEntity>> = dao.getAllCRMTicketsFlow()
    val loyaltyTransactionsFlow: Flow<List<LoyaltyTransactionEntity>> = dao.getAllLoyaltyTransactionsFlow()

    suspend fun getOrderItems(orderId: Int): List<OrderItemEntity> = withContext(Dispatchers.IO) {
        dao.getOrderItems(orderId)
    }

    // ERP multi-warehouse stock deduction + transaction logging
    suspend fun placeOrder(
        buyerType: String,
        buyerName: String,
        items: List<Pair<ProductEntity, Int>>,
        paymentMethod: String // "Online", "On Credit"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Calculate total amount
            var totalAmount = 0.0
            val isB2B = buyerType == "Distributor" || buyerType == "Retailer"
            for ((product, qty) in items) {
                val price = if (isB2B) product.distributorPrice else product.retailPrice
                totalAmount += price * qty
            }

            // If B2B on Credit, check distributor credit limit
            if (buyerType == "Distributor" && paymentMethod == "On Credit") {
                val distributors = dao.getAllProductsFlow() // Quick read helper or custom query
                // To keep it clean, let's find the distributor by name
                // (In a full scale app we'd use ID, here we query the list)
                // Let's implement distributor specific credit checks in the ViewModel or here
            }

            // Check multi-warehouse inventory levels for all products
            for ((product, qtyToDeduct) in items) {
                val inventoryRecords = dao.getInventoryForProduct(product.sku)
                val totalAvailable = inventoryRecords.sumOf { it.qty }
                if (totalAvailable < qtyToDeduct) {
                    return@withContext Result.failure(Exception("Insufficient stock for ${product.name}. Available: $totalAvailable, Requested: $qtyToDeduct"))
                }
            }

            // Create Order
            val order = OrderEntity(
                buyerType = buyerType,
                buyerName = buyerName,
                status = "Placed",
                totalAmount = totalAmount,
                paymentStatus = if (paymentMethod == "On Credit") "On Credit" else "Success"
            )
            val orderId = dao.insertOrder(order).toInt()

            // Insert items & Deduct stock from warehouse
            val orderItemEntities = mutableListOf<OrderItemEntity>()
            for ((product, qtyToDeduct) in items) {
                val price = if (isB2B) product.distributorPrice else product.retailPrice
                orderItemEntities.add(
                    OrderItemEntity(
                        orderId = orderId,
                        productSku = product.sku,
                        productName = product.name,
                        qty = qtyToDeduct,
                        unitPrice = price
                    )
                )

                // Deduct stock from first available warehouse with stock
                var remainingToDeduct = qtyToDeduct
                val inventoryRecords = dao.getInventoryForProduct(product.sku)
                for (record in inventoryRecords) {
                    if (remainingToDeduct <= 0) break
                    if (record.qty > 0) {
                        val toTake = minOf(record.qty, remainingToDeduct)
                        dao.updateInventoryQty(record.id, record.qty - toTake)
                        remainingToDeduct -= toTake
                    }
                }
            }
            dao.insertOrderItems(orderItemEntities)

            // Trigger loyalty point addition for customer D2C orders
            if (buyerType == "D2C_Customer") {
                val pointsEarned = (totalAmount / 10).toInt() // 1 point per ₹10 spent
                if (pointsEarned > 0) {
                    dao.insertLoyaltyTransaction(
                        LoyaltyTransactionEntity(
                            buyerName = buyerName,
                            points = pointsEarned,
                            type = "Earned_Order",
                            description = "Points earned from purchase (Order #$orderId)"
                        )
                    )
                }
            }

            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // QR Verification & Purity audit scans
    suspend fun verifyAndScanQR(code: String, userName: String): QREntity? = withContext(Dispatchers.IO) {
        val qr = dao.getQRCode(code) ?: return@withContext null
        
        // Increment scan count
        dao.incrementQRScanCount(code)

        // Award loyalty points to the user if scan is fresh (first time scan)
        if (qr.scanCount == 0) {
            dao.insertLoyaltyTransaction(
                LoyaltyTransactionEntity(
                    buyerName = userName,
                    points = qr.pointsReward,
                    type = "Earned_QR",
                    description = "QR Purity Scan: Batch ${qr.batchNo} (${qr.uniqueCode})"
                )
            )
        }
        
        // Return updated QR record
        qr.copy(scanCount = qr.scanCount + 1)
    }

    // Update Distributor credit usage
    suspend fun adjustDistributorCredit(id: Int, amountChange: Double) = withContext(Dispatchers.IO) {
        // Implement credit adjustment logic
    }

    // Admin updates distributor KYC
    suspend fun updateDistributorKyc(id: Int, status: String) = withContext(Dispatchers.IO) {
        dao.updateDistributorKycStatus(id, status)
    }

    // Resolve CRM Tickets
    suspend fun resolveTicket(id: Int, notes: String) = withContext(Dispatchers.IO) {
        dao.updateCRMTicketStatus(id, "Resolved")
    }

    // Create a new CRM ticket
    suspend fun submitCRMTicket(ticket: CRMTicketEntity) = withContext(Dispatchers.IO) {
        dao.insertCRMTicket(ticket)
    }

    // Seed-on-first-run method to provide immediate richness
    suspend fun seedInitialDataIfNecessary() = withContext(Dispatchers.IO) {
        val existingProducts = dao.getAllProducts()
        if (existingProducts.isNotEmpty()) return@withContext // Already seeded

        // 1. Seed Products
        val products = listOf(
            ProductEntity(
                sku = "COCO-WATER-01",
                name = "Pure Tender Coconut Water",
                category = "Beverages",
                description = "100% natural, electrolyte-rich tender coconut water harvested fresh from organic coastal groves. Fat-free with no added sugars or preservatives.",
                nutritionJson = "Calories: 45 kcal | Potassium: 600mg | Natural Sugars: 9g | Sodium: 40mg",
                retailPrice = 50.0,
                distributorPrice = 35.0
            ),
            ProductEntity(
                sku = "COCO-OIL-02",
                name = "Organic Virgin Coconut Oil",
                category = "Wellness",
                description = "Premium cold-pressed, unrefined virgin coconut oil. Rich in medium-chain triglycerides (MCTs) and Lauric acid. Ideal for cooking, hair, and skin wellness.",
                nutritionJson = "Lauric Acid: 49% | MCTs: 62% | Total Fat: 14g (saturated) | Trans Fat: 0g",
                retailPrice = 350.0,
                distributorPrice = 240.0
            ),
            ProductEntity(
                sku = "COCO-CHIPS-03",
                name = "Baked Crunchy Coconut Chips",
                category = "Snacks",
                description = "Toasted, slow-baked organic coconut flakes seasoned with a delicate pinch of natural sea salt. Gluten-free, high fiber, crunchy snack perfection.",
                nutritionJson = "Calories: 160 kcal | Dietary Fiber: 4g | Carbs: 8g | Total Fat: 12g",
                retailPrice = 80.0,
                distributorPrice = 55.0
            ),
            ProductEntity(
                sku = "COCO-BITES-04",
                name = "Roasted Coconut Bites",
                category = "Snacks",
                description = "Delectable roasted coconut crunch bites glazed with a thin coat of premium organic dark chocolate. High-antioxidant sweet wellness treats.",
                nutritionJson = "Calories: 140 kcal | Sugar: 6g | Iron: 1.2mg | Total Fat: 10g",
                retailPrice = 120.0,
                distributorPrice = 85.0
            ),
            ProductEntity(
                sku = "COCO-MILK-05",
                name = "Premium Fresh Coconut Milk",
                category = "Wellness",
                description = "100% natural and creamy coconut milk made from fresh handpicked organic coconuts. Perfect for cooking, smoothies, baking, and rich desserts.",
                nutritionJson = "Calories: 230 kcal | Fat: 24.0g | Saturated Fat: 22.0g | Carbs: 4.0g | Sodium: 10mg",
                retailPrice = 90.0,
                distributorPrice = 65.0
            )
        )
        dao.insertProducts(products)

        // 2. Seed Multi-Warehouse Inventory
        val inventories = listOf(
            // Coastal Hub (Kochi)
            InventoryEntity(warehouseName = "Coastal Hub (Kochi)", productSku = "COCO-WATER-01", qty = 1500),
            InventoryEntity(warehouseName = "Coastal Hub (Kochi)", productSku = "COCO-OIL-02", qty = 450),
            InventoryEntity(warehouseName = "Coastal Hub (Kochi)", productSku = "COCO-CHIPS-03", qty = 2000),
            InventoryEntity(warehouseName = "Coastal Hub (Kochi)", productSku = "COCO-BITES-04", qty = 1200),
            InventoryEntity(warehouseName = "Coastal Hub (Kochi)", productSku = "COCO-MILK-05", qty = 800),

            // Central Hub (Bangalore)
            InventoryEntity(warehouseName = "Central Hub (Bangalore)", productSku = "COCO-WATER-01", qty = 800),
            InventoryEntity(warehouseName = "Central Hub (Bangalore)", productSku = "COCO-OIL-02", qty = 180),
            InventoryEntity(warehouseName = "Central Hub (Bangalore)", productSku = "COCO-CHIPS-03", qty = 1000),
            InventoryEntity(warehouseName = "Central Hub (Bangalore)", productSku = "COCO-BITES-04", qty = 600),
            InventoryEntity(warehouseName = "Central Hub (Bangalore)", productSku = "COCO-MILK-05", qty = 500)
        )
        dao.insertInventory(inventories)

        // 3. Seed Distributors
        val distributors = listOf(
            DistributorEntity(
                name = "Apex Distributors Ltd",
                region = "Kerala Region",
                creditLimit = 500000.0,
                creditUsed = 125000.0,
                kycStatus = "Approved"
            ),
            DistributorEntity(
                name = "Southern Foodways",
                region = "Karnataka & TN",
                creditLimit = 300000.0,
                creditUsed = 240000.0,
                kycStatus = "Approved"
            ),
            DistributorEntity(
                name = "Maratha Logistics Pvt Ltd",
                region = "Maharashtra Region",
                creditLimit = 800000.0,
                creditUsed = 0.0,
                kycStatus = "Pending"
            )
        )
        dao.insertDistributors(distributors)

        // 4. Seed QR Codes for bottle authentication
        val qrCodes = listOf(
            QREntity(uniqueCode = "AURA-WATER-BATCH42", productSku = "COCO-WATER-01", batchNo = "WTR-042-KCH", scanCount = 0),
            QREntity(uniqueCode = "AURA-WATER-BATCH43", productSku = "COCO-WATER-01", batchNo = "WTR-043-KCH", scanCount = 0),
            QREntity(uniqueCode = "AURA-OIL-BATCH12", productSku = "COCO-OIL-02", batchNo = "OIL-012-KCH", scanCount = 0),
            QREntity(uniqueCode = "AURA-CHIPS-BATCH88", productSku = "COCO-CHIPS-03", batchNo = "CHP-088-KCH", scanCount = 0)
        )
        dao.insertQRCodes(qrCodes)

        // 5. Seed CRM Tickets
        val tickets = listOf(
            CRMTicketEntity(
                buyerName = "Rahul Sharma",
                buyerType = "D2C_Customer",
                subject = "Delayed Delivery South Bangalore",
                notes = "Customer says ordered 2 days ago via retailer portal but hasn't arrived. Need status check with Southern Foodways delivery hub.",
                status = "Open"
            ),
            CRMTicketEntity(
                buyerName = "Apex Distributors Ltd",
                buyerType = "Distributor",
                subject = "Credit Extension Request",
                notes = "Requesting credit limit bump of ₹2,00,000 to cover upcoming peak festival season inventory demand.",
                status = "In Progress"
            ),
            CRMTicketEntity(
                buyerName = "Pooja Hegde",
                buyerType = "D2C_Customer",
                subject = "QR Scanning Loyalty Point Issue",
                notes = "QR code on Virgin Coconut Oil bottle scanned successfully but initial points didn't show up. Handled manually and resolved.",
                status = "Resolved"
            )
        )
        for (ticket in tickets) {
            dao.insertCRMTicket(ticket)
        }

        // 6. Seed initial loyalty transactions
        val transactions = listOf(
            LoyaltyTransactionEntity(
                buyerName = "Ananya Roy",
                points = 150,
                type = "Earned_Order",
                description = "Account signup welcome bonus & first order reward"
            ),
            LoyaltyTransactionEntity(
                buyerName = "Vihaan Verma",
                points = 15,
                type = "Earned_QR",
                description = "Purity Verification Scan: Batch WTR-042-KCH"
            )
        )
        for (tx in transactions) {
            dao.insertLoyaltyTransaction(tx)
        }
    }
}
