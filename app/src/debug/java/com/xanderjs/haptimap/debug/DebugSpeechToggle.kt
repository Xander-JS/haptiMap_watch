package com.xanderjs.haptimap.debug

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Solo debug: enciende o apaga [DebugSpeechActivity] (extra booleano "enabled"); una app sí puede cambiar sus componentes. */
class DebugSpeechToggle : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = if (intent.getBooleanExtra("enabled", false)) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        context.packageManager.setComponentEnabledSetting(ComponentName(context, DebugSpeechActivity::class.java), state, PackageManager.DONT_KILL_APP)
    }
}
