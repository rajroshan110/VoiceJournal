package dev.voicejournal.ui.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.voicejournal.ui.folders.FoldersScreen
import dev.voicejournal.ui.journal.JournalScreen
import dev.voicejournal.ui.calendar.CalendarScreen
import dev.voicejournal.ui.insight.InsightScreen
import dev.voicejournal.ui.notedetail.NoteDetailScreen
import dev.voicejournal.ui.settings.SettingsScreen
import dev.voicejournal.ui.tags.TagsScreen
import dev.voicejournal.ui.trash.TrashScreen

private fun getTabIndex(route: String?): Int {
    if (route == null) return -1
    return when {
        route.startsWith(Screen.Journal.route) -> 0
        route.startsWith(Screen.Calendar.route) -> 1
        route.startsWith(Screen.Insight.route) -> 2
        route.startsWith(Screen.Folders.route) -> 3
        route.startsWith(Screen.Tags.route) -> 4
        route.startsWith(Screen.Settings.route) -> 5
        route.startsWith(Screen.Trash.route) -> 6
        else -> -1
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Journal.route,
        modifier = modifier,
        enterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
                }
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        },
        exitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
                        animationSpec = tween(220, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                        animationSpec = tween(220, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing))
                }
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.10f).toInt() },
                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
            }
        },
        popEnterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
                }
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (-fullWidth * 0.10f).toInt() },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        },
        popExitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (-fullWidth * 0.35f).toInt() },
                        animationSpec = tween(220, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                        animationSpec = tween(220, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing))
                }
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
            }
        }
    ) {
        composable(
            route = Screen.Journal.route,
            arguments = listOf(
                navArgument("person") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("tag") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialPerson = backStackEntry.arguments?.getString("person")
            val initialTag = backStackEntry.arguments?.getString("tag")
            JournalScreen(navController = navController, initialPerson = initialPerson, initialTag = initialTag)
        }
        composable(Screen.Calendar.route) { CalendarScreen(navController = navController) }
        composable(Screen.Insight.route) { InsightScreen(navController = navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
        composable(Screen.Folders.route) { FoldersScreen(navController = navController) }
        composable(Screen.Tags.route) { TagsScreen(navController = navController) }
        composable(Screen.Archive.route) { dev.voicejournal.ui.archive.ArchiveScreen(navController = navController) }
        composable(Screen.Draft.route) { dev.voicejournal.ui.draft.DraftScreen(navController = navController) }
        composable(Screen.Trash.route) { TrashScreen(navController = navController) }
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.LongType },
                navArgument("initialFolder") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialTag") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialTagType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() },
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() },
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing))
            }
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val initialFolder = backStackEntry.arguments?.getString("initialFolder")
            val initialTag = backStackEntry.arguments?.getString("initialTag")
            val initialTagType = backStackEntry.arguments?.getString("initialTagType")
            NoteDetailScreen(
                navController = navController,
                entryId = entryId,
                initialFolder = initialFolder,
                initialTag = initialTag,
                initialTagType = initialTagType
            )
        }
    }
}
