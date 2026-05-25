package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String,
    @Json(name = "password_hash") val passwordHash: String,
    @Json(name = "is_authenticated") val isAuthenticated: Boolean? = false,
    @Json(name = "last_login_time") val lastLoginTime: Long? = 0L
)

@JsonClass(generateAdapter = true)
data class SupabaseTenant(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "gender") val gender: String?,
    @Json(name = "id_proof_type") val idProofType: String?,
    @Json(name = "id_proof_number") val idProofNumber: String?,
    @Json(name = "photo_path") val photoPath: String? = null,
    @Json(name = "room_number") val roomNumber: String,
    @Json(name = "bed_number") val bedNumber: String,
    @Json(name = "sharing_type") val sharingType: String?,
    @Json(name = "monthly_rent") val monthlyRent: Double?,
    @Json(name = "deposit_amount") val depositAmount: Double?,
    @Json(name = "move_in_date") val moveInDate: String,
    @Json(name = "rent_due_date") val rentDueDate: Int?,
    @Json(name = "notes") val notes: String? = "",
    @Json(name = "is_archived") val isArchived: Boolean? = false,
    @Json(name = "sync_status") val syncStatus: String? = "SYNCED"
)

@JsonClass(generateAdapter = true)
data class SupabasePayment(
    @Json(name = "id") val id: Long,
    @Json(name = "tenant_id") val tenantId: Long,
    @Json(name = "tenant_name") val tenantName: String,
    @Json(name = "room_number") val roomNumber: String,
    @Json(name = "amount_paid") val amountPaid: Double?,
    @Json(name = "payment_mode") val paymentMode: String?,
    @Json(name = "month") val month: String,
    @Json(name = "payment_date") val paymentDate: Long?,
    @Json(name = "notes") val notes: String? = "",
    @Json(name = "sync_status") val syncStatus: String? = "SYNCED"
)

// Extension conversions between Room <-> Supabase DTO models
fun User.toSupabase() = SupabaseUser(
    email = email,
    name = name,
    passwordHash = passwordHash,
    isAuthenticated = isAuthenticated,
    lastLoginTime = lastLoginTime
)

fun SupabaseUser.toRoom() = User(
    email = email,
    name = name,
    passwordHash = passwordHash,
    isAuthenticated = isAuthenticated ?: false,
    lastLoginTime = lastLoginTime ?: System.currentTimeMillis()
)

fun Tenant.toSupabase() = SupabaseTenant(
    id = id,
    name = name,
    phone = phone,
    gender = gender,
    idProofType = idProofType,
    idProofNumber = idProofNumber,
    photoPath = photoPath,
    roomNumber = roomNumber,
    bedNumber = bedNumber,
    sharingType = sharingType,
    monthlyRent = monthlyRent,
    depositAmount = depositAmount,
    moveInDate = moveInDate,
    rentDueDate = rentDueDate,
    notes = notes,
    isArchived = isArchived,
    syncStatus = syncStatus
)

fun SupabaseTenant.toRoom() = Tenant(
    id = id,
    name = name,
    phone = phone,
    gender = gender ?: "Male",
    idProofType = idProofType ?: "Aadhaar",
    idProofNumber = idProofNumber ?: "",
    photoPath = photoPath,
    roomNumber = roomNumber,
    bedNumber = bedNumber,
    sharingType = sharingType ?: "Single",
    monthlyRent = monthlyRent ?: 0.0,
    depositAmount = depositAmount ?: 0.0,
    moveInDate = moveInDate,
    rentDueDate = rentDueDate ?: 1,
    notes = notes ?: "",
    isArchived = isArchived ?: false,
    syncStatus = syncStatus ?: "SYNCED"
)

fun RentPayment.toSupabase() = SupabasePayment(
    id = id,
    tenantId = tenantId,
    tenantName = tenantName,
    roomNumber = roomNumber,
    amountPaid = amountPaid,
    paymentMode = paymentMode,
    month = month,
    paymentDate = paymentDate,
    notes = notes,
    syncStatus = syncStatus
)

fun SupabasePayment.toRoom() = RentPayment(
    id = id,
    tenantId = tenantId,
    tenantName = tenantName,
    roomNumber = roomNumber,
    amountPaid = amountPaid ?: 0.0,
    paymentMode = paymentMode ?: "UPI",
    month = month,
    paymentDate = paymentDate ?: System.currentTimeMillis(),
    notes = notes ?: "",
    syncStatus = syncStatus ?: "SYNCED"
)

interface SupabaseApi {
    // ---- Users Endpoints ----
    @GET("rest/v1/users")
    suspend fun getUsers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabaseUser>

    @POST("rest/v1/users")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body user: SupabaseUser
    ): Response<Unit>

    // ---- Tenants Endpoints ----
    @GET("rest/v1/tenants")
    suspend fun getTenants(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabaseTenant>

    @POST("rest/v1/tenants")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertTenant(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body tenant: SupabaseTenant
    ): Response<Unit>

    @POST("rest/v1/tenants")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertTenants(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body tenants: List<SupabaseTenant>
    ): Response<Unit>

    @DELETE("rest/v1/tenants")
    suspend fun deleteTenant(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // ---- Rent Payments Endpoints ----
    @GET("rest/v1/rent_payments")
    suspend fun getPayments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabasePayment>

    @POST("rest/v1/rent_payments")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertPayment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body payment: SupabasePayment
    ): Response<Unit>

    @POST("rest/v1/rent_payments")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertPayments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body payments: List<SupabasePayment>
    ): Response<Unit>

    @DELETE("rest/v1/rent_payments")
    suspend fun deletePayment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<Unit>
}

object SupabaseClient {
    val baseUrl: String = try {
        BuildConfig.SUPABASE_URL
    } catch (e: Exception) {
        "https://ixhjpjrfsevwooseksqh.supabase.co"
    }.let { if (it.isBlank() || it.startsWith("MY_")) "https://ixhjpjrfsevwooseksqh.supabase.co" else it }

    val apiKey: String = try {
        BuildConfig.SUPABASE_KEY
    } catch (e: Exception) {
        "sb_publishable_sC8cj0r1mL-q0oB_sOQPsg_g2uBDNV4"
    }.let { if (it.isBlank() || it.startsWith("MY_")) "sb_publishable_sC8cj0r1mL-q0oB_sOQPsg_g2uBDNV4" else it }

    val authorizationHeader: String = "Bearer $apiKey"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val api: SupabaseApi by lazy {
        val rootUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        Retrofit.Builder()
            .baseUrl(rootUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}
