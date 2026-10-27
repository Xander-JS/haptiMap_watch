package com.xanderjs.haptimap.core.model

/** Por encima de este error (m) la posición no sirve para guiar giros. Se calibra en el paso 14. */
const val LOW_ACCURACY_THRESHOLD_METERS = 20f

/** Estado del GPS que el Location Engine expone a la app. */
sealed interface GpsStatus {
    data object NoPermission : GpsStatus
    data object LocationDisabled : GpsStatus
    data object Searching : GpsStatus
    data class Fix(val location: LocationFix) : GpsStatus
    data class LowAccuracy(val location: LocationFix) : GpsStatus

    companion object {
        fun from(location: LocationFix): GpsStatus =
            if (location.accuracyMeters > LOW_ACCURACY_THRESHOLD_METERS) LowAccuracy(location) else Fix(location)
    }
}
