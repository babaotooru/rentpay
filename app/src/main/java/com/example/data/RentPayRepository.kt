package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class RentPayRepository(private val db: RentPayDatabase) {

    // Helper properties for Supabase Client shortcut
    private val key get() = SupabaseClient.apiKey
    private val auth get() = SupabaseClient.authorizationHeader
    private val api get() = SupabaseClient.api

    // Auth Flows
    val activeUser: Flow<User?> = db.userDao.getActiveUserFlow()

    suspend fun findUserByEmail(email: String): User? {
        return db.userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: User) {
        db.userDao.insertUser(user)
        try {
            api.upsertUser(key, auth, user.toSupabase())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loginUser(email: String): Boolean {
        val user = db.userDao.getUserByEmail(email) ?: return false
        val loggedInUser = user.copy(isAuthenticated = true, lastLoginTime = System.currentTimeMillis())
        db.userDao.insertUser(loggedInUser)
        try {
            api.upsertUser(key, auth, loggedInUser.toSupabase())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    suspend fun logoutAll() {
        db.userDao.logoutAll()
    }

    // Tenant Flows
    val allTenants: Flow<List<Tenant>> = db.tenantDao.getAllTenants()

    fun getTenantByIdFlow(id: Long): Flow<Tenant?> {
        return db.tenantDao.getTenantByIdFlow(id)
    }

    suspend fun getTenantById(id: Long): Tenant? {
        return db.tenantDao.getTenantById(id)
    }

    suspend fun insertTenant(tenant: Tenant): Long {
        val newId = db.tenantDao.insertTenant(tenant)
        val finalTenant = tenant.copy(id = newId)
        try {
            api.upsertTenant(key, auth, finalTenant.toSupabase())
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep status pending if failed so we can retry during performSync
            db.tenantDao.updateTenant(finalTenant.copy(syncStatus = "PENDING"))
        }
        return newId
    }

    suspend fun updateTenant(tenant: Tenant) {
        db.tenantDao.updateTenant(tenant)
        try {
            api.upsertTenant(key, auth, tenant.toSupabase())
        } catch (e: Exception) {
            e.printStackTrace()
            db.tenantDao.updateTenant(tenant.copy(syncStatus = "PENDING"))
        }
    }

    suspend fun archiveTenant(id: Long) {
        db.tenantDao.archiveTenant(id)
        val tenant = db.tenantDao.getTenantById(id)
        if (tenant != null) {
            try {
                api.upsertTenant(key, auth, tenant.toSupabase())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Payment Flows
    val allPayments: Flow<List<RentPayment>> = db.paymentDao.getAllPayments()

    fun getPaymentsByTenant(tenantId: Long): Flow<List<RentPayment>> {
        return db.paymentDao.getPaymentsByTenant(tenantId)
    }

    fun getPaymentsByMonth(month: String): Flow<List<RentPayment>> {
        return db.paymentDao.getPaymentsByMonth(month)
    }

    suspend fun insertPayment(payment: RentPayment): Long {
        val newId = db.paymentDao.insertPayment(payment)
        val finalPayment = payment.copy(id = newId)
        try {
            api.upsertPayment(key, auth, finalPayment.toSupabase())
        } catch (e: Exception) {
            e.printStackTrace()
            db.paymentDao.insertPayment(finalPayment.copy(syncStatus = "PENDING"))
        }
        return newId
    }

    suspend fun deletePayment(payment: RentPayment) {
        db.paymentDao.deletePayment(payment)
        try {
            api.deletePayment(key, auth, "eq.${payment.id}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cloud Sync
    private val _syncingState = MutableStateFlow(false)
    val syncingState: StateFlow<Boolean> = _syncingState.asStateFlow()

    suspend fun performSync(): Boolean {
        _syncingState.value = true
        return try {
            // 1. Pull Users, Tenants, Payments from Supabase (REST API)
            val remoteUsers = api.getUsers(key, auth)
            val remoteTenants = api.getTenants(key, auth)
            val remotePayments = api.getPayments(key, auth)

            // 2. Save items into local database
            for (u in remoteUsers) {
                db.userDao.insertUser(u.toRoom())
            }
            for (t in remoteTenants) {
                db.tenantDao.insertTenant(t.toRoom())
            }
            for (p in remotePayments) {
                db.paymentDao.insertPayment(p.toRoom())
            }

            // 3. Push any unsynced local data to Cloud
            val localTenants = db.tenantDao.getAllTenants().first()
            val pendingTenants = localTenants.filter { it.syncStatus == "PENDING" }
            for (t in pendingTenants) {
                api.upsertTenant(key, auth, t.copy(syncStatus = "SYNCED").toSupabase())
                db.tenantDao.updateTenant(t.copy(syncStatus = "SYNCED"))
            }

            val localPayments = db.paymentDao.getAllPayments().first()
            val pendingPayments = localPayments.filter { it.syncStatus == "PENDING" }
            for (p in pendingPayments) {
                api.upsertPayment(key, auth, p.copy(syncStatus = "SYNCED").toSupabase())
                db.paymentDao.insertPayment(p.copy(syncStatus = "SYNCED"))
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _syncingState.value = false
        }
    }
}
