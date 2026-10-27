/**
 * Onboarding y tutorial háptico (sección 19).
 */
package com.xanderjs.haptimap.feature.onboarding

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Cada paso del flujo de permiso: el texto que explica la situación y la acción que la resuelve. */
internal enum class PermissionStep(@StringRes val message: Int, @StringRes val action: Int, val opensSettings: Boolean) {
    Rationale(R.string.location_rationale, R.string.location_allow, opensSettings = false),
    Denied(R.string.location_denied, R.string.location_try_again, opensSettings = false),
    Blocked(R.string.location_blocked, R.string.location_open_settings, opensSettings = true),
    Approximate(R.string.location_approximate, R.string.location_use_precise, opensSettings = false),
    ApproximateBlocked(R.string.location_approximate_blocked, R.string.location_open_settings, opensSettings = true),
}

/**
 * Pide la ubicación precisa y llama a [onGranted] cuando la tiene. La aproximada sola no basta.
 * Al volver de los ajustes de la app se comprueba de nuevo.
 */
@Composable
fun LocationPermissionScreen(onGranted: () -> Unit) {
    // setContent usa la propia Activity como LocalContext
    val activity = LocalContext.current as Activity
    var asked by rememberSaveable { mutableStateOf(false) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    // el diálogo del sistema pausa la Activity; si la respuesta llega sin pausa, el sistema denegó sin preguntar
    var dialogShown by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(activity.permissionStep(asked, blocked)) }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { result ->
        if (result[ACCESS_FINE_LOCATION] == true) {
            onGranted()
        } else {
            asked = true
            blocked = !dialogShown
            step = activity.permissionStep(asked, blocked)
        }
    }
    LifecycleResumeEffect(Unit) {
        if (activity.checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) onGranted()
        step = activity.permissionStep(asked, blocked)
        onPauseOrDispose { dialogShown = true }
    }

    PermissionContent(step) {
        if (step.opensSettings) {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
            )
        } else {
            dialogShown = false
            launcher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
        }
    }
}

/**
 * shouldShowRequestPermissionRationale no sirve para detectar el bloqueo: tras conceder solo la aproximada
 * devuelve false aunque el sistema aún ofrezca pasar a precisa. Por eso [blocked] viene del diálogo no mostrado.
 */
private fun Activity.permissionStep(asked: Boolean, blocked: Boolean): PermissionStep {
    val approximate = checkSelfPermission(ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
    return when {
        approximate && blocked -> PermissionStep.ApproximateBlocked
        approximate -> PermissionStep.Approximate
        blocked -> PermissionStep.Blocked
        asked -> PermissionStep.Denied
        else -> PermissionStep.Rationale
    }
}

@Composable
private fun PermissionContent(step: PermissionStep, onAction: () -> Unit) {
    RoundScreenTemplate {
        item {
            Text(
                text = stringResource(R.string.location_title),
                style = HaptiMapTheme.typography.title,
                color = HaptiMapTheme.colors.primary,
            )
        }
        item {
            Text(
                text = stringResource(step.message),
                style = HaptiMapTheme.typography.body,
                color = HaptiMapTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        item { HaptiButton(text = stringResource(step.action), onClick = onAction, modifier = Modifier.fillMaxWidth()) }
    }
}

@HaptiPreviews
@Composable
private fun RationalePreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { PermissionContent(PermissionStep.Rationale) {} }
}

@HaptiPreviews
@Composable
private fun ApproximatePreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { PermissionContent(PermissionStep.Approximate) {} }
}
