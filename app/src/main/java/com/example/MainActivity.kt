package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.launch
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainEcosystemScreen()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainEcosystemScreen(
    viewModel: EcosystemViewModel = viewModel()
) {
    val currentPersona by viewModel.currentPersona.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with Brand Logo & Perspective Switching Tabs
            EcosystemHeader(
                activePersona = currentPersona,
                onPersonaSelected = { viewModel.switchPersona(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // Firebase Cloud Authentication and Sync Panel
            FirebaseCloudSyncPanel(viewModel)

            // Main Workspace matching the active Persona with fade transitions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentPersona,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "persona_transition"
                ) { persona ->
                    when (persona) {
                        Persona.CUSTOMER -> CustomerWorkspace(viewModel)
                        Persona.DISTRIBUTOR -> DistributorWorkspace(viewModel)
                        Persona.RETAILER -> RetailerWorkspace(viewModel)
                        Persona.ADMIN -> AdminWorkspace(viewModel)
                    }
                }
            }
        }
    }
}

// --- SHARED BRAND HEADER ---
@Composable
fun EcosystemHeader(
    activePersona: Persona,
    onPersonaSelected: (Persona) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Programmatic Logo Drawing (Coconut with Aura Glow effect)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .drawBehind {
                        // Drawing golden aura outer glow
                        drawCircle(
                            color = Color(0xFFF1C40F),
                            radius = size.width * 0.45f,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "CocoAura Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "CocoAura Foods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pure Coconut. Pure Aura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Persona Selection Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Persona.values()) { persona ->
                val isSelected = persona == activePersona
                val label = when (persona) {
                    Persona.CUSTOMER -> "Customer"
                    Persona.DISTRIBUTOR -> "Distributor B2B"
                    Persona.RETAILER -> "Retailer B2B"
                    Persona.ADMIN -> "Admin Control"
                }
                val icon = when (persona) {
                    Persona.CUSTOMER -> Icons.Default.Person
                    Persona.DISTRIBUTOR -> Icons.Default.Business
                    Persona.RETAILER -> Icons.Default.Storefront
                    Persona.ADMIN -> Icons.Default.SettingsSuggest
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onPersonaSelected(persona) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        iconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("tab_${persona.name.lowercase()}")
                )
            }
        }
    }
}


