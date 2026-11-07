package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.MainViewModel
import com.example.ui.NavDestination
import com.example.ui.components.VoiceInputDialog
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkSecondary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val instruction by viewModel.inputInstruction.collectAsState()
    val isPlanning by viewModel.isPlanning.collectAsState()
    val planningError by viewModel.planningError.collectAsState()
    val recentTasks by viewModel.allTasks.collectAsState()
    val isListening by viewModel.voiceHelper.isListening.collectAsState()
    val isWakeWordListening by viewModel.voiceHelper.isWakeWordListening.collectAsState()
    val spokenText by viewModel.voiceHelper.spokenText.collectAsState()
    val voiceError by viewModel.voiceHelper.errorMessage.collectAsState()

    val prefs = viewModel.prefsRepo
    val wakeWord by prefs.wakeWord.collectAsState()
    val speakToWakeEnabled by prefs.speakToWakeEnabled.collectAsState()

    var showVoiceDialog by remember { mutableStateOf(false) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var showWakeWordPicker by remember { mutableStateOf(false) }

    // Waveform & Radar Pulse Transition
    val infiniteTransition = rememberInfiniteTransition(label = "home_radar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_scale"
    )

    // Android SAF document/photo picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        attachedFileName = uri?.lastPathSegment ?: "Selected Document"
    }

    // Microphone permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoiceDialog = true
            viewModel.voiceHelper.startListening()
        } else {
            showVoiceDialog = true
        }
    }

    if (showVoiceDialog) {
        VoiceInputDialog(
            isListening = isListening,
            recognizedText = spokenText,
            errorMessage = voiceError,
            onStartListening = { viewModel.voiceHelper.startListening() },
            onStopListening = { viewModel.voiceHelper.stopListening() },
            onSubmit = { text ->
                viewModel.updateInputInstruction(text)
                showVoiceDialog = false
            },
            onDismiss = { showVoiceDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Speak to Wake "Cyber Radar" Island Pill
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showWakeWordPicker = !showWakeWordPicker }
                    .testTag("speak_to_wake_home_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (speakToWakeEnabled && isWakeWordListening) Color(0xFF131D1A) else Color(0xFF16181E)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (speakToWakeEnabled && isWakeWordListening) StatusSuccess.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pulsing radar light
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(if (speakToWakeEnabled && isWakeWordListening) radarPulse else 1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (speakToWakeEnabled && isWakeWordListening) StatusSuccess else MaterialTheme.colorScheme.outline)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (speakToWakeEnabled && isWakeWordListening) "SPEAK TO WAKE ACTIVE" else "SPEAK TO WAKE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = if (speakToWakeEnabled && isWakeWordListening) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• \"$wakeWord\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkPrimary
                                    )
                                }
                                Text(
                                    text = if (speakToWakeEnabled && isWakeWordListening)
                                        "Say \"$wakeWord...\" anytime to wake agent"
                                    else
                                        "Tap to activate hands-free wake word",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Mini undulating audio visualizer bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (speakToWakeEnabled && isWakeWordListening) {
                                val heights = listOf(14.dp, 8.dp, 16.dp, 10.dp, 18.dp)
                                heights.forEachIndexed { i, h ->
                                    val barScale by infiniteTransition.animateFloat(
                                        initialValue = 0.4f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(400 + i * 150, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bar_$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h * barScale)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(StatusSuccess)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Interactive quick wake word selector
                    AnimatedVisibility(visible = showWakeWordPicker) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Choose Wake Phrase:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Jarvis", "Tasker", "Guruji", "TaskPilot").forEach { word ->
                                    val isSelected = wakeWord.equals(word, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) DarkPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                prefs.setWakeWord(word)
                                                if (!speakToWakeEnabled) prefs.setSpeakToWakeEnabled(true)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = word,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Raycast-style Master Command Input Terminal
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_input_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF383C48), Color(0xFF1D2028))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header label inside terminal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(DarkPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AGENT DIRECTIVE TERMINAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (instruction.isNotEmpty()) {
                            Text(
                                text = "${instruction.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Multiline Text field
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { viewModel.updateInputInstruction(it) },
                        placeholder = {
                            Text(
                                "What would you like me to do?\n(e.g., Open YouTube and search for Adarsh Chaturvedi 99...)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("instruction_text_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkPrimary.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                            focusedContainerColor = Color(0xFF0F1115),
                            unfocusedContainerColor = Color(0xFF0F1115),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Attachment pill if present
                    if (attachedFileName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = DarkPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = attachedFileName ?: "Attached Document",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove file",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { attachedFileName = null },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Command Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Voice Microphone Button
                            IconButton(
                                onClick = { audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E212A))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .testTag("voice_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = DarkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Document Attachment Button
                            IconButton(
                                onClick = { filePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E212A))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .testTag("attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach Document",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (instruction.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearInput() },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E212A))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                        .testTag("clear_input_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Input",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Run / Plan Directive Button with smooth glow
                        Button(
                            onClick = { viewModel.generatePlan(instruction) },
                            enabled = instruction.isNotBlank() && !isPlanning,
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("plan_task_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimary,
                                disabledContainerColor = DarkPrimary.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isPlanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Text("Execute Directive", fontWeight = FontWeight.Bold, color = Color(0xFF0F1115))
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF0F1115),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Planning Error Banner
        item {
            AnimatedVisibility(visible = planningError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = planningError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError
                        )
                    }
                }
            }
        }

        // 3. Featured Directives & Curated Prompts
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CURATED DIRECTIVES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "TAP TO RUN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Flagship YouTube creator card with gradient border
                val flagshipPrompt = "Open YouTube, search for Adarsh Chaturvedi 99, find his channel and open it. Return to the home screen when finished."
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateInputInstruction(flagshipPrompt) }
                        .testTag("flagship_youtube_directive_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14171E)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(DarkPrimary.copy(alpha = 0.6f), DarkSecondary.copy(alpha = 0.3f)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FEATURED WORKFLOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = DarkPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• YouTube Channel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Search Adarsh Chaturvedi 99 on YouTube, navigate to creator channel, and return to home.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Curated chips
                val otherExamples = listOf(
                    Pair("Open Chrome and search for latest Android updates.", Icons.Default.Language),
                    Pair("Open Google Maps and navigate to Central Park.", Icons.Default.LocationOn),
                    Pair("Create a reminder for tomorrow at 9 AM.", Icons.Default.Today),
                    Pair("Open Android Wi-Fi settings.", Icons.Default.Settings)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otherExamples.forEach { (prompt, icon) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateInputInstruction(prompt) }
                                .testTag("curated_directive_${prompt.take(12)}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Native App Connectors Grid
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTEGRATED DESTINATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "VIEW CATALOG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkPrimary,
                        modifier = Modifier.clickable { viewModel.setDestination(NavDestination.APPS) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val connectors = listOf(
                        Pair("YouTube", Icons.Default.PlayArrow),
                        Pair("Chrome", Icons.Default.Language),
                        Pair("Maps", Icons.Default.LocationOn),
                        Pair("Dialer", Icons.Default.Call),
                        Pair("Calendar", Icons.Default.Today),
                        Pair("Settings", Icons.Default.Settings)
                    )

                    items(connectors) { (name, icon) ->
                        Card(
                            modifier = Modifier
                                .width(94.dp)
                                .clickable { viewModel.setDestination(NavDestination.APPS) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1C1E26)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = name, tint = DarkPrimary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(StatusSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Ready", style = MaterialTheme.typography.labelSmall, color = StatusSuccess, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Activity
        if (recentTasks.isNotEmpty()) {
            item {
                val latest = recentTasks.first()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LAST COMPLETED TASK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDestination(NavDestination.ACTIVITY) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latest.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${latest.completedSteps}/${latest.totalSteps} steps completed • ${latest.durationMs / 1000}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
