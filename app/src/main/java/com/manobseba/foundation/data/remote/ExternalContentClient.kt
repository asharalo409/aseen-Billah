package com.manobseba.foundation.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class HourWeather(
    val timeIso: String,
    val tempC: Double,
    val feelsC: Double,
    val code: Int,
    val rainProb: Int,
    val windKmh: Double,
    val uvIndex: Double
)

@Serializable
data class DayWeather(
    val dateIso: String,
    val code: Int,
    val maxC: Double,
    val minC: Double,
    val sunriseIso: String,
    val sunsetIso: String,
    val uvMax: Double,
    val rainProbMax: Int,
    val rainMm: Double,
    val windMaxKmh: Double
)

@Serializable
data class WeatherReport(
    val fetchedAtMillis: Long,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val currentTimeIso: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val humidityPct: Int,
    val isDaytime: Boolean,
    val code: Int,
    val pressureHpa: Double,
    val windKmh: Double,
    val windDirDeg: Double,
    val usAqi: Int? = null,
    val pm25: Double? = null,
    val hours: List<HourWeather>,
    val days: List<DayWeather>
)

@Serializable
data class QuranAyahItem(
    val numberInSurah: Int,
    val arabic: String,
    val bengali: String
)

@Serializable
data class HadithItem(
    val number: Int,
    val arabic: String,
    val bengali: String,
    val gradeBn: String? = null,
    val bookSection: Int? = null
)

@Serializable
data class HadithBookOffline(
    val bookId: String,
    val savedAtMillis: Long,
    val sections: Map<String, String>,
    val items: List<HadithItem>
)

