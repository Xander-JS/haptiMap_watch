package com.xanderjs.haptimap.presentation.debug

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Pantalla temporal (paso 01): estado del GPS en crudo. Recolecta solo mientras está visible. */
@Composable
fun LocationDebugScreen(locationSource: LocationSource) {
    val status by remember(locationSource) { locationSource.gpsStatus() }
        .collectAsStateWithLifecycle(initialValue = GpsStatus.Searching)
    LocationDebugContent(status)
}

@Composable
private fun LocationDebugContent(status: GpsStatus) {
    val colors = HaptiMapTheme.colors
    val location = when (status) {
        is GpsStatus.Fix -> status.location
        is GpsStatus.LowAccuracy -> status.location
        else -> null
    }
    val (statusText, statusColor) = when (status) {
        GpsStatus.NoPermission -> R.string.gps_no_permission to colors.error
        GpsStatus.LocationDisabled -> R.string.gps_location_disabled to colors.error
        GpsStatus.Searching -> R.string.gps_searching to colors.onSurfaceVariant
        is GpsStatus.Fix -> R.string.gps_fix to colors.success
        is GpsStatus.LowAccuracy -> R.string.gps_low_accuracy to colors.warning
    }
    val empty = stringResource(R.string.debug_empty_value)

    RoundScreenTemplate {
        item { Text(stringResource(R.string.location_debug_title), style = HaptiMapTheme.typography.title, color = colors.primary) }
        item { StatusText(statusText, statusColor) }
        item { DebugRow(R.string.debug_latitude, location?.let { stringResource(R.string.debug_degrees_value, it.point.latitude) } ?: empty) }
        item { DebugRow(R.string.debug_longitude, location?.let { stringResource(R.string.debug_degrees_value, it.point.longitude) } ?: empty) }
        item { DebugRow(R.string.debug_accuracy, location?.let { stringResource(R.string.debug_meters_value, it.accuracyMeters) } ?: empty) }
        item { DebugRow(R.string.debug_speed, location?.speedMetersPerSecond?.let { stringResource(R.string.debug_speed_value, it) } ?: empty) }
        item { DebugRow(R.string.debug_bearing, location?.bearingDegrees?.let { stringResource(R.string.debug_bearing_value, it) } ?: empty) }
    }
}

@Composable
private fun StatusText(@StringRes text: Int, color: Color) {
    Text(stringResource(text), style = HaptiMapTheme.typography.body, color = color, textAlign = TextAlign.Center)
}

@Composable
private fun DebugRow(@StringRes label: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), style = HaptiMapTheme.typography.caption, color = HaptiMapTheme.colors.onSurfaceVariant)
        Text(value, style = HaptiMapTheme.typography.body, color = HaptiMapTheme.colors.onBackground)
    }
}

@HaptiPreviews
@Composable
private fun LocationDebugFixPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        LocationDebugContent(GpsStatus.Fix(LocationFix(GeoPoint(40.416775, -3.703790), 4.5f, 1.3f, 87f, 0L)))
    }
}

@HaptiPreviews
@Composable
private fun LocationDebugDisabledPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { LocationDebugContent(GpsStatus.LocationDisabled) }
}
