package dev.voicejournal.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.designsystem.components.SearchBar
import dev.voicejournal.ui.settings.components.CategoryNavCard
import dev.voicejournal.ui.settings.components.SettingSearchRow
import dev.voicejournal.ui.settings.model.SettingsSubScreen
import dev.voicejournal.ui.settings.screens.GeneralSettingsScreen
import dev.voicejournal.ui.settings.screens.LocalBackupScreen
import dev.voicejournal.ui.settings.screens.PrivacySecurityScreen
import dev.voicejournal.ui.theme.AppTheme

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

    var selectedCompactSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }

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
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                !isExpanded && selectedCompactSubScreen == SettingsSubScreen.GENERAL -> "General Settings"
                                !isExpanded && selectedCompactSubScreen == SettingsSubScreen.PRIVACY_SECURITY -> "Privacy & Security"
                                !isExpanded && selectedCompactSubScreen == SettingsSubScreen.SYNC_BACKUP -> "Sync & Backup"
                                else -> "Settings"
                            },
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSearchActive) {
                                focusManager.clearFocus()
                                viewModel.clearSearch()
                                isSearchFocused = false
                            } else if (!isExpanded && selectedCompactSubScreen != null) {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(if (uiState.isViewingAppLockDetail) PaddingValues(0.dp) else paddingValues)
                .background(colors.background)
        ) {
            // Search Bar Header - ONLY visible on main settings dashboard tab (selectedCompactSubScreen == null)
            if ((selectedCompactSubScreen == null || isExpanded) && !uiState.isViewingAppLockDetail) {
                Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp)) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "Search settings...",
                        requestFocusOnLaunch = false,
                        onFocusChanged = { isSearchFocused = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                                    viewModel.clearSearch()
                                    if (isExpanded) {
                                        viewModel.setActiveSubScreen(item.targetSubScreen)
                                    } else {
                                        selectedCompactSubScreen = item.targetSubScreen
                                    }
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
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryNavCard(
                            title = "General Settings",
                            subtitle = "Theme, journaling preferences, audio quality, editor",
                            icon = Icons.Default.Settings,
                            isSelected = uiState.activeSubScreen == SettingsSubScreen.GENERAL,
                            onClick = { viewModel.setActiveSubScreen(SettingsSubScreen.GENERAL) }
                        )

                        CategoryNavCard(
                            title = "Privacy & Security",
                            subtitle = "App lock, biometrics, screen privacy",
                            icon = Icons.Default.Lock,
                            isSelected = uiState.activeSubScreen == SettingsSubScreen.PRIVACY_SECURITY,
                            onClick = { viewModel.setActiveSubScreen(SettingsSubScreen.PRIVACY_SECURITY) }
                        )

                        CategoryNavCard(
                            title = "Sync & Backup",
                            subtitle = "Cloud sync, local export, import",
                            icon = Icons.Default.Share,
                            isSelected = uiState.activeSubScreen == SettingsSubScreen.SYNC_BACKUP,
                            onClick = { viewModel.setActiveSubScreen(SettingsSubScreen.SYNC_BACKUP) }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Pane (65% Width): Active Sub-Screen
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                            .background(colors.surfaceVariant, shape = RoundedCornerShape(16.dp))
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (uiState.activeSubScreen) {
                            SettingsSubScreen.GENERAL -> GeneralSettingsScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.PRIVACY_SECURITY -> PrivacySecurityScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.SYNC_BACKUP -> LocalBackupScreen(uiState = uiState, viewModel = viewModel)
                        }
                    }
                }
            } else {
                // Single Pane Compact View (Phones)
                if (selectedCompactSubScreen == null) {
                    // Dashboard Category List
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CategoryNavCard(
                            title = "General Settings",
                            subtitle = "Theme, journaling preferences, audio quality, editor, locale",
                            icon = Icons.Default.Settings,
                            onClick = { selectedCompactSubScreen = SettingsSubScreen.GENERAL }
                        )

                        CategoryNavCard(
                            title = "Privacy & Security",
                            subtitle = "App lock, biometrics, screen privacy",
                            icon = Icons.Default.Lock,
                            onClick = { selectedCompactSubScreen = SettingsSubScreen.PRIVACY_SECURITY }
                        )

                        CategoryNavCard(
                            title = "Sync & Backup",
                            subtitle = "Cloud sync, local export, import",
                            icon = Icons.Default.Share,
                            onClick = { selectedCompactSubScreen = SettingsSubScreen.SYNC_BACKUP }
                        )
                    }
                } else {
                    // Active Sub-Screen View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedCompactSubScreen) {
                            SettingsSubScreen.GENERAL -> GeneralSettingsScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.PRIVACY_SECURITY -> PrivacySecurityScreen(uiState = uiState, viewModel = viewModel)
                            SettingsSubScreen.SYNC_BACKUP -> LocalBackupScreen(uiState = uiState, viewModel = viewModel)
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
