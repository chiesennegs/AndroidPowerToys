package dev.androidpowertoys.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Can run with the display off. All provider operations are reads. */
@RunWith(AndroidJUnit4::class)
class ProviderReadTest {
    @Test fun visibleTablesExactReadsAndSnapshotRoundTrip() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = AndroidSettings(context)
        val secureBefore=backend.secureGranted
        val systemBefore=backend.systemGranted
        Table.entries.forEach { table ->
            val read = backend.list(table)
            assertNull(read.error)
            assertTrue(read.rows.isNotEmpty())
            assertEquals(read.rows.first().cell, backend.read(table, read.rows.first().name))
        }
        assertEquals(Cell.Absent, backend.read(Table.SECURE, "dev.androidpowertoys.nonexistent.readonly.test.9c014f"))
        val snapshot = backend.snapshot()
        assertEquals(snapshot, SnapshotJson.decode(SnapshotJson.encode(snapshot)))
        assertTrue(compareSnapshots(snapshot, snapshot).isEmpty())
        assertEquals(systemBefore,backend.systemGranted)
        assertEquals(secureBefore,backend.secureGranted)
    }
}
