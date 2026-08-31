package dev.voicejournal.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.settings.components.CategoryNavCard
import dev.voicejournal.ui.settings.components.SettingSearchRow
import dev.voicejournal.ui.components.getTagIcon
import dev.voicejournal.ui.settings.model.SettingsSubScreen
import dev.voicejournal.ui.settings.screens.GeneralSettingsScreen
import dev.voicejournal.ui.settings.screens.LocalBackupScreen
import dev.voicejournal.ui.settings.screens.PrivacySecurityScreen
import dev.voicejournal.ui.settings.screens.TagOrganiserScreen
import dev.voicejournal.ui.designsystem.theme.AppTheme

private val GitHubIcon: ImageVector = ImageVector.Builder(
    name = "GitHub",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.477f, 2f, 2f, 6.484f, 2f, 12.017f)
        curveToRelative(0f, 4.425f, 2.865f, 8.18f, 6.839f, 9.504f)
        curveToRelative(0.5f, 0.092f, 0.682f, -0.217f, 0.682f, -0.483f)
        curveToRelative(0f, -0.237f, -0.008f, -0.868f, -0.013f, -1.703f)
        curveToRelative(-2.782f, 0.605f, -3.369f, -1.343f, -3.369f, -1.343f)
        curveToRelative(-0.454f, -1.158f, -1.11f, -1.466f, -1.11f, -1.466f)
        curveToRelative(-0.908f, -0.62f, 0.069f, -0.608f, 0.069f, -0.608f)
        curveToRelative(1.003f, 0.07f, 1.53f, 1.032f, 1.53f, 1.032f)
        curveToRelative(0.892f, 1.53f, 2.341f, 1.088f, 2.91f, 0.832f)
        curveToRelative(0.092f, -0.647f, 0.35f, -1.088f, 0.636f, -1.338f)
        curveToRelative(-2.22f, -0.253f, -4.555f, -1.113f, -4.555f, -4.951f)
        curveToRelative(0f, -1.093f, 0.39f, -1.988f, 1.029f, -2.688f)
        curveToRelative(-0.103f, -0.253f, -0.446f, -1.272f, 0.098f, -2.65f)
        curveToRelative(0f, 0f, 0.84f, -0.27f, 2.75f, 1.026f)
        curveTo(10.608f, 7.868f, 11.305f, 7.765f, 12f, 7.762f)
        curveToRelative(0.695f, 0.003f, 1.392f, 0.106f, 2.191f, 0.306f)
        curveToRelative(1.909f, -1.296f, 2.747f, -1.027f, 2.747f, -1.027f)
        curveToRelative(0.546f, 1.379f, 0.202f, 2.398f, 0.1f, 2.651f)
        curveToRelative(0.64f, 0.7f, 1.028f, 1.595f, 1.028f, 2.688f)
        curveToRelative(0f, 3.848f, -2.339f, 4.695f, -4.566f, 4.943f)
        curveToRelative(0.359f, 0.309f, 0.678f, 0.92f, 0.678f, 1.855f)
        curveToRelative(0f, 1.338f, -0.012f, 2.419f, -0.012f, 2.747f)
        curveToRelative(0f, 0.268f, 0.18f, 0.58f, 0.688f, 0.482f)
        curveTo(19.138f, 20.194f, 22f, 16.44f, 22f, 12.017f)
        curveTo(22f, 6.484f, 17.522f, 2f, 12f, 2f)
        close()
    }
}.build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    var selectedCompactSubScreen by rememberSaveable { mutableStateOf<SettingsSubScreen?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.activeSubScreen) {
        if (selectedCompactSubScreen != null && selectedCompactSubScreen != uiState.activeSubScreen) {
            selectedCompactSubScreen = uiState.activeSubScreen
        }
    }

    val isSearchActive = isSearchFocused || uiState.isSearching || uiState.searchQuery.isNotEmpty()

    // System Back Button Interceptor
    BackHandler(enabled = isSearchActive || uiState.isViewingAppLockDetail || (!isExpanded && selectedCompactSubScreen != null)) {
        if (isSearchActive) {
            focusManager.clearFocus()
            viewModel.clearSearch()
            isSearchFocused = false
        } else if (uiState.isViewingAppLockDetail) {
            viewModel.setViewingAppLockDetail(false)
        } else if (!isExpanded && selectedCompactSubScreen != null) {
            selectedCompactSubScreen = null
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            if (!uiState.isViewingAppLockDetail) {
                if (isSearchActive) {
                    Surface(
                        color = colors.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                placeholder = "Search settings...",
                                showLeadingIcon = false,
                                requestFocusOnLaunch = true,
                                onClose = {
                                    focusManager.clearFocus()
                                    viewModel.clearSearch()
                                    isSearchFocused = false
                                },
                                onFocusChanged = { isSearchFocused = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = when {
                                    !isExpanded && selectedCompactSubScreen == SettingsSubScreen.GENERAL -> "General Settings"
                                    !isExpanded && selectedCompactSubScreen == SettingsSubScreen.PRIVACY_SECURITY -> "Privacy & Security"
                                    !isExpanded && selectedCompactSubScreen == SettingsSubScreen.SYNC_BACKUP -> "Sync & Backup"
                                    !isExpanded && selectedCompactSubScreen == SettingsSubScreen.TAG_ORGANISER -> "Tag Organiser"
                                    else -> "Settings"
                                },
                                color = colors.textPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (!isExpanded && selectedCompactSubScreen != null) {
                                    selectedCompactSubScreen = null
                                } else {
                                    onBackClick()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }
                        },
                        actions = {
                            if (selectedCompactSubScreen == null || isExpanded) {
                                IconButton(
                                    onClick = { isSearchFocused = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search settings",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(if (uiState.isViewingAppLockDetail) PaddingValues(0.dp) else paddingValues)
                .background(colors.background)
        ) {

            // Main Content Area
            if (uiState.isSearching) {
                // Search Mode Active
                if (uiState.searchResults.isEmpty()) {
                    // Empty Search Results State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No settings found for \"${uiState.searchQuery}\"",
                            color = colors.textSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.clearSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Clear Search")
                        }
                    }
                } else {
                    // Filtered Search List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.searchResults, key = { it.id }) { item ->
                            SettingSearchRow(
                                item = item,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.clearSearch()
                                    isSearchFocused = false
                                    viewModel.setActiveSubScreen(item.targetSubScreen)
                                    selectedCompactSubScreen = item.targetSubScreen
                                }
                            )
                        }
                    }
                }
            } else if (isExpanded) {
                // Master-Detail Layout (Medium / Expanded Windows)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Left Pane (35% Width): Dashboard Category Navigation List
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                    ) {
                        val minHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minHeight)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryNavCard(
                                title = "General Settings",
                                subtitle = "Theme, time format, editor, audio quality",
                                icon = Icons.Default.Settings,
                                isSelected = uiState.activeSubScreen == SettingsSubScreen.GENERAL,
                                onClick = {
                                    viewModel.setActiveSubScreen(SettingsSubScreen.GENERAL)
                                    selectedCompactSubScreen = SettingsSubScreen.GENERAL
                                }
                            )

                            CategoryNavCard(
                                title = "Privacy & Security",
                                subtitle = "App lock, biometrics, screen privacy",
                                icon = Icons.Default.Lock,
                                isSelected = uiState.activeSubScreen == SettingsSubScreen.PRIVACY_SECURITY,
                                onClick = {
                                    viewModel.setActiveSubScreen(SettingsSubScreen.PRIVACY_SECURITY)
                                    selectedCompactSubScreen = SettingsSubScreen.PRIVACY_SECURITY
                                }
                            )

                            CategoryNavCard(
                                title = "Sync & Backup",
                                subtitle = "Local backup archive, export, import",
                                icon = Icons.Default.Share,
                                isSelected = uiState.activeSubScreen == SettingsSubScreen.SYNC_BACKUP,
                                onClick = {
                                    viewModel.setActiveSubScreen(SettingsSubScreen.SYNC_BACKUP)
                                    selectedCompactSubScreen = SettingsSubScreen.SYNC_BACKUP
                                }
                            )

                            CategoryNavCard(
                                title = "Tag Organiser",
                                subtitle = "Choose tags, folders, organise notes",
                                icon = getTagIcon(colors.textPrimary),
                                isSelected = uiState.activeSubScreen == SettingsSubScreen.TAG_ORGANISER,
                                onClick = {
                                    viewModel.setActiveSubScreen(SettingsSubScreen.TAG_ORGANISER)
                                    selectedCompactSubScreen = SettingsSubScreen.TAG_ORGANISER
                                }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            SettingsFooter()

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Pane (65% Width): Active Sub-Screen
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                            .background(colors.surfaceVariant, shape = RoundedCornerShape(16.dp))
                    ) {
                        when (uiState.activeSubScreen) {
                            SettingsSubScreen.GENERAL -> Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                GeneralSettingsScreen(uiState = uiState, viewModel = viewModel)
                            }
                            SettingsSubScreen.PRIVACY_SECURITY -> PrivacySecurityScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.SYNC_BACKUP -> LocalBackupScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.TAG_ORGANISER -> TagOrganiserScreen(uiState = uiState, viewModel = viewModel)
                        }
                    }
                }
            } else {
                // Single Pane Compact View (Phones)
                if (selectedCompactSubScreen == null) {
                    // Dashboard Category List
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val minHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minHeight)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CategoryNavCard(
                                title = "General Settings",
                                subtitle = "Theme, time format, editor, audio quality",
                                icon = Icons.Default.Settings,
                                onClick = {
                                    selectedCompactSubScreen = SettingsSubScreen.GENERAL
                                    viewModel.setActiveSubScreen(SettingsSubScreen.GENERAL)
                                }
                            )

                            CategoryNavCard(
                                title = "Privacy & Security",
                                subtitle = "App lock, biometrics, screen privacy",
                                icon = Icons.Default.Lock,
                                onClick = {
                                    selectedCompactSubScreen = SettingsSubScreen.PRIVACY_SECURITY
                                    viewModel.setActiveSubScreen(SettingsSubScreen.PRIVACY_SECURITY)
                                }
                            )

                            CategoryNavCard(
                                title = "Sync & Backup",
                                subtitle = "Local backup archive, export, import",
                                icon = Icons.Default.Share,
                                onClick = {
                                    selectedCompactSubScreen = SettingsSubScreen.SYNC_BACKUP
                                    viewModel.setActiveSubScreen(SettingsSubScreen.SYNC_BACKUP)
                                }
                            )

                            CategoryNavCard(
                                title = "Tag Organiser",
                                subtitle = "Choose tags, folders, organise notes",
                                icon = getTagIcon(colors.textPrimary),
                                onClick = {
                                    selectedCompactSubScreen = SettingsSubScreen.TAG_ORGANISER
                                    viewModel.setActiveSubScreen(SettingsSubScreen.TAG_ORGANISER)
                                }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            SettingsFooter()

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // Active Sub-Screen View
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (selectedCompactSubScreen) {
                            SettingsSubScreen.GENERAL -> Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                GeneralSettingsScreen(uiState = uiState, viewModel = viewModel)
                            }
                            SettingsSubScreen.PRIVACY_SECURITY -> PrivacySecurityScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.SYNC_BACKUP -> LocalBackupScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.TAG_ORGANISER -> TagOrganiserScreen(uiState = uiState, viewModel = viewModel)
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsFooter(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "VoiceJournal is built with a local-first philosophy.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Your voice recordings and notes remain private on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { uriHandler.openUri("https://github.com/rajroshan110") }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = GitHubIcon,
                contentDescription = "GitHub profile",
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "github.com/rajroshan110",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
