package dev.voicejournal

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.ui.theme.AppThemeMode
import dev.voicejournal.ui.navigation.AppNavHost
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.theme.VoiceTheme
import javax.inject.Inject

import android.content.Intent

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    private var lastStopTimestamp: Long = 0L
    private var openEntryIdState = mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val appThemeMode by userPreferencesManager.appThemeMode.collectAsState(initial = AppThemeMode.DARK)
            val isScreenPrivacyEnabled by userPreferencesManager.isScreenPrivacyEnabled.collectAsState(initial = false)
            val appLockModeState = userPreferencesManager.appLockMode.collectAsState(initial = null)
            val appLockTimeout by userPreferencesManager.appLockTimeout.collectAsState(initial = AppLockTimeout.IMMEDIATELY)
            val customPin by userPreferencesManager.customPin.collectAsState(initial = null)
            val securityRecoveryReason by userPreferencesManager.securityRecoveryReason.collectAsState(initial = SecurityRecoveryReason.NONE)
            val scope = rememberCoroutineScope()

            val appLockMode = appLockModeState.value
            var isAppUnlocked by rememberSaveable { mutableStateOf(false) }
            var isInitialCheckDone by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current

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
                    if (currentMode != AppLockMode.NONE) {
                        isAppUnlocked = false
                    } else {
                        isAppUnlocked = true
                    }
                } else if (currentMode == AppLockMode.NONE) {
                    isAppUnlocked = true
                }
            }

            DisposableEffect(lifecycleOwner, appLockMode, appLockTimeout) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        lastStopTimestamp = android.os.SystemClock.elapsedRealtime()
                    } else if (event == Lifecycle.Event.ON_RESUME) {
                        val currentMode = appLockMode ?: AppLockMode.NONE
                        if (currentMode != AppLockMode.NONE && lastStopTimestamp > 0L) {
                            val elapsed = android.os.SystemClock.elapsedRealtime() - lastStopTimestamp
                            if (elapsed >= appLockTimeout.timeoutMillis) {
                                isAppUnlocked = false
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

                val activeMode = appLockMode ?: AppLockMode.NONE
                if (activeMode != AppLockMode.NONE && !isAppUnlocked) {
                    // Lock Screen Overlay
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.background
                    ) {
                        if (activeMode == AppLockMode.BIOMETRIC) {
                            BiometricLockScreen(
                                onAuthenticateClick = { triggerBiometricAuth { isAppUnlocked = true } },
                                onAutoAuthNeeded = { triggerBiometricAuth { isAppUnlocked = true } }
                            )
                        } else if (activeMode == AppLockMode.CUSTOM_PIN) {
                            val pinFailedAttempts by userPreferencesManager.pinFailedAttempts.collectAsState(initial = 0)
                            val pinLockoutEndTime by userPreferencesManager.pinLockoutEndTime.collectAsState(initial = 0L)
                            
                            CustomPinLockScreen(
                                correctPin = customPin ?: "",
                                failedAttempts = pinFailedAttempts,
                                lockoutEndTime = pinLockoutEndTime,
                                onUnlocked = { verifiedPin ->
                                    lifecycleScope.launch {
                                        userPreferencesManager.clearFailedPinAttempts()
                                        userPreferencesManager.migratePinIfNeeded(verifiedPin)
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

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val isFromNotification = intent.getBooleanExtra("from_notification", false) ||
                intent.action == "dev.voicejournal.action.NOTIFICATION_CLICK" ||
                intent.action?.startsWith("dev.voicejournal.action.OPEN_NOTE_") == true

        val idFromExtra = intent.getLongExtra("open_entry_id", -1L)
        val activePlayerEntryId = (dev.voicejournal.audio.AudioPlayerManager.instance?.playbackState?.value as? dev.voicejournal.audio.PlayerState.Playing)?.entryId
            ?: (dev.voicejournal.audio.AudioPlayerManager.instance?.playbackState?.value as? dev.voicejournal.audio.PlayerState.Paused)?.entryId
            ?: dev.voicejournal.audio.AudioPlayerManager.instance?.currentEntryId
            ?: -1L

        val targetId = if (idFromExtra != -1L) idFromExtra else activePlayerEntryId

        if (isFromNotification && targetId != -1L) {
            openEntryIdState.longValue = targetId
        } else if (idFromExtra != -1L) {
            openEntryIdState.longValue = idFromExtra
        }
    }

    private fun triggerBiometricAuth(onUnlocked: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onUnlocked()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Voice Journal")
            .setSubtitle("Use your biometric credential to access your journal")
            .setNegativeButtonText("Cancel")
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun BiometricLockScreen(
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
            text = "Authenticate to access your private journal notes.",
            color = colors.textSecondary,
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
