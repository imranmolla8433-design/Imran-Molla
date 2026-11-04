package com.example

import com.example.model.SavingPlanCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun savingPlan_hasExact104DaysAnd10400Target() {
        val allSavingDays = SavingPlanCalendar.buildAllSavingDays(emptySet())
        assertEquals(104, allSavingDays.size)
        assertEquals(10400, SavingPlanCalendar.TOTAL_TARGET_AMOUNT)

        // Verify starting month is December 2026 and ending month is June 2027
        assertEquals(2026, SavingPlanCalendar.PLAN_MONTHS.first().year)
        assertEquals(12, SavingPlanCalendar.PLAN_MONTHS.first().month)
        assertEquals(2027, SavingPlanCalendar.PLAN_MONTHS.last().year)
        assertEquals(6, SavingPlanCalendar.PLAN_MONTHS.last().month)

        // Every saving day must be even-numbered and ₹100
        allSavingDays.forEach { cell ->
            assertTrue(cell.dayOfMonth % 2 == 0)
            assertEquals(100, cell.amount)
            assertFalse(cell.isPaid)
        }
    }

    @Test
    fun dashboardAndMonthlyStats_calculateAccuratelyWhenDatesPaid() {
        val samplePaid = setOf("2026-12-02", "2026-12-04", "2026-12-06")
        val dashboard = SavingPlanCalendar.computeDashboardStats(samplePaid)

        assertEquals(10400, dashboard.totalTarget)
        assertEquals(300, dashboard.totalSaved)
        assertEquals(10100, dashboard.remainingAmount)
        assertEquals(3, dashboard.totalPaidDays)
        assertEquals(101, dashboard.totalDueDays)

        val decReport = SavingPlanCalendar.computeMonthlyReport(
            SavingPlanCalendar.PLAN_MONTHS[0],
            samplePaid
        )
        assertEquals(1500, decReport.monthlyTarget)
        assertEquals(300, decReport.amountSaved)
        assertEquals(1200, decReport.remainingAmount)
        assertEquals(3, decReport.completedPaymentDays)
        assertEquals(12, decReport.pendingPaymentDays)
    }
}
