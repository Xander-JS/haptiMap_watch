package com.xanderjs.haptimap.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
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
import com.xanderjs.haptimap.core.navigation.navigationServiceRuns
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import com.xanderjs.haptimap.core.network.RoutesRepository
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import com.xanderjs.haptimap.feature.navigationui.NavigationNotificationContent
import com.xanderjs.haptimap.feature.navigationui.NotificationDetail
import com.xanderjs.haptimap.feature.navigationui.NotificationThrottle
import com.xanderjs.haptimap.feature.navigationui.ObserveActiveRoute
import com.xanderjs.haptimap.feature.navigationui.navigationUiState
import com.xanderjs.haptimap.feature.navigationui.toNotificationContent
import com.xanderjs.haptimap.presentation.MainActivity
import com.xanderjs.haptimap.presentation.debug.rerouteRequest
import dagger.hilt.android.AndroidEntryPoint
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
                .setName(getString(R.string.navigation_channel_name))
                .build(),
        )
        // los fixes despiertan la CPU al llegar, pero lo que sigue (ventana de fusión háptica, esperas del
        // recálculo, la petición de red) son delay() que no la despiertan; el límite acota una fuga
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HaptiMap:navigation")
            .apply { setReferenceCounted(false); acquire(WAKE_LOCK_TIMEOUT.inWholeMilliseconds) }
        // movidos desde HaptiMapApplication sin cambios (pasos 07 y 08)
        scope.launch { engine.follow(locationSource.fixes()) }
        scope.launch { recalculator.follow(rerouteRequest(routes) { appLanguage() }, connectivity.status) }
        scope.launch {
            combine(store.state, activeRoute(), engine.progress) { session, active, progress ->
                navigationUiState(session, active, progress, confirming = false).toNotificationContent()
            }.filterNotNull().collect { content ->
                if (throttle.shouldPost(content, System.currentTimeMillis())) post(content)
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

    /** La notificación con su Ongoing Activity; [content] null solo si la sesión ya terminó y el servicio va a parar. */
    private fun notification(content: NavigationNotificationContent?): android.app.Notification {
        val title = getString(content?.title ?: R.string.navigation_channel_name)
        val text = when (val detail = content?.detail) {
            is NotificationDetail.Distance -> formatDistance(this, detail.meters)
            is NotificationDetail.FromRoute -> getString(NavR.string.navigation_from_route, formatDistance(this, detail.meters))
            is NotificationDetail.Destination -> detail.name
            null -> null
        }
        val status = if (text == null) title else getString(R.string.navigation_status, title, text)
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
