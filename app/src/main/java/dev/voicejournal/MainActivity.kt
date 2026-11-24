package dev.voicejournal

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.datastore.SecurityRecoveryReason
import androidx.compose.runtime.rememberCoroutineScope
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.ui.theme.AppThemeMode
import dev.voicejournal.ui.navigation.AppNavHost
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.theme.VoiceTheme
import dev.voicejournal.audio.AudioPlayerManager
import javax.inject.Inject

import android.content.Intent

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    private var lastStopTimestamp: Long = 0L
    private var openEntryIdState = mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastStopTimestamp = savedInstanceState?.getLong("last_stop_timestamp", 0L) ?: 0L
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val appThemeMode by userPreferencesManager.appThemeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val isScreenPrivacyEnabled by userPreferencesManager.isScreenPrivacyEnabled.collectAsState(initial = false)
            val appLockConfig by userPreferencesManager.appLockConfig.collectAsState(initial = null)
            val securityRecoveryReason by userPreferencesManager.securityRecoveryReason.collectAsState(initial = SecurityRecoveryReason.NONE)
            val scope = rememberCoroutineScope()

            val currentConfig = appLockConfig
            val appLockMode = currentConfig?.appLockMode
            var isAppUnlocked by rememberSaveable { mutableStateOf(false) }
            var isInitialCheckDone by rememberSaveable { mutableStateOf(false) }
            var biometricErrorMessage by remember { mutableStateOf<String?>(null) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val latestConfig by rememberUpdatedState(currentConfig)

            LaunchedEffect(isScreenPrivacyEnabled) {
                if (isScreenPrivacyEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(appLockMode) {
                val currentMode = appLockMode ?: return@LaunchedEffect
                if (!isInitialCheckDone) {
                    isInitialCheckDone = true
                    isAppUnlocked = (currentMode == AppLockMode.NONE)
                } else if (currentMode == AppLockMode.NONE) {
                    isAppUnlocked = true
                    biometricErrorMessage = null
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (!isChangingConfigurations) {
                            lastStopTimestamp = android.os.SystemClock.elapsedRealtime()
                        }
                    } else if (event == Lifecycle.Event.ON_START) {
                        val config = latestConfig
                        val currentMode = config?.appLockMode ?: AppLockMode.NONE
                        val timeout = config?.appLockTimeout ?: AppLockTimeout.IMMEDIATELY
                        val wasPicker = dev.voicejournal.ui.util.AppLockStateManager.consumeTransientPicker()
                        if (currentMode != AppLockMode.NONE && lastStopTimestamp > 0L) {
                            val elapsed = android.os.SystemClock.elapsedRealtime() - lastStopTimestamp
                            if (!wasPicker && elapsed >= timeout.timeoutMillis) {
                                isAppUnlocked = false
                                biometricErrorMessage = null
                            }
                            lastStopTimestamp = 0L
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            VoiceTheme(themeMode = appThemeMode) {
                if (securityRecoveryReason != SecurityRecoveryReason.NONE) {
                    AlertDialog(
                        onDismissRequest = { 
                            scope.launch { userPreferencesManager.clearSecurityRecoveryReason() }
                        },
                        title = { Text("Security Reset") },
                        text = { 
                            val message = when(securityRecoveryReason) {
                                SecurityRecoveryReason.KEYSTORE_INVALIDATED -> "Your device's biometric or secure lock screen settings changed. For your protection, your app lock has been reset."
                                SecurityRecoveryReason.RESTORE_INCONSISTENT_SECURITY_STATE -> "The restored backup contained an invalid security configuration. App Lock has been reset to protect your data."
                                else -> "A security error occurred and your app lock has been reset."
                            }
                            Text(message)
                        },
                        confirmButton = {
                            TextButton(onClick = { 
                                scope.launch { userPreferencesManager.clearSecurityRecoveryReason() }
                            }) {
                                Text("Got it")
                            }
                        }
                    )
                }

                val colors = AppTheme.colors

                if (currentConfig == null) {
                    // Awaiting security & lock preferences on initial cold frame:
                    // Render a clean background surface to prevent layout jumping or premature route mounting
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.background
                    ) {}
                } else if (currentConfig.appLockMode != AppLockMode.NONE && !isAppUnlocked) {
                    // Lock Screen Overlay
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.background
                    ) {
                        if (currentConfig.appLockMode == AppLockMode.BIOMETRIC) {
                            BiometricLockScreen(
                                errorMessage = biometricErrorMessage,
                                onAuthenticateClick = {
                                    biometricErrorMessage = null
                                    triggerBiometricAuth(
                                        onUnlocked = {
                                            biometricErrorMessage = null
                                            isAppUnlocked = true
                                        },
                                        onError = { message -> biometricErrorMessage = message }
                                    )
                                },
                                onAutoAuthNeeded = {
                                    triggerBiometricAuth(
                                        onUnlocked = {
                                            biometricErrorMessage = null
                                            isAppUnlocked = true
                                        },
                                        onError = { message -> biometricErrorMessage = message }
                                    )
                                }
                            )
                        } else if (currentConfig.appLockMode == AppLockMode.CUSTOM_PIN) {
                            val correctPin = currentConfig.customPin
                            if (!correctPin.isNullOrBlank()) {
                                CustomPinLockScreen(
                                    correctPin = correctPin,
                                    failedAttempts = currentConfig.pinFailedAttempts,
                                    lockoutEndTime = currentConfig.pinLockoutEndTime,
                                    onUnlocked = { _ ->
                                        lifecycleScope.launch {
                                            userPreferencesManager.clearFailedPinAttempts()
                                        }
                                        isAppUnlocked = true
                                    },
                                    onFailedAttempt = {
                                        lifecycleScope.launch {
                                            userPreferencesManager.registerFailedPinAttempt()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    val navController = rememberNavController()
                    val targetEntryId = openEntryIdState.longValue
                    AppNavHost(
                        navController = navController,
                        openEntryId = targetEntryId,
                        onEntryNavigated = { openEntryIdState.longValue = -1L },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("last_stop_timestamp", lastStopTimestamp)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val isFromNotification = intent.getBooleanExtra("from_notification", false) ||
                intent.action == "dev.voicejournal.action.NOTIFICATION_CLICK" ||
                intent.action?.startsWith("dev.voicejournal.action.OPEN_NOTE_") == true

        val idFromExtra = intent.getLongExtra("open_entry_id", -1L)
        val activePlayerEntryId = (audioPlayerManager.playbackState.value as? dev.voicejournal.audio.PlayerState.Playing)?.entryId
            ?: (audioPlayerManager.playbackState.value as? dev.voicejournal.audio.PlayerState.Paused)?.entryId
            ?: audioPlayerManager.currentEntryId
            ?: -1L

        val targetId = if (idFromExtra != -1L) idFromExtra else activePlayerEntryId

        if (isFromNotification && targetId != -1L) {
            openEntryIdState.longValue = targetId
        } else if (idFromExtra != -1L) {
            openEntryIdState.longValue = idFromExtra
        }
    }

    private fun triggerBiometricAuth(
        onUnlocked: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onError(null)
                onUnlocked()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w("BiometricAuth", "Biometric authentication error: code=$errorCode")
                val isUserCancel = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                if (!isUserCancel) {
                    onError(errString.toString().ifBlank { "Biometric authentication unavailable. Please try again." })
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w("BiometricAuth", "Biometric authentication failed: credential not recognized")
                onError("Authentication not recognized. Please try again.")
            }
        })

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Voice Journal")
            .setSubtitle("Use your biometric or device credential to access your journal")
            .setAllowedAuthenticators(authenticators)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e("BiometricAuth", "Failed to launch biometric prompt", e)
            onError("Unable to start biometric authentication.")
        }
    }
}

@Composable
fun BiometricLockScreen(
    errorMessage: String? = null,
    onAuthenticateClick: () -> Unit,
    onAutoAuthNeeded: () -> Unit
) {
    val colors = AppTheme.colors
    LaunchedEffect(Unit) {
        onAutoAuthNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = colors.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Voice Journal Locked",
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage ?: "Authenticate to access your private journal notes.",
            color = if (errorMessage != null) colors.error else colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAuthenticateClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
        ) {
            Text("Unlock with Biometrics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CustomPinLockScreen(
    correctPin: String,
    failedAttempts: Int,
    lockoutEndTime: Long,
    onUnlocked: (String) -> Unit,
    onFailedAttempt: () -> Unit
) {
    val colors = AppTheme.colors
    var enteredPin by rememberSaveable { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var remainingLockout by remember { mutableStateOf(0L) }

    LaunchedEffect(lockoutEndTime) {
        while (lockoutEndTime > System.currentTimeMillis()) {
            remainingLockout = lockoutEndTime - System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
        remainingLockout = 0L
    }

    val isLockedOut = remainingLockout > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = if (isError) colors.error else colors.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enter Custom PIN",
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val attemptsLeft = 5 - failedAttempts
        Text(
            text = if (isLockedOut) "Too many attempts. Try again in ${remainingLockout / 1000}s" 
                   else if (isError) "Incorrect PIN. $attemptsLeft attempts left." 
                   else if (failedAttempts > 0) "$attemptsLeft attempts left."
                   else "Enter your 4-digit security PIN",
            color = if (isError || isLockedOut) colors.error else colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (filled) (if (isError) colors.error else colors.primary) else colors.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Custom Numeric Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else {
                            Surface(
                                onClick = {
                                    if (isLockedOut) return@Surface
                                    
                                    if (key == "⌫") {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            isError = false
                                        }
                                    } else if (enteredPin.length < 4) {
                                        enteredPin += key
                                        isError = false
                                        if (enteredPin.length == 4) {
                                            if (enteredPin == correctPin) {
                                                onUnlocked(enteredPin)
                                            } else {
                                                isError = true
                                                enteredPin = ""
                                                onFailedAttempt()
                                            }
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = colors.surface,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
