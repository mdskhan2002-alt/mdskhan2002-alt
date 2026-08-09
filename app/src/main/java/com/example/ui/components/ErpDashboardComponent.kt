package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InventoryEntity
import com.example.ui.EcosystemViewModel
import com.example.ui.Supplier
import com.example.ui.RawMaterialShipment
import java.util.UUID

@Composable
fun ErpDashboardComponent(
    viewModel: EcosystemViewModel,
    modifier: Modifier = Modifier
) {
    // Collect state from view model
    val inventories by viewModel.inventory.collectAsStateWithLifecycle(emptyList())
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle(emptyList())
    val shipments by viewModel.rawMaterialShipments.collectAsStateWithLifecycle(emptyList())

    // Internal UI States
    var activeTabState by remember { mutableStateOf(0) } // 0: Inventory, 1: Suppliers & Procurement
    var selectedWarehouseFilter by remember { mutableStateOf("All") } // "All", "Kochi", "Bangalore"
    var inventorySearchQuery by remember { mutableStateOf("") }
    var supplierSearchQuery by remember { mutableStateOf("") }

    // Dialog & Modal Triggers
    var isAdjustStockOpen by remember { mutableStateOf(false) }
    var selectedInventoryToAdjust by remember { mutableStateOf<InventoryEntity?>(null) }
    var adjustQtyValue by remember { mutableStateOf("") }

    var isNewSupplierOpen by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierContact by remember { mutableStateOf("") }
    var newSupplierEmail by remember { mutableStateOf("") }
    var newSupplierPhone by remember { mutableStateOf("") }
    var newSupplierRegion by remember { mutableStateOf("Kerala Coast") }
    var newSupplierMaterials by remember { mutableStateOf("") }

    var isProcureOrderOpen by remember { mutableStateOf(false) }
    var selectedSupplierForOrder by remember { mutableStateOf<Supplier?>(null) }
    var selectedMaterialForOrder by remember { mutableStateOf("") }
    var procureQty by remember { mutableStateOf("1000") }
    var procureUnit by remember { mutableStateOf("Liters") }
    var procureCost by remember { mutableStateOf("15000") }

    // Calculate critical stats
    val totalStockItems = inventories.sumOf { it.qty }
    val lowStockCount = inventories.count { it.qty < 300 }
    val activeSuppliersCount = suppliers.size
    val pendingShipmentsCount = shipments.count { it.status != "Received" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- TITLE & CORE METRICS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "ERP Operations",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "CocoAura ERP System",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "Supply Chain, Multi-Warehouse Stocks & Raw Materials",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active Status",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Live Sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- STATS BANNER BAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statItems = listOf(
                Triple("Total Stock Units", "$totalStockItems", Icons.Default.ShoppingCart),
                Triple("Low-Stock SKU", "$lowStockCount Alert(s)", Icons.Default.AddCircle),
                Triple("Active Suppliers", "$activeSuppliersCount Verified", Icons.Default.Person),
                Triple("Active Shipments", "$pendingShipmentsCount Pending", Icons.Default.LocalShipping)
            )

            statItems.forEach { (label, value, icon) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Column {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (label.contains("Low") && lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION NAVIGATION TABS ---
        TabRow(
            selectedTabIndex = activeTabState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTabState == 0,
                onClick = { activeTabState = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Warehouse Stock", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.testTag("erp_tab_warehouse")
            )
            Tab(
                selected = activeTabState == 1,
                onClick = { activeTabState = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Suppliers", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.testTag("erp_tab_suppliers")
            )
            Tab(
                selected = activeTabState == 2,
                onClick = { activeTabState = 2 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("AI Logistics", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.testTag("erp_tab_ai_logistics")
            )
        }

        // --- TAB CONTENT DYNAMIC RENDER ---
        AnimatedContent(
            targetState = activeTabState,
            label = "erp_tab_switch"
        ) { activeTab ->
            when (activeTab) {
                0 -> {
                    // ==========================================
                    // 1. WAREHOUSE STOCK TAB
                    // ==========================================
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Filters Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Warehouse Filter Chips
                            listOf("All", "Kochi", "Bangalore").forEach { wh ->
                                val isSelected = selectedWarehouseFilter == wh
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedWarehouseFilter = wh }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("filter_warehouse_$wh")
                                ) {
                                    Text(
                                        text = if (wh == "All") "All Hubs" else "$wh Hub",
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = inventorySearchQuery,
                            onValueChange = { inventorySearchQuery = it },
                            placeholder = { Text("Search by SKU (e.g. WATER, OIL)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (inventorySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { inventorySearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 50.dp)
                                .testTag("erp_inventory_search"),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )

                        // Filters List
                        val filteredStocks = inventories.filter { stock ->
                            val matchesWh = selectedWarehouseFilter == "All" || stock.warehouseName.contains(selectedWarehouseFilter, ignoreCase = true)
                            val matchesSearch = inventorySearchQuery.isEmpty() || stock.productSku.contains(inventorySearchQuery, ignoreCase = true)
                            matchesWh && matchesSearch
                        }

                        if (filteredStocks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No stock entries match the selected filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredStocks.forEach { stock ->
                                    val isLow = stock.qty < 300
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                if (isLow) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = stock.productSku,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )

                                                    if (isLow) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "LOW STOCK",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = stock.warehouseName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                // Dynamic visual level bar
                                                val maxCap = 2000f
                                                val progress = (stock.qty / maxCap).coerceIn(0f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(progress)
                                                            .background(if (isLow) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                                                    )
                                                }
                                            }

                                            // Actions
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${stock.qty} units",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "In Stock",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        selectedInventoryToAdjust = stock
                                                        adjustQtyValue = stock.qty.toString()
                                                        isAdjustStockOpen = true
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                                        .testTag("adjust_stock_btn_${stock.productSku}_${stock.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Adjust Stock",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // ==========================================
                    // 2. SUPPLIERS & PROCUREMENT TAB
                    // ==========================================
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registered Raw Material Suppliers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            IconButton(
                                onClick = { isNewSupplierOpen = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                    .testTag("log_new_supplier_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Supplier",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Search Bar for suppliers
                        OutlinedTextField(
                            value = supplierSearchQuery,
                            onValueChange = { supplierSearchQuery = it },
                            placeholder = { Text("Search suppliers or raw materials...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (supplierSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { supplierSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 50.dp)
                                .testTag("erp_supplier_search"),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )

                        // Suppliers list
                        val filteredSuppliers = suppliers.filter { sup ->
                            supplierSearchQuery.isEmpty() ||
                            sup.name.contains(supplierSearchQuery, ignoreCase = true) ||
                            sup.suppliedMaterials.any { it.contains(supplierSearchQuery, ignoreCase = true) }
                        }

                        if (filteredSuppliers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No suppliers found matching your query.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredSuppliers.forEach { supplier ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column {
                                                    Text(
                                                        text = supplier.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "Region: ${supplier.region}  •  Lead time: ${supplier.leadTimeDays} days",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Rating indicator
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Rating",
                                                        tint = Color(0xFFFFA000),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = supplier.rating.toString(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            // Materials Chips row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                supplier.suppliedMaterials.forEach { mat ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = mat,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                            }

                                            Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Contact: ${supplier.contactName}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = supplier.email,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }

                                                // Order raw materials button
                                                Button(
                                                    onClick = {
                                                        selectedSupplierForOrder = supplier
                                                        selectedMaterialForOrder = supplier.suppliedMaterials.firstOrNull() ?: ""
                                                        isProcureOrderOpen = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .testTag("procure_btn_${supplier.id}")
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Text("Procure", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // PROCUREMENT SHIPMENTS TRACKING LIST
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Procurement Shipments Log",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        if (shipments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No shipments logged yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                shipments.forEach { shp ->
                                    val isReceived = shp.status == "Received"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = shp.id,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "•  ${shp.date}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Status Indicator Badge
                                                val (statusBg, statusFg) = when (shp.status) {
                                                    "Received" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                                    "In Transit" -> Pair(Color(0xFFE3F2FD), Color(0xFF1976D2))
                                                    else -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(statusBg)
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = shp.status,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = statusFg
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Column {
                                                    Text(
                                                        text = shp.materialName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = "Supplier: ${shp.supplierName}  •  Qty: ${shp.qty} ${shp.unit}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Total Value: ₹${shp.cost}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }

                                                // Receive Shipment Action button
                                                if (!isReceived) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.receiveRawMaterialShipment(shp.id)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .height(28.dp)
                                                            .testTag("receive_shipment_btn_${shp.id}")
                                                    ) {
                                                        Text("Receive Shipment", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                } else {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Received",
                                                            tint = Color(0xFF2E7D32),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Stock Restocked",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // ==========================================
                    // 3. AI LOGISTICS PREDICTOR TAB
                    // ==========================================
                    val isAnalyzing by viewModel.isAnalyzingSupplyChain.collectAsStateWithLifecycle(false)
                    val analysisResult by viewModel.supplyChainAnalysisResult.collectAsStateWithLifecycle(null)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Title Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "AI Logistics",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aura-Logistics AI Predictor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Predict stockouts, analyze demand velocity, audit logistics bottlenecks, and discover optimal replenishment actions using Gemini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Action Button
                        Button(
                            onClick = { viewModel.generateSupplyChainPredictions() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ai_logistics_analyze_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isAnalyzing
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Text("Consulting Gemini AI Co-Pilot...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Analyze & Predict Supply Chain Risks", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Analysis Content Display
                        if (isAnalyzing) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Scanning ledger files & calculating replenishment rates...",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (analysisResult != null) {
                            val result = analysisResult!!
                            
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Predictive Logistics Report",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        
                                        IconButton(
                                            onClick = { viewModel.generateSupplyChainPredictions() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                    
                                    val cleanedText = result
                                        .replace("###", "")
                                        .replace("####", "")
                                        .replace("**", "")
                                        .replace("🚨", "")
                                        .replace("⚠️", "")
                                        .replace("✅", "")
                                        .replace("-", "•")
                                    
                                    Text(
                                        text = cleanedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Interactive Quick Restock Hub Actions
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                    Text(
                                        text = "⚡ ERP Quick-Restock Interventions",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                val mWaterSupplier = suppliers.find { it.id == "SUP-101" }
                                                if (mWaterSupplier != null) {
                                                    selectedSupplierForOrder = mWaterSupplier
                                                    selectedMaterialForOrder = "Tender Coconut Water"
                                                    procureQty = "1500"
                                                    procureUnit = "Liters"
                                                    procureCost = "45000"
                                                    isProcureOrderOpen = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("quick_procure_kochi")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Text("Restock Kochi Hub: 1,500L Tender Water (Malabar)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val mSugarSupplier = suppliers.find { it.id == "SUP-103" }
                                                if (mSugarSupplier != null) {
                                                    selectedSupplierForOrder = mSugarSupplier
                                                    selectedMaterialForOrder = "Organic Coconut Sugar"
                                                    procureQty = "500"
                                                    procureUnit = "kg"
                                                    procureCost = "60000"
                                                    isProcureOrderOpen = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("quick_procure_bangalore")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Text("Restock Bangalore Hub: 500kg Sugar (Coastal)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = "Ready to Predict",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "AI Predictor is Ready",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Click the button above to generate a real-time stock shortage & logistics report with intelligent replenishment suggestions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. DIALOG: ADJUST STOCK LEVEL
    // ==========================================
    if (isAdjustStockOpen && selectedInventoryToAdjust != null) {
        val stock = selectedInventoryToAdjust!!
        Dialog(onDismissRequest = { isAdjustStockOpen = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Adjust Warehouse Inventory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                    Text(
                        text = "Product SKU: ${stock.productSku}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Warehouse: ${stock.warehouseName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Set Current Quantity",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = adjustQtyValue,
                        onValueChange = { adjustQtyValue = it },
                        placeholder = { Text("e.g. 500") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_adjust_qty"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val addedQty = adjustQtyValue.toIntOrNull()
                                if (addedQty != null) {
                                    viewModel.updateInventoryLevel(stock.id, addedQty)
                                }
                                isAdjustStockOpen = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("form_adjust_qty_submit"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { isAdjustStockOpen = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 4. DIALOG: REGISTER NEW SUPPLIER
    // ==========================================
    if (isNewSupplierOpen) {
        Dialog(onDismissRequest = { isNewSupplierOpen = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Register New Supplier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                    Text(text = "Supplier Business Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newSupplierName,
                        onValueChange = { newSupplierName = it },
                        placeholder = { Text("e.g. Kerala Coconut Groves") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_new_supplier_name"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Text(text = "Contact Representative", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newSupplierContact,
                        onValueChange = { newSupplierContact = it },
                        placeholder = { Text("e.g. Ramesh K.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_new_supplier_contact"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Email", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newSupplierEmail,
                                onValueChange = { newSupplierEmail = it },
                                placeholder = { Text("e.g. sales@keralagroves.in") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_new_supplier_email"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Phone", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newSupplierPhone,
                                onValueChange = { newSupplierPhone = it },
                                placeholder = { Text("e.g. +91 98765...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_new_supplier_phone"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Region", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newSupplierRegion,
                                onValueChange = { newSupplierRegion = it },
                                placeholder = { Text("e.g. Kerala Coast") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_new_supplier_region"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Supplied Items (Comma Separated)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = newSupplierMaterials,
                                onValueChange = { newSupplierMaterials = it },
                                placeholder = { Text("e.g. Tender Coconut Water, Husk") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_new_supplier_materials"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newSupplierName.isNotBlank()) {
                                    val mats = newSupplierMaterials.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    val supplier = Supplier(
                                        id = "SUP-${(100 + suppliers.size + 1)}",
                                        name = newSupplierName,
                                        contactName = newSupplierContact,
                                        email = newSupplierEmail,
                                        phone = newSupplierPhone,
                                        region = newSupplierRegion,
                                        suppliedMaterials = if (mats.isEmpty()) listOf("Tender Coconut Water") else mats,
                                        rating = 4.5f,
                                        leadTimeDays = 3
                                    )
                                    viewModel.addSupplier(supplier)
                                }
                                isNewSupplierOpen = false
                                // Reset fields
                                newSupplierName = ""
                                newSupplierContact = ""
                                newSupplierEmail = ""
                                newSupplierPhone = ""
                                newSupplierMaterials = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("form_new_supplier_submit"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Register Supplier", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { isNewSupplierOpen = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 5. DIALOG: LOG PROCUREMENT ORDER
    // ==========================================
    if (isProcureOrderOpen && selectedSupplierForOrder != null) {
        val supplier = selectedSupplierForOrder!!
        Dialog(onDismissRequest = { isProcureOrderOpen = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Order Raw Materials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                    Text(text = "Supplier: ${supplier.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Standard Lead Time: ${supplier.leadTimeDays} days", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text(text = "Select Material to Order", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    // Simple select list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        supplier.suppliedMaterials.forEach { mat ->
                            val isSelected = selectedMaterialForOrder == mat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedMaterialForOrder = mat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mat,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Quantity", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = procureQty,
                                onValueChange = { procureQty = it },
                                placeholder = { Text("e.g. 1000") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_procure_qty"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Unit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = procureUnit,
                                onValueChange = { procureUnit = it },
                                placeholder = { Text("e.g. Liters") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_procure_unit"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    Text(text = "Estimated Cost (INR)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = procureCost,
                        onValueChange = { procureCost = it },
                        placeholder = { Text("e.g. 15000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_procure_cost"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val qtyInt = procureQty.toIntOrNull() ?: 1000
                                val costDbl = procureCost.toDoubleOrNull() ?: 15000.0
                                val shipment = RawMaterialShipment(
                                    id = "SHP-${(5000 + shipments.size + 1)}",
                                    supplierId = supplier.id,
                                    supplierName = supplier.name,
                                    materialName = selectedMaterialForOrder,
                                    qty = qtyInt,
                                    unit = procureUnit,
                                    cost = costDbl,
                                    status = "In Transit",
                                    date = "ETA: ${supplier.leadTimeDays} days"
                                )
                                viewModel.orderRawMaterial(shipment)
                                isProcureOrderOpen = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("form_procure_submit"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dispatch Order", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { isProcureOrderOpen = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
