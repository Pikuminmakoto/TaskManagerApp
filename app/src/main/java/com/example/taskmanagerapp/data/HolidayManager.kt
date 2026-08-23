package com.example.taskmanagerapp.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

object HolidayManager {

    fun getHolidayName(date: LocalDate): String? {
        return buildHolidays(date.year)[date]
    }

    private fun buildHolidays(year: Int): Map<LocalDate, String> {

        val map = mutableMapOf<LocalDate, String>()

        map[LocalDate.of(year, 1, 1)] = "元日"
        map[nthMonday(year, 1, 2)] = "成人の日"
        map[LocalDate.of(year, 2, 11)] = "建国記念の日"
        map[LocalDate.of(year, 2, 23)] = "天皇誕生日"
        map[springEquinox(year)] = "春分の日"
        map[LocalDate.of(year, 4, 29)] = "昭和の日"
        map[LocalDate.of(year, 5, 3)] = "憲法記念日"
        map[LocalDate.of(year, 5, 4)] = "みどりの日"
        map[LocalDate.of(year, 5, 5)] = "こどもの日"
        map[nthMonday(year, 7, 3)] = "海の日"
        map[LocalDate.of(year, 8, 11)] = "山の日"
        map[nthMonday(year, 9, 3)] = "敬老の日"
        map[autumnEquinox(year)] = "秋分の日"
        map[nthMonday(year, 10, 2)] = "スポーツの日"
        map[LocalDate.of(year, 11, 3)] = "文化の日"
        map[LocalDate.of(year, 11, 23)] = "勤労感謝の日"

        // 振替休日（祝日が日曜の場合、直後の平日を休日にする）
        val substitutes = mutableMapOf<LocalDate, String>()

        for ((date, _) in map) {
            if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                var substitute = date.plusDays(1)
                while (map.containsKey(substitute)) {
                    substitute = substitute.plusDays(1)
                }
                substitutes[substitute] = "振替休日"
            }
        }

        map.putAll(substitutes)

        return map
    }

    private fun nthMonday(year: Int, month: Int, n: Int): LocalDate {
        val firstMonday = LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
        return firstMonday.plusWeeks((n - 1).toLong())
    }

    // 春分の日（近似計算式、1980〜2099年で有効）
    private fun springEquinox(year: Int): LocalDate {
        val day = (20.8431 + 0.242194 * (year - 1980)).toInt() -
                floor((year - 1980) / 4.0).toInt()
        return LocalDate.of(year, 3, day)
    }

    // 秋分の日（近似計算式、1980〜2099年で有効）
    private fun autumnEquinox(year: Int): LocalDate {
        val day = (23.2488 + 0.242194 * (year - 1980)).toInt() -
                floor((year - 1980) / 4.0).toInt()
        return LocalDate.of(year, 9, day)
    }
}