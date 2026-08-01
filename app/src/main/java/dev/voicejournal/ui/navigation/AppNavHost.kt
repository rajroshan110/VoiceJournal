package dev.voicejournal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import dev.voicejournal.ui.designsystem.motion.NavigationMotion
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
fun AppNavHost(
    navController: NavHostController,
    openEntryId: Long = -1L,
    onEntryNavigated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTargetId by androidx.compose.runtime.rememberUpdatedState(openEntryId)
    androidx.compose.runtime.LaunchedEffect(currentTargetId) {
        val targetId = currentTargetId
        if (targetId != -1L) {
            while (navController.currentDestination == null) {
                kotlinx.coroutines.delay(50L)
            }
            val targetRoute = Screen.NoteDetail.createRoute(targetId)
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != targetRoute) {
                navController.navigate(targetRoute) {
                    launchSingleTop = true
                }
            }
            onEntryNavigated()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Journal.route,
        modifier = modifier,
        enterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    NavigationMotion.TabSwitchForwardEnter
                } else {
                    NavigationMotion.TabSwitchBackwardEnter
                }
            } else {
                NavigationMotion.DefaultEnter
            }
        },
        exitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    NavigationMotion.TabSwitchForwardExit
                } else {
                    NavigationMotion.TabSwitchBackwardExit
                }
            } else {
                NavigationMotion.DefaultExit
            }
        },
        popEnterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    NavigationMotion.TabSwitchForwardEnter
                } else {
                    NavigationMotion.TabSwitchBackwardEnter
                }
            } else {
                NavigationMotion.ScreenPopEnter
            }
        },
        popExitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                if (targetIndex > initialIndex) {
                    NavigationMotion.TabSwitchForwardExit
                } else {
                    NavigationMotion.TabSwitchBackwardExit
                }
            } else {
                NavigationMotion.ScreenPopExit
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
            enterTransition = { NavigationMotion.ScreenPushEnter },
            exitTransition = { NavigationMotion.ScreenPushExit },
            popEnterTransition = { NavigationMotion.ScreenPopEnter },
            popExitTransition = { NavigationMotion.ScreenPopExit }
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
