package com.evgrama.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class ChargingHost(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val socketType: String = "",
    val pricePerHour: Double = 0.0,
    val available: Boolean = true,
    val rating: Double = 5.0,
    val hostUid: String = ""
)

data class BookingRecord(
    val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val userUid: String = "",
    val hostUid: String = "",
    val amountPaid: Double = 0.0,
    val durationHours: Int = 1,
    val date: String = "",
    val status: String = "Pending" // Pending, Completed, Cancelled
)

data class UserProfile(
    val uid: String = "",
    val role: String = "", // user, host
    val displayName: String = "",
    val vehicleModel: String = "",
    val batteryCapacity: String = "",
    val walletBalance: Double = 0.0
)

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getChargingHosts(): List<ChargingHost> {
        return try {
            val snapshot = db.collection("charging_points").get().await()
            snapshot.toObjects(ChargingHost::class.java).filter { it.available }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllChargingStations(): List<ChargingHost> {
        return try {
            db.collection("charging_points").get().await().toObjects(ChargingHost::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addChargingStation(host: ChargingHost) {
        db.collection("charging_points").document(host.id).set(host).await()
    }

    suspend fun saveUserProfile(uid: String, role: String) {
        val existing = getUserProfile(uid)
        if (existing == null) {
            updateUserProfile(UserProfile(uid = uid, role = role, displayName = "", walletBalance = 0.0))
        } else {
            updateUserProfile(existing.copy(role = role))
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) doc.toObject(UserProfile::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        db.collection("users").document(profile.uid).set(profile).await()
    }

    suspend fun getBookingHistory(uid: String, role: String): List<BookingRecord> {
        val field = if (role == "host") "hostUid" else "userUid"
        return try {
            db.collection("bookings")
                .whereEqualTo(field, uid)
                .get()
                .await()
                .toObjects(BookingRecord::class.java)
                .sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWalletBalance(uid: String): Double {
        return getUserProfile(uid)?.walletBalance ?: 0.0
    }

    suspend fun getUserRole(uid: String): String? {
        return getUserProfile(uid)?.role
    }

    suspend fun processBooking(booking: BookingRecord): Boolean {
        return try {
            val userRef = db.collection("users").document(booking.userUid)
            val balance = getWalletBalance(booking.userUid)
            
            if (balance >= booking.amountPaid) {
                userRef.update("walletBalance", balance - booking.amountPaid).await()
                val bookingRef = db.collection("bookings").document()
                bookingRef.set(booking.copy(id = bookingRef.id)).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String) {
        db.collection("bookings").document(bookingId).update("status", status).await()
    }

    suspend fun updateStationStatus(hostId: String, isAvailable: Boolean) {
        db.collection("charging_points").document(hostId).update("available", isAvailable).await()
    }
}
