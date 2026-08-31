package dev.voicejournal.ui.insight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.voicejournal.ui.insight.components.*
import dev.voicejournal.ui.navigation.BottomNavBar
import dev.voicejournal.ui.navigation.Screen
import dev.voicejournal.ui.designsystem.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InsightScreen(
    navController: NavController,
    viewModel: InsightViewModel = hiltViewModel()
) {
    val colors = AppTheme.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isWideLayout = configuration.screenWidthDp >= 600
    val isCompactLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || configuration.screenHeightDp < 480

    Scaffold(
        containerColor = colors.background,
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Unable to load insights",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = contentState.message,
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.retry() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(text = "Retry", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is InsightsContentState.Success -> {
                    val summary = contentState.summary

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Minimalistic Sticky Header: Period Selector starts right at top
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.background)
                                    .padding(
                                        start = 16.dp,
                                        top = if (isCompactLandscape) 2.dp else 6.dp,
                                        end = 16.dp,
                                        bottom = if (isCompactLandscape) 2.dp else 4.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                PeriodSelector(
                                    selectedPeriod = uiState.period,
                                    onPeriodSelect = { viewModel.setPeriod(it) },
                                    modifier = Modifier.widthIn(max = if (isCompactLandscape) 280.dp else androidx.compose.ui.unit.Dp.Unspecified)
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
                                            .padding(horizontal = 16.dp),
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
                                            modifier = Modifier.weight(1f)
                                        )

                                        MoodTrendsCard(
                                            dominantMood = summary.dominantMood,
                                            moodDistribution = summary.moodDistribution,
                                            dailyMoodPoints = summary.dailyMoodPoints,
                                            modifier = Modifier.weight(1f)
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
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        TopTagsCard(
                                            topTags = summary.topTags,
                                            onTagClick = { tag ->
                                                navController.navigate(Screen.Journal.createRoute(tag = tag))
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        PeopleMentionedCard(
                                            topPeople = summary.topPeople,
                                            onPersonClick = { person ->
                                                navController.navigate(Screen.Journal.createRoute(person = person))
                                            },
                                            modifier = Modifier.weight(1f)
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
