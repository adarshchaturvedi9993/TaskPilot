package com.example

import com.example.data.model.ActionType
import com.example.data.model.StepStatus
import com.example.data.model.TaskPlan
import com.example.data.model.TaskStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTaskPlanCreation() {
        val step1 = TaskStep(
            order = 1,
            title = "Check YouTube Availability",
            description = "Verify package on device",
            actionType = ActionType.CHECK_APP_INSTALLED,
            targetPackage = "com.google.android.youtube",
            status = StepStatus.PENDING
        )
        val step2 = TaskStep(
            order = 2,
            title = "Open YouTube App",
            description = "Launch app",
            actionType = ActionType.OPEN_APP,
            targetPackage = "com.google.android.youtube",
            status = StepStatus.PENDING
        )
        val step3 = TaskStep(
            order = 3,
            title = "Search for Adarsh Chaturvedi 99",
            description = "Channel search",
            actionType = ActionType.YOUTUBE_CHANNEL,
            targetData = "Adarsh Chaturvedi 99",
            status = StepStatus.PENDING
        )
        val step4 = TaskStep(
            order = 4,
            title = "Return to Home Screen",
            description = "Return home",
            actionType = ActionType.RETURN_HOME,
            status = StepStatus.PENDING
        )

        val plan = TaskPlan(
            title = "Find YouTube Channel: Adarsh Chaturvedi 99",
            summary = "Locate creator channel and navigate back.",
            rawInstruction = "Open YouTube, search for Adarsh Chaturvedi 99, find his channel and open it. Return to the home screen when finished.",
            steps = listOf(step1, step2, step3, step4)
        )

        assertNotNull(plan.id)
        assertEquals(4, plan.steps.size)
        assertEquals(ActionType.CHECK_APP_INSTALLED, plan.steps[0].actionType)
        assertEquals(ActionType.OPEN_APP, plan.steps[1].actionType)
        assertEquals(ActionType.YOUTUBE_CHANNEL, plan.steps[2].actionType)
        assertEquals(ActionType.RETURN_HOME, plan.steps[3].actionType)
        assertTrue(plan.title.contains("Adarsh Chaturvedi 99"))
    }

    @Test
    fun testActionTypeCategories() {
        assertEquals("System", ActionType.RETURN_HOME.category)
        assertEquals("Media", ActionType.YOUTUBE_SEARCH.category)
        assertEquals("Browser", ActionType.WEB_SEARCH.category)
        assertEquals("Navigation", ActionType.MAPS_NAVIGATE.category)
        assertEquals("Communication", ActionType.CALL_PHONE.category)
        assertEquals("Communication", ActionType.SEND_SMS.category)
        assertEquals("Productivity", ActionType.CREATE_CALENDAR_EVENT.category)
        assertEquals("Safety", ActionType.USER_CONFIRMATION.category)
        assertEquals("Safety", ActionType.USER_ACTION.category)
    }
}
