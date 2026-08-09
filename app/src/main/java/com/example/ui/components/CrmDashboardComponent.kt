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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.UUID

// ==========================================
// 1. DATA MODELS & DUMMY STRUCTURES
// ==========================================

data class CustomerSegment(
    val id: String,
    val name: String,
    val count: Int,
    val percentage: Int,
    val avgOrderValue: Double,
    val totalRevenue: Double,
    val icon: ImageVector,
    val description: String,
    val themeColor: Color
)

data class CrmOrder(
    val id: String,
    val customerName: String,
    val segmentId: String,
    val items: String,
    val totalAmount: Double,
    val date: String,
    val status: String
)

data class InteractionLog(
    val id: String,
    val customerName: String,
    val segmentId: String,
    val type: String, // "Call", "Email", "Chat", "Meeting"
    val notes: String,
    val timestamp: String,
    val agentName: String,
    val status: String // "Follow-up Scheduled", "Closed", "Action Required"
)

// ==========================================
// 2. MAIN CRM DASHBOARD COMPONENT
// ==========================================

@Composable
fun CrmDashboardComponent(
    modifier: Modifier = Modifier,
    onInteractionLogged: ((InteractionLog) -> Unit)? = null
) {
    // Initial dummy data lists
    val initialSegments = remember {
        listOf(
            CustomerSegment(
                id = "seg-1",
                name = "High-Value D2C",
                count = 145,
                percentage = 25,
                avgOrderValue = 3450.00,
                totalRevenue = 500250.00,
                icon = Icons.Default.WorkspacePremium,
                description = "Premium retail buyers with subscription preferences & high NPS",
                themeColor = Color(0xFFD4AF37) // Metallic Gold
            ),
            CustomerSegment(
                id = "seg-2",
                name = "Bulk Retailers",
                count = 48,
                percentage = 15,
                avgOrderValue = 24500.00,
                totalRevenue = 1176000.00,
                icon = Icons.Default.Storefront,
                description = "Distributors & wholesale kiranas buying in pallets",
                themeColor = Color(0xFF2E7D32) // Forest Green
            ),
            CustomerSegment(
                id = "seg-3",
                name = "Loyal Subscribers",
                count = 312,
                percentage = 40,
                avgOrderValue = 1850.00,
                totalRevenue = 577200.00,
                icon = Icons.Default.Autorenew,
                description = "Recurring monthly automatic orders for hydration packs",
                themeColor = Color(0xFF1976D2) // Active Blue
            ),
            CustomerSegment(
                id = "seg-4",
                name = "Inactive Leads",
                count = 220,
                percentage = 20,
                avgOrderValue = 0.0,
                totalRevenue = 0.0,
                icon = Icons.Default.PersonOutline,
                description = "Dormant accounts needing hyper-targeted marketing campaigns",
                themeColor = Color(0xFFC62828) // Deep Red
            )
        )
    }

    val initialOrders = remember {
        mutableStateListOf(
            CrmOrder("ORD-9204", "Aarav Mehta", "seg-1", "CocoAura Hydration Box x 2", 2880.0, "Today, 11:30 AM", "Pending"),
            CrmOrder("ORD-9203", "Kochi Kirana Hub", "seg-2", "Bulk Virgin Coconut Oil x 5", 45000.0, "Today, 09:15 AM", "Processing"),
            CrmOrder("ORD-9202", "Elena Rostova", "seg-3", "Monthly Water Auto-Ship", 1850.0, "Yesterday", "Completed"),
            CrmOrder("ORD-9201", "Priya Sharma", "seg-1", "Aura Gourmet Chips Premium Pack", 1250.0, "Yesterday", "Completed"),
            CrmOrder("ORD-9200", "Bangalore Mart", "seg-2", "Bulk Hydration Bottles x 10", 35000.0, "2 days ago", "Shipped"),
            CrmOrder("ORD-9199", "Rahul Singh", "seg-3", "Monthly Coconut Bites Pack", 1450.0, "3 days ago", "Completed")
        )
    }

    val initialInteractions = remember {
        mutableStateListOf(
            InteractionLog("INT-503", "Priya Sharma", "seg-1", "Call", "Inquired about coconut oil wholesale bulk discount. Highly interested in Q3 procurement.", "10 mins ago", "Aman R.", "Follow-up Scheduled"),
            InteractionLog("INT-502", "Elena Rostova", "seg-3", "Email", "In-app automated check-in. Customer confirmed subscription frequency is perfect.", "2 hours ago", "System", "Closed"),
            InteractionLog("INT-501", "Kochi Kirana Hub", "seg-2", "Chat", "Requested digital copy of custom VAT receipt and trade certification forms.", "Yesterday", "Rohan S.", "Closed"),
            InteractionLog("INT-500", "Aarav Mehta", "seg-1", "Meeting", "Discussed co-branding possibilities for high-end fitness center chains.", "2 days ago", "Meera K.", "Action Required"),
            InteractionLog("INT-499", "Dinesh K.", "seg-4", "Email", "Re-engagement email campaign sent. No response recorded as of yet.", "3 days ago", "Marketing Bot", "Follow-up Scheduled")
        )
    }

    // State Variables
    var selectedSegmentId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isNewInteractionModalOpen by remember { mutableStateOf(false) }

    // Form Fields for new interaction
    var formCustomerName by remember { mutableStateOf("") }
    var formSegmentId by remember { mutableStateOf("seg-1") }
    var formType by remember { mutableStateOf("Call") }
    var formNotes by remember { mutableStateOf("") }
    var formAgentName by remember { mutableStateOf("") }

    // Quick calculation values
    val totalLeads = initialSegments.sumOf { it.count }
    val conversionRate = 68.4
    val satisfactionScore = 4.8
    val pipelineValue = initialOrders.sumOf { it.totalAmount }

    // Filtered lists based on selection
    val filteredOrders = initialOrders.filter { order ->
        val matchesSegment = selectedSegmentId == null || order.segmentId == selectedSegmentId
        val matchesSearch = searchQuery.isEmpty() || order.customerName.contains(searchQuery, ignoreCase = true) || order.id.contains(searchQuery, ignoreCase = true)
        matchesSegment && matchesSearch
    }

    val filteredInteractions = initialInteractions.filter { interaction ->
        val matchesSegment = selectedSegmentId == null || interaction.segmentId == selectedSegmentId
        val matchesSearch = searchQuery.isEmpty() || interaction.customerName.contains(searchQuery, ignoreCase = true) || interaction.notes.contains(searchQuery, ignoreCase = true)
        matchesSegment && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 2.1 TITLE & PIPELINE VALUE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "CRM Hub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "CocoAura CRM Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Customer Relationships, Segments, & Interaction logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Active Pipeline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${"%,.2f".format(pipelineValue)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- 2.2 CRM STATS BANNER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statItems = listOf(
                Triple("Total Active Leads", "$totalLeads", Icons.Default.People),
                Triple("Avg. Satisfaction", "★ $satisfactionScore", Icons.Default.Star),
                Triple("Goal Conversion", "$conversionRate%", Icons.Default.TrendingUp)
            )

            statItems.forEach { (label, value, icon) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Column {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 2.3 CUSTOMER SEGMENTS GRID ---
        Text(
            text = "Customer Segments (Select one to filter orders & history)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(initialSegments) { segment ->
                val isSelected = selectedSegmentId == segment.id
                val borderStroke = if (isSelected) {
                    BorderStroke(2.dp, segment.themeColor)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                }

                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .clickable {
                            selectedSegmentId = if (isSelected) null else segment.id
                        }
                        .testTag("segment_card_${segment.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) segment.themeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = borderStroke
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = segment.icon,
                                    contentDescription = null,
                                    tint = segment.themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = segment.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = segment.themeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = segment.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            minLines = 2,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "ACCOUNTS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${segment.count} (${segment.percentage}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "AVG ORDER", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "₹${segment.avgOrderValue.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = segment.themeColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2.4 SEARCH AND CLEAR FILTER BAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customers or ID...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 50.dp)
                    .testTag("crm_search_field"),
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            if (selectedSegmentId != null) {
                Button(
                    onClick = { selectedSegmentId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Reset Segment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 2.5 TWO-PANEL TABS FOR RECENT ORDERS & INTERACTIONS ---
        var activeTabState by remember { mutableStateOf(0) } // 0 for Orders, 1 for Interactions

        TabRow(
            selectedTabIndex = activeTabState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTabState == 0,
                onClick = { activeTabState = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Recent Orders (${filteredOrders.size})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_orders")
            )
            Tab(
                selected = activeTabState == 1,
                onClick = { activeTabState = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Interactions (${filteredInteractions.size})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_interactions")
            )
        }

        // --- TAB CONTENTS ---
        AnimatedContent(
            targetState = activeTabState,
            label = "tab_content_switch"
        ) { activeTab ->
            when (activeTab) {
                0 -> {
                    // ORDERS LIST
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (filteredOrders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent orders match your segment/search filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            filteredOrders.forEach { order ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = order.id,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                val segmentBadge = initialSegments.find { it.id == order.segmentId }
                                                if (segmentBadge != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(segmentBadge.themeColor.copy(alpha = 0.1f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = segmentBadge.name,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = segmentBadge.themeColor
                                                        )
                                                    }
                                                }
                                            }

                                            // Status Badge
                                            val statusColor = when (order.status) {
                                                "Completed" -> Color(0xFF2E7D32)
                                                "Processing" -> Color(0xFFF57C00)
                                                "Shipped" -> Color(0xFF1976D2)
                                                else -> Color(0xFF757575)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(statusColor.copy(alpha = 0.12f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = order.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
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
                                                    text = order.customerName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = order.items,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Date: ${order.date}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }

                                            Text(
                                                text = "₹${"%,.2f".format(order.totalAmount)}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // INTERACTIONS CHRONOLOGY
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick Action: Add interaction log
                        CocoButton(
                            text = "Log New Interaction Call/Email",
                            onClick = { isNewInteractionModalOpen = true },
                            style = CocoButtonStyle.Accent,
                            leadingIcon = Icons.Default.Add,
                            fullWidth = true,
                            testTag = "log_new_interaction_button"
                        )

                        if (filteredInteractions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No logged interactions match selected filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            filteredInteractions.forEach { interaction ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Initials Avatar
                                        val initials = interaction.customerName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                                        val segmentColor = initialSegments.find { it.id == interaction.segmentId }?.themeColor ?: MaterialTheme.colorScheme.primary

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(segmentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = segmentColor
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = interaction.customerName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        val typeIcon = when (interaction.type) {
                                                            "Call" -> Icons.Default.Phone
                                                            "Email" -> Icons.Default.Mail
                                                            "Chat" -> Icons.Default.ChatBubble
                                                            else -> Icons.Default.Groups
                                                        }
                                                        Icon(
                                                            imageVector = typeIcon,
                                                            contentDescription = null,
                                                            tint = segmentColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "${interaction.type}  •  ${interaction.timestamp}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                // Follow-up status badge
                                                val statusColor = when (interaction.status) {
                                                    "Follow-up Scheduled" -> Color(0xFFF57C00)
                                                    "Closed" -> Color(0xFF2E7D32)
                                                    else -> Color(0xFFC62828)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(statusColor.copy(alpha = 0.1f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = interaction.status,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor
                                                    )
                                                }
                                            }

                                            Text(
                                                text = interaction.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Agent: ${interaction.agentName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )

                                                // Quick action: schedule follow up / mark closed toggle
                                                if (interaction.status != "Closed") {
                                                    Text(
                                                        text = "Mark Closed",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .clickable {
                                                                val index = initialInteractions.indexOfFirst { it.id == interaction.id }
                                                                if (index != -1) {
                                                                    initialInteractions[index] = interaction.copy(status = "Closed")
                                                                }
                                                            }
                                                            .padding(4.dp)
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
        }
    }

    // --- 2.6 NEW INTERACTION MODAL DIALOG ---
    if (isNewInteractionModalOpen) {
        Dialog(onDismissRequest = { isNewInteractionModalOpen = false }) {
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
                        text = "Log Customer Interaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                    // Customer Name input
                    Text(text = "Customer Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = formCustomerName,
                        onValueChange = { formCustomerName = it },
                        placeholder = { Text("e.g. Priyanjali S.") },
                        modifier = Modifier.fillMaxWidth().testTag("form_customer_name"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Agent Name input
                    Text(text = "Responsible Agent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = formAgentName,
                        onValueChange = { formAgentName = it },
                        placeholder = { Text("e.g. Aman R.") },
                        modifier = Modifier.fillMaxWidth().testTag("form_agent_name"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Segment selection Row
                    Text(text = "Customer Segment", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        initialSegments.forEach { seg ->
                            val isSelected = formSegmentId == seg.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) seg.themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSelected) seg.themeColor else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { formSegmentId = seg.id }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = seg.name.split(" ").firstOrNull() ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) seg.themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Interaction Type Row
                    Text(text = "Interaction Channel", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Call", "Email", "Chat", "Meeting").forEach { ch ->
                            val isSelected = formType == ch
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { formType = ch }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ch,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Notes input
                    Text(text = "Summary Notes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = formNotes,
                        onValueChange = { formNotes = it },
                        placeholder = { Text("Log detail notes about customer painpoints, requested quotes, or general updates here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("form_notes"),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 4
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isNewInteractionModalOpen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                if (formCustomerName.isNotBlank() && formNotes.isNotBlank()) {
                                    val newLog = InteractionLog(
                                        id = "INT-${UUID.randomUUID().toString().take(3).uppercase()}",
                                        customerName = formCustomerName,
                                        segmentId = formSegmentId,
                                        type = formType,
                                        notes = formNotes,
                                        timestamp = "Just now",
                                        agentName = if (formAgentName.isNotBlank()) formAgentName else "You (Admin)",
                                        status = "Follow-up Scheduled"
                                    )
                                    initialInteractions.add(0, newLog)
                                    onInteractionLogged?.invoke(newLog)
                                    
                                    // Reset form fields
                                    formCustomerName = ""
                                    formNotes = ""
                                    formAgentName = ""
                                    isNewInteractionModalOpen = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = formCustomerName.isNotBlank() && formNotes.isNotBlank()
                        ) {
                            Text("Log Log")
                        }
                    }
                }
            }
        }
    }
}
