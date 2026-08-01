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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.ui.theme.AppThemeMode
import dev.voicejournal.ui.navigation.AppNavHost
import dev.voicejournal.ui.theme.AppTheme
import dev.voicejournal.ui.theme.VoiceTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    private var lastStopTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appThemeMode by userPreferencesManager.appThemeMode.collectAsState(initial = AppThemeMode.DARK)
            val isScreenPrivacyEnabled by userPreferencesManager.isScreenPrivacyEnabled.collectAsState(initial = false)
            val appLockModeState = userPreferencesManager.appLockMode.collectAsState(initial = null)
            val appLockTimeout by userPreferencesManager.appLockTimeout.collectAsState(initial = AppLockTimeout.IMMEDIATELY)
            val customPin by userPreferencesManager.customPin.collectAsState(initial = null)

            val appLockMode = appLockModeState.value
            var isAppUnlocked by remember { mutableStateOf(false) }
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
                    if (event == Lifecycle.Event.ON_STOP) {
                        lastStopTimestamp = System.currentTimeMillis()
                    } else if (event == Lifecycle.Event.ON_START) {
                        val currentMode = appLockMode ?: AppLockMode.NONE
                        if (currentMode != AppLockMode.NONE && lastStopTimestamp > 0L) {
                            val elapsed = System.currentTimeMillis() - lastStopTimestamp
                            if (elapsed >= appLockTimeout.timeoutMillis) {
                                isAppUnlocked = false
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            VoiceTheme(themeMode = appThemeMode) {
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
                            CustomPinLockScreen(
                                correctPin = customPin ?: "",
                                onUnlocked = { isAppUnlocked = true }
                            )
                        }
                    }
                } else {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
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
    onUnlocked: () -> Unit
) {
    val colors = AppTheme.colors
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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

        Text(
            text = if (isError) "Incorrect PIN. Try again." else "Enter your 4-digit security PIN",
            color = if (isError) colors.error else colors.textSecondary,
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
                                                onUnlocked()
                                            } else {
                                                isError = true
                                                enteredPin = ""
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
