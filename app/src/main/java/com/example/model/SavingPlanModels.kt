package com.example.model

import java.text.NumberFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

data class SavingMonthSpec(
    val index: Int,
    val year: Int,
    val month: Int, // 1 = Jan .. 12 = Dec
    val nameBn: String,
    val nameEn: String,
    val yearBn: String,
    val totalDays: Int,
    val startDayOfWeekIndex: Int // 0 = Sunday .. 6 = Saturday
) {
    val evenSavingDaysCount: Int
        get() = totalDays / 2

    val monthlyTarget: Int
        get() = evenSavingDaysCount * 100

    val titleBn: String
        get() = "$nameBn $yearBn"

    val titleEn: String
        get() = "$nameEn $year"
}

data class CalendarDayCell(
    val dateIso: String,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val dayOfMonthBn: String,
    val weekdayIndex: Int, // 0 = Sun .. 6 = Sat
    val weekdayNameBn: String,
    val weekdayNameEn: String,
    val isSavingDay: Boolean, // true for even-numbered dates
    val amount: Int, // 100 for saving days, 0 for odd dates
    val isPaid: Boolean
)

data class MonthlyReportStats(
    val monthSpec: SavingMonthSpec,
    val monthlyTarget: Int,
    val amountSaved: Int,
    val remainingAmount: Int,
    val completedPaymentDays: Int,
    val pendingPaymentDays: Int,
    val progressFraction: Float
)

data class DashboardStats(
    val totalTarget: Int = 10400,
    val totalSaved: Int = 0,
    val remainingAmount: Int = 10400,
    val totalSavingDays: Int = 104,
    val totalPaidDays: Int = 0,
    val totalDueDays: Int = 104,
    val progressPercentage: Float = 0f,
    val progressFraction: Float = 0f
)

object SavingPlanCalendar {
    const val SAVING_AMOUNT_PER_DAY = 100
    const val TOTAL_SAVING_DAYS = 104
    const val TOTAL_TARGET_AMOUNT = 10400

    val WEEKDAYS_BN = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
    val WEEKDAYS_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val WEEKDAYS_FULL_BN = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    // 7 Months of the Saving Plan: 1 December 2026 to 30 June 2027
    val PLAN_MONTHS: List<SavingMonthSpec> = listOf(
        createMonthSpec(0, 2026, 12, "ডিসেম্বর", "December", "২০২৬", 31),
        createMonthSpec(1, 2027, 1, "জানুয়ারি", "January", "২০২৭", 31),
        createMonthSpec(2, 2027, 2, "ফেব্রুয়ারি", "February", "২০২৭", 28),
        createMonthSpec(3, 2027, 3, "মার্চ", "March", "২০২৭", 31),
        createMonthSpec(4, 2027, 4, "এপ্রিল", "April", "২০২৭", 30),
        createMonthSpec(5, 2027, 5, "মে", "May", "২০২৭", 31),
        createMonthSpec(6, 2027, 6, "জুন", "June", "২০২৭", 30)
    )

    private fun createMonthSpec(
        index: Int,
        year: Int,
        month: Int,
        nameBn: String,
        nameEn: String,
        yearBn: String,
        totalDays: Int
    ): SavingMonthSpec {
        val cal = GregorianCalendar(year, month - 1, 1)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday .. 7 = Saturday
        return SavingMonthSpec(
            index = index,
            year = year,
            month = month,
            nameBn = nameBn,
            nameEn = nameEn,
            yearBn = yearBn,
            totalDays = totalDays,
            startDayOfWeekIndex = dayOfWeek - 1
        )
    }

