package com.xanderjs.haptimap.feature.onboarding

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionUiState(val step: PermissionStep, val granted: Boolean)

/** Decide qué paso del flujo de permiso mostrar. `asked` y `blocked` sobreviven a la muerte del proceso. */
@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private var asked: Boolean
        get() = savedState[KEY_ASKED] ?: false
        set(value) { savedState[KEY_ASKED] = value }
    private var blocked: Boolean
        get() = savedState[KEY_BLOCKED] ?: false
        set(value) { savedState[KEY_BLOCKED] = value }

    private val _uiState = MutableStateFlow(currentState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    /** Al volver a primer plano (p. ej. desde los ajustes de la app) el permiso puede haber cambiado. */
    fun onResume() {
        _uiState.value = currentState()
    }

    /** [dialogShown]: el diálogo del sistema llegó a pausar la Activity; si no, el sistema denegó sin preguntar. */
    fun onPermissionResult(fineGranted: Boolean, dialogShown: Boolean) {
        if (!fineGranted) {
            asked = true
            blocked = !dialogShown
        }
        _uiState.value = currentState()
    }

    private fun currentState() = PermissionUiState(step = permissionStep(), granted = isGranted(ACCESS_FINE_LOCATION))

    /**
     * shouldShowRequestPermissionRationale no sirve para detectar el bloqueo: tras conceder solo la aproximada
     * devuelve false aunque el sistema aún ofrezca pasar a precisa. Por eso [blocked] viene del diálogo no mostrado.
     */
    private fun permissionStep(): PermissionStep {
        val approximate = isGranted(ACCESS_COARSE_LOCATION)
        return when {
            approximate && blocked -> PermissionStep.ApproximateBlocked
            approximate -> PermissionStep.Approximate
            blocked -> PermissionStep.Blocked
            asked -> PermissionStep.Denied
            else -> PermissionStep.Rationale
        }
    }

    private fun isGranted(permission: String) = context.checkSelfPermission(permission) == PERMISSION_GRANTED

    private companion object {
        const val KEY_ASKED = "asked"
        const val KEY_BLOCKED = "blocked"
    }
}
