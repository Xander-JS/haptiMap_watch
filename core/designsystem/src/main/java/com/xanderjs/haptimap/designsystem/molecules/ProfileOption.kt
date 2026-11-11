package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.color
import com.xanderjs.haptimap.designsystem.atoms.onColor
import com.xanderjs.haptimap.designsystem.atoms.outlineBorder
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Una opción elegible con icono, título y un subtítulo corto que la explica (paso 16, selector de perfil). Todo el
 * botón es el área táctil y crece hasta el ancho; la corona lo recorre en la lista. [selected] la pinta en el color de
 * su modo ([tone]: azul Focus, verde Tour; el preseleccionado, para confirmar de un toque); las demás van en
 * superficie con contorno y el icono en el color del modo, así cada modo se reconoce por su color. TalkBack la lee entera y
 * anuncia si está seleccionada. Genérica: los textos llegan ya resueltos, como en [PlaceRow].
 */
@Composable
fun ProfileOption(
    icon: Painter,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ModeTone = ModeTone.Focus,
) {
    val colors = HaptiMapTheme.colors
    val (container, content, secondary) = if (selected) {
        Triple(tone.color, tone.onColor, tone.onColor)
    } else {
        Triple(colors.surface, colors.onSurface, colors.onSurfaceVariant)
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = HaptiMapTheme.dimens.minTouchTarget).semantics { this.selected = selected },
        shape = HaptiMapTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content, secondaryContentColor = secondary),
        border = if (selected) null else outlineBorder(),
        icon = { HaptiIcon(painter = icon, contentDescription = null, tint = if (selected) content else tone.color) },
        label = { Text(title, style = HaptiMapTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = { Text(subtitle, style = HaptiMapTheme.typography.caption, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

@HaptiPreviews
@Composable
private fun ProfileOptionPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        ProfileOption(
            icon = painterResource(R.drawable.ic_profile_focus),
            title = stringResource(R.string.sample_profile_focus),
            subtitle = stringResource(R.string.sample_profile_focus_subtitle),
            selected = true,
            onClick = {},
        )
        ProfileOption(
            icon = painterResource(R.drawable.ic_profile_tour),
            title = stringResource(R.string.sample_profile_tour),
            subtitle = stringResource(R.string.sample_profile_tour_subtitle),
            selected = false,
            tone = ModeTone.Tour,
            onClick = {},
        )
    }
}
