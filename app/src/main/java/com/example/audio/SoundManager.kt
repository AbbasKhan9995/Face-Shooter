package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class SoundManager(private val preferencesManager: PreferencesManager) {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean
        get() = preferencesManager.isSoundEnabled
        set(value) {
            preferencesManager.isSoundEnabled = value
        }

    fun toggleSound(): Boolean {
        val newState = !isSoundEnabled
        isSoundEnabled = newState
        return newState
    }

    fun playShoot() {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 85
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                // Frequency sweep: 700Hz down to 220Hz
                val freq = 700.0 - 480.0 * progress
                val envelope = (1.0 - progress) * (1.0 - progress)
                val sample = sin(2.0 * PI * freq * t) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.75).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    /**
     * Pitch rises with combo count!
     */
    fun playPop(combo: Int = 1) {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 90
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            // Base pitch multiplies with combo
            val baseFreq = 440.0 * 1.12.pow(combo.coerceIn(1, 15).toDouble())
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val envelope = exp(- progress * 7.0) // snappy exponential decay
                // Pop tone + subtle harmonic
                val sample = (sin(2.0 * PI * baseFreq * t) * 0.8 +
                        sin(4.0 * PI * baseFreq * t) * 0.2) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.85).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playOrphanDrop(count: Int = 1) {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 180
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                // Falling swoop: 600Hz down to 160Hz
                val freq = 600.0 * (1.0 - 0.73 * progress)
                val envelope = sin(PI * progress) * (1.0 - 0.3 * progress)
                val sample = sin(2.0 * PI * freq * t) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.65).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playWin() {
        if (!isSoundEnabled) return
        scope.launch {
            // Ascending celebratory 4-note chime (C5, E5, G5, C6)
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            val noteDurationMs = 120
            val totalSamples = (sampleRate * (noteDurationMs * notes.size / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)
            val noteSamples = totalSamples / notes.size

            for (noteIdx in notes.indices) {
                val freq = notes[noteIdx]
                val offset = noteIdx * noteSamples
                for (i in 0 until noteSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / noteSamples
                    val envelope = exp(-progress * 3.5)
                    // Bell tone: fundamental + harmonic
                    val sample = (sin(2.0 * PI * freq * t) * 0.7 + sin(4.0 * PI * freq * t) * 0.3) * envelope
                    buffer[offset + i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playLose() {
        if (!isSoundEnabled) return
        scope.launch {
            // Descending sad tone (D4, C#4, C4, B3)
            val notes = doubleArrayOf(293.66, 277.18, 261.63, 246.94)
            val noteDurationMs = 150
            val totalSamples = (sampleRate * (noteDurationMs * notes.size / 1000.0)).toInt()
            val buffer = ShortArray(totalSamples)
            val noteSamples = totalSamples / notes.size

            for (noteIdx in notes.indices) {
                val freq = notes[noteIdx]
                val offset = noteIdx * noteSamples
                for (i in 0 until noteSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / noteSamples
                    val envelope = (1.0 - progress * 0.8)
                    val sample = sin(2.0 * PI * freq * t) * envelope
                    buffer[offset + i] = (sample * Short.MAX_VALUE * 0.6).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playSwap() {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 40
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val freq = 880.0
                val envelope = 1.0 - progress
                val sample = sin(2.0 * PI * freq * t) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.4).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val minBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val finalBytes = if (minBytes > 0) maxOf(minBytes, buffer.size * 2) else buffer.size * 2
            val finalBuffer = if (finalBytes > buffer.size * 2) {
                ShortArray(finalBytes / 2).apply {
                    System.arraycopy(buffer, 0, this, 0, buffer.size)
                }
            } else {
                buffer
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(finalBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(finalBuffer, 0, finalBuffer.size)
            track.play()
            // Release after track finishes
            scope.launch {
                val durationMs = (buffer.size * 1000L) / sampleRate + 50
                kotlinx.coroutines.delay(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
