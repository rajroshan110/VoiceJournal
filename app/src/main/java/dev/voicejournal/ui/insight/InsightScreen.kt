package dev.voicejournal.ui.insight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.voicejournal.ui.designsystem.components.buttons.AppPrimaryButton
import dev.voicejournal.ui.designsystem.theme.AppTheme
import dev.voicejournal.ui.designsystem.tokens.Radius
import dev.voicejournal.ui.designsystem.tokens.Spacing
import dev.voicejournal.ui.insight.components.*
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InsightScreen(
    navController: NavController,
    viewModel: InsightViewModel = hiltViewModel()
) {
    val colors = AppTheme.colors
    val windowInfo = AppTheme.windowInfo
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWideLayout = windowInfo.isWideLayout
    val isCompactLandscape = windowInfo.isCompactLandscape

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNavBar(navController = navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.background)
        ) {
            when (val contentState = uiState.contentState) {
                is InsightsContentState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.SpaceLg, vertical = Spacing.SpaceMd),
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        item {
                            InsightsShimmerSkeleton()
                        }
                    }
                }

                is InsightsContentState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(Spacing.SpaceXl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.RadiusLg))
                                .background(colors.surface)
                                .padding(Spacing.SpaceLg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Unable to load insights",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(Spacing.SpaceX2s))
                                Text(
                                    text = contentState.message,
                                    color = colors.textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(Spacing.SpaceLg))
                                AppPrimaryButton(
                                    onClick = { viewModel.retry() }
                                ) {
                                    Text(
                                        text = "Retry",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                is InsightsContentState.Success -> {
                    val summary = contentState.summary

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Spacing.SpaceLg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.SpaceMd)
                    ) {
                        // Minimalistic Sticky Header: Period Selector starts right at top
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.background)
                                    .statusBarsPadding()
                                    .padding(
                                        start = Spacing.SpaceLg,
                                        top = if (isCompactLandscape) Spacing.SpaceX3s else 6.dp,
                                        end = Spacing.SpaceLg,
                                        bottom = if (isCompactLandscape) Spacing.SpaceX3s else Spacing.SpaceX2s
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                PeriodSelector(
                                    selectedPeriod = uiState.period,
                                    onPeriodSelect = { viewModel.setPeriod(it) },
                                    modifier = Modifier.widthIn(max = if (isCompactLandscape) 280.dp else Dp.Unspecified)
                                )
                            }
                        }

                        // Responsive Cards Layout
                        if (isWideLayout) {
                            // 2-Column Responsive Layout for Medium / Expanded Screens
                            item {
                                if (uiState.isMoodEnabled) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(IntrinsicSize.Max),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        RecordingActivityCard(
                                            dailyCounts = summary.dailyCounts,
                                            totalEntries = summary.totalEntries,
                                            averagePerDay = summary.averagePerDay,
                                            activeStreak = summary.activeStreak,
                                            period = uiState.period,
                                            peakDay = summary.peakDay,
                                            peakCount = summary.peakCount,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )

                                        MoodTrendsCard(
                                            dominantMood = summary.dominantMood,
                                            moodDistribution = summary.moodDistribution,
                                            dailyMoodPoints = summary.dailyMoodPoints,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        RecordingActivityCard(
                                            dailyCounts = summary.dailyCounts,
                                            totalEntries = summary.totalEntries,
                                            averagePerDay = summary.averagePerDay,
                                            activeStreak = summary.activeStreak,
                                            period = uiState.period,
                                            peakDay = summary.peakDay,
                                            peakCount = summary.peakCount
                                        )
                                    }
                                }
                            }

                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    PeakReflectionCard(
                                        timeOfDayDistribution = summary.timeOfDayDistribution
                                    )
                                }
                            }

                            if (uiState.isTopicsEnabled && uiState.isPeopleEnabled) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(IntrinsicSize.Max),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        TopTagsCard(
                                            topTags = summary.topTags,
                                            onTagClick = { tag ->
                                                navController.navigate(Screen.Journal.createRoute(tag = tag))
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )

                                        PeopleMentionedCard(
                                            topPeople = summary.topPeople,
                                            onPersonClick = { person ->
                                                navController.navigate(Screen.Journal.createRoute(person = person))
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )
                                    }
                                }
                            } else if (uiState.isTopicsEnabled) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        TopTagsCard(
                                            topTags = summary.topTags,
                                            onTagClick = { tag ->
                                                navController.navigate(Screen.Journal.createRoute(tag = tag))
                                            }
                                        )
                                    }
                                }
                            } else if (uiState.isPeopleEnabled) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        PeopleMentionedCard(
                                            topPeople = summary.topPeople,
                                            onPersonClick = { person ->
                                                navController.navigate(Screen.Journal.createRoute(person = person))
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Single Column Vertical Layout for Compact Mobile Screens
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RecordingActivityCard(
                                        dailyCounts = summary.dailyCounts,
                                        totalEntries = summary.totalEntries,
                                        averagePerDay = summary.averagePerDay,
                                        activeStreak = summary.activeStreak,
                                        period = uiState.period,
                                        peakDay = summary.peakDay,
                                        peakCount = summary.peakCount
                                    )
                                }
                            }

                            if (uiState.isMoodEnabled) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        MoodTrendsCard(
                                            dominantMood = summary.dominantMood,
                                            moodDistribution = summary.moodDistribution,
                                            dailyMoodPoints = summary.dailyMoodPoints
                                        )
                                    }
                                }
                            }

                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    PeakReflectionCard(
                                        timeOfDayDistribution = summary.timeOfDayDistribution
                                    )
                                }
                            }

                            if (uiState.isTopicsEnabled) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        TopTagsCard(
                                            topTags = summary.topTags,
                                            onTagClick = { tag ->
                                                navController.navigate(Screen.Journal.createRoute(tag = tag))
                                            }
                                        )
                                    }
                                }
                            }

                            if (uiState.isPeopleEnabled) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        PeopleMentionedCard(
                                            topPeople = summary.topPeople,
                                            onPersonClick = { person ->
                                                navController.navigate(Screen.Journal.createRoute(person = person))
                                            }
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
}
