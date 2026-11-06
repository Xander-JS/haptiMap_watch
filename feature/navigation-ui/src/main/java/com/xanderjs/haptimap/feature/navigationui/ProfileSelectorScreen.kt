package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.molecules.ProfileOption
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate

/**
 * Elegir perfil (paso 16), tras elegir destino y antes de pedir la ruta. Dos opciones (Focus y Tour), cada una con
 * icono, título y un subtítulo corto; un toque la elige y sigue a [onSelected]. El último perfil va preseleccionado.
 */
@Composable
fun ProfileSelectorScreen(onSelected: () -> Unit, viewModel: ProfileSelectorViewModel = hiltViewModel()) {
    val preselected by viewModel.preselected.collectAsStateWithLifecycle()
    ProfileSelectorContent(
        preselected = preselected,
        onSelect = { profile ->
            viewModel.select(profile)
            onSelected()
        },
    )
}

/** La pantalla en un estado dado; las previews y capturas están en src/screenshotTest. */
@Composable
internal fun ProfileSelectorContent(preselected: NavigationProfile, onSelect: (NavigationProfile) -> Unit) {
    RoundScreenTemplate {
        item { HeadingText(stringResource(R.string.profile_title)) }
        item {
            ProfileOption(
                icon = painterResource(DsR.drawable.ic_profile_focus),
                title = stringResource(R.string.profile_focus_title),
                subtitle = stringResource(R.string.profile_focus_subtitle),
                selected = preselected == NavigationProfile.FOCUS,
                onClick = { onSelect(NavigationProfile.FOCUS) },
            )
        }
        item {
            ProfileOption(
                icon = painterResource(DsR.drawable.ic_profile_tour),
                title = stringResource(R.string.profile_tour_title),
                subtitle = stringResource(R.string.profile_tour_subtitle),
                selected = preselected == NavigationProfile.TOUR,
                onClick = { onSelect(NavigationProfile.TOUR) },
            )
        }
    }
}
