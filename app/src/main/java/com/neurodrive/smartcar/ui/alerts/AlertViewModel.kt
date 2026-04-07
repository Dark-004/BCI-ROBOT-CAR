package com.neurodrive.smartcar.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.repository.AlertEvent
import com.neurodrive.smartcar.data.repository.BioSignalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val bioSignalRepository: BioSignalRepository
) : ViewModel() {

    val currentAlert: StateFlow<AlertEvent?> = bioSignalRepository.currentAlert
    val alerts: StateFlow<List<AlertEvent>> = bioSignalRepository.alertEvents

    private val _alertHistory = MutableStateFlow<List<AlertEvent>>(emptyList())
    val alertHistory: StateFlow<List<AlertEvent>> = _alertHistory.asStateFlow()

    init {
        observeAlerts()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            bioSignalRepository.alertEvents.collect { alertList ->
                if (alertList.isNotEmpty()) {
                    _alertHistory.value =
                        (_alertHistory.value + alertList)
                            .distinctBy { it.id }
                            .takeLast(200)
                }
            }
        }
    }

    fun dismissAlert() {
        bioSignalRepository.dismissCurrentAlert()
    }

    fun clearAlerts() {
        bioSignalRepository.clearAlerts()
        _alertHistory.value = emptyList()
    }

    fun clearHistory() {
        _alertHistory.value = emptyList()
    }
}