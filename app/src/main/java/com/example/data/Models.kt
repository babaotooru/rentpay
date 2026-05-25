package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val isAuthenticated: Boolean = false,
    val lastLoginTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val gender: String, // Male, Female, Other
    val idProofType: String, // Aadhaar, PAN, Passport, Driving License, Voter ID
    val idProofNumber: String,
    val photoPath: String? = null, // Path to local storage or content URI
    val roomNumber: String,
    val bedNumber: String,
    val sharingType: String, // Single, 2-Sharing, 3-Sharing, 4-Sharing
    val monthlyRent: Double,
    val depositAmount: Double,
    val moveInDate: String, // YYYY-MM-DD
    val rentDueDate: Int, // 1 to 31
    val notes: String = "",
    val isArchived: Boolean = false,
    val syncStatus: String = "SYNCED" // PENDING, SYNCED
) : Serializable

@Entity(tableName = "rent_payments")
data class RentPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val tenantName: String,
    val roomNumber: String,
    val amountPaid: Double,
    val paymentMode: String, // Cash, UPI, Bank Transfer, Cheque
    val month: String, // Example: "May 2026"
    val paymentDate: Long, // timestamp
    val notes: String = "",
    val syncStatus: String = "SYNCED" // PENDING, SYNCED
) : Serializable
