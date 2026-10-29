/**
 * Onboarding y tutorial háptico (sección 19).
 */
package com.xanderjs.haptimap.feature.onboarding

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Cada paso del flujo de permiso: el texto que explica la situación y la acción que la resuelve. */
enum class PermissionStep(@StringRes val message: Int, @StringRes val action: Int, val opensSettings: Boolean) {
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
fun LocationPermissionScreen(onGranted: () -> Unit, viewModel: PermissionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // el diálogo del sistema pausa la Activity; si la respuesta llega sin pausa, el sistema denegó sin preguntar
    var dialogShown by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { result ->
        viewModel.onPermissionResult(fineGranted = result[ACCESS_FINE_LOCATION] == true, dialogShown = dialogShown)
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { dialogShown = true }
    }
    LaunchedEffect(uiState.granted) {
        if (uiState.granted) onGranted()
    }

    // setContent usa la propia Activity como LocalContext: startActivity no necesita FLAG_ACTIVITY_NEW_TASK
    val context = LocalContext.current
    PermissionContent(uiState.step) {
        if (uiState.step.opensSettings) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        } else {
            dialogShown = false
            launcher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
        }
    }
}

/**
 * GlanceScreenTemplate: el título siempre; el mensaje y la acción, cada uno en la primera vista solo si
 * cabe entero (los mensajes largos en español no caben en la redonda pequeña). Capturas en src/screenshotTest.
 */
@Composable
internal fun PermissionContent(step: PermissionStep, onAction: () -> Unit) {
    GlanceScreenTemplate(
        glance = {
            Text(
                text = stringResource(R.string.location_title),
                style = HaptiMapTheme.typography.title,
                color = HaptiMapTheme.colors.primary,
            )
        },
        details = listOf(
            @Composable {
                Text(
                    text = stringResource(step.message),
                    modifier = Modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction),
                    style = HaptiMapTheme.typography.body,
                    color = HaptiMapTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                )
            },
            @Composable { HaptiButton(text = stringResource(step.action), onClick = onAction, modifier = Modifier.fillMaxWidth()) },
        ),
    )
}
