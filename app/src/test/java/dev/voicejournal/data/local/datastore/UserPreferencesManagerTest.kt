package dev.voicejournal.data.local.datastore

import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import dev.voicejournal.data.security.KeyStoreHelper
import dev.voicejournal.data.security.KeystoreInvalidatedException
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.notedetail.components.isAllowedExternalUrl
import dev.voicejournal.ui.theme.AppThemeMode
import dev.voicejournal.ui.util.AppLockStateManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserPreferencesManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        AppLockStateManager.clearTransientPicker()
    }

    @After
    fun teardown() {
        AppLockStateManager.clearTransientPicker()
        unmockkObject(KeyStoreHelper)
        unmockkStatic(Base64::class)
        unmockkStatic(Log::class)
    }

    private fun createPrefsManager(): UserPreferencesManager {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tmpFolder.newFolder(), "user_prefs_test.preferences_pb") }
        )
        return UserPreferencesManager(dataStore)
    }

    @Test
    fun defaultEnumNames_matchRequirements() {
        assertEquals("WAV_16KHZ", AudioFormat.WAV_16KHZ.name)
        assertEquals("M4A_AAC_128KBPS", AudioFormat.M4A_AAC_128KBPS.name)
        assertEquals("DARK", AppThemeMode.DARK.name)
        assertEquals("LIGHT_PREMIUM", AppThemeMode.LIGHT_PREMIUM.name)
        assertEquals("LAST_DAYS", InsightDateRangeMode.LAST_DAYS.name)
        assertEquals("CURRENT_CALENDAR", InsightDateRangeMode.CURRENT_CALENDAR.name)
        assertEquals("NONE", AppLockMode.NONE.name)
        assertEquals("BIOMETRIC", AppLockMode.BIOMETRIC.name)
        assertEquals("CUSTOM_PIN", AppLockMode.CUSTOM_PIN.name)
        assertEquals("SYSTEM_DEFAULT", TimeFormat.SYSTEM_DEFAULT.name)
        assertEquals("TWELVE_HOUR", TimeFormat.TWELVE_HOUR.name)
        assertEquals("TWENTY_FOUR_HOUR", TimeFormat.TWENTY_FOUR_HOUR.name)
        assertEquals("SYSTEM_DEFAULT", StartOfWeek.SYSTEM_DEFAULT.name)
        assertEquals("MONDAY", StartOfWeek.MONDAY.name)
        assertEquals("SUNDAY", StartOfWeek.SUNDAY.name)
    }

    @Test
    fun appLockConfig_emitsCombinedValuesAtomically() = runBlocking {
        mockkObject(KeyStoreHelper)
        every { KeyStoreHelper.encrypt("4321") } returns "enc_4321"
        every { KeyStoreHelper.decrypt("enc_4321") } returns "4321"

        val manager = createPrefsManager()
        manager.setCustomPin("4321")
        manager.setAppLockMode(AppLockMode.CUSTOM_PIN)
        manager.setAppLockTimeout(AppLockTimeout.AFTER_1_MIN)

        val config = manager.appLockConfig.first()
        assertEquals(AppLockMode.CUSTOM_PIN, config.appLockMode)
        assertEquals(AppLockTimeout.AFTER_1_MIN, config.appLockTimeout)
        assertEquals("4321", config.customPin)
        assertEquals(0, config.pinFailedAttempts)
        assertEquals(0L, config.pinLockoutEndTime)
    }

    @Test
    fun appLockConfig_resetsInconsistentCustomPinStateWhenEncryptedPinMissing() = runBlocking {
        val manager = createPrefsManager()
        manager.setAppLockMode(AppLockMode.CUSTOM_PIN)

        val config = manager.appLockConfig.first()
        assertEquals(AppLockMode.NONE, config.appLockMode)
        assertNull(config.customPin)
        assertEquals(
            SecurityRecoveryReason.RESTORE_INCONSISTENT_SECURITY_STATE,
            manager.securityRecoveryReason.first()
        )
    }

    @Test
    fun appLockConfig_and_customPin_recoverWhenKeystoreInvalidated() = runBlocking {
        mockkObject(KeyStoreHelper)
        every { KeyStoreHelper.encrypt("1234") } returns "enc_corrupted"
        every { KeyStoreHelper.decrypt("enc_corrupted") } throws KeystoreInvalidatedException("corrupted")

        val manager = createPrefsManager()
        manager.setCustomPin("1234")
        manager.setAppLockMode(AppLockMode.CUSTOM_PIN)

        val config = manager.appLockConfig.first()
        assertEquals(AppLockMode.NONE, config.appLockMode)
        assertNull(config.customPin)
        assertEquals(
            SecurityRecoveryReason.KEYSTORE_INVALIDATED,
            manager.securityRecoveryReason.first()
        )

        // Also verify legacy customPin flow resets on KeystoreInvalidatedException
        val manager2 = createPrefsManager()
        manager2.setCustomPin("1234")
        manager2.setAppLockMode(AppLockMode.CUSTOM_PIN)
        assertNull(manager2.customPin.first())
        assertEquals(
            SecurityRecoveryReason.KEYSTORE_INVALIDATED,
            manager2.securityRecoveryReason.first()
        )
    }

    @Test
    fun appLockConfig_resetsWhenEncryptedPinIsBlankInCustomPinMode() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tmpFolder.newFolder(), "blank_pin_test.preferences_pb") }
        )
        dataStore.edit { prefs ->
            prefs[UserPreferencesManager.APP_LOCK_MODE] = AppLockMode.CUSTOM_PIN.name
            prefs[UserPreferencesManager.CUSTOM_PIN_ENCRYPTED] = "   "
        }
        val manager = UserPreferencesManager(dataStore)

        val config = manager.appLockConfig.first()
        assertEquals(AppLockMode.NONE, config.appLockMode)
        assertNull(config.customPin)
        assertEquals(
            SecurityRecoveryReason.RESTORE_INCONSISTENT_SECURITY_STATE,
            manager.securityRecoveryReason.first()
        )
    }

    @Test
    fun keyStoreHelper_decrypt_throwsKeystoreInvalidatedOnCorruptedCiphertext() {
        mockkStatic(Base64::class)
        mockkObject(KeyStoreHelper)
        every { KeyStoreHelper.deleteKey() } returns Unit

        // Blank payload returns null without throwing
        assertNull(KeyStoreHelper.decrypt("   "))

        // Invalid Base64 throws KeystoreInvalidatedException and deletes key
        every { Base64.decode("not_base64!", Base64.DEFAULT) } throws IllegalArgumentException("bad base64")
        try {
            KeyStoreHelper.decrypt("not_base64!")
            fail("Expected KeystoreInvalidatedException on invalid Base64")
        } catch (_: KeystoreInvalidatedException) {
            // expected
        }
        verify(atLeast = 1) { KeyStoreHelper.deleteKey() }

        // Payload <= IV_LENGTH (12 bytes) throws KeystoreInvalidatedException and deletes key
        every { Base64.decode("short_payload", Base64.DEFAULT) } returns ByteArray(12)
        try {
            KeyStoreHelper.decrypt("short_payload")
            fail("Expected KeystoreInvalidatedException on short payload")
        } catch (_: KeystoreInvalidatedException) {
            // expected
        }
        verify(atLeast = 2) { KeyStoreHelper.deleteKey() }
    }

    @Test
    fun appLockStateManager_transientPickerExpiresAndClears() {
        AppLockStateManager.notifySystemPickerLaunched(nowMs = 1_000L)
        assertTrue(AppLockStateManager.consumeTransientPicker(nowMs = 61_000L))
        // Consumed once -> second consume returns false
        assertFalse(AppLockStateManager.consumeTransientPicker(nowMs = 61_500L))

        // Expired after 5 minutes
        AppLockStateManager.notifySystemPickerLaunched(nowMs = 1_000L)
        assertFalse(
            AppLockStateManager.consumeTransientPicker(
                nowMs = 1_000L + AppLockStateManager.TRANSIENT_PICKER_TIMEOUT_MS
            )
        )

        // Explicit clear cancels pending bypass
        AppLockStateManager.notifySystemPickerLaunched(nowMs = 2_000L)
        AppLockStateManager.clearTransientPicker()
        assertFalse(AppLockStateManager.consumeTransientPicker(nowMs = 3_000L))
    }

    @Test
    fun linkActionPopup_isAllowedExternalUrl_validatesHttpAndHttpsOnly() {
        assertTrue(isAllowedExternalUrl("https://example.com"))
        assertTrue(isAllowedExternalUrl("http://example.com/path?query=1#hash"))
        assertTrue(isAllowedExternalUrl("HTTPS://EXAMPLE.COM/CaseInsensitive"))
        assertTrue(isAllowedExternalUrl("  https://voicejournal.dev/docs  "))

        assertFalse(isAllowedExternalUrl("content://media/external/images/1"))
        assertFalse(isAllowedExternalUrl("file:///data/data/dev.voicejournal/secret"))
        assertFalse(isAllowedExternalUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isAllowedExternalUrl("javascript:alert(1)"))
        assertFalse(isAllowedExternalUrl("tel:+1234567890"))
        assertFalse(isAllowedExternalUrl("http://"))
        assertFalse(isAllowedExternalUrl("https:///missing-host"))
        assertFalse(isAllowedExternalUrl("example.com"))
        assertFalse(isAllowedExternalUrl(""))
        assertFalse(isAllowedExternalUrl("   "))
        assertFalse(isAllowedExternalUrl("https://example.com\n"))
    }
}
