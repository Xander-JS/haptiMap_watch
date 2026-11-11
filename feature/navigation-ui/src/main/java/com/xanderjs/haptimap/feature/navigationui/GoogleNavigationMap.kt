package com.xanderjs.haptimap.feature.navigationui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * El mapa continuo propiamente dicho (paso 22): la ÚNICA pieza que depende del SDK de Maps. Pinta el [NavigationMapState]
 * con la cámara que controla [NavigationMapScreen] (seguimiento y recentrado).
 *
 * Coste: una sola carga de mapa por sesión (este Composable se compone una vez; mover la cámara al caminar NO cuenta
 * como carga nueva). Estilo JSON de los tokens, sin Map ID (SKU "Maps SDK" gratuito en lugar de "Dynamic Maps").
 * Sin capas de tráfico, edificios ni interiores: menos red y batería (la optimización completa es el paso 24).
 */
@Composable
fun GoogleNavigationMap(
    state: NavigationMapState,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    onMapLoaded: () -> Unit = {},
) {
    val colors = HaptiMapTheme.colors
    val context = LocalContext.current
    val properties = remember(colors) {
        MapProperties(
            // calles a medio camino entre el fondo y el contorno: se leen sin competir con la ruta en volt
            mapStyleOptions = MapStyleOptions(mapStyleJson(colors.background, lerp(colors.background, colors.outline, ROAD_BLEND), colors.onSurfaceVariant)),
            isBuildingEnabled = false,
            isIndoorEnabled = false,
            isTrafficEnabled = false,
        )
    }
    // sin controles del SDK: TimeText y el botón de recentrar propios ocupan su sitio; el giro y la inclinación, fuera
    // (cámara norte arriba). Arrastrar y hacer zoom sí, para explorar la ruta (recentrar vuelve al seguimiento).
    val uiSettings = remember {
        MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
        )
    }
    // marcador del usuario: una punta de flecha en onBackground (no en volt: se perdería sobre la ruta, que es volt) con
    // borde del fondo, que rota con el rumbo. Si el SDK no está listo para crear el bitmap, se cae al pin por defecto.
    val userIcon = remember(colors) {
        runCatching {
            MapsInitializer.initialize(context)
            userMarkerIcon(colors.onBackground.toArgb(), colors.background.toArgb())
        }.getOrNull()
    }
    // destino y POI: los discos de la app (IconDisc) con el icono del design system, en lugar de los pines de Google:
    // cielo para la llegada, orquídea para el Tour. Así el mapa entero habla el mismo idioma de color que la app.
    val destinationIcon = remember(colors) {
        pinMarkerIcon(context, DsR.drawable.ic_arrive, colors.success.toArgb(), colors.onSuccess.toArgb(), colors.background.toArgb())
    }
    val poiIcon = remember(state.poi?.category, colors) {
        // sin categoría (la fuente no la mapea), un icono genérico de lugar
        state.poi?.let {
            pinMarkerIcon(
                context,
                it.category?.explorationIcon ?: DsR.drawable.ic_exploration_landmark,
                colors.tour.toArgb(),
                colors.onTour.toArgb(),
                colors.background.toArgb(),
            )
        }
    }

    // la posición del usuario cambia con cada fix, pero la geometría de la ruta no: se mapea una vez por ruta
    val routePoints = remember(state.route) { state.route.map(GeoPoint::toLatLng) }
    val detourPoints = remember(state.detourLeg) { state.detourLeg?.map(GeoPoint::toLatLng) }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        onMapLoaded = onMapLoaded,
    ) {
        Polyline(points = routePoints, color = colors.navigationArrow, width = 14f)
        detourPoints?.let { Polyline(points = it, color = colors.tour, width = 14f) }
        Marker(
            state = rememberMarkerState(position = state.destination.toLatLng()),
            icon = destinationIcon,
            anchor = Offset(0.5f, 0.5f),
        )
        state.poi?.let { poi ->
            Marker(
                state = rememberMarkerState(position = poi.position.toLatLng()),
                title = poi.title,
                icon = poiIcon,
                anchor = Offset(0.5f, 0.5f),
            )
        }
        state.userPosition?.let { position ->
            Marker(
                state = rememberUpdatedMarkerState(position = position.toLatLng()),
                icon = userIcon,
                anchor = Offset(0.5f, 0.5f),
                rotation = state.userBearingDegrees ?: 0f,
                flat = true,
            )
        }
    }
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

/**
 * Pin del design system: disco del color del token [fillColor] con anillo [ringColor] (el fondo: lo separa de calles y
 * ruta) y el vector [iconRes] en [iconColor] al centro. null si el SDK aún no puede crear el bitmap (se cae al pin por defecto).
 */
private fun pinMarkerIcon(context: Context, iconRes: Int, fillColor: Int, iconColor: Int, ringColor: Int): BitmapDescriptor? = runCatching {
    MapsInitializer.initialize(context)
    val size = 88
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    val r = size * 0.42f
    canvas.drawCircle(c, c, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor; style = Paint.Style.FILL })
    canvas.drawCircle(
        c, c, r,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColor; style = Paint.Style.STROKE; strokeWidth = size * 0.05f },
    )
    ContextCompat.getDrawable(context, iconRes)?.apply {
        val inset = (size * 0.28f).toInt()
        setBounds(inset, inset, size - inset, size - inset)
        setTint(iconColor)
        draw(canvas)
    }
    BitmapDescriptorFactory.fromBitmap(bitmap)
}.getOrNull()

/** Punta de flecha rellena de [color] con contorno [strokeColor] (contraste sobre cualquier tesela), apuntando al norte sin rotar. */
private fun userMarkerIcon(color: Int, strokeColor: Int): BitmapDescriptor {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val arrow = Path().apply {
        moveTo(cx, cy - size * 0.40f)
        lineTo(cx + size * 0.28f, cy + size * 0.32f)
        lineTo(cx, cy + size * 0.12f)
        lineTo(cx - size * 0.28f, cy + size * 0.32f)
        close()
    }
    canvas.drawPath(arrow, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL })
    canvas.drawPath(
        arrow,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.04f
        },
    )
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private const val ROAD_BLEND = 0.5f
