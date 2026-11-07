package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StepStatus
import com.example.data.model.TaskOverallStatus
import com.example.ui.MainViewModel
import com.example.ui.NavDestination
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkSecondary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentPlan by viewModel.executionEngine.currentPlan.collectAsState()
    val overallStatus by viewModel.executionEngine.overallStatus.collectAsState()
    val currentStepIndex by viewModel.executionEngine.currentStepIndex.collectAsState()
    val elapsedSeconds by viewModel.executionEngine.elapsedSeconds.collectAsState()
    val errorMessage by viewModel.executionEngine.errorMessage.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "task_running_pulse")
    val runningGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "running_glow"
    )

    if (currentPlan == null) {
        // Empty state
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_tasks_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Active Execution",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Describe an instruction on the Home screen or say \"Jarvis\", \"Tasker\", or \"Guruji\" to wake the agent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.setDestination(NavDestination.HOME) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                        modifier = Modifier.testTag("go_to_home_button")
                    ) {
                        Text("Create a Directive", fontWeight = FontWeight.Bold, color = Color(0xFF0F1115))
                    }
                }
            }
        }
        return
    }

    val plan = currentPlan!!
    val totalSteps = plan.steps.size
    val completedSteps = plan.steps.count { it.status == StepStatus.COMPLETED }
    val targetProgress = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Task Monitor Header Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0xFF383C48), Color(0xFF1D2028)))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (overallStatus) {
                                                TaskOverallStatus.RUNNING -> StatusRunning
                                                TaskOverallStatus.COMPLETED -> StatusSuccess
                                                TaskOverallStatus.FAILED -> StatusError
                                                TaskOverallStatus.WAITING_CONFIRMATION, TaskOverallStatus.WAITING_USER_ACTION -> StatusWarning
                                                else -> Color.Gray
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (overallStatus) {
                                        TaskOverallStatus.RUNNING -> "EXECUTING TASK"
                                        TaskOverallStatus.COMPLETED -> "EXECUTION COMPLETE"
                                        TaskOverallStatus.FAILED -> "EXECUTION HALTED"
                                        TaskOverallStatus.WAITING_CONFIRMATION -> "AWAITING AUTHORIZATION"
                                        TaskOverallStatus.WAITING_USER_ACTION -> "MANUAL ACTION REQUIRED"
                                        TaskOverallStatus.CANCELLED -> "EXECUTION CANCELLED"
                                        else -> "TASK STATUS"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = when (overallStatus) {
                                        TaskOverallStatus.RUNNING -> StatusRunning
                                        TaskOverallStatus.COMPLETED -> StatusSuccess
                                        TaskOverallStatus.FAILED -> StatusError
                                        TaskOverallStatus.WAITING_CONFIRMATION, TaskOverallStatus.WAITING_USER_ACTION -> StatusWarning
                                        else -> Color.Gray
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = plan.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Chronometer Stopwatch Pill
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        val timeStr = String.format("%02d:%02d", mins, secs)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E212A))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = DarkPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = plan.rawInstruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Glowing Linear Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E212A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(DarkPrimary, DarkSecondary, StatusSuccess)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Step ${(currentStepIndex + 1).coerceAtMost(totalSteps)} of $totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkPrimary
                        )
                    }
                }
            }
        }

        // 2. Waiting for Confirmation Banner
        if (overallStatus == TaskOverallStatus.WAITING_CONFIRMATION) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("waiting_confirmation_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF231C13)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONSEQUENTIAL ACTION CONFIRMATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = StatusWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TaskPilot requires your explicit confirmation before launching this high-impact action (call, SMS, or event).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.stopExecution() },
                                modifier = Modifier.testTag("cancel_confirmation_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Abort")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { viewModel.confirmCurrentStep() },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("proceed_confirmation_button")
                            ) {
                                Icon(imageVector = Icons.Default.Done, contentDescription = null, tint = Color(0xFF0F1115), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorize & Proceed", fontWeight = FontWeight.Bold, color = Color(0xFF0F1115))
                            }
                        }
                    }
                }
            }
        }

        // 3. Waiting for Manual User Action Banner (Platform Anti-Bot Safeguard)
        if (overallStatus == TaskOverallStatus.WAITING_USER_ACTION) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("waiting_user_action_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1728)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkPrimary.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = DarkPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLATFORM POLICY SAFEGUARD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = DarkPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Actions such as subscribing to channels, liking videos, or posting comments cannot be automated by bots. Please perform the action manually in YouTube, then tap below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.completeUserAction() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("complete_user_action_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F1115), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I've Completed This Action", fontWeight = FontWeight.Bold, color = Color(0xFF0F1115))
                        }
                    }
                }
            }
        }

        // 4. Chronological Timeline Header & Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXECUTION TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (overallStatus == TaskOverallStatus.RUNNING ||
                    overallStatus == TaskOverallStatus.WAITING_CONFIRMATION ||
                    overallStatus == TaskOverallStatus.WAITING_USER_ACTION
                ) {
                    Button(
                        onClick = { viewModel.stopExecution() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("emergency_stop_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Task", color = StatusError, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else if (overallStatus == TaskOverallStatus.FAILED) {
                    Button(
                        onClick = { viewModel.retryExecution() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("retry_task_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Timeline Nodes with Connecting Line and Glowing States
        itemsIndexed(plan.steps) { index, step ->
            val isCurrent = index == currentStepIndex && overallStatus == TaskOverallStatus.RUNNING

            val cardBgColor by animateColorAsState(
                targetValue = when (step.status) {
                    StepStatus.RUNNING -> Color(0xFF1E1728)
                    StepStatus.COMPLETED -> Color(0xFF13151A)
                    StepStatus.FAILED -> Color(0xFF241518)
                    else -> Color(0xFF0F1115)
                },
                animationSpec = tween(400),
                label = "step_bg"
            )

            val borderColor by animateColorAsState(
                targetValue = when (step.status) {
                    StepStatus.RUNNING -> DarkPrimary.copy(alpha = runningGlow)
                    StepStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.4f)
                    StepStatus.FAILED -> StatusError.copy(alpha = 0.6f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
                animationSpec = tween(400),
                label = "step_border"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Vertical Timeline Node Track
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    // Node Pin
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .scale(if (isCurrent) 1.15f else 1f)
                            .clip(CircleShape)
                            .background(
                                when (step.status) {
                                    StepStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.2f)
                                    StepStatus.RUNNING -> StatusRunning.copy(alpha = 0.25f)
                                    StepStatus.FAILED -> StatusError.copy(alpha = 0.2f)
                                    StepStatus.WAITING_CONFIRMATION, StepStatus.WAITING_USER_ACTION -> StatusWarning.copy(alpha = 0.2f)
                                    else -> Color(0xFF1E212A)
                                }
                            )
                            .border(
                                1.dp,
                                when (step.status) {
                                    StepStatus.COMPLETED -> StatusSuccess
                                    StepStatus.RUNNING -> StatusRunning
                                    StepStatus.FAILED -> StatusError
                                    StepStatus.WAITING_CONFIRMATION, StepStatus.WAITING_USER_ACTION -> StatusWarning
                                    else -> Color.White.copy(alpha = 0.1f)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (step.status) {
                            StepStatus.COMPLETED -> Icon(Icons.Default.Check, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            StepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StatusRunning)
                            StepStatus.FAILED -> Icon(Icons.Default.Close, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                            StepStatus.WAITING_CONFIRMATION, StepStatus.WAITING_USER_ACTION -> Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                            else -> Text(text = "${step.order}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Vertical connecting line to next node
                    if (index < plan.steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(56.dp)
                                .background(
                                    if (step.status == StepStatus.COMPLETED) StatusSuccess.copy(alpha = 0.4f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Step Detail Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("timeline_step_card_$index"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${step.order}. ${step.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Status text badge
                            val (badgeText, badgeColor) = when (step.status) {
                                StepStatus.COMPLETED -> Pair("Completed", StatusSuccess)
                                StepStatus.PARTIALLY_COMPLETED -> Pair("Partial", StatusWarning)
                                StepStatus.RUNNING -> Pair("Executing...", StatusRunning)
                                StepStatus.FAILED -> Pair("Failed", StatusError)
                                StepStatus.WAITING_CONFIRMATION -> Pair("Awaiting Approval", StatusWarning)
                                StepStatus.WAITING_USER_ACTION -> Pair("Manual Action", StatusWarning)
                                StepStatus.SKIPPED -> Pair("Skipped", MaterialTheme.colorScheme.outline)
                                StepStatus.CANCELLED -> Pair("Cancelled", MaterialTheme.colorScheme.outline)
                                StepStatus.PENDING -> Pair("Pending", MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (step.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!step.resultMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = step.resultMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (step.status == StepStatus.FAILED) StatusError else StatusSuccess
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
