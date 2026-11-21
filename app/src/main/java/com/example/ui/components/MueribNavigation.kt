package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

enum class MueribTab {
    ANALYZER,
    HISTORY,
    SETTINGS
}

@Composable
fun MueribBottomBar(
    activeTab: MueribTab,
    totalHistoryCount: Int,
    onSelectTab: (MueribTab) -> Unit
) {
    val navBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = activeTab == MueribTab.ANALYZER,
            onClick = { onSelectTab(MueribTab.ANALYZER) },
            colors = navBarItemColors,
            icon = {
                Icon(
                    imageVector = if (activeTab == MueribTab.ANALYZER) {
                        Icons.AutoMirrored.Filled.MenuBook
                    } else {
                        Icons.AutoMirrored.Outlined.MenuBook
                    },
                    contentDescription = stringResource(R.string.tab_analyze)
                )
            },
            label = { Text(stringResource(R.string.tab_analyze)) },
            modifier = Modifier.testTag("nav_analyze")
        )
        NavigationBarItem(
            selected = activeTab == MueribTab.HISTORY,
            onClick = { onSelectTab(MueribTab.HISTORY) },
            colors = navBarItemColors,
            icon = {
                HistoryNavIcon(activeTab == MueribTab.HISTORY, totalHistoryCount)
            },
            label = { Text(stringResource(R.string.tab_history)) },
            modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
            selected = activeTab == MueribTab.SETTINGS,
            onClick = { onSelectTab(MueribTab.SETTINGS) },
            colors = navBarItemColors,
            icon = {
                Icon(
                    imageVector = if (activeTab == MueribTab.SETTINGS) {
                        Icons.Filled.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = "الإعدادات"
                )
            },
            label = { Text("الإعدادات") },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

@Composable
fun MueribNavRail(
    activeTab: MueribTab,
    totalHistoryCount: Int,
    onSelectTab: (MueribTab) -> Unit
) {
    val navRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationRailItem(
            selected = activeTab == MueribTab.ANALYZER,
            onClick = { onSelectTab(MueribTab.ANALYZER) },
            colors = navRailItemColors,
            icon = {
                Icon(
                    imageVector = if (activeTab == MueribTab.ANALYZER) {
                        Icons.AutoMirrored.Filled.MenuBook
                    } else {
                        Icons.AutoMirrored.Outlined.MenuBook
                    },
                    contentDescription = stringResource(R.string.tab_analyze)
                )
            },
            label = { Text(stringResource(R.string.tab_analyze)) },
            modifier = Modifier.testTag("nav_analyze")
        )
        NavigationRailItem(
            selected = activeTab == MueribTab.HISTORY,
            onClick = { onSelectTab(MueribTab.HISTORY) },
            colors = navRailItemColors,
            icon = {
                HistoryNavIcon(activeTab == MueribTab.HISTORY, totalHistoryCount)
            },
            label = { Text(stringResource(R.string.tab_history)) },
            modifier = Modifier.testTag("nav_history")
        )
        NavigationRailItem(
            selected = activeTab == MueribTab.SETTINGS,
            onClick = { onSelectTab(MueribTab.SETTINGS) },
            colors = navRailItemColors,
            icon = {
                Icon(
                    imageVector = if (activeTab == MueribTab.SETTINGS) {
                        Icons.Filled.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = "الإعدادات"
                )
            },
            label = { Text("الإعدادات") },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

@Composable
private fun HistoryNavIcon(selected: Boolean, totalHistoryCount: Int) {
    BadgedBox(
        badge = {
            if (totalHistoryCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(
                        text = if (totalHistoryCount > 99) "99+" else totalHistoryCount.toString()
                    )
                }
            }
        }
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.History else Icons.Outlined.History,
            contentDescription = stringResource(R.string.tab_history)
        )
    }
}
