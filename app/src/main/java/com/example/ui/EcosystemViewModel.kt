package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Persona { CUSTOMER, DISTRIBUTOR, RETAILER, ADMIN }

data class ChatMessage(
    val sender: String,
    val text: String,
    val isBot: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class Supplier(
    val id: String,
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val region: String,
    val suppliedMaterials: List<String>,
    val rating: Float,
    val leadTimeDays: Int,
    val isActive: Boolean = true
)

data class RawMaterialShipment(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val materialName: String,
    val qty: Int,
    val unit: String,
    val cost: Double,
    val status: String, // "Ordered", "In Transit", "Received"
    val date: String
)

class EcosystemViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize Database & Repository inside the ViewModel safely
    private val database = Room.databaseBuilder(
        application.applicationContext,
        EcosystemDatabase::class.java,
        "cocoaura_ecosystem.db"
    ).build()
    
    private val dao = database.ecosystemDao()
    val repository = EcosystemRepository(dao)

    // Current Persona
    val currentPersona = MutableStateFlow(Persona.CUSTOMER)

    // Repository Flows
    val products = repository.productsFlow
    val orders = repository.ordersFlow
    val inventory = repository.inventoryFlow
    val distributors = repository.distributorsFlow
    val qrCodes = repository.qrCodesFlow
    val crmTickets = repository.crmTicketsFlow
    val loyaltyTransactions = repository.loyaltyTransactionsFlow

    // --- Mock Database Supplier & Raw Material State ---
    val suppliers = MutableStateFlow<List<Supplier>>(
        listOf(
            Supplier("SUP-101", "Malabar Organic Coops", "Madhavan Nair", "madhavan@malabarorganic.coop", "+91 94471 23456", "Kerala Coast", listOf("Tender Coconut Water", "Virgin Copra"), 4.8f, 2),
            Supplier("SUP-102", "Aura Biodegradables Ltd", "Sunitha Rao", "contact@aurabiodeg.com", "+91 80234 56789", "Bangalore Industrial Area", listOf("Biodegradable Bottles", "Bamboo Straws"), 4.5f, 4),
            Supplier("SUP-103", "Coastal Sweeteners", "Joseph Kurian", "sales@coastalsweeteners.in", "+91 48425 11223", "Kochi Outer Ring", listOf("Organic Coconut Sugar", "Nectar Extract"), 4.2f, 3),
            Supplier("SUP-104", "Deccan Eco-Packaging", "Vikram Shah", "vikram@deccaneco.com", "+91 22285 77665", "Pune Central", listOf("Recycled Shipping Cartons", "Paper Labels"), 4.7f, 5)
        )
    )

    val rawMaterialShipments = MutableStateFlow<List<RawMaterialShipment>>(
        listOf(
            RawMaterialShipment("SHP-5001", "SUP-101", "Malabar Organic Coops", "Tender Coconut Water", 1200, "Liters", 36000.0, "Received", "2 days ago"),
            RawMaterialShipment("SHP-5002", "SUP-102", "Aura Biodegradables Ltd", "Biodegradable Bottles", 5000, "Units", 40000.0, "In Transit", "ETA: Tomorrow"),
            RawMaterialShipment("SHP-5003", "SUP-103", "Coastal Sweeteners", "Organic Coconut Sugar", 150, "kg", 18000.0, "Ordered", "ETA: 3 days")
        )
    )

    // Search and Filter States (Customer Catalog)
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    // Filtered Products Flow
    val filteredProducts: Flow<List<ProductEntity>> = combine(
        products, searchQuery, selectedCategory
    ) { prodList, query, cat ->
        prodList.filter { prod ->
            val matchesQuery = prod.name.contains(query, ignoreCase = true) || 
                               prod.sku.contains(query, ignoreCase = true)
            val matchesCategory = cat == "All" || prod.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    // Shopping Carts
    // D2C Customer Cart
    val customerCart = MutableStateFlow<Map<String, Int>>(emptyMap()) // sku -> qty
    // B2B Cart (used by Distributor and Retailer)
    val b2bCart = MutableStateFlow<Map<String, Int>>(emptyMap()) // sku -> qty

    // Selected B2B Partner perspective (for simulation)
    val activeDistributorId = MutableStateFlow<Int>(1) // Apex Distributors Ltd by default
    val activeRetailerName = MutableStateFlow("Pooja Kirana Store (Bangalore)")

    // B2B Payment Mode selection
    val b2bPaymentMode = MutableStateFlow("On Credit") // "On Credit" or "Online"

    // QR Code Verification State
    val qrScanCodeInput = MutableStateFlow("")
    val qrScanResult = MutableStateFlow<QREntity?>(null)
    val qrScanMessage = MutableStateFlow<String?>(null)

    // Support Ticket Inputs
    val ticketSubject = MutableStateFlow("")
    val ticketNotes = MutableStateFlow("")
    val isSubmittingTicket = MutableStateFlow(false)
    val ticketSuccessMessage = MutableStateFlow<String?>(null)

    // Chat Assistant State
    val chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "Aura-Bot",
                text = "Aloha! 🌴 I am Aura-Bot, your virtual CocoAura wellness companion. Ask me anything about our 100% natural coconut drinks, pure batch QR codes, healthy cooking oil, or B2B accounts. How can I brighten your aura today?",
                isBot = true
            )
        )
    )
    val chatInputText = MutableStateFlow("")
    val isChatLoading = MutableStateFlow(false)

    // --- Gemini Chat Settings ---
    val useDeepThinking = MutableStateFlow(false)
    val useWebSearch = MutableStateFlow(false)

    // --- Audio Transcription State ---
    val isTranscribing = MutableStateFlow(false)

    fun transcribeAudioInput(base64Data: String, mimeType: String) {
        isTranscribing.value = true
        viewModelScope.launch {
            val transcription = GeminiService.transcribeAudio(base64Data, mimeType)
            if (!transcription.isNullOrBlank()) {
                chatInputText.value = transcription
            }
            isTranscribing.value = false
        }
    }

    // --- AI Image Lab States ---
    val imagePrompt = MutableStateFlow("")
    val selectedAspectRatio = MutableStateFlow("1:1")
    val useStudioQuality = MutableStateFlow(false)
    val generatedImageBase64 = MutableStateFlow<String?>(null)
    val isGeneratingImage = MutableStateFlow(false)
    val imageGenerationError = MutableStateFlow<String?>(null)

    fun generateImage() {
        val prompt = imagePrompt.value.trim()
        if (prompt.isEmpty()) {
            imageGenerationError.value = "Prompt cannot be empty"
            return
        }

        isGeneratingImage.value = true
        imageGenerationError.value = null
        generatedImageBase64.value = null

        viewModelScope.launch {
            val base64 = GeminiService.generateImageWithGemini(
                prompt = prompt,
                aspectRatio = selectedAspectRatio.value,
                useStudioQuality = useStudioQuality.value
            )
            if (base64 != null) {
                generatedImageBase64.value = base64
            } else {
                imageGenerationError.value = "Failed to generate image. Please check API key/connection."
            }
            isGeneratingImage.value = false
        }
    }

    // --- Firebase Auth & Firestore Integration ---
    val firebaseUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    val isAuthLoading = MutableStateFlow(false)
    val authErrorMessage = MutableStateFlow<String?>(null)

    init {
        var isFirebaseInitialized = false
        try {
            val defaultOptions = com.google.firebase.FirebaseOptions.fromResource(application)
            if (defaultOptions != null) {
                com.google.firebase.FirebaseApp.initializeApp(application, defaultOptions)
                isFirebaseInitialized = true
                android.util.Log.i("FirebaseInit", "Firebase initialized successfully with default options.")
            } else {
                android.util.Log.i("FirebaseInit", "google-services.json not found. Initializing with fallback options.")
            }
        } catch (e: Exception) {
            android.util.Log.i("FirebaseInit", "Default initialization skipped: ${e.message}")
        }

        if (!isFirebaseInitialized) {
            try {
                if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
                    val fallbackOptions = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:1234567890")
                        .setApiKey("AIzaSyDummyKeyForInitialization")
                        .setProjectId("dummy-project-id")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(application, fallbackOptions)
                    isFirebaseInitialized = true
                    android.util.Log.i("FirebaseInit", "Firebase initialized with fallback dummy options.")
                } else {
                    isFirebaseInitialized = true
                }
            } catch (e: Exception) {
                android.util.Log.i("FirebaseInit", "Fallback initialization skipped: ${e.message}")
            }
        }

        try {
            if (isFirebaseInitialized) {
                firebaseUser.value = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    firebaseUser.value = auth.currentUser
                }
            } else {
                authErrorMessage.value = "Firebase services are offline."
            }
        } catch (e: Exception) {
            android.util.Log.i("FirebaseInit", "Failed to setup FirebaseAuth listeners: ${e.message}")
            authErrorMessage.value = "Firebase setup offline: ${e.localizedMessage}"
        }

        // Seed Database asynchronously on first-run
        viewModelScope.launch {
            repository.seedInitialDataIfNecessary()
        }
    }

    fun syncDataToFirestore(collection: String, docId: String, data: Map<String, Any>) {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection(collection)
                .document(docId)

            docRef.set(data)
                .addOnSuccessListener { android.util.Log.d("FirestoreSync", "Successfully synced $collection/$docId") }
                .addOnFailureListener { e -> android.util.Log.e("FirestoreSync", "Error syncing data", e) }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSync", "Exception in syncDataToFirestore", e)
        }
    }

    fun handleFirebaseSignOut() {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            firebaseUser.value = null
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuth", "Exception in handleFirebaseSignOut", e)
        }
    }

    fun handleFirebaseSignInAnonymously() {
        isAuthLoading.value = true
        authErrorMessage.value = null
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener { task ->
                    isAuthLoading.value = false
                    if (task.isSuccessful) {
                        firebaseUser.value = task.result?.user
                    } else {
                        authErrorMessage.value = task.exception?.message ?: "Sign in failed"
                    }
                }
        } catch (e: Exception) {
            isAuthLoading.value = false
            authErrorMessage.value = "Auth service unavailable: ${e.localizedMessage}"
            android.util.Log.e("FirebaseAuth", "Exception in handleFirebaseSignInAnonymously", e)
        }
    }

    // --- Tab Switching ---
    fun switchPersona(persona: Persona) {
        currentPersona.value = persona
    }

    // --- Customer Cart Management ---
    fun addToCustomerCart(sku: String) {
        val current = customerCart.value.toMutableMap()
        current[sku] = (current[sku] ?: 0) + 1
        customerCart.value = current
    }

    fun removeFromCustomerCart(sku: String) {
        val current = customerCart.value.toMutableMap()
        val qty = current[sku] ?: 0
        if (qty > 1) {
            current[sku] = qty - 1
        } else {
            current.remove(sku)
        }
        customerCart.value = current
    }

    fun clearCustomerCart() {
        customerCart.value = emptyMap()
    }

    // --- B2B Cart Management ---
    fun addToB2BCart(sku: String) {
        val current = b2bCart.value.toMutableMap()
        current[sku] = (current[sku] ?: 0) + 50 // Bulk packing (increment by 50)
        b2bCart.value = current
    }

    fun removeFromB2BCart(sku: String) {
        val current = b2bCart.value.toMutableMap()
        val qty = current[sku] ?: 0
        if (qty > 50) {
            current[sku] = qty - 50
        } else {
            current.remove(sku)
        }
        b2bCart.value = current
    }

    fun clearB2BCart() {
        b2bCart.value = emptyMap()
    }

    // --- Order Checkout Trigger ---
    fun checkoutCustomer(userName: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val cartItems = customerCart.value
            if (cartItems.isEmpty()) return@launch

            val allProducts = dao.getAllProducts()
            val itemsToBuy = mutableListOf<Pair<ProductEntity, Int>>()
            
            for ((sku, qty) in cartItems) {
                val prod = allProducts.find { it.sku == sku }
                if (prod != null) {
                    itemsToBuy.add(Pair(prod, qty))
                }
            }

            val result = repository.placeOrder(
                buyerType = "D2C_Customer",
                buyerName = userName,
                items = itemsToBuy,
                paymentMethod = "Online"
            )

            if (result.isSuccess) {
                clearCustomerCart()
                onComplete("Order Placed Successfully! Earned loyalty points tracked in ledger.")
            } else {
                onComplete("Checkout Failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun checkoutB2B(buyerType: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val cartItems = b2bCart.value
            if (cartItems.isEmpty()) return@launch

            val allProducts = dao.getAllProducts()
            val itemsToBuy = mutableListOf<Pair<ProductEntity, Int>>()
            
            for ((sku, qty) in cartItems) {
                val prod = allProducts.find { it.sku == sku }
                if (prod != null) {
                    itemsToBuy.add(Pair(prod, qty))
                }
            }

            // Find current partner name for audit trail
            val buyerName = if (buyerType == "Distributor") {
                val dists = dao.getAllDistributorsFlow().firstOrNull() ?: emptyList()
                val active = dists.find { it.id == activeDistributorId.value }
                active?.name ?: "Apex Distributors Ltd"
            } else {
                activeRetailerName.value
            }

            val result = repository.placeOrder(
                buyerType = buyerType,
                buyerName = buyerName,
                items = itemsToBuy,
                paymentMethod = b2bPaymentMode.value
            )

            if (result.isSuccess) {
                // If distributor placed on credit, increase credit usage
                if (buyerType == "Distributor" && b2bPaymentMode.value == "On Credit") {
                    val orderId = result.getOrNull() ?: 0
                    val details = dao.getOrderItems(orderId)
                    val cost = details.sumOf { it.qty * it.unitPrice }
                    
                    val dists = dao.getAllDistributorsFlow().firstOrNull() ?: emptyList()
                    val active = dists.find { it.id == activeDistributorId.value }
                    if (active != null) {
                        dao.updateDistributorCreditUsed(active.id, active.creditUsed + cost)
                    }
                }

                clearB2BCart()
                onComplete("B2B Purchase Order submitted successfully. Multi-warehouse stocks updated.")
            } else {
                onComplete("B2B Order Failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // --- QR Code Authentication ---
    fun scanQRCode(userName: String) {
        val code = qrScanCodeInput.value.trim()
        if (code.isEmpty()) return

        viewModelScope.launch {
            val result = repository.verifyAndScanQR(code, userName)
            if (result != null) {
                qrScanResult.value = result
                if (result.scanCount > 1) {
                    qrScanMessage.value = "⚠️ Warning: This bottle's QR code has been scanned ${result.scanCount} times! If you just purchased this bottle, please contact customer support immediately for counterfeit audit."
                } else {
                    qrScanMessage.value = "✅ Pure Aura Certified! This is a genuine CocoAura batch ${result.batchNo}. You have earned ${result.pointsReward} loyalty points!"
                }
            } else {
                qrScanResult.value = null
                qrScanMessage.value = "❌ Authentication Failed: This QR code is invalid and does not match any registered product batches in the CocoAura ledger."
            }
        }
    }

    fun clearQRResult() {
        qrScanResult.value = null
        qrScanMessage.value = null
        qrScanCodeInput.value = ""
    }

    // --- CRM Ticket Submission ---
    fun submitSupportTicket(buyerName: String, buyerType: String) {
        val subject = ticketSubject.value.trim()
        val notes = ticketNotes.value.trim()
        if (subject.isEmpty() || notes.isEmpty()) return

        viewModelScope.launch {
            isSubmittingTicket.value = true
            val ticket = CRMTicketEntity(
                buyerName = buyerName,
                buyerType = buyerType,
                subject = subject,
                notes = notes,
                status = "Open"
            )
            repository.submitCRMTicket(ticket)
            ticketSubject.value = ""
            ticketNotes.value = ""
            isSubmittingTicket.value = false
            ticketSuccessMessage.value = "Support ticket logged successfully! Live status updated."
        }
    }

    fun clearTicketSuccess() {
        ticketSuccessMessage.value = null
    }

    // --- Admin Operations ---
    fun resolveSupportTicket(ticketId: Int) {
        viewModelScope.launch {
            repository.resolveTicket(ticketId, "Resolved by Admin panel")
        }
    }

    fun approveDistributorKyc(distributorId: Int) {
        viewModelScope.launch {
            repository.updateDistributorKyc(distributorId, "Approved")
        }
    }

    fun rejectDistributorKyc(distributorId: Int) {
        viewModelScope.launch {
            repository.updateDistributorKyc(distributorId, "Rejected")
        }
    }

    // --- Gemini Chat Assistant ---
    fun sendMessageToAura() {
        val text = chatInputText.value.trim()
        if (text.isEmpty()) return

        val userMsg = ChatMessage(sender = "You", text = text, isBot = false)
        val currentMsgs = chatMessages.value.toMutableList()
        currentMsgs.add(userMsg)
        chatMessages.value = currentMsgs
        chatInputText.value = ""
        isChatLoading.value = true

        viewModelScope.launch {
            val replyText = GeminiService.chatWithAura(
                userMessage = text,
                useThinking = useDeepThinking.value,
                useSearch = useWebSearch.value
            )
            val botMsg = ChatMessage(sender = "Aura-Bot", text = replyText, isBot = true)
            val updatedMsgs = chatMessages.value.toMutableList()
            updatedMsgs.add(botMsg)
            chatMessages.value = updatedMsgs
            isChatLoading.value = false
        }
    }

    // --- AI Supply Chain Predictions ---
    val isAnalyzingSupplyChain = MutableStateFlow(false)
    val supplyChainAnalysisResult = MutableStateFlow<String?>(null)

    fun generateSupplyChainPredictions() {
        isAnalyzingSupplyChain.value = true
        supplyChainAnalysisResult.value = null
        viewModelScope.launch {
            try {
                // 1. Gather current inventory from flow
                val invList = database.ecosystemDao().getAllInventoryFlow().first()
                val inventorySummary = invList.joinToString("\n") { 
                    "${it.productSku} - ${it.warehouseName}: ${it.qty} units"
                }

                // 2. Gather suppliers
                val supList = suppliers.value
                val suppliersSummary = supList.joinToString("\n") {
                    "${it.id} (${it.name}) in region ${it.region} supplies ${it.suppliedMaterials.joinToString(", ")} with lead time ${it.leadTimeDays} days"
                }

                // 3. Gather shipments
                val shpList = rawMaterialShipments.value
                val shipmentsSummary = shpList.joinToString("\n") {
                    "${it.id}: ${it.materialName} from ${it.supplierName} - Qty: ${it.qty} ${it.unit} (Status: ${it.status}, Date/ETA: ${it.date})"
                }

                // 4. Gather order items
                val orderItems = database.ecosystemDao().getAllOrderItems()
                val ordersSummary = orderItems.joinToString("\n") {
                    "Product: ${it.productSku} (${it.productName}) - Qty sold: ${it.qty}"
                }

                // 5. Run prediction via Gemini Service
                val resultReport = GeminiService.analyzeSupplyChain(
                    inventorySummary = inventorySummary,
                    ordersSummary = ordersSummary.ifBlank { "No orders logged yet." },
                    suppliersSummary = suppliersSummary,
                    shipmentsSummary = shipmentsSummary.ifBlank { "No active shipments logged." }
                )
                supplyChainAnalysisResult.value = resultReport
            } catch (e: Exception) {
                android.util.Log.e("EcosystemViewModel", "Error analyzing supply chain", e)
                supplyChainAnalysisResult.value = "Failed to run AI analysis: ${e.localizedMessage}"
            } finally {
                isAnalyzingSupplyChain.value = false
            }
        }
    }

    // --- ERP & Supplier Operations ---
    fun updateInventoryLevel(inventoryId: Int, newQty: Int) {
        viewModelScope.launch {
            database.ecosystemDao().updateInventoryQty(inventoryId, newQty)
        }
    }

    fun addSupplier(supplier: Supplier) {
        val updated = suppliers.value.toMutableList()
        updated.add(supplier)
        suppliers.value = updated
    }

    fun orderRawMaterial(shipment: RawMaterialShipment) {
        val updated = rawMaterialShipments.value.toMutableList()
        updated.add(0, shipment)
        rawMaterialShipments.value = updated
    }

    fun receiveRawMaterialShipment(shipmentId: String) {
        val updated = rawMaterialShipments.value.toMutableList()
        val index = updated.indexOfFirst { it.id == shipmentId }
        if (index != -1) {
            val oldShipment = updated[index]
            if (oldShipment.status != "Received") {
                updated[index] = oldShipment.copy(status = "Received")
                rawMaterialShipments.value = updated
                
                // Automatically restock matching product inventory in the database to show real integration!
                viewModelScope.launch {
                    val matchedSku = when (oldShipment.materialName) {
                        "Tender Coconut Water" -> "COCO-WATER-01"
                        "Virgin Copra" -> "COCO-OIL-02"
                        "Organic Coconut Sugar" -> "COCO-CHIPS-03"
                        "Dark Chocolate Glaze" -> "COCO-BITES-04"
                        "Fresh Coconut Milk" -> "COCO-MILK-05"
                        else -> null
                    }
                    if (matchedSku != null) {
                        val currentInvList = database.ecosystemDao().getAllInventoryFlow().first()
                        val record = currentInvList.find { 
                            it.productSku == matchedSku && it.warehouseName.contains("Kochi") 
                        }
                        if (record != null) {
                            database.ecosystemDao().updateInventoryQty(record.id, record.qty + oldShipment.qty)
                        }
                    }
                }
            }
        }
    }
}
