package com.xanderjs.haptimap.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.location.LocationSource
import com.xanderjs.haptimap.core.location.fixes
import com.xanderjs.haptimap.core.navigation.NavigationEngine
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteRecalculator
import com.xanderjs.haptimap.core.navigation.ARRIVED_WAKE_LOCK_LINGER
import com.xanderjs.haptimap.core.navigation.navigationServiceRuns
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.RoutesRepository
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.di.AppLanguageState
import com.xanderjs.haptimap.feature.navigationui.NavigationNotificationContent
import com.xanderjs.haptimap.feature.navigationui.NotificationDetail
import com.xanderjs.haptimap.feature.navigationui.NotificationThrottle
import com.xanderjs.haptimap.feature.navigationui.ObserveActiveRoute
import com.xanderjs.haptimap.feature.navigationui.TourDetourController
import com.xanderjs.haptimap.feature.navigationui.TourSuggestion
import com.xanderjs.haptimap.feature.navigationui.TourSuggestionController
import com.xanderjs.haptimap.feature.navigationui.navigationUiState
import com.xanderjs.haptimap.feature.navigationui.toNotificationContent
import com.xanderjs.haptimap.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.xanderjs.haptimap.feature.navigationui.R as NavR

/**
 * Servicio en primer plano de tipo location (paso 10): mantiene vivos el GPS, el Navigation Engine y el
 * recálculo con la pantalla apagada o la app en segundo plano, y muestra la notificación con la Ongoing Activity.
 * Lo arranca HaptiMapApplication; se para solo cuando [navigationServiceRuns] lo dice. Los hápticos siguen en
 * el ámbito de la app (el banco de pruebas los usa sin navegar); el servicio los cubre igual: proceso y CPU despiertos.
 */
