package com.example.healtapp.core.dataimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportCsvFormatTest {

    @Test
    fun rowKind_parsesType() {
        assertEquals("hydration", ImportCsvFormat.rowKind("hydration;2026-05-14T10:00:00;250"))
        assertEquals("sleep", ImportCsvFormat.rowKind("sleep;2026-05-13T23:00:00;2026-05-14T07:00:00"))
    }

    @Test
    fun rowKind_ignoresCommentsAndBlank() {
        assertNull(ImportCsvFormat.rowKind("  # comment"))
        assertNull(ImportCsvFormat.rowKind("   "))
    }
}
