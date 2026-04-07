package com.neurodrive.smartcar.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
        private const val USERS_COLLECTION = "users"  // ✅ lowercase "users" - match your Firestore
    }

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            Log.e(TAG, "❌ No user logged in")
            _profileState.update {
                it.copy(isLoading = false, error = "No user logged in")
            }
            return
        }

        val uid = currentUser.uid
        Log.d(TAG, "Loading profile for UID: $uid")

        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }

            try {
                val document = firestore
                    .collection(USERS_COLLECTION)  // ✅ "users" not "Users"
                    .document(uid)
                    .get()
                    .await()

                if (document.exists()) {
                    // ✅ Log all data to verify
                    Log.d(TAG, "✅ Document data: ${document.data}")

                    _profileState.update {
                        it.copy(
                            isLoading = false,
                            // ✅ Match EXACT Firestore field names
                            fullName = document.getString("name") ?: "",           // "name" NOT "fullName"
                            username = document.getString("username") ?: "",
                            email = document.getString("email") ?: "",
                            phoneNumber = document.getString("phoneNumber") ?: "",
                            role = document.getString("role") ?: "",
                            profileImageUrl = document.getString("profilePhotoUrl") ?: "",  // "profilePhotoUrl"
                            userId = document.getString("userId") ?: "",
                            isEmailVerified = document.getBoolean("isEmailVerified") ?: false,
                            isPremium = document.getBoolean("isPremium") ?: false,
                            error = null
                        )
                    }

                    // ✅ Debug log
                    Log.d(TAG, "✅ Name: ${document.getString("name")}")
                    Log.d(TAG, "✅ Email: ${document.getString("email")}")
                    Log.d(TAG, "✅ Phone: ${document.getString("phoneNumber")}")
                    Log.d(TAG, "✅ Role: ${document.getString("role")}")
                    Log.d(TAG, "✅ Username: ${document.getString("username")}")

                } else {
                    Log.e(TAG, "❌ No document found for UID: $uid")
                    _profileState.update {
                        it.copy(
                            isLoading = false,
                            error = "Profile data not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading profile: ${e.message}", e)
                _profileState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}

// ✅ Profile State - matches Firestore fields
data class ProfileState(
    val isLoading: Boolean = false,
    val fullName: String = "",              // maps to "name" in Firestore
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = "",
    val profileImageUrl: String = "",       // maps to "profilePhotoUrl" in Firestore
    val userId: String = "",
    val isEmailVerified: Boolean = false,
    val isPremium: Boolean = false,
    val error: String? = null
)