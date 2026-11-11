package com.xanderjs.haptimap.feature.navigationui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.templates.SuggestionCard
import kotlin.time.Duration
import com.xanderjs.haptimap.designsystem.R as DsR

/** Icono de exploración de cada categoría (paso 19). Café/restaurante reutiliza el icono de restaurante del paso 11. */
@get:DrawableRes
val ExplorationCategory.explorationIcon: Int
    get() = when (this) {
        ExplorationCategory.MUSEUM -> DsR.drawable.ic_exploration_museum
        ExplorationCategory.PARK -> DsR.drawable.ic_exploration_park
        ExplorationCategory.LANDMARK -> DsR.drawable.ic_exploration_landmark
        ExplorationCategory.VIEWPOINT -> DsR.drawable.ic_exploration_viewpoint
        ExplorationCategory.CULTURAL_SITE -> DsR.drawable.ic_exploration_cultural
        ExplorationCategory.CAFE_RESTAURANT -> DsR.drawable.ic_category_restaurant
    }

@get:StringRes
val ExplorationCategory.labelRes: Int
    get() = when (this) {
        ExplorationCategory.MUSEUM -> R.string.tour_category_museum
        ExplorationCategory.PARK -> R.string.tour_category_park
        ExplorationCategory.LANDMARK -> R.string.tour_category_landmark
        ExplorationCategory.VIEWPOINT -> R.string.tour_category_viewpoint
        ExplorationCategory.CULTURAL_SITE -> R.string.tour_category_cultural
        ExplorationCategory.CAFE_RESTAURANT -> R.string.tour_category_cafe_restaurant
    }

/** Tiempo extra del desvío: segundos por debajo de 1 min, minutos redondeados por encima. */
@Composable
fun addedTimeLabel(addedTime: Duration): String {
    val seconds = addedTime.inWholeSeconds
    return if (seconds < 60) {
        stringResource(R.string.tour_suggestion_added_time_sec, seconds.toInt())
    } else {
        stringResource(R.string.tour_suggestion_added_time_min, ((seconds + 30) / 60).toInt())
    }
}

/**
 * La tarjeta de sugerencia del Tour (paso 19) mientras hay una en pantalla; nada en cualquier otro estado. La monta
 * MainActivity por encima de las pantallas, con el estado y las acciones del [TourSuggestionController].
 *
 * La cuenta atrás es solo visual: baja de [timeoutSeconds] a 0 con el reloj de frames. Quien de verdad cierra por
 * tiempo es el controlador, con el mismo tiempo de ajustes; así el número y el anillo no son otra fuente de verdad.
 */
@Composable
fun TourSuggestionOverlay(
    state: TourSuggestion,
    timeoutSeconds: Int,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val poi = (state as? TourSuggestion.Showing)?.poi ?: return
    val category = poi.candidate.category ?: return // la tubería descarta los POIs sin categoría antes de evaluar

    val totalMillis = timeoutSeconds * 1000L
    var remainingMillis by remember(poi.candidate.id) { mutableLongStateOf(totalMillis) }
    LaunchedEffect(poi.candidate.id) {
        val start = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            remainingMillis = (totalMillis - (now - start)).coerceAtLeast(0L)
            if (remainingMillis <= 0L) break
        }
    }
    val secondsRemaining = ((remainingMillis + 999) / 1000).toInt() // redondea hacia arriba: no muestra 0 hasta agotarse
    val progress = if (totalMillis == 0L) 0f else remainingMillis.toFloat() / totalMillis

    SuggestionCard(
        title = poi.candidate.name,
        categoryIcon = painterResource(category.explorationIcon),
        categoryName = stringResource(category.labelRes),
        addedTime = addedTimeLabel(poi.addedTime),
        progress = progress,
        countdownDescription = pluralStringResource(R.plurals.tour_suggestion_countdown, secondsRemaining, secondsRemaining),
        dismissLabel = stringResource(R.string.tour_suggestion_dismiss),
        onDismiss = onDismiss,
        acceptAction = { HaptiEdgeButton(text = stringResource(R.string.tour_suggestion_accept), onClick = onAccept, tone = ModeTone.Tour) },
    )
}
