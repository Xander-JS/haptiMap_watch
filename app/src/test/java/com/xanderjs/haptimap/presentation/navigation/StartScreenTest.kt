package com.xanderjs.haptimap.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartScreenTest {

    @Test
    fun `first launch goes permission, then the tutorial`() {
        assertEquals(Screen.Permission, startScreen(hasPermission = false, navigating = false, tutorialSeen = false))
        assertEquals(Screen.Tutorial, screenAfterPermission(tutorialSeen = false))
    }

    @Test
    fun `with permission and the tutorial unseen, the tutorial comes before the resume prompt`() {
        assertEquals(Screen.Tutorial, startScreen(hasPermission = true, navigating = false, tutorialSeen = false))
    }

    @Test
    fun `once seen or skipped, straight to the normal flow`() {
        assertEquals(Screen.ResumeRoute, startScreen(hasPermission = true, navigating = false, tutorialSeen = true))
        assertEquals(Screen.Search, screenAfterPermission(tutorialSeen = true))
        assertEquals(Screen.Permission, startScreen(hasPermission = false, navigating = false, tutorialSeen = true))
    }

    @Test
    fun `an active navigation is never replaced by the tutorial`() {
        assertEquals(Screen.Search, startScreen(hasPermission = true, navigating = true, tutorialSeen = false))
        assertEquals(Screen.Search, startScreen(hasPermission = true, navigating = true, tutorialSeen = true))
    }
}
