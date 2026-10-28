package com.xanderjs.haptimap.core.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern

/**
 * Reproductor sobre el Vibrator del sistema. Con control de amplitud, la forma de onda entera; sin él,
 * solo los tiempos (los patrones están pensados para leerse también así).
 *
 * Uso ALARM: la navegación que el usuario ha empezado sigue sonando con No molestar por defecto y con la app
 * en segundo plano (paso 10). Si el usuario apaga la vibración de alarmas, el sistema la calla y no se fuerza.
 * El paso 25 puede añadir un ajuste para respetar No molestar.
 */
class AndroidHapticPlayer(context: Context) : HapticPlayer {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    private val hasVibrator = vibrator.hasVibrator()
    private val hasAmplitude = vibrator.hasAmplitudeControl()

    init {
        HapticEngine.logger.info("vibrator=$hasVibrator amplitudeControl=$hasAmplitude sdk=${Build.VERSION.SDK_INT}")
    }

    override fun play(event: HapticEvent, pattern: HapticPattern) {
        if (!hasVibrator) return
        val effect = if (hasAmplitude) {
            VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, NO_REPEAT)
        } else {
            VibrationEffect.createWaveform(pattern.onOffTimings(), NO_REPEAT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    override fun cancel() = vibrator.cancel()

    private companion object {
        const val NO_REPEAT = -1
    }
}
