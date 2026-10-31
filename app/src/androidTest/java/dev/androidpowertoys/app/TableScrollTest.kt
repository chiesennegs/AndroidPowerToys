package dev.androidpowertoys.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableScrollTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun position()=compose.onNodeWithTag("settings-list").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
    private fun select(current: String,next: String) {
        compose.onNodeWithText("$current ▾").performClick()
        compose.onNodeWithText(next,useUnmergedTree=true).performClick()
        compose.waitForIdle()
    }
    @Test fun newTablesStartAtTopAndVisitedTablesRestoreTheirOwnDepth() {
        compose.waitUntil(20000) { compose.onAllNodesWithText("Read-only mode").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("settings-list").performScrollToIndex(18)
        val system=position();assertTrue(system>10)
        compose.onNodeWithTag("settings-search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Go to top").assertIsDisplayed()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(compose.activity.cacheDir,"pinned-search-scroll.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        }
        select("System","Secure");assertEquals(0f,position(),0.01f)
        compose.onNodeWithTag("settings-search").assertIsDisplayed()
        compose.onNodeWithTag("settings-list").performScrollToIndex(28)
        val secure=position();assertTrue(secure>20)
        select("Secure","Global");assertEquals(0f,position(),0.01f)
        compose.onNodeWithTag("settings-list").performScrollToIndex(38)
        val global=position();assertTrue(global>30)
        select("Global","System");assertEquals(system,position(),0.01f)
        select("System","Secure");assertEquals(secure,position(),0.01f)
        select("Secure","Global");assertEquals(global,position(),0.01f)
        compose.onNodeWithTag("settings-search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Go to top").performClick()
        compose.waitForIdle()
        assertEquals(0f,position(),0.01f)
        compose.onNodeWithContentDescription("Go to top").assertDoesNotExist()
        select("Global","System");assertEquals(system,position(),0.01f)
        compose.onNodeWithTag("settings-search").performTextInput("screen_brightness")
        compose.waitForIdle()
        assertEquals(0f,position(),0.01f)
        compose.onNodeWithContentDescription("Clear search").performClick()
        assertEquals("",compose.onNodeWithTag("settings-search").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Read-only mode").assertIsDisplayed()
        compose.onNodeWithText("Diagnose").assertDoesNotExist()
    }
}
