package dev.voicejournal.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

class M4aEncoder {
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private val bufferInfo = MediaCodec.BufferInfo()
    private var totalBytesAccepted: Long = 0L

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BYTES_PER_SAMPLE = 2 // 16-bit PCM
        private const val BYTES_PER_FRAME = CHANNELS * BYTES_PER_SAMPLE
    }

    fun start(outputFile: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()

        mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        trackIndex = -1
        muxerStarted = false
        totalBytesAccepted = 0L
    }

    fun encodePcmChunk(buffer: ByteArray, bytesRead: Int) {
        val codec = mediaCodec ?: return
        
        var offset = 0
        while (offset < bytesRead) {
            val inputBufferIndex = codec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                
                val remaining = bytesRead - offset
                val capacity = inputBuffer?.capacity() ?: 0
                val chunkSize = minOf(remaining, capacity)
                
                inputBuffer?.put(buffer, offset, chunkSize)
                val presentationTimeUs = (totalBytesAccepted / BYTES_PER_FRAME) * 1_000_000L / SAMPLE_RATE
                codec.queueInputBuffer(inputBufferIndex, 0, chunkSize, presentationTimeUs, 0)
                totalBytesAccepted += chunkSize
                offset += chunkSize
            }
            drainCodec(false)
        }
    }

    fun finish() {
        val codec = mediaCodec ?: return
        var queuedEos = false
        var attempts = 0
        while (!queuedEos && attempts < 10) {
            val inputBufferIndex = codec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val presentationTimeUs = (totalBytesAccepted / BYTES_PER_FRAME) * 1_000_000L / SAMPLE_RATE
                codec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                queuedEos = true
            } else {
                drainCodec(false)
                attempts++
            }
        }
        drainCodec(true)
        
        try {
            codec.stop()
        } catch (e: Exception) {
            // Ignore error if already stopped
        }
        codec.release()
        mediaCodec = null

        if (muxerStarted) {
            try {
                mediaMuxer?.stop()
            } catch (e: Exception) {
                // Ignore error if muxer had no valid tracks
            }
        }
        try {
            mediaMuxer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaMuxer = null
    }

    private fun drainCodec(endOfStream: Boolean) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return

        var retryCount = 0
        while (true) {
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break
                } else {
                    retryCount++
                    if (retryCount > 50) {
                        break // Safety limit to prevent infinite loop
                    }
                    continue
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) throw IllegalStateException("format changed twice")
                val newFormat = codec.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
            } else if (encoderStatus >= 0) {
                val encodedData = codec.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    if (!muxerStarted) throw RuntimeException("muxer hasn't started")
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }

                codec.releaseOutputBuffer(encoderStatus, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            }
        }
    }
}
