package dev.voicejournal.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AppLockTimeout
import dev.voicejournal.ui.settings.SettingsUiState
import dev.voicejournal.ui.settings.SettingsViewModel
import dev.voicejournal.ui.designsystem.theme.AppTheme

private val FingerprintIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Fingerprint",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(17.81f, 4.47f)
            curveTo(17.73f, 4.47f, 17.65f, 4.45f, 17.58f, 4.41f)
            curveTo(15.66f, 3.42f, 13.88f, 3.01f, 11.96f, 3.01f)
            curveTo(10.05f, 3.01f, 8.26f, 3.43f, 6.36f, 4.41f)
            curveTo(6.05f, 4.57f, 5.68f, 4.44f, 5.52f, 4.13f)
            curveTo(5.36f, 3.82f, 5.49f, 3.45f, 5.8f, 3.29f)
            curveTo(7.91f, 2.2f, 9.88f, 1.74f, 11.96f, 1.74f)
            curveTo(14.05f, 1.74f, 16.03f, 2.2f, 18.14f, 3.29f)
            curveTo(18.45f, 3.45f, 18.57f, 3.83f, 18.41f, 4.13f)
            curveTo(18.28f, 4.35f, 18.05f, 4.47f, 17.81f, 4.47f)
            close()
            moveTo(3.64f, 7.62f)
            curveTo(3.52f, 7.62f, 3.4f, 7.58f, 3.3f, 7.51f)
            curveTo(3.01f, 7.31f, 2.94f, 6.91f, 3.14f, 6.62f)
            curveTo(5.27f, 3.48f, 8.39f, 1.63f, 12f, 1.63f)
            curveTo(15.61f, 1.63f, 18.73f, 3.48f, 20.86f, 6.62f)
            curveTo(21.06f, 6.91f, 20.99f, 7.31f, 20.7f, 7.51f)
            curveTo(20.41f, 7.71f, 20.01f, 7.64f, 19.81f, 7.35f)
            curveTo(17.91f, 4.54f, 15.13f, 2.9f, 12f, 2.9f)
            curveTo(8.87f, 2.9f, 6.09f, 4.54f, 4.19f, 7.35f)
            curveTo(4.07f, 7.53f, 3.86f, 7.62f, 3.64f, 7.62f)
            close()
            moveTo(9.5f, 22f)
            curveTo(9.22f, 22f, 9f, 21.78f, 9f, 21.5f)
            verticalLineTo(16.5f)
            curveTo(9f, 14.85f, 10.35f, 13.5f, 12f, 13.5f)
            curveTo(13.65f, 13.5f, 15f, 14.85f, 15f, 16.5f)
            verticalLineTo(21.5f)
            curveTo(15f, 21.78f, 14.78f, 22f, 14.5f, 22f)
            curveTo(14.22f, 22f, 14f, 21.78f, 14f, 21.5f)
            verticalLineTo(16.5f)
            curveTo(14f, 15.4f, 13.1f, 14.5f, 12f, 14.5f)
            curveTo(10.9f, 14.5f, 10f, 15.4f, 10f, 16.5f)
            verticalLineTo(21.5f)
            curveTo(10f, 21.78f, 9.78f, 22f, 9.5f, 22f)
            close()
        }
    }.build()
}

private val NoLockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NoLock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 17f)
            curveTo(13.1f, 17f, 14f, 16.1f, 14f, 15f)
            curveTo(14f, 13.9f, 13.1f, 13f, 12f, 13f)
            curveTo(10.9f, 13f, 10f, 13.9f, 10f, 15f)
            curveTo(10f, 16.1f, 10.9f, 17f, 12f, 17f)
            close()
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            horizontalLineTo(9f)
            curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f)
            curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
            moveTo(18f, 20f)
            horizontalLineTo(6f)
            verticalLineTo(10f)
            horizontalLineTo(18f)
            verticalLineTo(20f)
            close()
        }
    }.build()
}

@Composable
fun AppLockDetailScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Back Icon Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(42.dp)
                .background(colors.surface, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title & Subtitle
        Text(
            text = "Lock your journal",
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Keep your journal private by adding an extra layer of security",
            color = colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: Ways to lock
        Text(
            text = "Ways to lock",
            color = colors.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 1: Same as screen lock (Biometric)
        AppLockOptionCard(
            title = "Same as screen lock",
            icon = {
                Icon(
                    imageVector = FingerprintIcon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            },
            isSelected = uiState.appLockMode == AppLockMode.BIOMETRIC,
            onClick = { viewModel.setAppLockMode(AppLockMode.BIOMETRIC) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: Custom PIN
        AppLockOptionCard(
            title = "Custom PIN",
            icon = {
                Text(
                    text = "***",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            },
            isSelected = uiState.appLockMode == AppLockMode.CUSTOM_PIN,
            onClick = { viewModel.setAppLockMode(AppLockMode.CUSTOM_PIN) },
            trailingAction = if (uiState.appLockMode == AppLockMode.CUSTOM_PIN) {
                {
                    TextButton(
                        onClick = { viewModel.modifyCustomPin() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modify PIN",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Modify", fontSize = 13.sp)
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Option 3: No lock
        AppLockOptionCard(
            title = "No lock",
            icon = {
                Icon(
                    imageVector = NoLockIcon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            },
            isSelected = uiState.appLockMode == AppLockMode.NONE,
            onClick = { viewModel.setAppLockMode(AppLockMode.NONE) }
        )

        // Section 2: Enforce lock timeout
        if (uiState.appLockMode != AppLockMode.NONE) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enforce lock",
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppLockTimeout.entries.forEach { timeout ->
                AppLockOptionCard(
                    title = timeout.displayName,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    isSelected = uiState.appLockTimeout == timeout,
                    onClick = { viewModel.setAppLockTimeout(timeout) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Important Warning Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Important",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If you forget your Custom PIN, you will lose access to your journal. There is no recovery option.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AppLockOptionCard(
    title: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (trailingAction != null) {
                trailingAction()
                Spacer(modifier = Modifier.width(8.dp))
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
            )
        }
    }
}
