package com.neurodrive.smartcar.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.local.UserPreferences
import com.neurodrive.smartcar.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Minimum splash duration for animation
            delay(1500)

            val onboardingComplete = userPreferences.onboardingComplete.first()

            when {
                !onboardingComplete -> {
                    _uiState.value = SplashUiState.NavigateToOnboarding
                }
                firebaseAuthRepository.isUserLoggedIn -> {
                    _uiState.value = SplashUiState.NavigateToDashboard
                }
                else -> {
                    _uiState.value = SplashUiState.NavigateToLogin
                }
            }
        }
    }
}

sealed class SplashUiState {
    object Loading : SplashUiState()
    object NavigateToLogin : SplashUiState()
    object NavigateToDashboard : SplashUiState()
    object NavigateToOnboarding : SplashUiState()
}