package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun testAppIdentifierAndDefaults() {
        val appName = "TaskPilot"
        assertEquals("TaskPilot", appName)
        assertTrue(appName.isNotBlank())
    }
}
