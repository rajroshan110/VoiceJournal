package dev.voicejournal.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.voicejournal.ui.designsystem.theme.AppTheme

private val BookIcon: ImageVector = ImageVector.Builder(
        name = "Book",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 2f)
            horizontalLineTo(6f)
            curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(6f, 4f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(8f)
            lineTo(8.5f, 10.5f)
            lineTo(6f, 12f)
            verticalLineTo(4f)
            close()
        }
    }.build()

private val CalendarIcon: ImageVector = ImageVector.Builder(
        name = "CalendarToday",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(20f, 3f)
            horizontalLineToRelative(-1f)
            verticalLineTo(1f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(2f)
            horizontalLineTo(7f)
            verticalLineTo(1f)
            horizontalLineTo(5f)
            verticalLineToRelative(2f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(20f, 21f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(13f)
            close()
        }
    }.build()

private val AnalyticsIcon: ImageVector = ImageVector.Builder(
        name = "Analytics",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(9f, 17f)
            horizontalLineTo(7f)
            verticalLineToRelative(-5f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(5f)
            close()
            moveTo(13f, 17f)
            horizontalLineToRelative(-2f)
            verticalLineTo(7f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(10f)
            close()
            moveTo(17f, 17f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-7f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(7f)
            close()
        }
    }.build()

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onJournalReselected: (() -> Unit)? = null
) {
    val items = listOf(Screen.Journal, Screen.Calendar, Screen.Insight)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = AppTheme.colors
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || configuration.screenHeightDp < 480

    NavigationBar(
        containerColor = colors.secondaryBackground,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = if (isCompactLandscape) modifier.height(56.dp) else modifier
    ) {
        items.forEach { screen ->
            val isSelected = when (screen) {
                is Screen.Journal -> currentRoute == "Journal" || currentRoute?.startsWith("Journal?") == true || currentRoute == Screen.Journal.route
                else -> currentRoute == screen.route
            }
            val icon: ImageVector
            val label: String

            when (screen) {
                is Screen.Journal -> {
                    icon = BookIcon
                    label = "Journal"
                }
                is Screen.Calendar -> {
                    icon = CalendarIcon
                    label = "Calendar"
                }
                is Screen.Insight -> {
                    icon = AnalyticsIcon
                    label = "Insights"
                }
                else -> {
                    icon = BookIcon
                    label = screen.route
                }
            }

            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                alwaysShowLabel = !isCompactLandscape,
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        if (screen is Screen.Journal) {
                            onJournalReselected?.invoke()
                        }
                    } else {
                        val targetRoute = when (screen) {
                            is Screen.Journal -> Screen.Journal.createRoute()
                            else -> screen.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
