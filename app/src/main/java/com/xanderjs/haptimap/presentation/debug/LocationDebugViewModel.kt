package com.xanderjs.haptimap.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.model.GpsStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LocationDebugViewModel @Inject constructor(locationSource: LocationSource) : ViewModel() {
    // sin margen de espera: el GPS se para en cuanto la pantalla deja de estar visible (criterio 5 del paso 01)
    val uiState: StateFlow<GpsStatus> = locationSource.gpsStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), GpsStatus.Searching)
}
