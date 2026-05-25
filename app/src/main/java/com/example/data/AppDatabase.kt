package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET isAuthenticated = 0")
    suspend fun logoutAll()

    @Query("SELECT * FROM users WHERE isAuthenticated = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE isAuthenticated = 1 LIMIT 1")
    suspend fun getActiveUser(): User?
}

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllTenants(): Flow<List<Tenant>>

    @Query("SELECT * FROM tenants WHERE id = :id LIMIT 1")
    fun getTenantByIdFlow(id: Long): Flow<Tenant?>

    @Query("SELECT * FROM tenants WHERE id = :id LIMIT 1")
    suspend fun getTenantById(id: Long): Tenant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant): Long

    @Update
    suspend fun updateTenant(tenant: Tenant)

    @Query("UPDATE tenants SET isArchived = 1 WHERE id = :id")
    suspend fun archiveTenant(id: Long)

    @Delete
    suspend fun deleteTenant(tenant: Tenant)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM rent_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<RentPayment>>

    @Query("SELECT * FROM rent_payments WHERE tenantId = :tenantId ORDER BY paymentDate DESC")
    fun getPaymentsByTenant(tenantId: Long): Flow<List<RentPayment>>

    @Query("SELECT * FROM rent_payments WHERE month = :month")
    fun getPaymentsByMonth(month: String): Flow<List<RentPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: RentPayment): Long

    @Delete
    suspend fun deletePayment(payment: RentPayment)
}

@Database(entities = [User::class, Tenant::class, RentPayment::class], version = 2, exportSchema = false)
abstract class RentPayDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val tenantDao: TenantDao
    abstract val paymentDao: PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: RentPayDatabase? = null

        fun getDatabase(context: Context): RentPayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RentPayDatabase::class.java,
                    "rentpay_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