class ExternalContentClient(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("manobseba_ext_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private val GRADE_MAP = mapOf(
            "sahih" to "সহীহ",
            "hasan" to "হাসান",
            "da'if" to "যঈফ",
            "daif" to "যঈফ",
            "hasan sahih" to "হাসান সহীহ",
            "mawdu" to "জাল"
        )

        fun weatherDescBn(code: Int): Pair<String, String> = when {
            code == 0 -> "পরিষ্কার আকাশ" to "clear"
            code == 1 -> "প্রায় পরিষ্কার" to "clear"
            code == 2 -> "আংশিক মেঘলা" to "partly"
            code == 3 -> "মেঘলা" to "cloud"
            code == 45 || code == 48 -> "কুয়াশা" to "fog"
            code in 51..57 -> "গুঁড়ি গুঁড়ি বৃষ্টি" to "drizzle"
            code == 61 -> "হালকা বৃষ্টি" to "rain"
            code == 63 -> "মাঝারি বৃষ্টি" to "rain"
            code == 65 -> "ভারী বৃষ্টি" to "heavy"
            code in 66..67 -> "হিমশীতল বৃষ্টি" to "rain"
            code in 71..77 || code == 85 || code == 86 -> "তুষারপাত" to "snow"
            code == 80 -> "হালকা ঝড়ো বৃষ্টি" to "rain"
            code == 81 -> "ঝড়ো বৃষ্টি" to "rain"
            code == 82 -> "প্রবল বৃষ্টি" to "heavy"
            code == 95 -> "বজ্রসহ বৃষ্টি" to "thunder"
            code == 96 || code == 99 -> "বজ্র ও শিলাবৃষ্টি" to "thunder"
            else -> "স্বাভাবিক আবহাওয়া" to "cloud"
        }

        fun hafeziPageImageUrl(page: Int): String {
            val padded = page.coerceIn(1, 604).toString().padStart(3, '0')
            return "https://raw.githubusercontent.com/QuranHub/quran-pages-images/main/easy-tajweed/page$padded.jpg"
        }
    }

    fun getCachedWeather(lat: Double, lng: Double): WeatherReport? {
        val raw = prefs.getString("wx_cache", null) ?: return null
        return runCatching {
            val cached = json.decodeFromString<WeatherReport>(raw)
            if (kotlin.math.abs(cached.lat - lat) < 0.08 && kotlin.math.abs(cached.lng - lng) < 0.08) cached else null
        }.getOrNull()
    }

    suspend fun fetchWeather(locationName: String, lat: Double, lng: Double, forceRefresh: Boolean = false): Result<WeatherReport> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val cached = getCachedWeather(lat, lng)
                if (cached != null && System.currentTimeMillis() - cached.fetchedAtMillis < 25 * 60_000L) {
                    return@withContext Result.success(cached)
                }
            }
            runCatching {
                val q = "latitude=${String.format(java.util.Locale.US, "%.3f", lat)}&longitude=${String.format(java.util.Locale.US, "%.3f", lng)}"
                val forecastUrl = "https://api.open-meteo.com/v1/forecast?$q" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m" +
                    "&hourly=temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m,uv_index" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max,precipitation_sum,wind_speed_10m_max" +
                    "&timezone=auto&past_days=1&forecast_days=7&wind_speed_unit=kmh"

                val root = JSONObject(httpGet(forecastUrl))
                val cur = root.getJSONObject("current")
                val hourly = root.getJSONObject("hourly")
                val daily = root.getJSONObject("daily")

                var usAqi: Int? = null
                var pm25: Double? = null
                runCatching {
                    val aqiRoot = JSONObject(httpGet("https://air-quality-api.open-meteo.com/v1/air-quality?$q&current=us_aqi,pm2_5&timezone=auto"))
                    val aqiCur = aqiRoot.optJSONObject("current")
                    if (aqiCur != null) {
                        if (aqiCur.has("us_aqi") && !aqiCur.isNull("us_aqi")) usAqi = aqiCur.optInt("us_aqi")
                        if (aqiCur.has("pm2_5") && !aqiCur.isNull("pm2_5")) pm25 = aqiCur.optDouble("pm2_5")
                    }
                }

                val hTimes = hourly.getJSONArray("time")
                val hTemps = hourly.getJSONArray("temperature_2m")
                val hFeels = hourly.getJSONArray("apparent_temperature")
                val hRain = hourly.getJSONArray("precipitation_probability")
                val hCode = hourly.getJSONArray("weather_code")
                val hWind = hourly.getJSONArray("wind_speed_10m")
                val hUv = hourly.getJSONArray("uv_index")
                val hours = (0 until hTimes.length()).map { i ->
                    HourWeather(
                        timeIso = hTimes.optString(i),
                        tempC = hTemps.optDouble(i, 28.0),
                        feelsC = hFeels.optDouble(i, 30.0),
                        code = hCode.optInt(i, 0),
                        rainProb = hRain.optInt(i, 0),
                        windKmh = hWind.optDouble(i, 5.0),
                        uvIndex = hUv.optDouble(i, 0.0)
                    )
                }

                val dDates = daily.getJSONArray("time")
                val dCodes = daily.getJSONArray("weather_code")
                val dMax = daily.getJSONArray("temperature_2m_max")
                val dMin = daily.getJSONArray("temperature_2m_min")
                val dSunrise = daily.getJSONArray("sunrise")
                val dSunset = daily.getJSONArray("sunset")
                val dUv = daily.getJSONArray("uv_index_max")
                val dRainProb = daily.getJSONArray("precipitation_probability_max")
                val dRainSum = daily.getJSONArray("precipitation_sum")
                val dWind = daily.getJSONArray("wind_speed_10m_max")
                val days = (0 until dDates.length()).map { i ->
                    DayWeather(
                        dateIso = dDates.optString(i),
                        code = dCodes.optInt(i, 0),
                        maxC = dMax.optDouble(i, 32.0),
                        minC = dMin.optDouble(i, 24.0),
                        sunriseIso = dSunrise.optString(i),
                        sunsetIso = dSunset.optString(i),
                        uvMax = dUv.optDouble(i, 5.0),
                        rainProbMax = dRainProb.optInt(i, 0),
                        rainMm = dRainSum.optDouble(i, 0.0),
                        windMaxKmh = dWind.optDouble(i, 10.0)
                    )
                }

                val report = WeatherReport(
                    fetchedAtMillis = System.currentTimeMillis(),
                    locationName = locationName,
                    lat = lat,
                    lng = lng,
                    currentTimeIso = cur.optString("time"),
                    tempC = cur.optDouble("temperature_2m", 28.0),
                    feelsLikeC = cur.optDouble("apparent_temperature", 30.0),
                    humidityPct = cur.optInt("relative_humidity_2m", 70),
                    isDaytime = cur.optInt("is_day", 1) == 1,
                    code = cur.optInt("weather_code", 0),
                    pressureHpa = cur.optDouble("surface_pressure", 1012.0),
                    windKmh = cur.optDouble("wind_speed_10m", 8.0),
                    windDirDeg = cur.optDouble("wind_direction_10m", 180.0),
                    usAqi = usAqi,
                    pm25 = pm25,
                    hours = hours,
                    days = days
                )
                prefs.edit().putString("wx_cache", json.encodeToString(report)).apply()
                report
            }
        }

    suspend fun fetchSurah(surahNumber: Int): Result<List<QuranAyahItem>> = withContext(Dispatchers.IO) {
        val key = "surah_$surahNumber"
        val cached = prefs.getString(key, null)
        if (!cached.isNullOrBlank()) {
            runCatching { json.decodeFromString<List<QuranAyahItem>>(cached) }
                .onSuccess { return@withContext Result.success(it) }
        }
        runCatching {
            val url = "https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,bn.bengali"
            val root = JSONObject(httpGet(url))
            val data = root.getJSONArray("data")
            val arAyahs = data.getJSONObject(0).getJSONArray("ayahs")
            val bnAyahs = data.getJSONObject(1).getJSONArray("ayahs")
            val items = (0 until arAyahs.length()).map { i ->
                val ar = arAyahs.getJSONObject(i)
                val bn = bnAyahs.getJSONObject(i)
                QuranAyahItem(
                    numberInSurah = ar.optInt("numberInSurah", i + 1),
                    arabic = ar.optString("text"),
                    bengali = bn.optString("text")
                )
            }
            prefs.edit().putString(key, json.encodeToString(items)).apply()
            items
        }
    }

    fun getSavedHadithBookIds(): List<String> =
        prefs.all.keys.filter { it.startsWith("hadith_book_") }.map { it.removePrefix("hadith_book_") }

    fun getCachedHadithBook(bookId: String): HadithBookOffline? {
        val raw = prefs.getString("hadith_book_$bookId", null) ?: return null
        return runCatching { json.decodeFromString<HadithBookOffline>(raw) }.getOrNull()
    }

    fun deleteHadithBook(bookId: String) {
        prefs.edit().remove("hadith_book_$bookId").apply()
    }

    suspend fun downloadHadithBook(bookId: String): Result<HadithBookOffline> = withContext(Dispatchers.IO) {
        runCatching {
            val benUrls = listOf(
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ben-$bookId.min.json",
                "https://raw.githubusercontent.com/fawazahmed0/hadith-api/1/editions/ben-$bookId.min.json"
            )
            val benJson = JSONObject(httpGetFirst(benUrls))
            val hadithsArr = benJson.getJSONArray("hadiths")

            val araMap = mutableMapOf<Int, String>()
            runCatching {
                val araUrls = listOf(
                    "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$bookId.min.json",
                    "https://raw.githubusercontent.com/fawazahmed0/hadith-api/1/editions/ara-$bookId.min.json"
                )
                val araJson = JSONObject(httpGetFirst(araUrls))
                val araArr = araJson.optJSONArray("hadiths")
                if (araArr != null) {
                    for (i in 0 until araArr.length()) {
                        val obj = araArr.optJSONObject(i) ?: continue
                        val txt = obj.optString("text").trim()
                        if (txt.isNotEmpty()) araMap[obj.optInt("hadithnumber")] = txt
                    }
                }
            }

            val sections = mutableMapOf<String, String>()
            val secObj = benJson.optJSONObject("metadata")?.optJSONObject("section")
            if (secObj != null) {
                val keys = secObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = secObj.optString(k).trim()
                    if (k != "0" && v.isNotEmpty()) sections[k] = v
                }
            }

            val items = mutableListOf<HadithItem>()
            for (i in 0 until hadithsArr.length()) {
                val obj = hadithsArr.optJSONObject(i) ?: continue
                val bnText = obj.optString("text").trim()
                if (bnText.isEmpty()) continue
                val num = obj.optInt("hadithnumber", i + 1)
                val rawGrade = obj.optJSONArray("grades")?.optJSONObject(0)?.optString("grade")?.trim()?.lowercase()
                val secNum = obj.optJSONObject("reference")?.optInt("book", 0)?.takeIf { it > 0 }
                items += HadithItem(
                    number = num,
                    arabic = araMap[num].orEmpty(),
                    bengali = bnText,
                    gradeBn = GRADE_MAP[rawGrade],
                    bookSection = secNum
                )
            }
            check(items.isNotEmpty()) { "হাদিস ডেটা খালি পাওয়া গেছে" }

            val book = HadithBookOffline(
                bookId = bookId,
                savedAtMillis = System.currentTimeMillis(),
                sections = sections,
                items = items
            )
            prefs.edit().putString("hadith_book_$bookId", json.encodeToString(book)).apply()
            book
        }
    }

    private fun httpGetFirst(urls: List<String>): String {
        var lastErr: Exception? = null
        for (u in urls) {
            try {
                return httpGet(u)
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: IllegalStateException("নেটওয়ার্ক সংযোগ ব্যর্থ")
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 25_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
