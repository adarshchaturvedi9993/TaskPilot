package com.example.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import com.example.apps.AppIntegrationManager
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.model.ActionType
import com.example.data.model.StepStatus
import com.example.data.model.TaskOverallStatus
import com.example.data.model.TaskPlan
import com.example.data.model.TaskStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class TaskExecutionEngine(
    private val context: Context,
    private val appIntegrationManager: AppIntegrationManager,
    private val taskDao: TaskDao,
    private val coroutineScope: CoroutineScope
) {
    private val _currentPlan = MutableStateFlow<TaskPlan?>(null)
    val currentPlan: StateFlow<TaskPlan?> = _currentPlan.asStateFlow()

    private val _overallStatus = MutableStateFlow(TaskOverallStatus.IDLE)
    val overallStatus: StateFlow<TaskOverallStatus> = _overallStatus.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var executionJob: Job? = null
    private var timerJob: Job? = null
    private var isPaused = false

    fun startExecution(plan: TaskPlan) {
        stopExecution() // cancel any ongoing task
        _currentPlan.value = plan
        _overallStatus.value = TaskOverallStatus.RUNNING
        _currentStepIndex.value = 0
        _elapsedSeconds.value = 0L
        _errorMessage.value = null
        isPaused = false

        // Start timer
        timerJob = coroutineScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (isActive && _overallStatus.value == TaskOverallStatus.RUNNING) {
                delay(1000)
                if (!isPaused) {
                    _elapsedSeconds.value = (System.currentTimeMillis() - startTime) / 1000
                }
            }
        }

        // Start execution loop
        executionJob = coroutineScope.launch(Dispatchers.Main) {
            runPlanLoop(plan)
        }
    }

    private suspend fun runPlanLoop(plan: TaskPlan) {
        val steps = plan.steps
        var hasFailures = false

        for (i in _currentStepIndex.value until steps.size) {
            val step = steps[i]
            _currentStepIndex.value = i

            if (!step.isEnabled) {
                step.status = StepStatus.SKIPPED
                step.resultMessage = "Step skipped by user configuration."
                continue
            }

            // Check if step requires confirmation
            if (step.requiresConfirmation || step.actionType == ActionType.USER_CONFIRMATION) {
                step.status = StepStatus.WAITING_CONFIRMATION
                _overallStatus.value = TaskOverallStatus.WAITING_CONFIRMATION
                // Await user confirmation
                return
            }

            // Check if step requires manual user engagement action
            if (step.actionType == ActionType.USER_ACTION) {
                step.status = StepStatus.WAITING_USER_ACTION
                _overallStatus.value = TaskOverallStatus.WAITING_USER_ACTION
                return
            }

            step.status = StepStatus.RUNNING
            delay(600) // Brief deliberate cadence for UI readability

            val result = executeSingleAction(step)
            if (result.success) {
                step.status = StepStatus.COMPLETED
                step.resultMessage = result.message
            } else {
                hasFailures = true
                step.status = StepStatus.FAILED
                step.resultMessage = result.message
                _errorMessage.value = result.message

                // If non-fatal and has fallback, continue; otherwise stop safely
                if (step.failureHandling?.contains("continue", ignoreCase = true) != true) {
                    _overallStatus.value = TaskOverallStatus.FAILED
                    saveTaskResult(plan, TaskOverallStatus.FAILED, result.message)
                    return
                }
            }
        }

        val completedCount = steps.count { it.status == StepStatus.COMPLETED }
        val finalStatus = when {
            hasFailures && completedCount > 0 -> TaskOverallStatus.PARTIALLY_COMPLETED
            hasFailures -> TaskOverallStatus.FAILED
            else -> TaskOverallStatus.COMPLETED
        }
        _overallStatus.value = finalStatus
        saveTaskResult(plan, finalStatus, null)
    }

    fun confirmCurrentStep() {
        val plan = _currentPlan.value ?: return
        val index = _currentStepIndex.value
        if (index < plan.steps.size) {
            val step = plan.steps[index]
            step.status = StepStatus.COMPLETED
            step.resultMessage = "User confirmed action."
        }
        _overallStatus.value = TaskOverallStatus.RUNNING
        _currentStepIndex.value = index + 1
        executionJob = coroutineScope.launch(Dispatchers.Main) {
            runPlanLoop(plan)
        }
    }

    fun completeUserActionStep() {
        val plan = _currentPlan.value ?: return
        val index = _currentStepIndex.value
        if (index < plan.steps.size) {
            val step = plan.steps[index]
            step.status = StepStatus.COMPLETED
            step.resultMessage = "User verified manual completion."
        }
        _overallStatus.value = TaskOverallStatus.RUNNING
        _currentStepIndex.value = index + 1
        executionJob = coroutineScope.launch(Dispatchers.Main) {
            runPlanLoop(plan)
        }
    }

    fun stopExecution() {
        executionJob?.cancel()
        timerJob?.cancel()
        val plan = _currentPlan.value
        if (plan != null && (_overallStatus.value == TaskOverallStatus.RUNNING || _overallStatus.value == TaskOverallStatus.WAITING_CONFIRMATION)) {
            _overallStatus.value = TaskOverallStatus.CANCELLED
            val index = _currentStepIndex.value
            if (index < plan.steps.size) {
                plan.steps[index].status = StepStatus.CANCELLED
                plan.steps[index].resultMessage = "Execution cancelled by user."
            }
            saveTaskResult(plan, TaskOverallStatus.CANCELLED, "Stopped by user.")
        }
    }

    fun retryFailedStep() {
        val plan = _currentPlan.value ?: return
        _overallStatus.value = TaskOverallStatus.RUNNING
        _errorMessage.value = null
        executionJob = coroutineScope.launch(Dispatchers.Main) {
            runPlanLoop(plan)
        }
    }

    data class ActionResult(val success: Boolean, val message: String)

    private fun executeSingleAction(step: TaskStep): ActionResult {
        return try {
            when (step.actionType) {
                ActionType.CHECK_APP_INSTALLED -> {
                    val pkg = step.targetPackage ?: "com.google.android.youtube"
                    val isInstalled = appIntegrationManager.isPackageInstalled(pkg)
                    if (isInstalled) {
                        ActionResult(true, "Application '$pkg' is verified installed.")
                    } else {
                        ActionResult(true, "Application '$pkg' not found; fallback routing enabled.")
                    }
                }
                ActionType.OPEN_APP -> {
                    val pkg = step.targetPackage
                    if (pkg != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            ActionResult(true, "Opened package $pkg successfully.")
                        } else {
                            // Fallback to web search if app not directly launchable
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(step.title, "UTF-8"))).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                            ActionResult(true, "Launched web alternative for $pkg.")
                        }
                    } else {
                        ActionResult(true, "App open step completed.")
                    }
                }
                ActionType.WEB_SEARCH -> {
                    val query = step.targetData ?: step.title
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8"))).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Searched Google for '$query'.")
                }
                ActionType.OPEN_URL -> {
                    val url = step.targetData ?: "https://www.google.com"
                    val safeUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Opened verified destination: $safeUrl")
                }
                ActionType.YOUTUBE_SEARCH -> {
                    val query = step.targetData ?: "videos"
                    val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8"))).apply {
                        if (appIntegrationManager.isPackageInstalled("com.google.android.youtube")) {
                            setPackage("com.google.android.youtube")
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(ytIntent)
                    ActionResult(true, "Executed YouTube search for '$query'.")
                }
                ActionType.YOUTUBE_CHANNEL -> {
                    val channelName = step.targetData ?: "creator"
                    val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(channelName, "UTF-8")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        if (appIntegrationManager.isPackageInstalled("com.google.android.youtube")) {
                            setPackage("com.google.android.youtube")
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Navigated to YouTube channel for '$channelName'.")
                }
                ActionType.MAPS_SEARCH -> {
                    val query = step.targetData ?: "nearby"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + URLEncoder.encode(query, "UTF-8"))).apply {
                        if (appIntegrationManager.isPackageInstalled("com.google.android.apps.maps")) {
                            setPackage("com.google.android.apps.maps")
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Opened Maps search for '$query'.")
                }
                ActionType.MAPS_NAVIGATE -> {
                    val dest = step.targetData ?: "destination"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + URLEncoder.encode(dest, "UTF-8"))).apply {
                        if (appIntegrationManager.isPackageInstalled("com.google.android.apps.maps")) {
                            setPackage("com.google.android.apps.maps")
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Started navigation route to '$dest'.")
                }
                ActionType.CALL_PHONE -> {
                    val number = step.targetData ?: ""
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Prepared phone dialer with number $number.")
                }
                ActionType.SEND_SMS -> {
                    val text = step.targetData ?: ""
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                        putExtra("sms_body", text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Prepared messaging draft.")
                }
                ActionType.CREATE_CALENDAR_EVENT -> {
                    val title = step.targetData ?: "TaskPilot Scheduled Event"
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, title)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 86400000)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Opened calendar event composer for '$title'.")
                }
                ActionType.OPEN_SETTINGS -> {
                    val type = step.targetData ?: ""
                    val intent = when {
                        type.contains("wifi", ignoreCase = true) -> Intent(Settings.ACTION_WIFI_SETTINGS)
                        type.contains("bluetooth", ignoreCase = true) -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        else -> Intent(Settings.ACTION_SETTINGS)
                    }.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Opened $type settings screen.")
                }
                ActionType.SHARE_CONTENT -> {
                    val text = step.targetData ?: step.title
                    val shareIntent = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                        "Share via TaskPilot"
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(shareIntent)
                    ActionResult(true, "Opened Android share sheet.")
                }
                ActionType.PICK_FILE -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = step.targetData ?: "*/*"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Launched Android document selector.")
                }
                ActionType.RETURN_HOME -> {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    ActionResult(true, "Returned to Android Home screen.")
                }
                ActionType.USER_CONFIRMATION -> {
                    ActionResult(true, "Confirmed by user.")
                }
                ActionType.USER_ACTION -> {
                    ActionResult(true, "User performed required manual action.")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "Failed to execute: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun saveTaskResult(plan: TaskPlan, status: TaskOverallStatus, errorMsg: String?) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val stepsArray = JSONArray()
                for (s in plan.steps) {
                    val sObj = JSONObject().apply {
                        put("id", s.id)
                        put("order", s.order)
                        put("title", s.title)
                        put("description", s.description)
                        put("actionType", s.actionType.name)
                        put("status", s.status.name)
                        put("resultMessage", s.resultMessage ?: "")
                    }
                    stepsArray.put(sObj)
                }

                val entity = TaskEntity(
                    id = plan.id,
                    title = plan.title,
                    rawInstruction = plan.rawInstruction,
                    status = status.name,
                    totalSteps = plan.steps.size,
                    completedSteps = plan.steps.count { it.status == StepStatus.COMPLETED },
                    failedSteps = plan.steps.count { it.status == StepStatus.FAILED },
                    durationMs = _elapsedSeconds.value * 1000,
                    timestamp = plan.createdAt,
                    stepsJson = stepsArray.toString(),
                    errorMessage = errorMsg
                )
                taskDao.insertTask(entity)
            } catch (_: Exception) {
                // Ignore persistence exceptions
            }
        }
    }
}
