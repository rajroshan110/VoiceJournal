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
        route == "Journal" || route.startsWith("Journal?") || route.startsWith(Screen.Journal.route) -> 0
        route.startsWith(Screen.Calendar.route) -> 1
        route.startsWith(Screen.Insight.route) -> 2
        route.startsWith(Screen.Folders.route) -> 3
        route.startsWith(Screen.Tags.route) -> 4
        route.startsWith(Screen.Archive.route) -> 5
        route.startsWith(Screen.Draft.route) -> 6
        route.startsWith(Screen.Trash.route) -> 7
        route.startsWith(Screen.Settings.route) -> 8
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val isReducedMotion = androidx.compose.runtime.remember(context) {
        NavigationMotion.isReducedMotion(context)
    }

    val currentTargetId by androidx.compose.runtime.rememberUpdatedState(openEntryId)
    androidx.compose.runtime.LaunchedEffect(currentTargetId) {
        val targetId = currentTargetId
        if (targetId != -1L) {
            while (navController.currentDestination == null) {
                kotlinx.coroutines.delay(50L)
            }
            val currentRoute = navController.currentDestination?.route
            val isAlreadyOnTargetNote = currentRoute?.startsWith("NoteDetail") == true &&
                    navController.currentBackStackEntry?.arguments?.getLong("entryId") == targetId
            if (!isAlreadyOnTargetNote) {
                val targetRoute = Screen.NoteDetail.createRoute(targetId)
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
            if (isReducedMotion) {
                androidx.compose.animation.EnterTransition.None
            } else {
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
            }
        },
        exitTransition = {
            if (isReducedMotion) {
                androidx.compose.animation.ExitTransition.None
            } else {
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
            }
        },
        popEnterTransition = {
            if (isReducedMotion) {
                androidx.compose.animation.EnterTransition.None
            } else {
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
            }
        },
        popExitTransition = {
            if (isReducedMotion) {
                androidx.compose.animation.ExitTransition.None
            } else {
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
            enterTransition = { if (isReducedMotion) androidx.compose.animation.EnterTransition.None else NavigationMotion.ScreenPushEnter },
            exitTransition = { if (isReducedMotion) androidx.compose.animation.ExitTransition.None else NavigationMotion.ScreenPushExit },
            popEnterTransition = { if (isReducedMotion) androidx.compose.animation.EnterTransition.None else NavigationMotion.ScreenPopEnter },
            popExitTransition = { if (isReducedMotion) androidx.compose.animation.ExitTransition.None else NavigationMotion.ScreenPopExit }
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
