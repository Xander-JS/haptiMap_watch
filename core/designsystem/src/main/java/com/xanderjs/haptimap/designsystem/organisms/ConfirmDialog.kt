package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogContent
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Pregunta de confirmación a pantalla completa: [title] y dos botones con texto, [confirmLabel] (primario)
 * y [dismissLabel] (secundario). Botones con texto y no los iconos ✓/✗ de Wear: TalkBack lee la acción
 * concreta ("Detener") y se ven como el resto de la app. Deslizar a la derecha es [onDismiss].
 */
@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(visible = visible, onDismissRequest = onDismiss, title = { ConfirmTitle(title) }, modifier = modifier) {
        confirmButtons(confirmLabel, dismissLabel, onConfirm, onDismiss)
    }
}

/** El mismo diálogo sin su ventana, para previews y capturas: layoutlib no dibuja ventanas de diálogo. */
@Composable
fun ConfirmDialogContent(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialogContent(title = { ConfirmTitle(title) }, modifier = modifier) {
        confirmButtons(confirmLabel, dismissLabel, onConfirm, onDismiss)
    }
}

@Composable
private fun ConfirmTitle(title: String) =
    Text(title, style = HaptiMapTheme.typography.title, color = HaptiMapTheme.colors.onBackground, textAlign = TextAlign.Center)

private fun ScalingLazyListScope.confirmButtons(confirmLabel: String, dismissLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    item { HaptiButton(text = confirmLabel, onClick = onConfirm, modifier = Modifier.fillMaxWidth()) }
    item { HaptiButton(text = dismissLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth(), variant = HaptiButtonVariant.Secondary) }
}

@HaptiPreviews
@Composable
private fun ConfirmDialogPreview() = PreviewSurface {
    ConfirmDialogContent(
        title = stringResource(R.string.sample_confirm_title),
        confirmLabel = stringResource(R.string.sample_confirm_yes),
        dismissLabel = stringResource(R.string.sample_confirm_no),
        onConfirm = {},
        onDismiss = {},
    )
}
