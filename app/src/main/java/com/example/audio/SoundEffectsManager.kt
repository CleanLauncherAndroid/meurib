package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffectType {
    TAP,
    HARAKA,
    ANALYZE_START,
    SUCCESS,
    OPEN_CARD,
    COPY,
    BOOKMARK,
    TOGGLE,
    ERROR
}

/**
 * 100% offline synthesized acoustic sound engine using Android AudioTrack.
 * Generates warm Andalusian-inspired plucked & chime harmonics in PCM-16 without
 * requiring any bundled audio binaries or external permissions.
 */
class SoundEffectsManager {
    private val sampleRate = 24_000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pcmCache = mutableMapOf<SoundEffectType, ShortArray>()

    @Volatile
    private var released = false

    fun play(type: SoundEffectType) {
        if (released) return
        scope.launch {
            if (released) return@launch
            runCatching {
                val samples = synchronized(pcmCache) {
                    pcmCache.getOrPut(type) { synthesize(type) }
                }
                playSamples(samples)
            }
        }
    }

    fun release() {
        released = true
        scope.cancel()
        synchronized(pcmCache) {
            pcmCache.clear()
        }
    }

    private fun playSamples(samples: ShortArray) {
        val byteSize = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(byteSize, minBuf))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            val written = track.write(samples, 0, samples.size)
            if (written > 0) {
                track.play()
                val durationMs = (samples.size * 1000L / sampleRate) + 25L
                Thread.sleep(durationMs)
            }
        } finally {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
            }
            runCatching { track.release() }
        }
    }

    private fun synthesize(type: SoundEffectType): ShortArray {
        return when (type) {
            SoundEffectType.TAP -> renderTone(
                durationMs = 42,
                notes = listOf(Note(freq = 440.0, startMs = 0, decay = 48.0, gain = 0.22))
            )
            SoundEffectType.HARAKA -> renderTone(
                durationMs = 68,
                notes = listOf(
                    Note(freq = 659.25, startMs = 0, decay = 42.0, gain = 0.24),
                    Note(freq = 987.77, startMs = 14, decay = 46.0, gain = 0.14)
                )
            )
            SoundEffectType.ANALYZE_START -> renderTone(
                durationMs = 150,
                notes = listOf(
                    Note(freq = 440.0, startMs = 0, decay = 22.0, gain = 0.24),
                    Note(freq = 587.33, startMs = 45, decay = 20.0, gain = 0.28)
                )
            )
            SoundEffectType.SUCCESS -> renderTone(
                durationMs = 340,
                notes = listOf(
                    Note(freq = 587.33, startMs = 0, decay = 11.0, gain = 0.24),   // D5
                    Note(freq = 739.99, startMs = 55, decay = 10.5, gain = 0.24),  // F#5
                    Note(freq = 880.00, startMs = 110, decay = 9.0, gain = 0.28),  // A5
                    Note(freq = 1174.66, startMs = 165, decay = 8.5, gain = 0.20) // D6 shimmer
                )
            )
            SoundEffectType.OPEN_CARD -> renderTone(
                durationMs = 125,
                notes = listOf(
                    Note(freq = 523.25, startMs = 0, decay = 26.0, gain = 0.20),
                    Note(freq = 659.25, startMs = 24, decay = 22.0, gain = 0.22)
                )
            )
            SoundEffectType.COPY -> renderTone(
                durationMs = 135,
                notes = listOf(
                    Note(freq = 783.99, startMs = 0, decay = 32.0, gain = 0.22),
                    Note(freq = 1046.50, startMs = 45, decay = 26.0, gain = 0.24)
                )
            )
            SoundEffectType.BOOKMARK -> renderTone(
                durationMs = 180,
                notes = listOf(
                    Note(freq = 587.33, startMs = 0, decay = 16.0, gain = 0.25),
                    Note(freq = 880.00, startMs = 35, decay = 14.0, gain = 0.24)
                )
            )
            SoundEffectType.TOGGLE -> renderTone(
                durationMs = 55,
                notes = listOf(Note(freq = 523.25, startMs = 0, decay = 40.0, gain = 0.20))
            )
            SoundEffectType.ERROR -> renderTone(
                durationMs = 200,
                notes = listOf(
                    Note(freq = 261.63, startMs = 0, decay = 16.0, gain = 0.26),
                    Note(freq = 196.00, startMs = 75, decay = 14.0, gain = 0.26)
                )
            )
        }
    }

    private data class Note(
        val freq: Double,
        val startMs: Int,
        val decay: Double,
        val gain: Double
    )

    private fun renderTone(durationMs: Int, notes: List<Note>): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val out = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val tSec = i.toDouble() / sampleRate
            var sampleVal = 0.0
            for (note in notes) {
                val startSec = note.startMs / 1000.0
                if (tSec >= startSec) {
                    val dt = tSec - startSec
                    // Smooth 6ms cosine fade-in + exponential harmonic decay
                    val attack = if (dt < 0.006) (dt / 0.006) else 1.0
                    val env = attack * exp(-note.decay * dt)
                    // Fundamental + warm 2nd & subtle 3rd acoustic overtones
                    val wave = sin(2.0 * PI * note.freq * dt) * 0.72 +
                        sin(2.0 * PI * (note.freq * 2.0) * dt) * 0.21 +
                        sin(2.0 * PI * (note.freq * 3.0) * dt) * 0.07
                    sampleVal += wave * env * note.gain
                }
            }
            // Tail de-click fade out over final 8ms
            val remaining = totalSamples - 1 - i
            val fadeSamples = (sampleRate * 8) / 1000
            if (remaining < fadeSamples) {
                sampleVal *= remaining.toDouble() / fadeSamples
            }
            val clamped = sampleVal.coerceIn(-0.92, 0.92)
            out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}

@Immutable
class FeedbackController(
    private val soundManager: SoundEffectsManager?,
    private val hapticFeedback: HapticFeedback?,
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean
) {
    fun perform(
        sound: SoundEffectType,
        strongHaptic: Boolean = false
    ) {
        if (soundEnabled) {
            soundManager?.play(sound)
        }
        if (hapticsEnabled) {
            runCatching {
                hapticFeedback?.performHapticFeedback(
                    if (strongHaptic) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                )
            }
        }
    }

    fun previewSoundOnly(sound: SoundEffectType) {
        soundManager?.play(sound)
    }

    fun previewHapticOnly() {
        runCatching {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

val LocalFeedbackController = staticCompositionLocalOf {
    FeedbackController(
        soundManager = null,
        hapticFeedback = null,
        soundEnabled = false,
        hapticsEnabled = false
    )
}

@Composable
fun rememberFeedbackController(
    soundManager: SoundEffectsManager,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean
): FeedbackController {
    val haptic = LocalHapticFeedback.current
    return remember(soundManager, haptic, soundEnabled, hapticsEnabled) {
        FeedbackController(
            soundManager = soundManager,
            hapticFeedback = haptic,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled
        )
    }
}
