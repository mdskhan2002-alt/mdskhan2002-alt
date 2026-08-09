package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val sku: String,
    val name: String,
    val category: String,
    val description: String,
    val nutritionJson: String,
    val retailPrice: Double,
    val distributorPrice: Double,
    val isAvailable: Boolean = true
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val buyerType: String, // "D2C_Customer", "Distributor", "Retailer"
    val buyerName: String,
    val status: String,    // "Placed", "Processing", "Shipped", "Delivered", "Cancelled"
    val totalAmount: Double,
    val paymentStatus: String, // "Pending", "Success", "Failed", "On Credit"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val productSku: String,
    val productName: String,
    val qty: Int,
    val unitPrice: Double
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val warehouseName: String, // "Coastal Hub (Kochi)", "Central Hub (Bangalore)"
    val productSku: String,
    val qty: Int
)

@Entity(tableName = "distributors")
data class DistributorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val region: String,
    val creditLimit: Double,
    val creditUsed: Double,
    val kycStatus: String // "Pending", "Approved", "Rejected"
)

@Entity(tableName = "qr_codes")
data class QREntity(
    @PrimaryKey val uniqueCode: String,
    val productSku: String,
    val batchNo: String,
    val scanCount: Int = 0,
    val pointsReward: Int = 15
)

@Entity(tableName = "crm_tickets")
data class CRMTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val buyerName: String,
    val buyerType: String,
    val subject: String,
    val notes: String,
    val status: String, // "Open", "In Progress", "Resolved"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loyalty_transactions")
data class LoyaltyTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val buyerName: String,
    val points: Int,
    val type: String, // "Earned_QR", "Redeemed_Item"
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
