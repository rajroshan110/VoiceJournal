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

    fun start(outputFile: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 16000, 1)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()

        mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        trackIndex = -1
        muxerStarted = false
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
                codec.queueInputBuffer(inputBufferIndex, 0, chunkSize, System.nanoTime() / 1000, 0)
                offset += chunkSize
            }
            drainCodec(false)
        }
    }

    fun finish() {
        val codec = mediaCodec ?: return
        val inputBufferIndex = codec.dequeueInputBuffer(10000)
        if (inputBufferIndex >= 0) {
            codec.queueInputBuffer(inputBufferIndex, 0, 0, System.nanoTime() / 1000, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainCodec(true)
        
        codec.stop()
        codec.release()
        mediaCodec = null

        if (muxerStarted) {
            mediaMuxer?.stop()
        }
        mediaMuxer?.release()
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
