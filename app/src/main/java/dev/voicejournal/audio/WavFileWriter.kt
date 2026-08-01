package dev.voicejournal.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFileWriter {
    private var randomAccessFile: RandomAccessFile? = null
    private var payloadSize = 0
    private val sampleRate = 16000
    private val channels = 1
    private val bitsPerSample = 16

    fun start(outputFile: File) {
        randomAccessFile = RandomAccessFile(outputFile, "rw")
        randomAccessFile?.setLength(0)
        writeHeader(0)
        payloadSize = 0
    }

    fun writePcmChunk(buffer: ByteArray, bytesRead: Int) {
        if (bytesRead > 0) {
            randomAccessFile?.write(buffer, 0, bytesRead)
            payloadSize += bytesRead
        }
    }

    fun finish() {
        randomAccessFile?.seek(0)
        writeHeader(payloadSize)
        randomAccessFile?.close()
        randomAccessFile = null
    }

    private fun writeHeader(dataSize: Int) {
        val header = ByteArray(44)
        val byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(36 + dataSize) // Chunk size
        byteBuffer.put("WAVE".toByteArray())

        // fmt sub-chunk
        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16) // Subchunk1Size
        byteBuffer.putShort(1) // AudioFormat (1 = PCM)
        byteBuffer.putShort(channels.toShort()) // NumChannels
        byteBuffer.putInt(sampleRate) // SampleRate
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        byteBuffer.putInt(byteRate) // ByteRate
        val blockAlign = channels * (bitsPerSample / 8)
        byteBuffer.putShort(blockAlign.toShort()) // BlockAlign
        byteBuffer.putShort(bitsPerSample.toShort()) // BitsPerSample

        // data sub-chunk
        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(dataSize) // Subchunk2Size

        randomAccessFile?.write(header)
    }
}
