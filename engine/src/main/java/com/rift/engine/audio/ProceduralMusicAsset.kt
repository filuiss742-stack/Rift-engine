package com.rift.engine.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Deterministic original world-score renderer. It writes a small PCM WAV on demand,
 * allowing the Android and desktop hosts to use their native decoders without
 * shipping platform-specific codec libraries.
 */
object ProceduralMusicAsset {
    const val SAMPLE_RATE = 22050
    const val DEFAULT_DURATION_SECONDS = 38

    private enum class Waveform { SINE, TRIANGLE, SOFT_SQUARE, NOISE }

    @JvmStatic
    fun write(world: String, output: File, durationSeconds: Int = DEFAULT_DURATION_SECONDS): File {
        require(world in setOf("earth", "mira", "thea")) { "Unknown world soundtrack: $world" }
        require(durationSeconds in 1..180) { "Duration must be between 1 and 180 seconds." }
        output.parentFile?.mkdirs()
        val samples = FloatArray(SAMPLE_RATE * durationSeconds)
        when (world) {
            "earth" -> composeEarth(samples, durationSeconds)
            "mira" -> composeMira(samples, durationSeconds)
            "thea" -> composeThea(samples, durationSeconds)
        }
        writePcmWav(output, samples)
        return output
    }

    private fun addTone(
        target: FloatArray,
        startSeconds: Double,
        durationSeconds: Double,
        frequency: Double,
        amplitude: Double,
        waveform: Waveform = Waveform.SINE,
        attackSeconds: Double = 0.08,
        releaseSeconds: Double = 0.35,
        vibrato: Double = 0.0,
        seed: Long = 0L
    ) {
        val first = max(0, (startSeconds * SAMPLE_RATE).toInt())
        val count = min(target.size - first, (durationSeconds * SAMPLE_RATE).toInt())
        if (count <= 0) return
        var phase = 0.0
        var noiseState = 0.0
        val random = Random(seed)
        for (offset in 0 until count) {
            val t = offset.toDouble() / SAMPLE_RATE
            val remaining = durationSeconds - t
            val envelope = min(1.0, min(
                t / max(attackSeconds, 0.001),
                remaining / max(releaseSeconds, 0.001)
            )).coerceAtLeast(0.0)
            val wobble = 1.0 + vibrato * sin(2.0 * PI * 4.6 * t)
            phase += 2.0 * PI * frequency * wobble / SAMPLE_RATE
            val wave = when (waveform) {
                Waveform.SINE -> sin(phase)
                Waveform.TRIANGLE -> {
                    val cycle = (phase / (2.0 * PI)) - kotlin.math.floor(phase / (2.0 * PI))
                    1.0 - 4.0 * abs(cycle - 0.5)
                }
                Waveform.SOFT_SQUARE -> tanh(2.2 * sin(phase))
                Waveform.NOISE -> {
                    noiseState = 0.72 * noiseState + 0.28 * (random.nextDouble() * 2.0 - 1.0)
                    noiseState
                }
            }
            val index = first + offset
            target[index] += (amplitude * envelope * wave).toFloat()
        }
    }

    private fun addChord(
        target: FloatArray,
        start: Double,
        duration: Double,
        notes: DoubleArray,
        amplitude: Double,
        waveform: Waveform,
        attack: Double,
        release: Double,
        vibrato: Double = 0.0,
        seed: Long = 0L
    ) {
        notes.forEachIndexed { index, frequency ->
            addTone(target, start, duration, frequency, amplitude, waveform,
                attack, release, vibrato, seed + index * 17L)
        }
    }

    private fun composeEarth(out: FloatArray, duration: Int) {
        val chords = arrayOf(
            doubleArrayOf(220.00, 261.63, 329.63),
            doubleArrayOf(196.00, 246.94, 293.66),
            doubleArrayOf(174.61, 220.00, 261.63),
            doubleArrayOf(196.00, 246.94, 293.66)
        )
        var bar = 0
        var start = 0.0
        while (start < duration) {
            val notes = chords[bar % chords.size]
            addChord(out, start, 4.5, notes, 0.075, Waveform.SINE, 1.0, 1.6, seed = bar.toLong())
            addTone(out, start, 3.6, notes[0] / 2.0, 0.10, Waveform.TRIANGLE, 0.2, 1.0)
            for (beat in 0..3) {
                val note = notes[(beat + 1) % 3] * if (beat == 1 || beat == 3) 2.0 else 1.0
                addTone(out, start + beat * 0.92, 0.52, note, 0.045,
                    Waveform.SINE, 0.015, 0.42, seed = bar * 10L + beat)
            }
            bar++
            start += 4.0
        }
        val melody = doubleArrayOf(
            523.25, 493.88, 440.00, 392.00, 440.00, 493.88, 587.33, 523.25,
            493.88, 440.00, 392.00, 349.23, 392.00, 440.00, 493.88, 440.00
        )
        melody.forEachIndexed { index, note ->
            addTone(out, 1.2 + index * 2.1, 1.25, note, 0.045,
                Waveform.SINE, 0.025, 0.8, 0.002, index.toLong())
        }
    }

