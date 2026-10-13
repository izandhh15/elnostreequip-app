package cat.elnostreequip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.elnostreequip.app.ui.screens.ArticleScreen
import cat.elnostreequip.app.ui.screens.HomeScreen
import cat.elnostreequip.app.ui.screens.MatchesScreen
import cat.elnostreequip.app.ui.screens.PlayerScreen
import cat.elnostreequip.app.ui.screens.SettingsScreen
import cat.elnostreequip.app.ui.screens.TeamScreen
import cat.elnostreequip.app.ui.screens.TeamsScreen
import cat.elnostreequip.app.ui.theme.ElNostreEquipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElNostreEquipTheme {
                ElNostreEquipNav()
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Inici", Icons.Filled.Home),
    Tab("teams", "Equips", Icons.Filled.People),
    Tab("matches", "Partits", Icons.Filled.SportsSoccer),
    Tab("settings", "Ajustos", Icons.Filled.Settings)
)

@Composable
private fun ElNostreEquipNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                TABS.forEach { tab ->
                    val selected = currentRoute == tab.route ||
                        (tab.route == "teams" && currentRoute?.startsWith("team/") == true) ||
                        (tab.route == "teams" && currentRoute?.startsWith("player/") == true) ||
                        (tab.route == "home" && currentRoute?.startsWith("article/") == true)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.tertiary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onArticleClick = { id -> navController.navigate("article/$id") },
                    onMatchesClick = { navController.navigate("matches") }
                )
            }
            composable(
                "article/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                ArticleScreen(
                    articleId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("teams") {
                TeamsScreen(
                    onTeamClick = { equipId -> navController.navigate("team/$equipId") }
                )
            }
            composable(
                "team/{equipId}",
                arguments = listOf(navArgument("equipId") { type = NavType.IntType })
            ) { entry ->
                TeamScreen(
                    equipId = entry.arguments?.getInt("equipId") ?: 39,
                    onPlayerClick = { id -> navController.navigate("player/$id") }
                )
            }
            composable(
                "player/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                PlayerScreen(playerId = entry.arguments?.getLong("id") ?: 0L)
            }
            composable("matches") {
                MatchesScreen(onArticleClick = { id -> navController.navigate("article/$id") })
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
