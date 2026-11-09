package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.rememberCameraPositionState
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.MapControlButton
import com.xanderjs.haptimap.designsystem.atoms.clockBandTop
import com.xanderjs.haptimap.designsystem.molecules.MapManeuverCard
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Vista de mapa continuo (paso 22), mientras se navega (NavigationUiState.Navigating). Mapa a pantalla completa con
 * la maniobra, la posición y la ruta; fuera de ruta, la llegada y el ambiente siguen en Eco (no hay guía que seguir
 * sobre el mapa, o no debe renderizarse). El háptico sigue siendo el canal principal; el mapa es un complemento.
 *
 * La cámara sigue al usuario (norte arriba, zoom peatonal). Si el usuario arrastra el mapa a mano, el seguimiento se
 * pausa y aparece recentrar. Norte arriba y no rumbo arriba: la cámara solo se mueve al cambiar la posición (no con
 * cada giro de la muñeca), lo que gasta menos; el rumbo lo da la flecha de la maniobra.
 */
@Composable
fun NavigationMapScreen(
    guidance: GuidanceUi,
    notice: NavigationNotice?,
    onStop: () -> Unit,
    viewModel: NavigationMapViewModel = hiltViewModel(),
) {
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState()
    var isFollowing by rememberSaveable { mutableStateOf(true) }

    val position = mapState?.userPosition
    // la posición solo cambia con un fix bueno (ya viene limitada en NavigationProgress): no recomponemos con cada GPS
    LaunchedEffect(position, isFollowing) {
        if (isFollowing && position != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(position.latitude, position.longitude), WALKING_ZOOM))
        }
    }
    // mover el mapa a mano pausa el seguimiento y muestra recentrar
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.cameraMoveStartedReason }.collect { reason ->
            if (reason == CameraMoveStartedReason.GESTURE) isFollowing = following(isFollowing, MapGesture.USER_PANNED)
        }
    }

    NavigationMapContent(
        guidance = guidance,
        notice = notice,
        recenterVisible = !isFollowing,
        onRecenter = { isFollowing = following(isFollowing, MapGesture.RECENTER) },
        onStop = onStop,
        map = { mapState?.let { GoogleNavigationMap(it, cameraPositionState, Modifier.matchParentSize()) } },
    )
}

/**
 * El mapa con sus superposiciones; [map] es una ranura para que las capturas pongan un marcador neutro en lugar de
 * teselas reales (sin red ni coste). Arriba, la maniobra (y el aviso si lo hay); a un lado, recentrar cuando el
 * seguimiento está pausado; abajo, Detener, siempre accesible. Todo con componentes del design system.
 */
@Composable
internal fun NavigationMapContent(
    guidance: GuidanceUi,
    notice: NavigationNotice?,
    recenterVisible: Boolean,
    onRecenter: () -> Unit,
    onStop: () -> Unit,
    map: @Composable BoxScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(HaptiMapTheme.colors.background)) {
        map()
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = clockBandTop(LocalOffline.current), start = HaptiMapTheme.spacing.m, end = HaptiMapTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
        ) {
            notice?.let { NavigationNoticeLabel(it) }
            MapManeuverCard(direction = guidance.direction, distanceMeters = guidance.distanceMeters)
        }
        if (recenterVisible) {
            MapControlButton(
                iconRes = DsR.drawable.ic_recenter,
                contentDescription = stringResource(DsR.string.map_recenter),
                onClick = onRecenter,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = HaptiMapTheme.spacing.s),
            )
        }
        HaptiButton(
            text = stringResource(R.string.navigation_stop),
            onClick = onStop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = HaptiMapTheme.spacing.m, start = HaptiMapTheme.spacing.xl, end = HaptiMapTheme.spacing.xl)
                .fillMaxWidth(),
            variant = HaptiButtonVariant.Secondary,
            maxLines = 1,
        )
    }
}
