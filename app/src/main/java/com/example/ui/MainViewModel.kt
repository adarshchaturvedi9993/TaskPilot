package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apps.AppIntegrationInfo
import com.example.apps.AppIntegrationManager
import com.example.data.ai.AiTaskPlanner
import com.example.data.local.TaskEntity
import com.example.data.local.TaskPilotDatabase
import com.example.data.model.ActionType
import com.example.data.model.StepStatus
import com.example.data.model.TaskOverallStatus
import com.example.data.model.TaskPlan
import com.example.data.model.TaskStep
import com.example.data.prefs.UserPreferencesRepository
import com.example.engine.TaskExecutionEngine
import com.example.voice.VoiceRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TaskPilotDatabase.getDatabase(application)
    val taskDao = database.taskDao()
    val prefsRepo = UserPreferencesRepository(application)
    val appIntegrationManager = AppIntegrationManager(application)
    val aiPlanner = AiTaskPlanner(application, appIntegrationManager)
    val voiceHelper = VoiceRecognitionHelper(application)
    val executionEngine = TaskExecutionEngine(application, appIntegrationManager, taskDao, viewModelScope)

    // Current navigation tab
    private val _currentDestination = MutableStateFlow(NavDestination.HOME)
    val currentDestination: StateFlow<NavDestination> = _currentDestination.asStateFlow()

    // Instruction input
    private val _inputInstruction = MutableStateFlow("")
    val inputInstruction: StateFlow<String> = _inputInstruction.asStateFlow()

    // AI Planning states
    private val _isPlanning = MutableStateFlow(false)
    val isPlanning: StateFlow<Boolean> = _isPlanning.asStateFlow()

    private val _reviewPlan = MutableStateFlow<TaskPlan?>(null)
    val reviewPlan: StateFlow<TaskPlan?> = _reviewPlan.asStateFlow()

    private val _planningError = MutableStateFlow<String?>(null)
    val planningError: StateFlow<String?> = _planningError.asStateFlow()

    // Speak to Wake state
    private val _showWakeOverlay = MutableStateFlow(false)
    val showWakeOverlay: StateFlow<Boolean> = _showWakeOverlay.asStateFlow()

    private val _detectedWakeWord = MutableStateFlow("Jarvis")
    val detectedWakeWord: StateFlow<String> = _detectedWakeWord.asStateFlow()

    // Apps list
    private val _installedApps = MutableStateFlow<List<AppIntegrationInfo>>(emptyList())
    val installedApps: StateFlow<List<AppIntegrationInfo>> = _installedApps.asStateFlow()

    // History filter & search
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("ALL")

    val allTasks = combine(taskDao.getAllTasks(), searchQuery, statusFilter) { tasks, query, filter ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.rawInstruction.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "ALL" -> true
                else -> task.status.equals(filter, ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App launch test feedback
    private val _testMessage = MutableStateFlow<String?>(null)
    val testMessage: StateFlow<String?> = _testMessage.asStateFlow()

    init {
        refreshApps()
        observeSpeakToWake()
    }

    private fun observeSpeakToWake() {
        viewModelScope.launch {
            combine(prefsRepo.speakToWakeEnabled, prefsRepo.wakeWord) { enabled, word ->
                Pair(enabled, word)
            }.collect { (enabled, word) ->
                if (enabled) {
                    voiceHelper.startWakeWordListening(word) { detectedWord, followUpCommand ->
                        onWakeWordTriggered(detectedWord, followUpCommand)
                    }
                } else {
                    voiceHelper.stopWakeWordListening()
                }
            }
        }
    }

    fun onWakeWordTriggered(wakeWord: String, followUpCommand: String?) {
        _detectedWakeWord.value = wakeWord
        if (!followUpCommand.isNullOrBlank()) {
            // Hotword was spoken along with the command (e.g. "Jarvis search for cricket news")
            _inputInstruction.value = followUpCommand
            _showWakeOverlay.value = false
            generatePlan(followUpCommand)
        } else {
            // Hotword spoken alone: open listening overlay
            _showWakeOverlay.value = true
            voiceHelper.startListening()
        }
    }

    fun dismissWakeOverlay() {
        _showWakeOverlay.value = false
        voiceHelper.stopListening()
        // Re-arm speak to wake if enabled
        if (prefsRepo.speakToWakeEnabled.value) {
            voiceHelper.startWakeWordListening(prefsRepo.wakeWord.value) { detectedWord, followUpCommand ->
                onWakeWordTriggered(detectedWord, followUpCommand)
            }
        }
    }

    fun setDestination(dest: NavDestination) {
        _currentDestination.value = dest
    }

    fun updateInputInstruction(text: String) {
        _inputInstruction.value = text
    }

    fun clearInput() {
        _inputInstruction.value = ""
        _planningError.value = null
    }

    fun refreshApps() {
        _installedApps.value = appIntegrationManager.getAllIntegrations()
    }

    fun generatePlan(instruction: String = _inputInstruction.value) {
        if (instruction.isBlank()) {
            _planningError.value = "Please enter an instruction first."
            return
        }

        _isPlanning.value = true
        _planningError.value = null
        _reviewPlan.value = null

        viewModelScope.launch {
            val isLocalOnly = prefsRepo.aiProvider.value == "local"
            val apiKey = prefsRepo.geminiApiKey.value
            val model = prefsRepo.selectedModel.value

            val result = aiPlanner.planTask(
                instruction = instruction,
                apiKey = apiKey,
                selectedModel = model,
                forceLocalEngine = isLocalOnly
            )

            _isPlanning.value = false
            result.onSuccess { plan ->
                _reviewPlan.value = plan
            }.onFailure { err ->
                _planningError.value = err.localizedMessage ?: "Failed to generate task plan."
            }
        }
    }

    fun dismissPlanReview() {
        _reviewPlan.value = null
    }

    fun toggleStepEnabled(index: Int) {
        val plan = _reviewPlan.value ?: return
        if (index in plan.steps.indices) {
            val step = plan.steps[index]
            step.isEnabled = !step.isEnabled
            // Trigger state change
            _reviewPlan.value = plan.copy(steps = plan.steps.toList())
        }
    }

    fun startExecutingPlan(plan: TaskPlan) {
        _reviewPlan.value = null
        _currentDestination.value = NavDestination.TASKS
        executionEngine.startExecution(plan)
    }

    fun confirmCurrentStep() {
        executionEngine.confirmCurrentStep()
    }

    fun completeUserAction() {
        executionEngine.completeUserActionStep()
    }

    fun stopExecution() {
        executionEngine.stopExecution()
    }

    fun retryExecution() {
        executionEngine.retryFailedStep()
    }

    fun retryHistoricalTask(task: TaskEntity) {
        val steps = parseStepsFromJson(task.stepsJson)
        val plan = TaskPlan(
            title = task.title,
            summary = "Retrying: ${task.rawInstruction}",
            rawInstruction = task.rawInstruction,
            steps = steps
        )
        _currentDestination.value = NavDestination.TASKS
        executionEngine.startExecution(plan)
    }

    private fun parseStepsFromJson(jsonStr: String): List<TaskStep> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<TaskStep>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = try {
                    ActionType.valueOf(obj.getString("actionType"))
                } catch (_: Exception) {
                    ActionType.WEB_SEARCH
                }
                list.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        order = obj.optInt("order", i + 1),
                        title = obj.optString("title", "Step ${i + 1}"),
                        description = obj.optString("description", ""),
                        actionType = type,
                        status = StepStatus.PENDING
                    )
                )
            }
            list
        } catch (_: Exception) {
            aiPlanner.planWithLocalSemanticEngine("").steps
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            taskDao.deleteTask(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            taskDao.clearAll()
        }
    }

    fun testAppLaunch(integrationId: String) {
        val (success, msg) = appIntegrationManager.testLaunchApp(integrationId)
        _testMessage.value = msg
    }

    fun clearTestMessage() {
        _testMessage.value = null
    }
}

enum class NavDestination(val label: String) {
    HOME("Home"),
    TASKS("Tasks"),
    APPS("Apps"),
    ACTIVITY("Activity"),
    SETTINGS("Settings")
}
