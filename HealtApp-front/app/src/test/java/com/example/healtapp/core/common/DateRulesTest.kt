package com.example.healtapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateRulesTest {

    @Test
    fun `isFuture returns true for future dates`() {
        val tomorrow = LocalDate.now().plusDays(1)
        assertTrue(DateRules.isFuture(tomorrow))
    }

    @Test
    fun `isFuture returns false for today and past dates`() {
        val today = LocalDate.now()
        val yesterday = LocalDate.now().minusDays(1)
        
        assertFalse(DateRules.isFuture(today))
        assertFalse(DateRules.isFuture(yesterday))
    }

    @Test
    fun `isFutureDateString returns correct boolean`() {
        val tomorrowString = LocalDate.now().plusDays(1).toString()
        val todayString = LocalDate.now().toString()
        val invalidString = "invalid-date"

        assertTrue(DateRules.isFutureDateString(tomorrowString))
        assertFalse(DateRules.isFutureDateString(todayString))
        assertFalse(DateRules.isFutureDateString(invalidString))
    }

    @Test
    fun `clampToTodayOrPast clamps future dates to today`() {
        val future = LocalDate.now().plusDays(5)
        val today = LocalDate.now()
        val past = LocalDate.now().minusDays(2)

        assertEquals(today, DateRules.clampToTodayOrPast(future))
        assertEquals(today, DateRules.clampToTodayOrPast(today))
        assertEquals(past, DateRules.clampToTodayOrPast(past))
    }
}
