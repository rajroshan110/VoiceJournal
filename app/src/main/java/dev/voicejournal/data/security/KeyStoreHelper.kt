package dev.voicejournal.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import dev.voicejournal.BuildConfig
import java.security.InvalidKeyException
import java.security.UnrecoverableKeyException
import java.security.GeneralSecurityException
import android.util.Log

class KeystoreInvalidatedException(message: String, cause: Throwable? = null) : Exception(message, cause)

object KeyStoreHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "voicejournal_pin_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128

    // Debug Simulation Hook
    var simulateKeystoreInvalidation = false

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.i("Security", "Keystore key deleted due to invalidation")
            }
        } catch (e: Exception) {
            Log.e("Security", "Failed to delete Keystore key", e)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            entry.secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Return IV + CipherText as a single Base64 string
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String): String? {
        if (BuildConfig.DEBUG && simulateKeystoreInvalidation) {
            deleteKey()
            throw KeystoreInvalidatedException("Simulated Keystore Invalidation")
        }
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            if (combined.size <= IV_LENGTH) return null
            
            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            
            val cipherText = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e("Security", "Keystore key permanently invalidated", e)
            deleteKey()
            throw KeystoreInvalidatedException("Key permanently invalidated", e)
        } catch (e: UnrecoverableKeyException) {
            Log.e("Security", "Keystore key unrecoverable", e)
            deleteKey()
            throw KeystoreInvalidatedException("Key unrecoverable", e)
        } catch (e: InvalidKeyException) {
            Log.e("Security", "Keystore key invalid", e)
            deleteKey()
            throw KeystoreInvalidatedException("Invalid key", e)
        } catch (e: GeneralSecurityException) {
            Log.e("Security", "General security exception during decryption", e)
            deleteKey()
            throw KeystoreInvalidatedException("General security exception", e)
        } catch (e: Exception) {
            Log.e("Security", "Unknown exception during decryption", e)
            null
        }
    }
}
