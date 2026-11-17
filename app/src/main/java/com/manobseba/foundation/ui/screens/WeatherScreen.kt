package com.manobseba.foundation.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.DistrictsData
import com.manobseba.foundation.data.remote.ExternalContentClient
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.MetricBadgeCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

@Composable
fun WeatherScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val wx by vm.weather.collectAsStateWithLifecycle()
    val busy by vm.weatherBusy.collectAsStateWithLifecycle()
    val err by vm.weatherError.collectAsStateWithLifecycle()
    val selectedDistrict by vm.selectedDistrict.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("লাইভ আবহাওয়া ও নিরাপত্তা", "$selectedDistrict • Open-Meteo", modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { vm.refreshWeather(force = true) }, enabled = !busy) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("আপডেট")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                DistrictsData.ALL.take(20).forEach { d ->
                    FilterChip(selected = d.name == selectedDistrict, onClick = { vm.selectDistrict(d.name) }, label = { Text(d.name) })
                }
            }
        }

        if (!err.isNullOrBlank()) {
            item {
                FoundationCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(err!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        wx?.let { report ->
            val (descBn, _) = ExternalContentClient.weatherDescBn(report.code)
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                if (report.isDaytime) listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                else listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(report.locationName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${PrayerCalculator.bnDigits(report.tempC.roundToInt())}°C", style = MaterialTheme.typography.displaySmall, color = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(descBn, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE0F2FE))
                                Text("অনুভূত ${PrayerCalculator.bnDigits(report.feelsLikeC.roundToInt())}°C", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBadgeCard("আর্দ্রতা", "${PrayerCalculator.bnDigits(report.humidityPct)}%", Icons.Default.WaterDrop, Color(0xFF0284C7), Modifier.weight(1f))
                    MetricBadgeCard("বাতাসের গতি", "${PrayerCalculator.bnDigits(report.windKmh.roundToInt())} কিমি/ঘ", Icons.Default.Air, Color(0xFF0F766E), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBadgeCard("বায়ু মান (AQI)", report.usAqi?.let { PrayerCalculator.bnDigits(it) } ?: "--", Icons.Default.WbSunny, Color(0xFFD97706), Modifier.weight(1f), caption = report.pm25?.let { "PM2.5: ${PrayerCalculator.bnDigits("%.1f".format(it))}" })
                    MetricBadgeCard("বায়ুচাপ", "${PrayerCalculator.bnDigits(report.pressureHpa.roundToInt())} hPa", Icons.Default.WbSunny, Color(0xFF7C3AED), Modifier.weight(1f))
                }
            }

            val next24 = report.hours.take(24)
            if (next24.size >= 4) {
                item {
                    FoundationCard {
                        Text("আগামী ২৪ ঘণ্টার তাপমাত্রার গ্রাফ", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        val lineColor = MaterialTheme.colorScheme.primary
                        val minT = next24.minOf { it.tempC }
                        val maxT = (next24.maxOf { it.tempC }).coerceAtLeast(minT + 1.0)
                        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            val path = Path()
                            next24.forEachIndexed { idx, h ->
                                val x = (idx.toFloat() / (next24.size - 1)) * size.width
                                val y = size.height - (((h.tempC - minT) / (maxT - minT)).toFloat() * (size.height - 16.dp.toPx()) + 8.dp.toPx())
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                            drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                }
            }

            items(report.days, key = { it.dateIso }) { day ->
                val (dBn, _) = ExternalContentClient.weatherDescBn(day.code)
                FoundationCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(day.dateIso, fontWeight = FontWeight.SemiBold)
                            Text("$dBn • বৃষ্টির সম্ভাবনা ${PrayerCalculator.bnDigits(day.rainProbMax)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${PrayerCalculator.bnDigits(day.maxC.roundToInt())}° / ${PrayerCalculator.bnDigits(day.minC.roundToInt())}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
