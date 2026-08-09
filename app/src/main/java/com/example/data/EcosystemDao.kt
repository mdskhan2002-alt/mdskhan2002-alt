package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EcosystemDao {

    // --- Products ---
    @Query("SELECT * FROM products")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)


    // --- Orders & Items ---
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, newStatus: String)

    @Query("UPDATE orders SET paymentStatus = :paymentStatus WHERE id = :orderId")
    suspend fun updateOrderPaymentStatus(orderId: Int, paymentStatus: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: Int): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItems(): List<OrderItemEntity>


    // --- Inventory ---
    @Query("SELECT * FROM inventory")
    fun getAllInventoryFlow(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE productSku = :productSku")
    suspend fun getInventoryForProduct(productSku: String): List<InventoryEntity>

    @Query("UPDATE inventory SET qty = :newQty WHERE id = :inventoryId")
    suspend fun updateInventoryQty(inventoryId: Int, newQty: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: List<InventoryEntity>)


    // --- Distributors ---
    @Query("SELECT * FROM distributors")
    fun getAllDistributorsFlow(): Flow<List<DistributorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistributors(distributors: List<DistributorEntity>)

    @Query("UPDATE distributors SET kycStatus = :kycStatus WHERE id = :distributorId")
    suspend fun updateDistributorKycStatus(distributorId: Int, kycStatus: String)

    @Query("UPDATE distributors SET creditUsed = :creditUsed WHERE id = :distributorId")
    suspend fun updateDistributorCreditUsed(distributorId: Int, creditUsed: Double)


    // --- QR Codes ---
    @Query("SELECT * FROM qr_codes")
    fun getAllQRCodesFlow(): Flow<List<QREntity>>

    @Query("SELECT * FROM qr_codes WHERE uniqueCode = :code")
    suspend fun getQRCode(code: String): QREntity?

    @Query("UPDATE qr_codes SET scanCount = scanCount + 1 WHERE uniqueCode = :code")
    suspend fun incrementQRScanCount(code: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQRCodes(qrCodes: List<QREntity>)


    // --- CRM Tickets ---
    @Query("SELECT * FROM crm_tickets ORDER BY createdAt DESC")
    fun getAllCRMTicketsFlow(): Flow<List<CRMTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCRMTicket(ticket: CRMTicketEntity)

    @Query("UPDATE crm_tickets SET status = :status WHERE id = :id")
    suspend fun updateCRMTicketStatus(id: Int, status: String)


    // --- Loyalty Transactions ---
    @Query("SELECT * FROM loyalty_transactions ORDER BY createdAt DESC")
    fun getAllLoyaltyTransactionsFlow(): Flow<List<LoyaltyTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyTransaction(transaction: LoyaltyTransactionEntity)
}
