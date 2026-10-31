package dev.androidpowertoys.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DescriptionCoverageTest {
    @Test fun reportProviderVisibleDescriptionCoverageWithoutValues() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val backend=AndroidSettings(context)
        val report=Table.entries.joinToString("\n") { table ->
            val read=backend.list(table);assertNull(read.error)
            val described=read.rows.count { explanations.containsKey(table to it.name) }
            assertTrue(described>0)
            "${table.label}: $described / ${read.rows.size} visible keys have sourced descriptions"
        }
        java.io.File(context.cacheDir,"description-coverage.txt").writeText(report)
        println(report)
    }
}
