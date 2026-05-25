package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RentPayViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RentPayDatabase.getDatabase(application)
    val repository = RentPayRepository(db)

    // Current Property Selector (Multi-property support)
    private val _selectedProperty = MutableStateFlow("All Properties")
    val selectedProperty: StateFlow<String> = _selectedProperty.asStateFlow()

    val propertiesList = listOf("All Properties", "Premium PG Boys", "Emerald Heights Girls", "Silicon Oasis Hostel")

    fun selectProperty(propertyName: String) {
        _selectedProperty.value = propertyName
    }

    // Active screen / Navigation Support (Custom simple route states to control app flow beautifully)
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Splash)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Auth States
    val activeUser: StateFlow<User?> = repository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Tenant lists and filter
    val tenants: StateFlow<List<Tenant>> = combine(repository.allTenants, _selectedProperty) { list, prop ->
        if (prop == "All Properties") {
            list
        } else {
            list.filter { it.notes.contains("[$prop]", ignoreCase = true) || it.roomNumber.startsWith(prop.take(3)) }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payment raw list
    val payments: StateFlow<List<RentPayment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Selector for rent collection (e.g. "May 2026")
    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Available months list for selection
    val availableMonths = listOf("May 2026", "April 2026", "March 2026", "February 2026", "January 2026")

    // Search and filters for Payment History
    private val _paymentSearchQuery = MutableStateFlow("")
    val paymentSearchQuery: StateFlow<String> = _paymentSearchQuery.asStateFlow()

    private val _paymentModeFilter = MutableStateFlow("All")
    val paymentModeFilter: StateFlow<String> = _paymentModeFilter.asStateFlow()

    init {
        // Set default month to May 2026 (matching our mock system timeline)
        _selectedMonth.value = "May 2026"
        
        // Setup a mock default account and some sample tenants if the app is empty, so it looks gorgeous on first launch
        viewModelScope.launch {
            try {
                // Check if any user exists, otherwise create a mock user
                val existing = repository.findUserByEmail("owner@rentpay.com")
                if (existing == null) {
                    repository.registerUser(
                        User(
                            email = "owner@rentpay.com",
                            name = "Rajesh Kumar",
                            passwordHash = "password123",
                            isAuthenticated = false
                        )
                    )
                }
                // Auto login for test/dev environment convenience if none is authenticated
                val currentActive = db.userDao.getActiveUser()
                if (currentActive == null) {
                    // Auto login as default to avoid forcing sign in unless they log out
                    repository.loginUser("owner@rentpay.com")
                    _currentScreen.value = AppScreen.Dashboard
                } else {
                    _currentScreen.value = AppScreen.Dashboard
                }

                // Populate mock tenants if empty
                repository.allTenants.first().let { currentTenants ->
                    if (currentTenants.isEmpty()) {
                        createMockData()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Gracefully fallback to Dashboard in case of any database/network startup issues
                _currentScreen.value = AppScreen.Dashboard
            }
        }
    }

    fun searchPayments(query: String) {
        _paymentSearchQuery.value = query
    }

    fun filterPaymentMode(mode: String) {
        _paymentModeFilter.value = mode
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    // Tenant detail context state
    private val _selectedTenantId = MutableStateFlow<Long?>(null)
    val selectedTenantId: StateFlow<Long?> = _selectedTenantId.asStateFlow()

    val tenantDetail: StateFlow<Tenant?> = _selectedTenantId
        .flatMapLatest { id ->
            if (id != null) repository.getTenantByIdFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tenantPayments: StateFlow<List<RentPayment>> = _selectedTenantId
        .flatMapLatest { id ->
            if (id != null) repository.getPaymentsByTenant(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTenant(id: Long) {
        _selectedTenantId.value = id
        navigateTo(AppScreen.TenantDetail)
    }

    // Calculations & Statistics
    // expected amount this month based on active tenants in selected property
    val monthlyStatsState: StateFlow<MonthlyStats> = combine(
        tenants,
        payments,
        _selectedMonth
    ) { activeTenants, allPayments, selectedMonth ->
        val monthPayments = allPayments.filter { it.month == selectedMonth }
        val totalExpected = activeTenants.sumOf { it.monthlyRent }
        val collected = monthPayments.sumOf { it.amountPaid }
        val pending = (totalExpected - collected).coerceAtLeast(0.0)

        val paidTenantIds = monthPayments.map { it.tenantId }.toSet()
        val paidTenantsCount = activeTenants.count { paidTenantIds.contains(it.id) }
        val pendingTenantsCount = activeTenants.size - paidTenantsCount

        MonthlyStats(
            totalTenants = activeTenants.size,
            paidTenants = paidTenantsCount,
            pendingTenants = pendingTenantsCount,
            totalCollected = collected,
            pendingAmount = pending,
            expectedAmount = totalExpected
        )
    }
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyStats(0, 0, 0, 0.0, 0.0, 0.0)
    )

    // Authenticate Actions
    fun handleLogin(email: String, pwhash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.findUserByEmail(email)
            if (user != null && user.passwordHash == pwhash) {
                _authError.value = null
                repository.loginUser(email)
                onSuccess()
                navigateTo(AppScreen.Dashboard)
            } else {
                _authError.value = "Invalid email or password."
            }
        }
    }

    fun handleSignup(name: String, email: String, pwhash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || name.isBlank() || pwhash.isBlank()) {
                _authError.value = "All fields are required."
                return@launch
            }
            val existing = repository.findUserByEmail(email)
            if (existing != null) {
                _authError.value = "User already exists with this email."
                return@launch
            }
            repository.registerUser(User(email, name, pwhash, isAuthenticated = false))
            _authError.value = null
            repository.loginUser(email)
            onSuccess()
            navigateTo(AppScreen.Dashboard)
        }
    }

    fun handleLogout() {
        viewModelScope.launch {
            repository.logoutAll()
            _selectedTenantId.value = null
            navigateTo(AppScreen.Login)
        }
    }

    // CRUD Tenant Actions
    fun saveTenant(
        name: String, phone: String, gender: String, idProofType: String, idProofNumber: String,
        photoPath: String?, roomNumber: String, bedNumber: String, sharingType: String,
        monthlyRent: Double, depositAmount: Double, moveInDate: String, rentDueDate: Int, notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // Append property signature inside note to allow support for multi-property filtration nicely
            val enrichedNotes = if (_selectedProperty.value != "All Properties") {
                "$notes [${_selectedProperty.value}]"
            } else {
                notes
            }

            val tenant = Tenant(
                name = name, phone = phone, gender = gender, idProofType = idProofType,
                idProofNumber = idProofNumber, photoPath = photoPath, roomNumber = roomNumber,
                bedNumber = bedNumber, sharingType = sharingType, monthlyRent = monthlyRent,
                depositAmount = depositAmount, moveInDate = moveInDate, rentDueDate = rentDueDate,
                notes = enrichedNotes
            )
            repository.insertTenant(tenant)
            repository.performSync()
            onSuccess()
        }
    }

    fun updateTenantDetails(
        id: Long, name: String, phone: String, gender: String, idProofType: String, idProofNumber: String,
        photoPath: String?, roomNumber: String, bedNumber: String, sharingType: String,
        monthlyRent: Double, depositAmount: Double, moveInDate: String, rentDueDate: Int, notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getTenantById(id) ?: return@launch
            val updated = existing.copy(
                name = name, phone = phone, gender = gender, idProofType = idProofType,
                idProofNumber = idProofNumber, photoPath = photoPath, roomNumber = roomNumber,
                bedNumber = bedNumber, sharingType = sharingType, monthlyRent = monthlyRent,
                depositAmount = depositAmount, moveInDate = moveInDate, rentDueDate = rentDueDate,
                notes = notes
            )
            repository.updateTenant(updated)
            repository.performSync()
            onSuccess()
        }
    }

    fun removeTenant(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(id)
            if (tenant != null) {
                // Delete payment mappings too for perfect DB clean up
                repository.archiveTenant(id)
                _selectedTenantId.value = null
                repository.performSync()
                onSuccess()
            }
        }
    }

    // Register Payments
    fun recordRentPaid(tenantId: Long, month: String, amount: Double, mode: String, note: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId) ?: return@launch
            val payment = RentPayment(
                tenantId = tenantId,
                tenantName = tenant.name,
                roomNumber = tenant.roomNumber,
                amountPaid = amount,
                paymentMode = mode,
                month = month,
                paymentDate = System.currentTimeMillis(),
                notes = note
            )
            repository.insertPayment(payment)
            repository.performSync()
            onSuccess()
        }
    }

    fun deleteRentRecord(payment: RentPayment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.performSync()
        }
    }

    // Create Initial Sample Data for the PG owner
    private suspend fun createMockData() {
        val mockTenants = listOf(
            Tenant(
                name = "Aman Verma", phone = "9876543210", gender = "Male", idProofType = "Aadhaar",
                idProofNumber = "3456-7890-1234", photoPath = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=120",
                roomNumber = "101", bedNumber = "A", sharingType = "2-Sharing", monthlyRent = 8500.0,
                depositAmount = 15000.0, moveInDate = "2026-01-10", rentDueDate = 5, notes = "Works at TCS, quiet guy"
            ),
            Tenant(
                name = "Priya Sharma", phone = "9123456789", gender = "Female", idProofType = "Aadhaar",
                idProofNumber = "8765-4321-0987", photoPath = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=120",
                roomNumber = "204", bedNumber = "B", sharingType = "Single", monthlyRent = 12000.0,
                depositAmount = 24000.0, moveInDate = "2025-11-15", rentDueDate = 7, notes = "Medical PostGrad student [Emerald Heights Girls]"
            ),
            Tenant(
                name = "Rahul Nair", phone = "8899001122", gender = "Male", idProofType = "PAN Card",
                idProofNumber = "ABCDE1234F", photoPath = null,
                roomNumber = "102", bedNumber = "C", sharingType = "3-Sharing", monthlyRent = 6000.0,
                depositAmount = 10000.0, moveInDate = "2026-03-01", rentDueDate = 10, notes = "Intern at TechMahindra [Premium PG Boys]"
            ),
            Tenant(
                name = "Sneha Reddy", phone = "7766554433", gender = "Female", idProofType = "Passport",
                idProofNumber = "Z9876543", photoPath = null,
                roomNumber = "302", bedNumber = "A", sharingType = "2-Sharing", monthlyRent = 9500.0,
                depositAmount = 18000.0, moveInDate = "2026-02-05", rentDueDate = 5, notes = "UX Designer [Emerald Heights Girls]"
            )
        )

        for (m in mockTenants) {
            val tenantId = repository.insertTenant(m)
            // Prepopulate some payments for March & April 2026 so history is beautiful
            repository.insertPayment(
                RentPayment(
                    tenantId = tenantId,
                    tenantName = m.name,
                    roomNumber = m.roomNumber,
                    amountPaid = m.monthlyRent,
                    paymentMode = "UPI",
                    month = "April 2026",
                    paymentDate = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                )
            )
            if (m.name != "Rahul Nair") {
                // Rahul Nair keeps pending list realistic for current month (May 2026)
                repository.insertPayment(
                    RentPayment(
                        tenantId = tenantId,
                        tenantName = m.name,
                        roomNumber = m.roomNumber,
                        amountPaid = m.monthlyRent,
                        paymentMode = if (m.idProofType == "Passport") "Bank Transfer" else "UPI",
                        month = "May 2026",
                        paymentDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

sealed class AppScreen {
    object Splash : AppScreen()
    object Login : AppScreen()
    object Signup : AppScreen()
    object Dashboard : AppScreen()
    object AddTenant : AppScreen()
    object TenantDetail : AppScreen()
    object RentCollection : AppScreen()
    object PaymentHistory : AppScreen()
}

data class MonthlyStats(
    val totalTenants: Int,
    val paidTenants: Int,
    val pendingTenants: Int,
    val totalCollected: Double,
    val pendingAmount: Double,
    val expectedAmount: Double
)
