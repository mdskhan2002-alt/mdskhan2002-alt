package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        InventoryEntity::class,
        DistributorEntity::class,
        QREntity::class,
        CRMTicketEntity::class,
        LoyaltyTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EcosystemDatabase : RoomDatabase() {
    abstract fun ecosystemDao(): EcosystemDao
}
