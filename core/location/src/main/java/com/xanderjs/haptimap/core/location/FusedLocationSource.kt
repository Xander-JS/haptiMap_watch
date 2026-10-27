package com.xanderjs.haptimap.core.location

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "HaptiLocation"

/** Location Engine sobre Fused Location Provider, con prioridad de alta precisión. */
class FusedLocationSource(context: Context) : LocationSource {
    private val context = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(this.context)
    private val locationManager = this.context.getSystemService(LocationManager::class.java)

    override fun gpsStatus(intervalMillis: Long): Flow<GpsStatus> = gpsStatusFlow(
        // la ubicación aproximada no basta para guiar giros: solo cuenta la precisa
        hasPermission = { context.checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED },
        isLocationEnabled = locationManager::isLocationEnabled,
        providerChanges = providerChanges(),
        fixes = { fixes(intervalMillis) },
    )

    private fun providerChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        // PROVIDERS_CHANGED lo envía el sistema, que llega también a receptores no exportados
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }

    @SuppressLint("MissingPermission") // gpsStatusFlow solo recolecta fixes con el permiso concedido
    private fun fixes(intervalMillis: Long): Flow<LocationFix> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toFix()) }
            }
        }
        Log.d(TAG, "Location updates started (interval $intervalMillis ms)")
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { Log.w(TAG, "Location updates request failed", it) }
        awaitClose {
            client.removeLocationUpdates(callback)
            Log.d(TAG, "Location updates stopped")
        }
    }
}

private fun Location.toFix() = LocationFix(
    point = GeoPoint(latitude, longitude),
    accuracyMeters = accuracy,
    speedMetersPerSecond = if (hasSpeed()) speed else null,
    bearingDegrees = if (hasBearing()) bearing else null,
    timestampMillis = time,
)