    private fun composeMira(out: FloatArray, duration: Int) {
        val chords = arrayOf(
            doubleArrayOf(146.83, 174.61, 220.00),
            doubleArrayOf(130.81, 164.81, 196.00),
            doubleArrayOf(110.00, 146.83, 174.61),
            doubleArrayOf(130.81, 164.81, 196.00)
        )
        var bar = 0
        var start = 0.0
        while (start < duration) {
            val notes = chords[bar % chords.size]
            addChord(out, start, 5.0, notes, 0.095, Waveform.TRIANGLE, 1.5, 2.0, 0.003, bar.toLong())
            addTone(out, start, 4.0, notes[0] / 2.0, 0.09, Waveform.SINE, 1.2, 1.4)
            if (bar % 2 == 0) addTone(out, start + 2.0, 1.1, 880.0, 0.018,
                Waveform.SINE, 0.2, 0.8, 0.004)
            bar++
            start += 4.0
        }
        val flute = doubleArrayOf(
            440.00, 523.25, 493.88, 392.00, 349.23, 392.00, 440.00, 0.0,
            392.00, 440.00, 523.25, 587.33, 523.25, 440.00, 392.00, 0.0
        )
        flute.forEachIndexed { index, note ->
            if (note > 0.0) addTone(out, 0.8 + index * 2.15, 1.65, note, 0.055,
                Waveform.SINE, 0.12, 1.1, 0.012, index + 90L)
        }
        var hit = 0
        var time = 0.4
        while (time < duration) {
            addTone(out, time, 0.22, 62.0, 0.075, Waveform.NOISE, 0.002, 0.20, seed = hit + 200L)
            hit++
            time += 4.0
        }
    }

    private fun composeThea(out: FloatArray, duration: Int) {
        addChord(out, 0.0, duration.toDouble(), doubleArrayOf(55.0, 58.27, 82.41),
            0.10, Waveform.SOFT_SQUARE, 2.0, 3.0, 0.008, 310L)
        addChord(out, 0.0, duration.toDouble(), doubleArrayOf(110.0, 116.54),
            0.035, Waveform.TRIANGLE, 3.0, 3.0, 0.014, 410L)
        var pulse = 0
        var start = 0.0
        val bass = doubleArrayOf(41.2, 43.65, 55.0, 61.74)
        while (start < duration) {
            val shifted = start + if (pulse % 3 == 1) 0.18 else 0.0
            addTone(out, shifted, 0.72, bass[pulse % bass.size], 0.13,
                Waveform.SOFT_SQUARE, 0.015, 0.55, 0.025, pulse + 500L)
            if (pulse % 3 == 0) addTone(out, shifted + 0.28, 0.38,
                880.0 + 13.0 * pulse, 0.018, Waveform.NOISE, 0.005, 0.32, seed = pulse + 600L)
            pulse++
            start += 1.5
        }
    }

    private fun writePcmWav(output: File, samples: FloatArray) {
        val peak = samples.maxOfOrNull { abs(it.toDouble()) } ?: 0.0
        val gain = 0.82 / max(peak, 0.82)
        val dataBytes = samples.size * 2
        BufferedOutputStream(FileOutputStream(output)).use { stream ->
            stream.write(byteArrayOf(
                'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte()
            ))
            writeIntLe(stream, 36 + dataBytes)
            stream.write(byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()))
            stream.write(byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte()))
            writeIntLe(stream, 16)
            writeShortLe(stream, 1)
            writeShortLe(stream, 1)
            writeIntLe(stream, SAMPLE_RATE)
            writeIntLe(stream, SAMPLE_RATE * 2)
            writeShortLe(stream, 2)
            writeShortLe(stream, 16)
            stream.write(byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()))
            writeIntLe(stream, dataBytes)
            val block = ByteArray(8192)
            var cursor = 0
            while (cursor < samples.size) {
                var bytes = 0
                while (bytes + 1 < block.size && cursor < samples.size) {
                    val limited = tanh(samples[cursor].toDouble() * gain * 1.35) / tanh(1.35)
                    val pcm = (limited.coerceIn(-1.0, 1.0) * 32767.0).toInt()
                    block[bytes++] = (pcm and 0xFF).toByte()
                    block[bytes++] = ((pcm ushr 8) and 0xFF).toByte()
                    cursor++
                }
                stream.write(block, 0, bytes)
            }
        }
    }

    private fun writeIntLe(output: java.io.OutputStream, value: Int) {
        output.write(value and 0xFF)
        output.write((value ushr 8) and 0xFF)
        output.write((value ushr 16) and 0xFF)
        output.write((value ushr 24) and 0xFF)
    }

    private fun writeShortLe(output: java.io.OutputStream, value: Int) {
        output.write(value and 0xFF)
        output.write((value ushr 8) and 0xFF)
    }
}