@AndroidEntryPoint
class NavigationService : Service() {
    @Inject lateinit var store: NavigationSessionStore
    @Inject lateinit var engine: NavigationEngine
    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var recalculator: RouteRecalculator
    @Inject lateinit var routes: RoutesRepository
    @Inject lateinit var connectivity: ConnectivityMonitor
    @Inject lateinit var activeRoute: ObserveActiveRoute
    @Inject lateinit var language: AppLanguageState
    @Inject lateinit var tourSuggestions: TourSuggestionController
    @Inject lateinit var tourDetour: TourDetourController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val throttle = NotificationThrottle()
    private lateinit var wakeLock: PowerManager.WakeLock
    @Volatile private var lastStartId = 0
    private var stopWatcher: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "create")
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(localized().getString(R.string.navigation_channel_name))
                .build(),
        )
        // paso 20: canal de la sugerencia de POI. IMPORTANCE_HIGH para que el full-screen intent despierte el reloj;
        // sin sonido ni vibración (nuestro háptico POI_SUGGESTION ya avisa, no lo doblamos).
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(SUGGESTION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(localized().getString(R.string.suggestion_channel_name))
                .setVibrationEnabled(false)
                .setSound(null, null)
                .build(),
        )
        // los fixes despiertan la CPU al llegar, pero lo que sigue (ventana de fusión háptica, esperas del
        // recálculo, la petición de red) son delay() que no la despiertan; el límite acota una fuga
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HaptiMap:navigation")
            .apply { setReferenceCounted(false); acquire(WAKE_LOCK_TIMEOUT.inWholeMilliseconds) }
        // paso 15A: mientras se navega y unos segundos tras llegar, no el minuto entero que sigue el servicio
        scope.launch {
            navigationServiceRuns(store.state, ARRIVED_WAKE_LOCK_LINGER).collect { held ->
                if (held) wakeLock.acquire(WAKE_LOCK_TIMEOUT.inWholeMilliseconds) else if (wakeLock.isHeld) wakeLock.release()
                Log.d(TAG, "wake lock ${if (held) "held" else "released"}")
            }
        }
        // movidos desde HaptiMapApplication sin cambios (pasos 07 y 08)
        scope.launch { engine.follow(locationSource.fixes()) }
        // sin clave, routes falla con el error de clave (MISSING_KEY): el recálculo lo trata como error final.
        // De camino a un POI del desvío (ToPoi), tourDetour::recalculate mantiene el POI como parada intermedia.
        scope.launch { recalculator.follow(tourDetour::recalculate, connectivity.status) }
        scope.launch {
            combine(store.state, activeRoute(), engine.progress) { session, active, progress ->
                navigationUiState(session, active, progress, confirming = false).toNotificationContent()
            }.filterNotNull().collect { content ->
                if (throttle.shouldPost(content, System.currentTimeMillis())) post(content)
            }
        }
        // paso 20: aceptar sin mirar. Con una sugerencia activa, un full-screen intent despierta el reloj y trae la
        // tarjeta interactiva al frente (MainActivity, que se pone sobre el bloqueo); al cerrarse se retira el aviso.
        scope.launch {
            tourSuggestions.state.collect { suggestion ->
                if (suggestion is TourSuggestion.Showing) postSuggestionAlert(suggestion.poi.candidate.name) else cancelSuggestionAlert()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        val content = navigationUiState(store.state.value, null, engine.progress.value, confirming = false).toNotificationContent()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(content), FOREGROUND_SERVICE_TYPE_LOCATION)
        Log.i(TAG, "start #$startId (session ${store.state.value::class.simpleName})")
        // se para solo, y solo tras startForeground: parar antes tumba la app; con el id, un arranque más nuevo gana
        if (stopWatcher == null) {
            stopWatcher = scope.launch {
                navigationServiceRuns(store.state).collect { runs ->
                    if (!runs) {
                        Log.i(TAG, "stop (session ${store.state.value::class.simpleName})")
                        stopSelf(lastStartId)
                    }
                }
            }
        }
        // tras matar el proceso no vuelve solo: la sesión estaría en Idle y el paso 06 ofrece retomar la ruta
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        Log.i(TAG, "destroy")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun post(content: NavigationNotificationContent) {
        val notification = notification(content)
        // una línea por publicación (como mucho cada 5 s): con la pantalla apagada, la prueba de que siguen llegando fixes
        Log.d(TAG, "notify ${notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)} · ${notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)}")
        // denegado (API 33+): sin notificación ni icono en la esfera, la navegación sigue igual
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) return
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    /**
     * Aviso de sugerencia de POI (paso 20): un full-screen intent que, con la pantalla apagada o en ambiente,
     * despierta el reloj y trae [MainActivity] al frente con la tarjeta interactiva, para aceptarla con el botón físico
     * o el del borde sin tener que buscar la app. Con la app ya en primer plano el sistema no lo lanza a pantalla
     * completa (la tarjeta ya está); silencioso, porque el háptico POI_SUGGESTION ya avisó. Se retira al cerrarse.
     */
    private fun postSuggestionAlert(placeName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_NAVIGATION, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, SUGGESTION_CHANNEL_ID)
            .setSmallIcon(com.xanderjs.haptimap.designsystem.R.drawable.ic_exploration_landmark)
            .setContentTitle(placeName)
            .setContentText(localized().getString(R.string.suggestion_alert_prompt))
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        NotificationManagerCompat.from(this).notify(SUGGESTION_NOTIFICATION_ID, notification)
    }

    private fun cancelSuggestionAlert() = NotificationManagerCompat.from(this).cancel(SUGGESTION_NOTIFICATION_ID)

    /**
     * Context con el idioma de la app (paso 25): la notificación y su Ongoing Activity salen fuera de Compose,
     * así que no heredan el idioma por app que [com.xanderjs.haptimap.presentation.ProvideAppLanguage] envuelve
     * sobre la UI. Se aplica aquí con el código ya resuelto por [AppLanguageState], único sitio que lo decide,
     * para que título, distancias (su separador decimal) y estados sigan el ajuste y no el idioma del sistema.
     * shortcut: el nombre del canal solo se relee al recrear el servicio; cambiar de idioma navegando no lo renombra
     * hasta la siguiente navegación. Es texto de ajustes del sistema, no de la notificación en curso; revisar si molesta.
     */
    private fun localized(): Context {
        val config = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language.current)) }
        return createConfigurationContext(config)
    }

    /** La notificación con su Ongoing Activity; [content] null solo si la sesión ya terminó y el servicio va a parar. */
    private fun notification(content: NavigationNotificationContent?): android.app.Notification {
        val ctx = localized()
        val title = ctx.getString(content?.title ?: R.string.navigation_channel_name)
        val text = when (val detail = content?.detail) {
            is NotificationDetail.Distance -> formatDistance(ctx, detail.meters)
            is NotificationDetail.FromRoute -> ctx.getString(NavR.string.navigation_from_route, formatDistance(ctx, detail.meters))
            is NotificationDetail.Destination -> detail.name
            null -> null
        }
        val status = if (text == null) title else ctx.getString(R.string.navigation_status, title, text)
        val icon = content?.icon ?: com.xanderjs.haptimap.designsystem.R.drawable.ic_straight
        val touch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_NAVIGATION, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(touch)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        // la plantilla va como parte: un "#" en el nombre del destino no se toma por una variable
        OngoingActivity.Builder(this, NOTIFICATION_ID, builder)
            .setStaticIcon(icon)
            .setTouchIntent(touch)
            .setContentDescription(status)
            .setStatus(Status.Builder().addTemplate("#status#").addPart("status", Status.TextPart(status)).build())
            .build()
            .apply(this)
        return builder.build()
    }

    companion object {
        const val EXTRA_OPEN_NAVIGATION = "com.xanderjs.haptimap.OPEN_NAVIGATION"
        private const val TAG = "HaptiService"
        private const val CHANNEL_ID = "navigation"
        private const val NOTIFICATION_ID = 10
        private const val SUGGESTION_CHANNEL_ID = "suggestion"
        private const val SUGGESTION_NOTIFICATION_ID = 11
        // shortcut: igual que la edad máxima de la caché (6 h); una navegación más larga pierde el bloqueo, revisar en el paso 23
        private val WAKE_LOCK_TIMEOUT = 6.hours

        /** Solo con la app en primer plano (Android 12+) y el permiso de ubicación concedido (Android 14+). */
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, NavigationService::class.java))
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException: la navegación sigue mientras la app esté visible
                Log.w(TAG, "start not allowed", e)
            }
        }
    }
}
