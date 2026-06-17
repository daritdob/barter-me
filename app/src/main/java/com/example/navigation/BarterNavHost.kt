package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.glassmorphic
import com.example.ui.screens.*
import com.example.ui.viewmodel.BarterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarterNavHost(
    viewModel: BarterViewModel,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showTopBar = currentRoute in setOf(
        BarterDestinations.EXPLORE,
        BarterDestinations.SAVED,
        BarterDestinations.INBOX,
        BarterDestinations.NOTIFICATIONS
    )
    val showBottomBar = currentRoute in BarterDestinations.topLevelRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                BarterTopBar(
                    isDarkMode = isDarkMode,
                    currentRoute = currentRoute,
                    onLogoClick = {
                        navController.navigate(BarterDestinations.EXPLORE) {
                            popUpTo(BarterDestinations.EXPLORE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onNotificationsClick = {
                        navController.navigate(BarterDestinations.NOTIFICATIONS) {
                            popUpTo(BarterDestinations.EXPLORE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(BarterDestinations.PROFILE_ME) {
                            popUpTo(BarterDestinations.EXPLORE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BarterBottomBar(
                    isDarkMode = isDarkMode,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(BarterDestinations.EXPLORE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BarterDestinations.EXPLORE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BarterDestinations.EXPLORE) {
                ExploreScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { navController.navigate(BarterDestinations.chatRoute(it)) },
                    onNavigateToProfile = { navController.navigate(BarterDestinations.profileRoute(it)) }
                )
            }
            composable(BarterDestinations.SAVED) {
                SavedScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { navController.navigate(BarterDestinations.chatRoute(it)) },
                    onNavigateToProfile = { navController.navigate(BarterDestinations.profileRoute(it)) }
                )
            }
            composable(BarterDestinations.INBOX) {
                InboxScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { navController.navigate(BarterDestinations.chatRoute(it)) },
                    onNavigateToExplore = {
                        navController.navigate(BarterDestinations.EXPLORE) {
                            popUpTo(BarterDestinations.EXPLORE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(BarterDestinations.NOTIFICATIONS) {
                NotificationCenter(viewModel = viewModel)
            }
            composable(BarterDestinations.PROFILE) { entry ->
                val userId = entry.arguments?.getString("userId") ?: "me"
                ProfileScreen(
                    viewModel = viewModel,
                    userId = userId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(BarterDestinations.CHAT) { entry ->
                val listingId = entry.arguments?.getString("listingId")?.toIntOrNull() ?: 0
                ChatScreen(
                    viewModel = viewModel,
                    listingId = listingId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BarterTopBar(
    isDarkMode: Boolean,
    currentRoute: String?,
    onLogoClick: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onLogoClick)
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Barter-me Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Barter-me",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .testTag("dark_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle color scheme",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .testTag("nav_top_notifications")
                ) {
                    Icon(
                        imageVector = if (currentRoute == BarterDestinations.NOTIFICATIONS) {
                            Icons.Default.Notifications
                        } else {
                            Icons.Outlined.Notifications
                        },
                        contentDescription = "Match Alerts",
                        tint = if (currentRoute == BarterDestinations.NOTIFICATIONS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .testTag("nav_top_profile")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "My Trust Profile",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BarterBottomBar(
    isDarkMode: Boolean,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .testTag("bottom_navigation")
            .glassmorphic(cornerRadius = 0.dp, borderWidth = 0.dp, isDarkTheme = isDarkMode),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == BarterDestinations.EXPLORE,
            onClick = { onNavigate(BarterDestinations.EXPLORE) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == BarterDestinations.EXPLORE) Icons.Default.Explore else Icons.Outlined.Explore,
                    contentDescription = "Explore feed"
                )
            },
            label = { Text("Explore") },
            modifier = Modifier.testTag("nav_explore")
        )
        NavigationBarItem(
            selected = currentRoute == BarterDestinations.SAVED,
            onClick = { onNavigate(BarterDestinations.SAVED) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == BarterDestinations.SAVED) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Saved Items"
                )
            },
            label = { Text("Saved") },
            modifier = Modifier.testTag("nav_saved")
        )
        NavigationBarItem(
            selected = currentRoute == BarterDestinations.INBOX,
            onClick = { onNavigate(BarterDestinations.INBOX) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == BarterDestinations.INBOX) Icons.Default.Chat else Icons.Outlined.Chat,
                    contentDescription = "Active trade chats"
                )
            },
            label = { Text("Chats") },
            modifier = Modifier.testTag("nav_chats")
        )
        NavigationBarItem(
            selected = currentRoute == BarterDestinations.NOTIFICATIONS,
            onClick = { onNavigate(BarterDestinations.NOTIFICATIONS) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == BarterDestinations.NOTIFICATIONS) {
                        Icons.Default.NotificationsActive
                    } else {
                        Icons.Outlined.NotificationsActive
                    },
                    contentDescription = "Notifications"
                )
            },
            label = { Text("Alerts") },
            modifier = Modifier.testTag("nav_notifications")
        )
        NavigationBarItem(
            selected = currentRoute == BarterDestinations.PROFILE_ME,
            onClick = { onNavigate(BarterDestinations.PROFILE_ME) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == BarterDestinations.PROFILE_ME) {
                        Icons.Default.AccountCircle
                    } else {
                        Icons.Outlined.AccountCircle
                    },
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            modifier = Modifier.testTag("nav_profile_me")
        )
    }
}
