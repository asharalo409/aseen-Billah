package com.manobseba.foundation.domain

import com.manobseba.foundation.data.DistrictsData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

data class PrayerLoc(
    val name: String = "ঢাকা",
    val lat: Double = 23.81,
    val lng: Double = 90.41,
    val tzHours: Double = 6.0
)

data class PrayerTimesMillis(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
)

data class PrayerWindow(
    val key: String,
    val label: String,
    val startMillis: Long,
    val endMillis: Long
)

data class PrayerStatus(
    val inPrayer: Boolean,
    val current: PrayerWindow,
    val next: PrayerWindow,
    val todayTimes: PrayerTimesMillis
)

data class HijriDateParts(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameBn: String
)

object PrayerCalculator {
    private const val RAD = PI / 180.0
    private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val ZONE_DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")

    val PRAYER_LABELS = mapOf(
        "fajr" to "ফজর",
        "sunrise" to "সূর্যোদয়",
        "dhuhr" to "যোহর",
        "asr" to "আসর",
        "maghrib" to "মাগরিব",
        "isha" to "এশা",
        "gap" to "মাকরুহ/বিরতি",
        "sehri" to "সাহরি",
        "iftar" to "ইফতার"
    )

    val HIJRI_MONTHS_BN = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শা’বান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )

    val BANGLA_MONTHS = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )

    val EN_MONTHS_BN = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    val WEEKDAYS_BN = listOf(
        "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার", "রবিবার"
    )

    fun bnDigits(raw: Any?): String {
        val s = raw?.toString() ?: ""
        val sb = StringBuilder(s.length)
        for (ch in s) {
            if (ch in '0'..'9') sb.append(BN_DIGITS[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun formatTaka(amount: Double): String {
        val rounded = amount.roundToLong()
        val formatted = String.format(Locale.US, "%,d", rounded)
        return "৳${bnDigits(formatted)}"
    }

    fun districtLoc(districtName: String): PrayerLoc {
        val d = DistrictsData.findByName(districtName)
        return PrayerLoc(d.name, d.lat, d.lng, 6.0)
    }

    private fun sinD(d: Double) = sin(d * RAD)
    private fun cosD(d: Double) = cos(d * RAD)
    private fun tanD(d: Double) = tan(d * RAD)
    private fun asinD(x: Double) = asin(x.coerceIn(-1.0, 1.0)) / RAD
    private fun acosD(x: Double) = acos(x.coerceIn(-1.0, 1.0)) / RAD
    private fun acotD(x: Double) = atan(1.0 / x) / RAD
    private fun atan2D(y: Double, x: Double) = atan2(y, x) / RAD

    private fun fix(a: Double, b: Double): Double {
        var r = a - b * floor(a / b)
        if (r < 0) r += b
        return r
    }

    private fun julian(yIn: Int, mIn: Int, d: Int): Double {
        var y = yIn
        var m = mIn
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun sun(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sinD(g) + 0.02 * sinD(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(atan2D(cosD(e) * sinD(l), cosD(l)) / 15.0, 24.0)
        val decl = asinD(sinD(e) * sinD(l))
        val eqt = q / 15.0 - ra
        return decl to eqt
    }

    fun computePrayerTimes(
        date: LocalDate,
        loc: PrayerLoc,
        asrFactor: Int = 2,
        adjustmentsMinutes: Map<String, Int> = emptyMap()
    ): PrayerTimesMillis {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        val jd = julian(y, m, d) - loc.lng / 360.0

        fun mid(t: Double): Double = fix(12.0 - sun(jd + t).second, 24.0)

        fun byAngle(ang: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(jd + t).first
            val x = (-sinD(ang) - sinD(decl) * sinD(loc.lat)) / (cosD(decl) * cosD(loc.lat))
            if (x < -1.0 || x > 1.0) return Double.NaN
            val h = acosD(x) / 15.0
            return mid(t) + if (ccw) -h else h
        }

        fun asr(t: Double): Double {
            val decl = sun(jd + t).first
            return byAngle(-acotD(asrFactor + tanD(abs(loc.lat - decl))), t, false)
        }

        val off = loc.tzHours - loc.lng / 15.0
        val baseUtc = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        fun toMillis(hourLocal: Double, key: String): Long {
            val safeH = if (hourLocal.isNaN()) 12.0 else hourLocal
            val adjMin = adjustmentsMinutes[key] ?: 0
            return baseUtc + ((safeH - loc.tzHours) * 3_600_000.0).toLong() + adjMin * 60_000L
        }

        return PrayerTimesMillis(
            fajr = toMillis(byAngle(18.0, 5.0 / 24.0, true) + off, "fajr"),
            sunrise = toMillis(byAngle(0.833, 6.0 / 24.0, true) + off, "sunrise"),
            dhuhr = toMillis(mid(12.0 / 24.0) + off, "dhuhr"),
            asr = toMillis(asr(13.0 / 24.0) + off, "asr"),
            maghrib = toMillis(byAngle(0.833, 18.0 / 24.0, false) + off, "maghrib"),
            isha = toMillis(byAngle(18.0, 18.0 / 24.0, false) + off, "isha")
        )
    }

    fun getStatus(
        nowMillis: Long = System.currentTimeMillis(),
        loc: PrayerLoc = PrayerLoc(),
        asrFactor: Int = 2
    ): PrayerStatus {
        val zone = ZONE_DHAKA
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val todayT = computePrayerTimes(today, loc, asrFactor)
        val effDate = if (nowMillis < todayT.fajr) today.minusDays(1) else today
        val t = computePrayerTimes(effDate, loc, asrFactor)
        val nextDayFajr = computePrayerTimes(effDate.plusDays(1), loc, asrFactor).fajr

        val windows = listOf(
            PrayerWindow("fajr", "ফজর", t.fajr, t.sunrise),
            PrayerWindow("dhuhr", "যোহর", t.dhuhr, t.asr),
            PrayerWindow("asr", "আসর", t.asr, t.maghrib),
            PrayerWindow("maghrib", "মাগরিব", t.maghrib, t.isha),
            PrayerWindow("isha", "এশা", t.isha, nextDayFajr)
        )

        for (i in windows.indices) {
            val w = windows[i]
            if (nowMillis in w.startMillis until w.endMillis) {
                val nextW = if (i < windows.lastIndex) {
                    windows[i + 1]
                } else {
                    PrayerWindow("fajr", "ফজর", w.endMillis, w.endMillis)
                }
                return PrayerStatus(true, w, nextW, todayT)
            }
        }
        return PrayerStatus(
            inPrayer = false,
            current = PrayerWindow("gap", "চাশত / সূর্যোদয় বিরতি", t.sunrise, t.dhuhr),
            next = windows[1],
            todayTimes = todayT
        )
    }

    fun formatClockBn(epochMillis: Long): String {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(ZONE_DHAKA)
        val h24 = dt.hour
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val min = dt.minute.toString().padStart(2, '0')
        val ap = if (h24 >= 12) "PM" else "AM"
        return "${bnDigits("$h12:$min")} $ap"
    }

    fun formatCountdownBn(deltaMillis: Long): String {
        val totalSec = (deltaMillis.coerceAtLeast(0L)) / 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        val hh = h.toString().padStart(2, '0')
        val mm = m.toString().padStart(2, '0')
        val ss = s.toString().padStart(2, '0')
        return bnDigits("$hh:$mm:$ss")
    }

    fun formatGregorianBn(date: LocalDate): String {
        val weekday = WEEKDAYS_BN[(date.dayOfWeek.value - 1).coerceIn(0, 6)]
        val monthName = EN_MONTHS_BN[(date.monthValue - 1).coerceIn(0, 11)]
        return "$weekday, ${bnDigits(date.dayOfMonth)} $monthName ${bnDigits(date.year)}"
    }

    fun hijriDate(date: LocalDate): HijriDateParts {
        val hijrah = HijrahDate.from(date)
        val d = hijrah.get(ChronoField.DAY_OF_MONTH)
        val m = hijrah.get(ChronoField.MONTH_OF_YEAR)
        val y = hijrah.get(ChronoField.YEAR)
        return HijriDateParts(
            day = d,
            month = m,
            year = y,
            monthNameBn = HIJRI_MONTHS_BN[(m - 1).coerceIn(0, 11)]
        )
    }

    fun formatHijriBn(date: LocalDate): String {
        val h = hijriDate(date)
        return "${bnDigits(h.day)} ${h.monthNameBn} ${bnDigits(h.year)} হিজরি"
    }

    fun formatBanglaDateApprox(date: LocalDate): String {
        val gYear = date.year
        val isLeap = date.isLeapYear
        val monthLengths = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, if (isLeap) 30 else 29, 30)
        val pohela = LocalDate.of(gYear, 4, 14)
        val refYearStart = if (date.isBefore(pohela)) LocalDate.of(gYear - 1, 4, 14) else pohela
        val bnYear = refYearStart.year - 593
        var daysLeft = java.time.temporal.ChronoUnit.DAYS.between(refYearStart, date).toInt()
        var mIdx = 0
        while (mIdx < 11 && daysLeft >= monthLengths[mIdx]) {
            daysLeft -= monthLengths[mIdx]
            mIdx++
        }
        val bnDay = daysLeft + 1
        return "${bnDigits(bnDay)} ${BANGLA_MONTHS[mIdx]} ${bnDigits(bnYear)} বঙ্গাব্দ"
    }

    fun qiblaBearing(lat: Double, lng: Double): Double {
        val p1 = lat * RAD
        val p2 = 21.4225 * RAD
        val dl = (39.8262 - lng) * RAD
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return ((atan2(y, x) / RAD) + 360.0) % 360.0
    }
}