// ==========================================
// 1. CUSTOMER WORKSPACE
// ==========================================
@Composable
fun CustomerWorkspace(viewModel: EcosystemViewModel) {
    val productsList by viewModel.filteredProducts.collectAsStateWithLifecycle(emptyList())
    val searchVal by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeCat by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cart by viewModel.customerCart.collectAsStateWithLifecycle()
    val loyaltyTxs by viewModel.loyaltyTransactions.collectAsStateWithLifecycle(emptyList())
    val qrMsg by viewModel.qrScanMessage.collectAsStateWithLifecycle()
    val ticketSuccess by viewModel.ticketSuccessMessage.collectAsStateWithLifecycle()
    
    val totalPoints = loyaltyTxs.filter { it.buyerName == "Rahul Sharma" }.sumOf { 
        if (it.type == "Redeemed_Item") -it.points else it.points 
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Banner (Programmatic Design Drawing)
        item {
            CustomerHeroBanner()
        }

        // CocoAura Brand Film Segment
        item {
            CocoAuraVideoPlayer()
        }

        // Live Chat Assistant Slide-In / Toggle Button
        item {
            AuraChatAssistantCard(viewModel)
        }

        // Shared Component Library Showcase (Buttons and Cards)
        item {
            CocoAuraDesignSystemShowcase()
        }

        // Search and Category Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchVal,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search refreshing coconut water, oils...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )

                // Category Tabs
                val categories = listOf("All", "Beverages", "Snacks", "Wellness")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = cat == activeCat
                        Button(
                            onClick = { viewModel.selectedCategory.value = cat },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Product Catalog Title
        item {
            Text(
                text = "Natural Wellness Catalog",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Product Cards Grid
        items(productsList) { product ->
            val qtyInCart = cart[product.sku] ?: 0
            CustomerProductCard(
                product = product,
                qtyInCart = qtyInCart,
                onAdd = { viewModel.addToCustomerCart(product.sku) },
                onRemove = { viewModel.removeFromCustomerCart(product.sku) }
            )
        }

        // Interactive QR verification (Bottle Scan Simulator)
        item {
            CustomerQRScannerCard(viewModel = viewModel, qrMsg = qrMsg)
        }

        // Interactive Support Ticket (CRM Request Form)
        item {
            CustomerSupportTicketCard(viewModel = viewModel, ticketSuccess = ticketSuccess)
        }

        // Loyalty Program Dashboard Ledger
        item {
            LoyaltyDashboardSection(totalPoints = totalPoints, transactions = loyaltyTxs)
        }

        // Shopping Cart Sheet Summary
        if (cart.isNotEmpty()) {
            item {
                CustomerCartSummaryCard(viewModel = viewModel, cart = cart, products = productsList)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CustomerHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Render the brand banner image
        Image(
            painter = painterResource(id = R.drawable.img_hero_banner),
            contentDescription = "CocoAura Brand Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark forest-green gradient overlay to ensure text remains perfectly readable on any background image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0B4B32).copy(alpha = 0.9f),
                            Color(0xFF0B4B32).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Banner text overlay content
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.7f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1C40F).copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "100% PURE & ORGANIC",
                    fontSize = 10.sp,
                    color = Color(0xFFF1C40F),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Pure Coconut.\nPure Aura.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )
            Text(
                text = "Experience India's premium sustainable coconut ecosystem.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun CocoAuraVideoPlayer(modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("coco_aura_video_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Video Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "CocoAura Brand Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Watch our farm-to-family journey & commercial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Video Player Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val videoUri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4")
                            setVideoURI(videoUri)
                            
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                videoViewInstance = this
                            }
                            
                            setOnErrorListener { _, _, _ ->
                                Toast.makeText(ctx, "Loading promotional video stream...", Toast.LENGTH_SHORT).show()
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        videoViewInstance = view
                    }
                )

                // Interactive Overlay Center Play Button
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                videoViewInstance?.let {
                                    it.start()
                                    isPlaying = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Video",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Control Deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            videoViewInstance?.let {
                                if (it.isPlaying) {
                                    it.pause()
                                    isPlaying = false
                                } else {
                                    it.start()
                                    isPlaying = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("play_pause_video_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play Video",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reset/Replay Button
                    OutlinedButton(
                        onClick = {
                            videoViewInstance?.let {
                                it.seekTo(0)
                                it.start()
                                isPlaying = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("replay_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Restart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Replay",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Streaming: CocoAura Story",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun CustomerProductCard(
    product: ProductEntity,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    var expandedNutrition by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.sku.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = product.category,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "SKU: ${product.sku}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Custom Programmatic Product Image placeholder or Real High-Quality Image Preview
                var showFullLabel by remember { mutableStateOf(false) }
                val imageResId = when (product.sku) {
                    "COCO-WATER-01" -> R.drawable.img_coco_aura_water_bottle
                    "COCO-MILK-05" -> R.drawable.img_coco_aura_milk_label
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable { if (imageResId != null) showFullLabel = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageResId != null) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom Image",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp).padding(2.dp)
                            )
                        }
                    } else {
                        val icon = when (product.sku) {
                            "COCO-OIL-02" -> Icons.Default.Opacity
                            "COCO-CHIPS-03" -> Icons.Default.BakeryDining
                            else -> Icons.Default.Cookie
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = product.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Full-Screen High-Resolution Label / Packaging Infographic Inspector
                if (showFullLabel && imageResId != null) {
                    Dialog(onDismissRequest = { showFullLabel = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("label_inspector_dialog"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Official Packaging & Ingredients Label",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { showFullLabel = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Preview")
                                    }
                                }

                                Image(
                                    painter = painterResource(id = imageResId),
                                    contentDescription = "Full Label Infographic",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(380.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                Text(
                                    text = "Tap close or click outside to return",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Nutrition Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedNutrition = !expandedNutrition }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expandedNutrition) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Nutrition Details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View Nutrition Panel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expandedNutrition) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = product.nutritionJson,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${product.retailPrice}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (qtyInCart == 0) {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_to_cart_${product.sku.lowercase()}")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Cart", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Remove, "Remove", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Text(
                            text = qtyInCart.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, "Add", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}


// --- QR BOTTLE SCANNER SIMULATOR ---
@Composable
fun CustomerQRScannerCard(
    viewModel: EcosystemViewModel,
    qrMsg: String?
) {
    val scanInput by viewModel.qrScanCodeInput.collectAsStateWithLifecycle()
    val qrResult by viewModel.qrScanResult.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qr_scanner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "QR Verify",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aura QR Verification Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Verify bottle purity audits & check counterfeit flags. Tap on a batch code below to load or enter yours.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select Sample Pure Batch Code:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("AURA-WATER-BATCH42", "AURA-OIL-BATCH12", "AURA-INVALID").forEach { batch ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.qrScanCodeInput.value = batch }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = batch, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = scanInput,
                    onValueChange = { viewModel.qrScanCodeInput.value = it },
                    placeholder = { Text("Enter Batch Security Code") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.scanQRCode("Rahul Sharma") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("verify_qr_button")
                ) {
                    Text("Verify", fontWeight = FontWeight.Bold)
                }
            }

            if (qrMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val isSuccess = qrMsg.startsWith("✅")
                val isWarning = qrMsg.startsWith("⚠️")
                val bgColor = when {
                    isSuccess -> Color(0xFFE8F8F5)
                    isWarning -> Color(0xFFFEF9E7)
                    else -> Color(0xFFFDEDEC)
                }
                val textColor = when {
                    isSuccess -> Color(0xFF117A65)
                    isWarning -> Color(0xFFB7950B)
                    else -> Color(0xFF922B21)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = qrMsg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        if (qrResult != null) {
                            Text(
                                text = "Registered Batch: ${qrResult!!.batchNo} | Points Gained: ${qrResult!!.pointsReward} pts | System Scan Count: ${qrResult!!.scanCount} times",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Clear verification results",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            modifier = Modifier
                                .clickable { viewModel.clearQRResult() }
                                .drawBehind {
                                    drawLine(
                                        color = textColor,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}


// --- CUSTOMER SUPPORT CRM CARD ---
@Composable
fun CustomerSupportTicketCard(
    viewModel: EcosystemViewModel,
    ticketSuccess: String?
) {
    val subject by viewModel.ticketSubject.collectAsStateWithLifecycle()
    val notes by viewModel.ticketNotes.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmittingTicket.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "CRM Support",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Customer Support Ticket (CRM)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Have issues with shipping or quality? Open a direct ticket to our customer success team.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { viewModel.ticketSubject.value = it },
                placeholder = { Text("Subject / Issue Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ticket_subject"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.ticketNotes.value = it },
                placeholder = { Text("Elaborate your notes...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .testTag("ticket_notes"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Button(
                onClick = { viewModel.submitSupportTicket("Rahul Sharma", "D2C_Customer") },
                enabled = !isSubmitting && subject.isNotEmpty() && notes.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ticket_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit Ticket to ERP Desk", fontWeight = FontWeight.Bold)
            }

            if (ticketSuccess != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F8F5))
                        .clickable { viewModel.clearTicketSuccess() }
                        .padding(10.dp)
                ) {
                    Text(
                        text = ticketSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF117A65)
                    )
                }
            }
        }
    }
}


// --- LOYALTY PROGRAM DASHBOARD ---
@Composable
fun LoyaltyDashboardSection(
    totalPoints: Int,
    transactions: List<LoyaltyTransactionEntity>
) {
    var expandedHistory by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Loyalty Points",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aura Loyalty Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Gold Tier",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accumulated Balance:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$totalPoints Aura Points",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = { expandedHistory = !expandedHistory },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (expandedHistory) "Hide Ledger" else "View Ledger",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = expandedHistory) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    
                    Text(
                        text = "Transaction History Log:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    transactions.forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = tx.description,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (tx.type == "Earned_QR") "Source: QR Scan" else "Source: ERP Purchase",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = "+${tx.points} pts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                    }
                }
            }
        }
    }
}


// --- CUSTOMER SHOPPING CART SECTION ---
@Composable
fun CustomerCartSummaryCard(
    viewModel: EcosystemViewModel,
    cart: Map<String, Int>,
    products: List<ProductEntity>
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "D2C Basket Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "${cart.values.sum()} Items",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }

            var totalPrice = 0.0
            cart.forEach { (sku, qty) ->
                val prod = products.find { it.sku == sku }
                if (prod != null) {
                    totalPrice += prod.retailPrice * qty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${prod.name} (x$qty)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "₹${prod.retailPrice * qty}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Amount:",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "₹$totalPrice",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        viewModel.checkoutCustomer("Rahul Sharma") { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("checkout_button")
                ) {
                    Text("Checkout Order", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}


// --- GEMINI REAL-TIME CHAT ASSISTANT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraChatAssistantCard(viewModel: EcosystemViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val inputText by viewModel.chatInputText.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }

    val useDeepThinking by viewModel.useDeepThinking.collectAsStateWithLifecycle()
    val useWebSearch by viewModel.useWebSearch.collectAsStateWithLifecycle()
    val isTranscribing by viewModel.isTranscribing.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var audioFileUri by remember { mutableStateOf<java.io.File?>(null) }

    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted. Tap microphone to record.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission denied. Cannot transcribe audio.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startRecording() {
        try {
            val file = java.io.File(context.cacheDir, "aura_recorded_audio.3gp")
            audioFileUri = file
            
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                android.media.MediaRecorder()
            }
            
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            
            mediaRecorder = recorder
            isRecording = true
            Toast.makeText(context, "Recording voice query...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("AudioRecord", "Recording failed to start", e)
            Toast.makeText(context, "Recording simulated microphone audio...", Toast.LENGTH_SHORT).show()
            isRecording = true
        }
    }

    fun stopAndTranscribe() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
            }
            isRecording = false
            Toast.makeText(context, "Transcribing voice query...", Toast.LENGTH_SHORT).show()

            if (audioFileUri != null && audioFileUri!!.exists() && audioFileUri!!.length() > 0) {
                val bytes = audioFileUri!!.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                viewModel.transcribeAudioInput(base64, "audio/3gpp")
            } else {
                // Fallback simulated queries
                val simulatedQueries = listOf(
                    "Are CocoAura products 100% organic and sustainable?",
                    "What are the benefits of your electrolyte-rich Coconut Water?",
                    "How can I sign up as an official regional distributor?"
                )
                viewModel.chatInputText.value = simulatedQueries.random()
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioRecord", "Failed to stop/transcribe", e)
            isRecording = false
            val simulatedQueries = listOf(
                "Are CocoAura products 100% organic and sustainable?",
                "What are the benefits of your electrolyte-rich Coconut Water?",
                "How can I sign up as an official regional distributor?"
            )
            viewModel.chatInputText.value = simulatedQueries.random()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Aura Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aura-Bot AI Assistant",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isLoading) "Aura-Bot is typing..." else "Ask details, recipe hacks & B2B credit terms",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // AI Model & Search Settings Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = useDeepThinking,
                            onClick = { viewModel.useDeepThinking.value = !useDeepThinking },
                            label = { Text("Deep Thinking (pro)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        FilterChip(
                            selected = useWebSearch,
                            onClick = { viewModel.useWebSearch.value = !useWebSearch },
                            label = { Text("Web Grounding", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !useDeepThinking
                        )
                    }

                    if (useDeepThinking) {
                        Text(
                            text = "⚡ Powered by gemini-3.1-pro-preview (ThinkingLevel.HIGH)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            messages.forEach { msg ->
                                val alignment = if (msg.isBot) Alignment.Start else Alignment.End
                                val cardBg = if (msg.isBot) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                                val cardText = if (msg.isBot) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = alignment
                                ) {
                                    Text(
                                        text = msg.sender,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (msg.isBot) 0.dp else 12.dp,
                                                    bottomEnd = if (msg.isBot) 12.dp else 0.dp
                                                )
                                            )
                                            .background(cardBg)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 11.sp,
                                            color = cardText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.chatInputText.value = it },
                            placeholder = { Text("Ask Aura-Bot...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("chat_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Audio Transcription Microphone Button
                        IconButton(
                            onClick = {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    if (isRecording) {
                                        stopAndTranscribe()
                                    } else {
                                        startRecording()
                                    }
                                } else {
                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop Recording" else "Record Audio",
                                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { viewModel.sendMessageToAura() },
                            enabled = inputText.isNotEmpty() && !isLoading,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (inputText.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. DISTRIBUTOR WORKSPACE
// ==========================================
@Composable
fun DistributorWorkspace(viewModel: EcosystemViewModel) {
    val context = LocalContext.current
    val productsList by viewModel.products.collectAsStateWithLifecycle(emptyList())
    val distributors by viewModel.distributors.collectAsStateWithLifecycle(emptyList())
    val activeDistributorId by viewModel.activeDistributorId.collectAsStateWithLifecycle()
    val b2bCart by viewModel.b2bCart.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle(emptyList())

    val activeDistributor = distributors.find { it.id == activeDistributorId } ?: DistributorEntity(
        id = 1,
        name = "Apex Distributors Ltd",
        region = "Kerala Region",
        creditLimit = 500000.0,
        creditUsed = 125000.0,
        kycStatus = "Approved"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Distributor Account:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        distributors.forEach { d ->
                            val isSel = d.id == activeDistributorId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.activeDistributorId.value = d.id }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = d.name.substringBefore(" "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            B2BLEDGERCard(
                partnerName = activeDistributor.name,
                creditLimit = activeDistributor.creditLimit,
                creditUsed = activeDistributor.creditUsed,
                kycStatus = activeDistributor.kycStatus
            )
        }

        item {
            Text(
                text = "B2B Distributor Bulk Orders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (activeDistributor.kycStatus == "Approved") {
            items(productsList) { prod ->
                val qtyInB2B = b2bCart[prod.sku] ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imageResId = when (prod.sku) {
                            "COCO-WATER-01" -> R.drawable.img_coco_aura_water_bottle
                            "COCO-MILK-05" -> R.drawable.img_coco_aura_milk_label
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageResId != null) {
                                Image(
                                    painter = painterResource(id = imageResId),
                                    contentDescription = prod.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val icon = when (prod.sku) {
                                    "COCO-OIL-02" -> Icons.Default.Opacity
                                    "COCO-CHIPS-03" -> Icons.Default.BakeryDining
                                    else -> Icons.Default.Cookie
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = prod.name,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prod.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "B2B Price: ₹${prod.distributorPrice}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "|  SKU: ${prod.sku}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (qtyInB2B == 0) {
                            Button(
                                onClick = { viewModel.addToB2BCart(prod.sku) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("+50 Units", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.removeFromB2BCart(prod.sku) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircle, "Less", tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    text = qtyInB2B.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { viewModel.addToB2BCart(prod.sku) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.AddCircle, "More", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFDEDEC))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Block, "Blocked", tint = Color(0xFF922B21), modifier = Modifier.size(36.dp))
                        Text(
                            text = "B2B Bulk Purchases Locked",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF922B21)
                        )
                        Text(
                            text = "Maratha Logistics Pvt Ltd's KYC state is currently PENDING review. Switch tabs to Admin Control and approve KYC to unlock their line of credit catalog immediately.",
                            fontSize = 11.sp,
                            color = Color(0xFF922B21).copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (b2bCart.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "B2B Bulk Basket Confirmation",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        var bulkTotal = 0.0
                        b2bCart.forEach { (sku, qty) ->
                            val prod = productsList.find { it.sku == sku }
                            if (prod != null) {
                                bulkTotal += prod.distributorPrice * qty
                                Text(
                                    text = "- ${prod.name}: $qty Units (₹${prod.distributorPrice * qty})",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "B2B Payment Method:",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            val payMode by viewModel.b2bPaymentMode.collectAsStateWithLifecycle()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("On Credit", "Online").forEach { m ->
                                    val isM = payMode == m
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isM) Color.White else Color.White.copy(alpha = 0.15f))
                                            .clickable { viewModel.b2bPaymentMode.value = m }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = m,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isM) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Contract Value:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹$bulkTotal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.checkoutB2B("Distributor") { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Submit PO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Account Bulk Order History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val distOrders = orders.filter { it.buyerName == activeDistributor.name }
        if (distOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No previous purchase orders logged under this distributor.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(distOrders) { ord ->
                B2BOrderLogItem(orderId = ord.id, status = ord.status, total = ord.totalAmount, payStatus = ord.paymentStatus)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun B2BLEDGERCard(
    partnerName: String,
    creditLimit: Double,
    creditUsed: Double,
    kycStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credit Line & KYC Audit Desk",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val kycColor = if (kycStatus == "Approved") Color(0xFF27AE60) else Color(0xFFF39C12)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(kycColor.copy(alpha = 0.1f))
                        .border(1.dp, kycColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "KYC: $kycStatus",
                        fontSize = 10.sp,
                        color = kycColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total Credit Limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text(text = "₹$creditLimit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Credit Balance Used", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text(text = "₹$creditUsed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Available Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    val avail = creditLimit - creditUsed
                    Text(text = "₹$avail", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF27AE60))
                }
            }
        }
    }
}

@Composable
fun B2BOrderLogItem(
    orderId: Int,
    status: String,
    total: Double,
    payStatus: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Purchase Order #$orderId", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Billing: $payStatus", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text(text = "|  Status: $status", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "₹$total",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


// ==========================================
// 3. RETAILER WORKSPACE
// ==========================================
@Composable
fun RetailerWorkspace(viewModel: EcosystemViewModel) {
    val context = LocalContext.current
    val productsList by viewModel.products.collectAsStateWithLifecycle(emptyList())
    val b2bCart by viewModel.b2bCart.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle(emptyList())
    val activeRetailerName by viewModel.activeRetailerName.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Active Kirana Store Profile",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeRetailerName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Assigned Distributor: Apex Distributors Ltd (Kochi Hub)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Kirana Storefront Catalog",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(productsList) { prod ->
            val qtyInB2B = b2bCart[prod.sku] ?: 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val imageResId = when (prod.sku) {
                        "COCO-WATER-01" -> R.drawable.img_coco_aura_water_bottle
                        "COCO-MILK-05" -> R.drawable.img_coco_aura_milk_label
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageResId != null) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = prod.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val icon = when (prod.sku) {
                                "COCO-OIL-02" -> Icons.Default.Opacity
                                "COCO-CHIPS-03" -> Icons.Default.BakeryDining
                                else -> Icons.Default.Cookie
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = prod.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Retailer Price: ₹${prod.distributorPrice}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    if (qtyInB2B == 0) {
                        Button(
                            onClick = { viewModel.addToB2BCart(prod.sku) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+50 units", fontSize = 10.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { viewModel.removeFromB2BCart(prod.sku) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.RemoveCircle, "Less", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(text = qtyInB2B.toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            IconButton(onClick = { viewModel.addToB2BCart(prod.sku) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.AddCircle, "More", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        if (b2bCart.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        viewModel.checkoutB2B("Retailer") { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Bulk Kirana Purchase (₹${b2bCart.entries.sumOf { (sku, q) -> (productsList.find { it.sku == sku }?.distributorPrice ?: 0.0) * q }})", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Storefront Shared Link Setup", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Text(text = "D2C Customer storefront dynamic referral link configured: \nhttps://cocoaura.in/store/pooja-kirana", fontSize = 11.sp)
                    Button(
                        onClick = { Toast.makeText(context, "Link Copied to Clipboard!", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Copy Store Link", fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


// ==========================================
// 4. ADMIN CONTROL PANEL WORKSPACE
// ==========================================
@Composable
fun AdminWorkspace(viewModel: EcosystemViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle(emptyList())
    val inventories by viewModel.inventory.collectAsStateWithLifecycle(emptyList())
    val tickets by viewModel.crmTickets.collectAsStateWithLifecycle(emptyList())
    val distributors by viewModel.distributors.collectAsStateWithLifecycle(emptyList())
    val qrCodes by viewModel.qrCodes.collectAsStateWithLifecycle(emptyList())

    var adminTabState by remember { mutableStateOf(0) } // 0: CRM & Customer Sales, 1: ERP Operations

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CocoAura Admin Control Panel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Tab Switcher for Admin Operations
                TabRow(
                    selectedTabIndex = adminTabState,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = adminTabState == 0,
                        onClick = { adminTabState = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("CRM & Sales Portal", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.testTag("admin_tab_crm")
                    )
                    Tab(
                        selected = adminTabState == 1,
                        onClick = { adminTabState = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("ERP & Suppliers", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.testTag("admin_tab_erp")
                    )
                }
            }
        }

        if (adminTabState == 0) {
            // ==========================================
            // TAB 0: CRM & CUSTOMER SALES
            // ==========================================
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Gross Revenue", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            val gross = orders.sumOf { it.totalAmount }
                            Text(text = "₹$gross", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "D2C + B2B combined", fontSize = 9.sp, color = Color(0xFF27AE60), fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Purchase Orders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            Text(text = "${orders.size} POs", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Active tracking", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                CrmDashboardComponent(
                    modifier = Modifier.fillMaxWidth(),
                    onInteractionLogged = { interaction ->
                        viewModel.syncDataToFirestore(
                            collection = "interactions",
                            docId = interaction.id,
                            data = mapOf(
                                "id" to interaction.id,
                                "customerName" to interaction.customerName,
                                "segmentId" to interaction.segmentId,
                                "type" to interaction.type,
                                "notes" to interaction.notes,
                                "timestamp" to interaction.timestamp,
                                "agentName" to interaction.agentName,
                                "status" to interaction.status
                            )
                        )
                    }
                )
            }

            item {
                AiImageLabCard(viewModel)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Sales by Product SKU (Volume)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val skus = listOf("COCO-WATER-01", "COCO-OIL-02", "COCO-CHIPS-03", "COCO-BITES-04")
                        skus.forEach { sku ->
                            val qtySold = if (sku == "COCO-WATER-01") 600 else if (sku == "COCO-CHIPS-03") 200 else 50
                            val maxVol = 1000f
                            val progress = (qtySold / maxVol).coerceIn(0f, 1f)

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(text = sku.substringAfter("-"), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "$qtySold units", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Partner KYC & Compliance Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(distributors) { dist ->
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
                            Column {
                                Text(text = dist.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Region: ${dist.region} | Credit Limit: ₹${dist.creditLimit}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }

                            val statusColor = when (dist.kycStatus) {
                                "Approved" -> Color(0xFF27AE60)
                                "Rejected" -> Color(0xFFC0392B)
                                else -> Color(0xFFD4AC0D)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = dist.kycStatus, fontSize = 9.sp, color = statusColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (dist.kycStatus == "Pending") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.approveDistributorKyc(dist.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Approve KYC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.rejectDistributorKyc(dist.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reject Partner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Live Support Tickets Queue (CRM)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val openTickets = tickets.filter { it.status != "Resolved" }
            if (openTickets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Hooray! Support queue is empty. Clean CRM slate.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(openTickets) { ticket ->
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
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = ticket.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "Logged by: ${ticket.buyerName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(text = "|  Role: ${ticket.buyerType}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = ticket.status, fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(text = ticket.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), lineHeight = 16.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { viewModel.resolveSupportTicket(ticket.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Mark Ticket as Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Registered Batch QR Codes (Counterfeit audit)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(qrCodes) { code ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Security Key: ${code.uniqueCode}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(text = "Assigned SKU: ${code.productSku} | Batch: ${code.batchNo}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (code.scanCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Scanned: ${code.scanCount} times",
                            fontSize = 10.sp,
                            color = if (code.scanCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // TAB 1: ERP & SUPPLIERS Operations
            // ==========================================
            item {
                ErpDashboardComponent(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Multi-Warehouse ERP Stocks (Kochi vs Bangalore)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        inventories.groupBy { it.warehouseName }.forEach { (whName, stockList) ->
                            Text(text = whName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            stockList.forEach { stock ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    val shortSku = stock.productSku.replace("COCO-", "")
                                    Text(text = shortSku, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(60.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val maxQty = 2000f
                                    val fill = (stock.qty / maxQty).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fill)
                                                .background(if (stock.qty < 300) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${stock.qty} left", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CocoAuraDesignSystemShowcase() {
    var expanded by remember { mutableStateOf(false) }
    var primaryClickCount by remember { mutableStateOf(0) }
    var secondaryClickCount by remember { mutableStateOf(0) }
    var accentClickCount by remember { mutableStateOf(0) }
    var isButtonLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    CocoCard(
        style = CocoCardStyle.Branded,
        testTag = "design_system_showcase_card"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "CocoAura Design System",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Shared Component Library & Responsive Tokens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("toggle_design_showcase")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Card Styles Showcase
                Text(
                    text = "Responsive Cards (CocoCardStyle)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CocoCard(
                            style = CocoCardStyle.Filled,
                            horizontalPadding = 10.dp,
                            verticalPadding = 10.dp
                        ) {
                            Text(
                                text = "Filled Card",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Soft surfaceVariant container with 50% opacity.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CocoCard(
                            style = CocoCardStyle.Elevated,
                            horizontalPadding = 10.dp,
                            verticalPadding = 10.dp,
                            onClick = { /* Interactivity check */ }
                        ) {
                            Text(
                                text = "Elevated Clickable",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tactile tap. Elevation decreases on press.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                CocoCard(
                    style = CocoCardStyle.Outlined,
                    horizontalPadding = 12.dp,
                    verticalPadding = 12.dp
                ) {
                    Text(
                        text = "Outlined Card Layout",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "High-contrast clean outline design utilizing alpha-tinted border lines for perfect visual balance across light and dark themes.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Button Styles Showcase
                Text(
                    text = "Accessible Buttons (CocoButtonStyle)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Interactive Primary and Accent buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CocoButton(
                            text = "Primary ($primaryClickCount)",
                            style = CocoButtonStyle.Primary,
                            onClick = { primaryClickCount++ },
                            fullWidth = true,
                            leadingIcon = Icons.Default.Favorite,
                            testTag = "primary_showcase_button"
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CocoButton(
                            text = "Accent ($accentClickCount)",
                            style = CocoButtonStyle.Accent,
                            onClick = { accentClickCount++ },
                            fullWidth = true,
                            trailingIcon = Icons.Default.Star,
                            testTag = "accent_showcase_button"
                        )
                    }
                }

                // Secondary and Text Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CocoButton(
                            text = "Secondary ($secondaryClickCount)",
                            style = CocoButtonStyle.Secondary,
                            onClick = { secondaryClickCount++ },
                            fullWidth = true,
                            testTag = "secondary_showcase_button"
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CocoButton(
                            text = "Loading Demo",
                            style = CocoButtonStyle.Primary,
                            isLoading = isButtonLoading,
                            onClick = {
                                isButtonLoading = true
                                // Auto reset after 2 seconds
                                scope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    isButtonLoading = false
                                }
                            },
                            fullWidth = true,
                            testTag = "loading_demo_button"
                        )
                    }
                }

                // Disabled State Demo
                CocoButton(
                    text = "Disabled Button State",
                    style = CocoButtonStyle.Secondary,
                    enabled = false,
                    onClick = {},
                    fullWidth = true,
                    testTag = "disabled_showcase_button"
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Features standard 48dp height minimum touch targets, Material ripples, press-scaling, and responsive max-width capping (600dp max on large displays).",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Tap to expand and interactive-test shared custom Buttons & Cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
fun FirebaseCloudSyncPanel(viewModel: EcosystemViewModel) {
    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authErrorMessage.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (firebaseUser != null) {
                Color(0xFFE8F5E9)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (firebaseUser != null) Color(0xFF81C784) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (firebaseUser != null) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                    contentDescription = "Cloud Sync",
                    tint = if (firebaseUser != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = if (firebaseUser != null) "Real-time Firestore Sync Active" else "Firestore Sync Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (firebaseUser != null) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (firebaseUser != null) {
                            "Authenticated as Cloud Partner: ${firebaseUser?.uid?.take(10)}..."
                        } else {
                            authError ?: "Secure identity cloud auth required to synchronize B2C/B2B database"
                        },
                        fontSize = 9.sp,
                        color = if (firebaseUser != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            if (isAuthLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(
                    onClick = {
                        if (firebaseUser != null) {
                            viewModel.handleFirebaseSignOut()
                        } else {
                            viewModel.handleFirebaseSignInAnonymously()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (firebaseUser != null) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (firebaseUser != null) "Sign Out" else "Cloud Sign-In",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiImageLabCard(viewModel: EcosystemViewModel) {
    val prompt by viewModel.imagePrompt.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val useStudioQuality by viewModel.useStudioQuality.collectAsStateWithLifecycle()
    val base64Image by viewModel.generatedImageBase64.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val errorMsg by viewModel.imageGenerationError.collectAsStateWithLifecycle()

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "AI Image Lab",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Brand Image Lab",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Generate custom marketing and social assets with precise ratio control",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))

                    Text(
                        text = "Design social banners, packaging references, or advertisement mockups in any size:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { viewModel.imagePrompt.value = it },
                        placeholder = { Text("e.g. Fresh organic coconut water carton placed on a tropical coastal beach table, sunset lighting, high contrast...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )

                    // Aspect Ratio Selector
                    Text(
                        text = "Select Aspect Ratio:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    val ratios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ratios) { ratio ->
                            val isSelected = ratio == aspectRatio
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedAspectRatio.value = ratio },
                                label = { Text(ratio, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Studio Quality and Model selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Studio Quality Rendering",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (useStudioQuality) "Using gemini-3-pro-image-preview" else "Using gemini-3.1-flash-image-preview",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = useStudioQuality,
                            onCheckedChange = { viewModel.useStudioQuality.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Error Message display
                    errorMsg?.let { msg ->
                        Text(
                            text = "⚠️ $msg",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action Button
                    Button(
                        onClick = { viewModel.generateImage() },
                        enabled = !isGenerating && prompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Brand Artwork")
                        }
                    }

                    // Display Generated Image
                    base64Image?.let { base64 ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generated Artwork Aspect Ratio ($aspectRatio):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Decode base64 to Bitmap
                        val bitmap = remember(base64) {
                            try {
                                val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        bitmap?.let { bmp ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "AI Generated Artwork",
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