    fun formatIsoDate(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun buildMonthCells(
        monthSpec: SavingMonthSpec,
        paidDates: Set<String>
    ): List<CalendarDayCell> {
        val cells = ArrayList<CalendarDayCell>(monthSpec.totalDays)
        for (day in 1..monthSpec.totalDays) {
            val dateIso = formatIsoDate(monthSpec.year, monthSpec.month, day)
            val isEven = (day % 2 == 0)
            val weekdayIdx = (monthSpec.startDayOfWeekIndex + (day - 1)) % 7
            cells.add(
                CalendarDayCell(
                    dateIso = dateIso,
                    year = monthSpec.year,
                    month = monthSpec.month,
                    dayOfMonth = day,
                    dayOfMonthBn = toBengaliNumber(day),
                    weekdayIndex = weekdayIdx,
                    weekdayNameBn = WEEKDAYS_FULL_BN[weekdayIdx],
                    weekdayNameEn = WEEKDAYS_EN[weekdayIdx],
                    isSavingDay = isEven,
                    amount = if (isEven) SAVING_AMOUNT_PER_DAY else 0,
                    isPaid = isEven && paidDates.contains(dateIso)
                )
            )
        }
        return cells
    }

    fun buildAllSavingDays(paidDates: Set<String>): List<CalendarDayCell> {
        return PLAN_MONTHS.flatMap { spec ->
            buildMonthCells(spec, paidDates).filter { it.isSavingDay }
        }
    }

    fun computeDashboardStats(paidDates: Set<String>): DashboardStats {
        val validSavingDates = PLAN_MONTHS.flatMap { spec ->
            (2..spec.totalDays step 2).map { day ->
                formatIsoDate(spec.year, spec.month, day)
            }
        }.toSet()

        val paidCount = paidDates.count { it in validSavingDates }
        val totalSaved = paidCount * SAVING_AMOUNT_PER_DAY
        val remaining = (TOTAL_TARGET_AMOUNT - totalSaved).coerceAtLeast(0)
        val dueDays = (TOTAL_SAVING_DAYS - paidCount).coerceAtLeast(0)
        val fraction = (paidCount.toFloat() / TOTAL_SAVING_DAYS.toFloat()).coerceIn(0f, 1f)
        val percentage = fraction * 100f

        return DashboardStats(
            totalTarget = TOTAL_TARGET_AMOUNT,
            totalSaved = totalSaved,
            remainingAmount = remaining,
            totalSavingDays = TOTAL_SAVING_DAYS,
            totalPaidDays = paidCount,
            totalDueDays = dueDays,
            progressPercentage = percentage,
            progressFraction = fraction
        )
    }

    fun computeMonthlyReport(
        monthSpec: SavingMonthSpec,
        paidDates: Set<String>
    ): MonthlyReportStats {
        val totalEvenDays = monthSpec.evenSavingDaysCount
        var completedDays = 0
        for (day in 2..monthSpec.totalDays step 2) {
            val iso = formatIsoDate(monthSpec.year, monthSpec.month, day)
            if (paidDates.contains(iso)) {
                completedDays++
            }
        }
        val pendingDays = (totalEvenDays - completedDays).coerceAtLeast(0)
        val target = monthSpec.monthlyTarget
        val saved = completedDays * SAVING_AMOUNT_PER_DAY
        val remaining = (target - saved).coerceAtLeast(0)
        val fraction = if (totalEvenDays > 0) {
            (completedDays.toFloat() / totalEvenDays.toFloat()).coerceIn(0f, 1f)
        } else 0f

        return MonthlyReportStats(
            monthSpec = monthSpec,
            monthlyTarget = target,
            amountSaved = saved,
            remainingAmount = remaining,
            completedPaymentDays = completedDays,
            pendingPaymentDays = pendingDays,
            progressFraction = fraction
        )
    }

    fun formatRupee(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return "₹${formatter.format(amount)}"
    }

    fun formatRupeeBn(amount: Int): String {
        val formattedEn = NumberFormat.getNumberInstance(Locale.US).format(amount)
        return "₹${toBengaliDigits(formattedEn)}"
    }

    fun toBengaliNumber(number: Int): String {
        return toBengaliDigits(number.toString())
    }

    fun toBengaliDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
