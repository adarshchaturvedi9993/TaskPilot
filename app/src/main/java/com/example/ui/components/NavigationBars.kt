package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskOverallStatus
import com.example.ui.NavDestination
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun TaskPilotTopBar(
    currentStatus: TaskOverallStatus,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "TaskPilot Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "TASKPILOT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "AI Android Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Agent Status Pill
        val (statusText, statusColor, statusIcon) = when (currentStatus) {
            TaskOverallStatus.IDLE -> Triple("Agent Ready", StatusSuccess, Icons.Default.CheckCircle)
            TaskOverallStatus.PLANNING -> Triple("Planning...", DarkPrimary, Icons.Default.Refresh)
            TaskOverallStatus.REVIEWING -> Triple("Review Plan", DarkPrimary, Icons.Default.Warning)
            TaskOverallStatus.RUNNING -> Triple("Executing", StatusRunning, Icons.Default.PlayArrow)
            TaskOverallStatus.WAITING_CONFIRMATION -> Triple("Confirm Action", StatusWarning, Icons.Default.Warning)
            TaskOverallStatus.WAITING_USER_ACTION -> Triple("User Action", StatusWarning, Icons.Default.Warning)
            TaskOverallStatus.COMPLETED -> Triple("Completed", StatusSuccess, Icons.Default.CheckCircle)
            TaskOverallStatus.PARTIALLY_COMPLETED -> Triple("Partial", StatusWarning, Icons.Default.Warning)
            TaskOverallStatus.FAILED -> Triple("Failed", StatusError, Icons.Default.Warning)
            TaskOverallStatus.CANCELLED -> Triple("Cancelled", MaterialTheme.colorScheme.outline, Icons.Default.Warning)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable { onStatusClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("topbar_status_pill"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
fun TaskPilotBottomNav(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    activeTaskRunning: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(NavDestination.HOME, Icons.Default.Home, Icons.Outlined.Home),
            Triple(NavDestination.TASKS, Icons.Default.PlayArrow, Icons.Outlined.PlayCircle),
            Triple(NavDestination.APPS, Icons.Default.Apps, Icons.Outlined.Apps),
            Triple(NavDestination.ACTIVITY, Icons.Default.History, Icons.Outlined.History),
            Triple(NavDestination.SETTINGS, Icons.Default.Settings, Icons.Outlined.Settings)
        )

        items.forEach { (destination, selectedIcon, unselectedIcon) ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    if (destination == NavDestination.TASKS && activeTaskRunning) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = StatusRunning)
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                contentDescription = destination.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) selectedIcon else unselectedIcon,
                            contentDescription = destination.label
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
            )
        }
    }
}
