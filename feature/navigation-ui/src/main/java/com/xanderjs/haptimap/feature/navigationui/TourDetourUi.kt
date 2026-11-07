package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.designsystem.organisms.DetourCard

/**
 * La tarjeta del desvío del Tour (paso 20) mientras hay uno en curso; nada en [TourDetour.Inactive]. La monta
 * MainActivity por encima de las pantallas, con el estado del [TourDetourController] y [onCancel]. En ambiente la
 * vista de ambiente la tapa (se respeta Eco/AOD), como la tarjeta de sugerencia.
 *
 * El [status] cuenta a dónde va el desvío: la distancia al POI mientras se va (lo que pide el paso 20), y el estado
 * al calcular la ruta, al llegar al POI y al volver. Las maniobras las sigue dando el háptico.
 */
@Composable
fun TourDetourOverlay(
    state: TourDetour,
    distanceToPoiMeters: Int?,
    onCancel: () -> Unit,
) {
    val poi = state.poi ?: return
    val category = poi.candidate.category ?: return
    val context = LocalContext.current
    val status = when (state) {
        is TourDetour.Loading -> stringResource(R.string.tour_detour_finding)
        is TourDetour.ToPoi -> distanceToPoiMeters?.let { formatDistance(context, it) } ?: stringResource(R.string.tour_detour_heading)
        is TourDetour.PoiReached -> stringResource(R.string.tour_detour_reached)
        is TourDetour.Returning -> stringResource(R.string.tour_detour_returning)
        TourDetour.Inactive -> return
    }
    DetourCard(
        title = poi.candidate.name,
        categoryIcon = painterResource(category.explorationIcon),
        categoryName = stringResource(category.labelRes),
        status = status,
        cancelLabel = stringResource(R.string.tour_detour_cancel),
        onCancel = onCancel,
    )
}
