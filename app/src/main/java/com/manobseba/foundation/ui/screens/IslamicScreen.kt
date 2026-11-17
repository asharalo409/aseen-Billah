package com.manobseba.foundation.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.manobseba.foundation.data.DistrictsData
import com.manobseba.foundation.data.SeedCatalog
import com.manobseba.foundation.data.remote.ExternalContentClient
import com.manobseba.foundation.data.remote.HadithBookOffline
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun IslamicScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    var tabIdx by remember { mutableIntStateOf(0) }
    val tabs = listOf("নামাজ", "কিবলা", "তসবিহ", "আমল", "জাকাত", "কুরআন", "হাদিস")

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tabIdx, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = tabIdx == idx,
                    onClick = { tabIdx = idx },
                    text = { Text(label) },
                    modifier = Modifier.testTag("islamic_tab_$idx")
                )
            }
        }
        when (tabIdx) {
            0 -> SalatTab(vm)
            1 -> QiblaTab(vm)
            2 -> TasbihTab(vm)
            3 -> AmolTab(vm)
            4 -> ZakatTab()
            5 -> QuranTab(vm)
            else -> HadithTab(vm)
        }
    }
}

@Composable
private fun SalatTab(vm: MainViewModel) {
    val loc by vm.prayerLoc.collectAsStateWithLifecycle()
    val asrFactor by vm.asrFactor.collectAsStateWithLifecycle()
    val selectedDistrict by vm.selectedDistrict.collectAsStateWithLifecycle()
    val logs by vm.salatLogs.collectAsStateWithLifecycle()
    val todayKey = LocalDate.now().toString()
    val todayDone = logs.firstOrNull { it.dateKey == todayKey }?.prayers.orEmpty()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val status = remember(nowMillis / 1000L, loc, asrFactor) { PrayerCalculator.getStatus(nowMillis, loc, asrFactor) }
    var showDistPicker by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                    .background(Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF0F766E))))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("বর্তমান ওয়াক্ত: ${status.current.label}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text(
                                "পরবর্তী ${status.next.label}: ${PrayerCalculator.formatCountdownBn(status.next.startMillis - nowMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        FilledTonalButton(onClick = { showDistPicker = !showDistPicker }) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(selectedDistrict)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${PrayerCalculator.formatHijriBn(LocalDate.now())} • সাহরির শেষ: ${PrayerCalculator.formatClockBn(status.todayTimes.fajr)} • ইফতার: ${PrayerCalculator.formatClockBn(status.todayTimes.maghrib)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1FAE5)
                    )
                }
            }
        }

        if (showDistPicker) {
            item {
                FoundationCard {
                    Text("জেলা নির্বাচন করুন", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DistrictsData.ALL.forEach { d ->
                            FilterChip(
                                selected = d.name == selectedDistrict,
                                onClick = { vm.selectDistrict(d.name); showDistPicker = false },
                                label = { Text(d.name) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = asrFactor == 2, onClick = { vm.setAsrSchool(2) }, label = { Text("হানাফী আসর") })
                FilterChip(selected = asrFactor == 1, onClick = { vm.setAsrSchool(1) }, label = { Text("শাফেয়ী/জুমহুর আসর") })
            }
        }

        val prayers = listOf(
            Triple("fajr", "ফজর", status.todayTimes.fajr),
            Triple("sunrise", "সূর্যোদয়", status.todayTimes.sunrise),
            Triple("dhuhr", "যোহর", status.todayTimes.dhuhr),
            Triple("asr", "আসর", status.todayTimes.asr),
            Triple("maghrib", "মাগরিব", status.todayTimes.maghrib),
            Triple("isha", "এশা", status.todayTimes.isha)
        )
        items(prayers, key = { it.first }) { (key, label, millis) ->
            val trackable = key != "sunrise"
            val done = key in todayDone
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trackable) {
                            Checkbox(checked = done, onCheckedChange = { vm.toggleSalatPrayer(todayKey, key) })
                        } else {
                            Spacer(Modifier.width(12.dp))
                        }
                        Column {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(PrayerCalculator.formatClockBn(millis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (status.current.key == key) {
                        StatusPill("চলমান", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaTab(vm: MainViewModel) {
    val context = LocalContext.current
    val loc by vm.prayerLoc.collectAsStateWithLifecycle()
    val bearing = remember(loc) { PrayerCalculator.qiblaBearing(loc.lat, loc.lng) }
    var headingDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            val rMat = FloatArray(9)
            val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rMat, event.values)
                SensorManager.getOrientation(rMat, orientation)
                val azimuth = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                headingDeg = azimuth
                hasSensor = true
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotSensor != null) {
            sm.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm?.unregisterListener(listener) }
    }

    val needleAngle = (bearing.toFloat() - headingDeg + 360f) % 360f
    val aligned = hasSensor && (abs(((needleAngle + 180f) % 360f) - 180f) < 8f)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            FoundationCard {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("কিবলা কম্পাস (${loc.name})", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "উত্তর থেকে ঘড়ির কাঁটার দিকে ${PrayerCalculator.bnDigits("%.1f".format(bearing))}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                        val ringColor = if (aligned) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize().rotate(needleAngle)) {
                            drawCircle(color = ringColor.copy(alpha = 0.14f))
                            drawCircle(color = ringColor, style = Stroke(width = 4.dp.toPx()))
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val path = Path().apply {
                                moveTo(cx, cy - 75.dp.toPx())
                                lineTo(cx - 16.dp.toPx(), cy)
                                lineTo(cx + 16.dp.toPx(), cy)
                                close()
                            }
                            drawPath(path, color = if (aligned) Color(0xFF16A34A) else Color(0xFFD97706))
                            drawCircle(Color(0xFF0F172A), radius = 8.dp.toPx(), center = Offset(cx, cy))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            aligned -> "আপনি কিবলামুখী আছেন"
                            hasSensor -> "ফোনটি সমতল রেখে তীরের দিকে ঘুরুন"
                            else -> "সেন্সর অনুপলব্ধ — উত্তর দিক থেকে ${PrayerCalculator.bnDigits("%.1f".format(bearing))}° কোণে কাবা শরীফ"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (aligned) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TasbihTab(vm: MainViewModel) {
    val context = LocalContext.current
    val count by vm.tasbihCount.collectAsStateWithLifecycle()
    val rounds by vm.tasbihRounds.collectAsStateWithLifecycle()
    val total by vm.tasbihTotal.collectAsStateWithLifecycle()
    val target by vm.tasbihTarget.collectAsStateWithLifecycle()
    var zikrIdx by remember { mutableIntStateOf(0) }

    fun triggerVibration(longPulse: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(if (longPulse) 140L else 20L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            FoundationCard {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ডিজিটাল তসবিহ", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SeedCatalog.DEFAULT_ZIKR.forEachIndexed { idx, (bn, _) ->
                            FilterChip(selected = zikrIdx == idx, onClick = { zikrIdx = idx }, label = { Text(bn) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(SeedCatalog.DEFAULT_ZIKR[zikrIdx].second, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(188.dp).clickable {
                            val roundDone = vm.tapTasbih()
                            triggerVibration(roundDone)
                        }.testTag("tasbih_tap_button")
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(PrayerCalculator.bnDigits(count), style = MaterialTheme.typography.displaySmall, color = Color.White)
                            Text(if (target > 0) "/ ${PrayerCalculator.bnDigits(target)}" else "অসীম", color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("রাউন্ড: ${PrayerCalculator.bnDigits(rounds)} • সর্বমোট: ${PrayerCalculator.bnDigits(total)}", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(33, 99, 100, 0).forEach { n ->
                            FilterChip(selected = target == n, onClick = { vm.setTasbihTarget(n) }, label = { Text(if (n == 0) "অসীম" else PrayerCalculator.bnDigits(n)) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { vm.resetTasbih() }) { Text("রিসেট") }
                }
            }
        }
    }
}

@Composable
private fun AmolTab(vm: MainViewModel) {
    val itemsList by vm.customAmolItems.collectAsStateWithLifecycle()
    val logs by vm.salatLogs.collectAsStateWithLifecycle()
    val todayKey = LocalDate.now().toString()
    val done = logs.firstOrNull { it.dateKey == todayKey }?.amolDone.orEmpty()
    var newAmol by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            FoundationCard {
                Text("আজকের দৈনিক আমল", style = MaterialTheme.typography.titleMedium)
                Text("সম্পন্ন: ${PrayerCalculator.bnDigits(done.size)}/${PrayerCalculator.bnDigits(itemsList.size)}", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                // 7-day bar chart
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    (6 downTo 0).forEach { offset ->
                        val dk = LocalDate.now().minusDays(offset.toLong()).toString()
                        val dCount = logs.firstOrNull { it.dateKey == dk }?.amolDone?.size ?: 0
                        val frac = if (itemsList.isNotEmpty()) (dCount.toFloat() / itemsList.size).coerceIn(0.08f, 1f) else 0.08f
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height((40 * frac).dp)
                                    .clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary)
                            )
                            Text(if (offset == 0) "আজ" else PrayerCalculator.bnDigits(offset), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        items(itemsList, key = { it }) { item ->
            val checked = item in done
            FoundationCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = checked, onCheckedChange = { vm.toggleAmolItem(todayKey, item) })
                    Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newAmol, onValueChange = { newAmol = it }, label = { Text("নিজের আমল যোগ করুন") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = { vm.addCustomAmolItem(newAmol); newAmol = "" }) {
                    Icon(Icons.Default.Add, contentDescription = "যোগ করুন")
                }
            }
        }
    }
}

@Composable
private fun ZakatTab() {
    var goldPrice by remember { mutableStateOf("140000") }
    var silverPrice by remember { mutableStateOf("2200") }
    var basis by remember { mutableStateOf("silver") }
    var cash by remember { mutableStateOf("") }
    var goldVori by remember { mutableStateOf("") }
    var silverVori by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("") }
    var debt by remember { mutableStateOf("") }

    val gp = goldPrice.toDoubleOrNull() ?: 0.0
    val sp = silverPrice.toDoubleOrNull() ?: 0.0
    val wealth = (cash.toDoubleOrNull() ?: 0.0) + (goldVori.toDoubleOrNull() ?: 0.0) * gp + (silverVori.toDoubleOrNull() ?: 0.0) * sp + (trade.toDoubleOrNull() ?: 0.0)
    val net = (wealth - (debt.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
    val nisab = if (basis == "silver") 52.5 * sp else 7.5 * gp
    val due = if (nisab > 0 && net >= nisab) net * 0.025 else 0.0

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            FoundationCard {
                Text("জাকাত ক্যালকুলেটর", style = MaterialTheme.typography.titleLarge)
                Text("সোনার নিসাব ৭.৫ ভরি, রূপার নিসাব ৫২.৫ ভরি", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = basis == "silver", onClick = { basis = "silver" }, label = { Text("রূপার নিসাব") })
                    FilterChip(selected = basis == "gold", onClick = { basis = "gold" }, label = { Text("সোনার নিসাব") })
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = goldPrice, onValueChange = { goldPrice = it }, label = { Text("১ ভরি সোনা (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = silverPrice, onValueChange = { silverPrice = it }, label = { Text("১ ভরি রূপা (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = cash, onValueChange = { cash = it }, label = { Text("নগদ টাকা ও ব্যাংক জমা") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = goldVori, onValueChange = { goldVori = it }, label = { Text("সোনা (ভরি)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = silverVori, onValueChange = { silverVori = it }, label = { Text("রূপা (ভরি)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = trade, onValueChange = { trade = it }, label = { Text("ব্যবসার পণ্য ও অন্যান্য") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = debt, onValueChange = { debt = it }, label = { Text("মোট দেনা (বিয়োগ)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("মোট সম্পদ: ${PrayerCalculator.formatTaka(wealth)}")
                        Text("নিসাব: ${PrayerCalculator.formatTaka(nisab)}")
                        Text("প্রদেয় জাকাত (২.৫%): ${PrayerCalculator.formatTaka(due)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranTab(vm: MainViewModel) {
    val ayahs by vm.surahItems.collectAsStateWithLifecycle()
    val busy by vm.surahBusy.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf("surah") } // surah | hafezi
    var selectedSurahNum by remember { mutableStateOf<Int?>(null) }
    var hafeziPage by remember { mutableIntStateOf(1) }
    var search by remember { mutableStateOf("") }

    if (mode == "hafezi") {
        val currentSurah = remember(hafeziPage) { DistrictsData.surahAtPage(hafeziPage) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { mode = "surah" }) { Text("সূরা তালিকা") }
                    Text("পৃষ্ঠা ${PrayerCalculator.bnDigits(hafeziPage)}/৬০৪ • ${currentSurah.name}", fontWeight = FontWeight.Bold)
                }
            }
            item {
                FoundationCard {
                    AsyncImage(
                        model = ExternalContentClient.hafeziPageImageUrl(hafeziPage),
                        contentDescription = "হাফেজি কুরআন পৃষ্ঠা $hafeziPage",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FilledTonalButton(onClick = { if (hafeziPage > 1) hafeziPage-- }, enabled = hafeziPage > 1) { Text("পূর্ববর্তী পৃষ্ঠা") }
                        Button(onClick = { if (hafeziPage < 604) hafeziPage++ }, enabled = hafeziPage < 604) { Text("পরবর্তী পৃষ্ঠা") }
                    }
                }
            }
        }
    } else if (selectedSurahNum != null) {
        val meta = DistrictsData.SURAHS[(selectedSurahNum!! - 1).coerceIn(0, 113)]
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { selectedSurahNum = null }) { Text("< সূরা তালিকা") }
                    FilledTonalButton(onClick = { hafeziPage = meta.page; mode = "hafezi" }) { Text("হাফেজি মুসহাফ") }
                }
                Spacer(Modifier.height(8.dp))
                FoundationCard {
                    Text("${PrayerCalculator.bnDigits(meta.number)}. ${meta.name} (${meta.meaning})", style = MaterialTheme.typography.titleLarge)
                    Text("${if (meta.madani) "মাদানী" else "মাক্কী"} • ${PrayerCalculator.bnDigits(meta.ayahs)} আয়াত", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (busy) {
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            items(ayahs, key = { it.numberInSurah }) { ay ->
                FoundationCard {
                    StatusPill("আয়াত ${PrayerCalculator.bnDigits(ay.numberInSurah)}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(ay.arabic, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(ay.bengali, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    } else {
        val list = remember(search) {
            val q = search.trim()
            if (q.isEmpty()) DistrictsData.SURAHS else DistrictsData.SURAHS.filter { it.name.contains(q) || it.number.toString() == q }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("সূরা খুঁজুন...") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = { mode = "hafezi" }) { Text("হাফেজি") }
                }
            }
            items(list, key = { it.number }) { s ->
                FoundationCard(modifier = Modifier.clickable {
                    selectedSurahNum = s.number
                    vm.loadSurah(s.number)
                }) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${PrayerCalculator.bnDigits(s.number)}. ${s.name}", style = MaterialTheme.typography.titleMedium)
                            Text("${s.meaning} • ${PrayerCalculator.bnDigits(s.ayahs)} আয়াত", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("পৃ. ${PrayerCalculator.bnDigits(s.page)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithTab(vm: MainViewModel) {
    val context = LocalContext.current
    val savedIds by vm.savedHadithBooks.collectAsStateWithLifecycle()
    val busyId by vm.hadithBusyId.collectAsStateWithLifecycle()
    var activeBook by remember { mutableStateOf<HadithBookOffline?>(null) }
    var search by remember { mutableStateOf("") }

    if (activeBook != null) {
        val book = activeBook!!
        val bookMeta = SeedCatalog.HADITH_BOOKS.firstOrNull { it.id == book.bookId }
        val filtered = remember(book, search) {
            val q = search.trim()
            if (q.isEmpty()) book.items.take(60)
            else book.items.filter { it.number.toString() == q || it.bengali.contains(q) }.take(60)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledTonalButton(onClick = { activeBook = null }) { Text("< কিতাব তালিকা") }
                    IconButton(onClick = { vm.removeHadithBook(book.bookId); activeBook = null }) {
                        Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
                    }
                }
                SectionHeader(bookMeta?.name ?: book.bookId, "${PrayerCalculator.bnDigits(book.items.size)}টি হাদিস")
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("হাদিস নম্বর বা বাংলা শব্দ খুঁজুন...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            items(filtered, key = { it.number }) { item ->
                FoundationCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        StatusPill("হাদিস ${PrayerCalculator.bnDigits(item.number)}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = {
                            ExportShareHelper.copyText(context, "${item.arabic}\n\n${item.bengali}\n— ${bookMeta?.name} ${item.number}")
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "কপি", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (item.arabic.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(item.arabic, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.bengali, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionHeader("বিশুদ্ধ হাদিস গ্রন্থাগার", "একবার সেভ করলে অফলাইনেও পড়া যাবে") }
            items(SeedCatalog.HADITH_BOOKS, key = { it.id }) { meta ->
                val isSaved = meta.id in savedIds
                val isBusy = busyId == meta.id
                FoundationCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.name, style = MaterialTheme.typography.titleMedium)
                            Text(meta.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSaved) {
                            Button(onClick = { activeBook = vm.getCachedHadithBook(meta.id) }) { Text("পড়ুন") }
                        } else {
                            FilledTonalButton(onClick = { vm.downloadHadithBook(meta.id) }, enabled = !isBusy) {
                                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Download, contentDescription = "ডাউনলোড", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isBusy) "চলছে..." else "সেভ")
                            }
                        }
                    }
                }
            }
        }
    }
}
