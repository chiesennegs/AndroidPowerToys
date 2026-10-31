package dev.androidpowertoys.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** No writes, grants, app-ops changes, or settings-service changes. */
@RunWith(AndroidJUnit4::class)
class ReadOnlySmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun browseWithoutChangingSettingsOrAccess() {
        val backend = AndroidSettings(compose.activity)
        val secureBefore=backend.secureGranted
        val systemBefore=backend.systemGranted
        Table.entries.forEach { table -> val read = backend.list(table); assertNull("${table.label}: ${read.error}",read.error); assertTrue("${table.label} should expose some rows",read.rows.isNotEmpty()) }
        assertEquals(Cell.Absent, backend.read(Table.SECURE,"dev.androidpowertoys.nonexistent.readonly.test.9c014f"))
        compose.waitUntil(20000) { compose.onAllNodesWithText("Read-only mode").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Read-only mode").assertIsDisplayed()
        val viewport=compose.onNodeWithTag("settings-list").fetchSemanticsNode().boundsInRoot
        val window=compose.onNodeWithTag("app-window").fetchSemanticsNode().boundsInRoot
        val fraction=viewport.height/window.height
        if(!compose.activity.isInMultiWindowMode) assertTrue("Settings viewport occupies ${(fraction*100).toInt()}% of the window; expected at least 75%",fraction>=0.75f)
        val search=compose.onNodeWithTag("settings-search").fetchSemanticsNode().boundsInRoot
        val pinned=compose.onNodeWithTag("pinned-search").fetchSemanticsNode().boundsInRoot
        val header=compose.onNodeWithTag("app-header").fetchSemanticsNode().boundsInRoot
        val density=compose.activity.resources.displayMetrics.density
        val searchDp=search.height/density
        val titleGapDp=(search.top-header.bottom)/density
        assertTrue("Search must retain a 48 dp touch target",searchDp>=47.9f)
        if(compose.activity.resources.configuration.fontScale<=1.05f) assertTrue("Compact search should be shorter than the prior 56 dp field",searchDp<=50f)
        assertTrue("Title/search separation must remain at least 12 dp",titleGapDp>=11.9f)
        java.io.File(compose.activity.cacheDir,"layout-metrics.txt").writeText("viewportHeight=${viewport.height} windowHeight=${window.height} fraction=$fraction searchHeightDp=$searchDp titleGapDp=$titleGapDp pinnedHeightPx=${pinned.height} unobscuredRowsFraction=${(viewport.height-pinned.height)/window.height}")
        compose.waitForIdle()
        Thread.sleep(400)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(compose.activity.cacheDir,"settings-layout.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        }
        val repo=SafetyRepositories.get(compose.activity)
        compose.activity.runOnUiThread { compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        val revision=repo.revision.value
        assertNull(synchronized(repo) { repo.trials.get() })
        compose.activity.runOnUiThread { compose.activity.startForegroundService(android.content.Intent(compose.activity,RevertService::class.java).setAction("probe")) }
        compose.waitUntil(10000) { repo.revision.value>revision }
        assertEquals("No pending trial.",synchronized(repo) { repo.message })
        val baseline=synchronized(repo) { repo.baseline!! }
        assertTrue(baseline.values.isNotEmpty())
        assertEquals(baseline,BaselineStore(compose.activity).loadOrCapture(backend))
        compose.onNodeWithContentDescription("Table actions").performClick()
        compose.onNodeWithText("Enable editing").performClick()
        compose.onNodeWithText("Set up editing").assertIsDisplayed()
        compose.onNodeWithText(if(systemBefore) "Write permission detected for System" else "Write permission not granted for System").assertIsDisplayed()
        if(systemBefore) compose.onNodeWithText("Enable System editing").assertIsEnabled() else compose.onNodeWithText("Enable System editing").assertIsNotEnabled()
        compose.onNodeWithText("Open Modify system settings").assertExists()
        compose.onNode(hasText("Secure") and hasAnyAncestor(isDialog())).performClick()
        if(secureBefore) compose.onNodeWithText("Enable Secure editing").assertIsEnabled() else compose.onNodeWithText("Enable Secure editing").assertIsNotEnabled()
        compose.onNodeWithText("Set up wirelessly on this phone").performScrollTo().performClick()
        compose.onNodeWithText("Wireless setup · this phone").assertIsDisplayed()
        compose.onNodeWithTag("wireless-setup-list").performScrollToNode(hasText("Review wireless grant…"))
        compose.onNodeWithText("Review wireless grant…").assertIsNotEnabled()
        compose.onNodeWithText("Review wireless revoke…").assertIsNotEnabled()
        compose.onNodeWithText("Back to editing setup").performClick()
        compose.onNodeWithText("Copy grant command").performScrollTo().performClick()
        val clipboard=compose.activity.getSystemService(android.content.ClipboardManager::class.java)
        assertEquals("adb shell pm grant --user current dev.androidpowertoys.app android.permission.WRITE_SECURE_SETTINGS",clipboard.primaryClip!!.getItemAt(0).text.toString())
        compose.waitForIdle()
        Thread.sleep(400)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(compose.activity.cacheDir,"editing-setup.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        }
        compose.onNode(hasText("Global") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        if(secureBefore) compose.onNodeWithText("Enable Global editing").assertIsEnabled() else compose.onNodeWithText("Enable Global editing").assertIsNotEnabled()
        compose.onNodeWithText("Stay read-only").performClick()
        compose.onNodeWithText("Read-only mode").assertIsDisplayed()
        assertEquals(secureBefore,backend.secureGranted)
        assertEquals(systemBefore,backend.systemGranted)
        compose.onNodeWithContentDescription("Table actions").performClick()
        compose.onNodeWithText("Defaults & starting-value restore").performClick()
        compose.onNodeWithText("Defaults & restore").assertIsDisplayed()
        compose.onNodeWithText("Find a default or starting value").performTextInput("screen_brightness")
        compose.onNodeWithText("System · screen_brightness").assertIsDisplayed()
        compose.onNodeWithText("Tables", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("settings-search").performTextInput("screen_brightness")
        compose.onNode(hasText("screen_brightness") and !hasSetTextAction(), useUnmergedTree = true).performClick()
        compose.onNode(hasText("Display / usability · 15-second trial") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.waitForIdle()
        Thread.sleep(400) // Let the platform dialog/IME transition settle before visual capture.
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(compose.activity.cacheDir,"safety-detail.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        }
        compose.onNodeWithText("Close", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("settings-search").performTextClearance()
        compose.onNodeWithText("System ▾").performClick()
        compose.onNodeWithText("Secure", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("settings-search").performTextInput("android_id")
        compose.onNode(hasText("android_id") and !hasSetTextAction(), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Diagnose").assertDoesNotExist()
        compose.onNodeWithText("About", useUnmergedTree = true).performClick()
        compose.onNodeWithText("About & support").assertIsDisplayed()
        compose.onNodeWithContentDescription("Android PowerToys app logo").assertIsDisplayed()
        // Inspect publisher/contact artwork in both app themes; links are never opened.
        for(theme in listOf("Dark","Light")) {
            compose.onNodeWithTag("about-list").performScrollToNode(hasText(theme))
            compose.onNodeWithText(theme).performClick()
            compose.onNodeWithTag("about-list").performScrollToIndex(0)
            compose.onNodeWithText("© 2026 MooseAI, LLC").performScrollTo()
            compose.onNodeWithText("Created by MooseAI, LLC").assertIsDisplayed()
            compose.onNodeWithContentDescription("MooseAI, LLC logo").assertIsDisplayed()
            compose.onNodeWithText("Email support").assertIsDisplayed()
            compose.waitForIdle()
            Thread.sleep(300)
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
                java.io.File(compose.activity.cacheDir,"mooseai-${theme.lowercase()}.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
            }
        }

        compose.onNodeWithTag("about-list").performScrollToNode(hasText("Advanced permission details"))
        compose.onNodeWithText("Advanced permission details").performClick()
        compose.onNodeWithText("Copy grant command").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Open Modify system settings").assertExists()
        compose.onNodeWithText("Check access again").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Copy grant command").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Access",useUnmergedTree=true).assertDoesNotExist()
        compose.onNodeWithText("History", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Change history").assertIsDisplayed()
        assertEquals(secureBefore,backend.secureGranted)
        val snapshot = backend.snapshot()
        assertEquals(snapshot, SnapshotJson.decode(SnapshotJson.encode(snapshot)))
    }
}
