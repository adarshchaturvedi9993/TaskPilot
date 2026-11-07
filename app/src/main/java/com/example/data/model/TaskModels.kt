package com.example.data.model

import java.util.UUID

enum class ActionType(val displayName: String, val category: String) {
    CHECK_APP_INSTALLED("Check App Availability", "System"),
    OPEN_APP("Open Application", "Navigation"),
    WEB_SEARCH("Web Search", "Browser"),
    OPEN_URL("Open Web Page", "Browser"),
    YOUTUBE_SEARCH("Search YouTube", "Media"),
    YOUTUBE_CHANNEL("Open YouTube Channel", "Media"),
    MAPS_SEARCH("Search Location", "Navigation"),
    MAPS_NAVIGATE("Start Navigation", "Navigation"),
    CALL_PHONE("Prepare Phone Call", "Communication"),
    SEND_SMS("Prepare Text Message", "Communication"),
    CREATE_CALENDAR_EVENT("Create Calendar Event", "Productivity"),
    OPEN_SETTINGS("Open Settings", "System"),
    SHARE_CONTENT("Share Content", "System"),
    PICK_FILE("Select File / Document", "Storage"),
    RETURN_HOME("Return to Home Screen", "System"),
    USER_CONFIRMATION("User Confirmation", "Safety"),
    USER_ACTION("User Intervention Required", "Safety")
}

enum class StepStatus {
    PENDING,
    RUNNING,
    WAITING_CONFIRMATION,
    WAITING_USER_ACTION,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED,
    SKIPPED,
    CANCELLED
}

enum class TaskOverallStatus {
    IDLE,
    PLANNING,
    REVIEWING,
    RUNNING,
    WAITING_CONFIRMATION,
    WAITING_USER_ACTION,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED,
    CANCELLED
}

data class TaskStep(
    val id: String = UUID.randomUUID().toString(),
    val order: Int,
    val title: String,
    val description: String,
    val actionType: ActionType,
    val targetPackage: String? = null,
    val targetData: String? = null,
    val requiredPermission: String? = null,
    val requiresConfirmation: Boolean = false,
    val verificationCondition: String? = null,
    val failureHandling: String? = "Stop and prompt user",
    var status: StepStatus = StepStatus.PENDING,
    var resultMessage: String? = null,
    var isEnabled: Boolean = true
)

data class TaskPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val rawInstruction: String,
    val steps: List<TaskStep>,
    val requiresUserConfirmation: Boolean = false,
    val estimatedDurationSec: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)
