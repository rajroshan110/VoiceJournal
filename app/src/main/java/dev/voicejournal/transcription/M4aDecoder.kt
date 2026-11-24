package dev.voicejournal.transcription

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import dev.voicejournal.BuildConfig
import java.io.File
import java.nio.ByteOrder

object M4aDecoder {
    private const val TAG = "WhisperProfile"

    private class ResizableFloatArray(initialCapacity: Int = 262144) {
        var data = FloatArray(initialCapacity.coerceAtLeast(1024))
        var size = 0
            private set

        fun add(element: Float) {
            if (size == data.size) {
                data = data.copyOf((data.size * 2).coerceAtLeast(size + 1))
            }
            data[size++] = element
        }

        fun toFloatArray(): FloatArray = data.copyOf(size)
    }

    fun decodeM4aToFloatArray(file: File): FloatArray {
        val start = System.currentTimeMillis()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                throw IllegalArgumentException("No audio track found in file")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("No mime type")

            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            val initialCapacity = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                val durationUs = format.getLong(MediaFormat.KEY_DURATION)
                ((durationUs * sampleRate) / 1_000_000L).toInt().coerceIn(4096, 8_000_000)
            } else {
                262144
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val outputFloatBuffer = ResizableFloatArray(initialCapacity)
            var isEOS = false

            while (true) {
                if (!isEOS) {
                    val inIndex = codec.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)
                        val sampleSize = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outIndex >= 0) {
                    val endOfStream = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    if (info.size > 0) {
                        val outBuffer = codec.getOutputBuffer(outIndex)
                        if (outBuffer != null) {
                            outBuffer.position(info.offset)
                            outBuffer.limit(info.offset + info.size)
                            outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                            if (channels == 2) {
                                while (outBuffer.remaining() >= 4) {
                                    val left = outBuffer.short.toFloat() / 32768.0f
                                    val right = outBuffer.short.toFloat() / 32768.0f
                                    outputFloatBuffer.add((left + right) / 2.0f)
                                }
                            } else if (channels > 2) {
                                val frameBytes = channels * 2
                                val invChannels = 1.0f / channels.toFloat()
                                while (outBuffer.remaining() >= frameBytes) {
                                    var sum = 0.0f
                                    for (ch in 0 until channels) {
                                        sum += outBuffer.short.toFloat() / 32768.0f
                                    }
                                    outputFloatBuffer.add(sum * invChannels)
                                }
                            } else {
                                while (outBuffer.remaining() >= 2) {
                                    val sample = outBuffer.short
                                    outputFloatBuffer.add(sample.toFloat() / 32768.0f)
                                }
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (endOfStream) {
                        break
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFormat = codec.outputFormat
                    if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }

            val floatArray = outputFloatBuffer.toFloatArray()
            val result = AudioResampler.resampleTo16kHz(floatArray, sampleRate)
            val duration = System.currentTimeMillis() - start
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "[Pipeline Profiler] Audio Decoding (M4A MediaCodec): ${duration}ms (Decoded ${result.size} samples at 16kHz)")
            }
            return result

        } finally {
            codec?.stop()
            codec?.release()
            extractor.release()
        }
    }
}
