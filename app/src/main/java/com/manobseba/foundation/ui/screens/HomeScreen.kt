package com.manobseba.foundation.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.SeedCatalog
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.GoalProgressCard
import com.manobseba.foundation.ui.components.MetricBadgeCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.theme.paletteForKey
import com.manobseba.foundation.ui.viewmodel.AiTaskState
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenDonate: () -> Unit,
    onOpenPublicForm: (String) -> Unit,
    onGoBlood: () -> Unit,
    onGoFinance: () -> Unit,
    onGoProjects: () -> Unit,
    onGoIslamic: () -> Unit,
    onGoWeather: () -> Unit,
    onGoAiHub: () -> Unit,
    onGoOverview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val funds by vm.funds.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val bloodRequests by vm.bloodRequests.collectAsStateWithLifecycle()
    val fieldReports by vm.fieldReports.collectAsStateWithLifecycle()
    val notices by vm.notices.collectAsStateWithLifecycle()
    val profile by vm.userProfile.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val prayerLoc by vm.prayerLoc.collectAsStateWithLifecycle()
    val asrFactor by vm.asrFactor.collectAsStateWithLifecycle()
    val aiHealth by vm.aiHealthSummary.collectAsStateWithLifecycle()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val prayerStatus = remember(nowMillis / 1000L, prayerLoc, asrFactor) {
        PrayerCalculator.getStatus(nowMillis, prayerLoc, asrFactor)
    }
    val todayDate = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of("Asia/Dhaka")).toLocalDate()
    val palette = paletteForKey(profile?.themePalette)

    val totalDonations = remember(transactions) { transactions.filter { it.kind == "donation" }.sumOf { it.amount } }
    val totalFees = remember(transactions) { transactions.filter { it.kind == "fee" }.sumOf { it.amount } }
    val totalExpense = remember(transactions) { transactions.filter { it.kind == "expense" }.sumOf { it.amount } }
    val balance = (totalDonations + totalFees) - totalExpense

    val activeBlood = remember(bloodRequests) { bloodRequests.filter { !it.managed } }
    val committee = remember(members) { members.filter { it.active && (it.rank ?: 100) < 20 }.take(6) }

    val donutPalette = listOf(
        Color(0xFF087A43),
        Color(0xFF0284C7),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFFE11D48)
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Clock, Prayer Countdown & Weather Strip
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(palette.primary, palette.deepGradientEnd)))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings?.orgName ?: "মানবসেবা ফাউন্ডেশন",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings?.slogan ?: "মানুষের পাশে, মানবতার পাশে",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.clickable { onGoWeather() }.testTag("home_weather_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WbSunny, contentDescription = "আবহাওয়া", tint = Color(0xFFFDE68A), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = weather?.let { "${prayerLoc.name} ${PrayerCalculator.bnDigits(it.tempC.toInt())}°C" } ?: prayerLoc.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = PrayerCalculator.formatClockBn(nowMillis),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Text(
                                text = PrayerCalculator.formatGregorianBn(todayDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "${PrayerCalculator.formatHijriBn(todayDate)} • ${PrayerCalculator.formatBanglaDateApprox(todayDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.clickable { onGoIslamic() }.testTag("home_prayer_chip")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${prayerStatus.current.label} ওয়াক্ত",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFDE68A)
                                )
                                Text(
                                    text = "পরবর্তী ${prayerStatus.next.label}: ${PrayerCalculator.formatCountdownBn(prayerStatus.next.startMillis - nowMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onOpenDonate,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = palette.primary),
                            modifier = Modifier.weight(1f).testTag("home_donate_button")
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("দান করুন", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onOpenPublicForm("aid") },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.20f), contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("home_aid_button")
                        ) {
                            Text("সহায়তা আবেদন")
                        }
                        FilledTonalButton(
                            onClick = { ExportShareHelper.dialPhone(context, "999") },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                            modifier = Modifier.testTag("home_999_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "জরুরি ৯৯৯", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("৯৯৯")
                        }
                    }
                }
            }
        }

        // 2. Urgent Blood SOS Ticker if active requests exist
        if (activeBlood.isNotEmpty()) {
            item {
                FoundationCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Bloodtype, contentDescription = "রক্ত প্রয়োজন", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "জরুরি রক্ত প্রয়োজন (${PrayerCalculator.bnDigits(activeBlood.size)}টি আবেদন)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            val first = activeBlood.first()
                            Text(
                                text = "${first.bloodGroup} • ${first.hospital} • ${first.patientProblem}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onGoBlood, modifier = Modifier.testTag("home_blood_sos_btn")) {
                            Text("দেখুন", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // 3. Key Financial Summary + Quick Actions
        item {
            SectionHeader("স্বচ্ছ আর্থিক চিত্র", "রিয়েল-টাইম আয়, ব্যয় ও স্থিতি", "বিস্তারিত", onGoFinance)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBadgeCard(
                    label = "মোট আয়",
                    value = PrayerCalculator.formatTaka(totalDonations + totalFees),
                    icon = Icons.Default.Payments,
                    tint = Color(0xFF087A43),
                    modifier = Modifier.weight(1f)
                )
                MetricBadgeCard(
                    label = "মোট ব্যয়",
                    value = PrayerCalculator.formatTaka(totalExpense),
                    icon = Icons.Default.VolunteerActivism,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBadgeCard(
                    label = "বর্তমান স্থিতি",
                    value = PrayerCalculator.formatTaka(balance),
                    icon = Icons.Default.AccountBalanceWallet,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                MetricBadgeCard(
                    label = "সক্রিয় সদস্য",
                    value = "${PrayerCalculator.bnDigits(members.count { it.active })} জন",
                    icon = Icons.Default.Group,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Strategic Goal Progress & AI Health Insight
        item {
            GoalProgressCard(
                title = settings?.goalTitle ?: "তহবিল সংগ্রহের লক্ষ্যমাত্রা",
                collected = (totalDonations + totalFees).coerceAtLeast(settings?.collectedAmount ?: 0.0),
                target = settings?.targetAmount ?: 200000.0
            )
        }

        item {
            FoundationCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("মানবসেবা AI স্বাস্থ্য বিশ্লেষণ", style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { vm.generateExecutiveHealthSummary() }, modifier = Modifier.testTag("home_ai_health_btn")) {
                        Text("বিশ্লেষণ করুন")
                    }
                }
                Spacer(Modifier.height(6.dp))
                when (val s = aiHealth) {
                    is AiTaskState.Idle -> Text(
                        "সংগঠনের আয়-ব্যয়, প্রকল্প ও ত্রাণের বর্তমান অবস্থান সম্পর্কে AI-এর সংক্ষিপ্ত সারসংক্ষেপ পেতে ট্যাপ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    is AiTaskState.Loading -> Text("মানবসেবা AI বিশ্লেষণ করছে...", style = MaterialTheme.typography.bodySmall)
                    is AiTaskState.Streaming -> Text(s.text, style = MaterialTheme.typography.bodyMedium)
                    is AiTaskState.Success -> Text(s.data, style = MaterialTheme.typography.bodyMedium)
                    is AiTaskState.Error -> Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onGoAiHub, modifier = Modifier.testTag("home_open_ai_hub")) {
                        Text("AI চ্যাট ও খাতা স্ক্যানার")
                    }
                    TextButton(onClick = onGoOverview) {
                        Text("এক্সিকিউটিভ রিপোর্ট")
                    }
                }
            }
        }

        // 5. Fund Breakdown Donut Chart
        if (funds.isNotEmpty()) {
            item {
                FoundationCard {
                    Text("তহবিল অনুযায়ী ব্যয় ও জমা", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val fundTotals = funds.mapIndexed { idx, fund ->
                        val amt = transactions.filter { it.fundId == fund.id && it.kind != "expense" }.sumOf { it.amount }
                        Triple(fund.name, amt, donutPalette[idx % donutPalette.size])
                    }
                    val totalPositive = fundTotals.sumOf { it.second }.coerceAtLeast(1.0)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Canvas(modifier = Modifier.size(116.dp).padding(6.dp)) {
                            var start = -90f
                            val strokeW = 20.dp.toPx()
                            fundTotals.forEach { (_, amt, color) ->
                                val sweep = ((amt / totalPositive) * 360.0).toFloat().coerceAtLeast(4f)
                                drawArc(
                                    color = color,
                                    startAngle = start,
                                    sweepAngle = sweep - 2f,
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                )
                                start += sweep
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            fundTotals.forEach { (name, amt, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(PrayerCalculator.formatTaka(amt), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Active Projects Horizontal Strip
        if (projects.isNotEmpty()) {
            item {
                SectionHeader("চলমান মানবিক প্রকল্প", "মানুষের কল্যাণে চলমান উদ্যোগ", "সব প্রকল্প", onGoProjects)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(projects, key = { it.id }) { proj ->
                        val raised = transactions.filter { it.fundId == proj.fundId && it.kind != "expense" }.sumOf { it.amount }
                        val ratio = (raised / proj.goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.width(260.dp).clickable { onGoProjects() }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                StatusPill(
                                    text = proj.area ?: "সারাদেশ",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(proj.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "লক্ষ্য: ${PrayerCalculator.formatTaka(proj.goal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Executive Committee & Verified Field Works
        if (committee.isNotEmpty()) {
            item {
                SectionHeader("কার্যনির্বাহী পর্ষদ", "নেতৃত্ব ও সমন্বয়কারী")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(committee, key = { it.id }) { m ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.width(156.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(m.fullName.take(1), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(m.fullName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(m.roleLabel ?: SeedCatalog.roleLabel(m.role), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        if (fieldReports.isNotEmpty()) {
            item {
                SectionHeader("মাঠ পর্যায়ের কার্যক্রম", "যাচাইকৃত ত্রাণ ও সেবা", "মানচিত্র", onGoProjects)
                Spacer(Modifier.height(6.dp))
                fieldReports.take(2).forEach { rep ->
                    FoundationCard(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(rep.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusPill(
                                text = if (rep.status == "verified") "যাচাইকৃত" else "অপেক্ষমাণ",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${rep.location ?: ""} • ব্যয় ${PrayerCalculator.formatTaka(rep.spent)} • উপকারভোগী ${PrayerCalculator.bnDigits(rep.beneficiaries)} জন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 8. Official Notices with Like / Support Reactions
        if (notices.isNotEmpty()) {
            item {
                SectionHeader("অফিসিয়াল নোটিশ বোর্ড", "সর্বশেষ বিজ্ঞপ্তি ও ঘোষণা")
                Spacer(Modifier.height(6.dp))
                notices.take(3).forEach { notice ->
                    FoundationCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notice.urgent) Icons.Default.NotificationsActive else Icons.Default.Campaign,
                                contentDescription = null,
                                tint = if (notice.urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(notice.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(notice.body, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { vm.reactNotice(notice, isLike = true) }) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "পছন্দ", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("পছন্দ (${PrayerCalculator.bnDigits(notice.likeCount)})")
                            }
                            FilledTonalButton(onClick = { vm.reactNotice(notice, isLike = false) }) {
                                Icon(Icons.Default.Favorite, contentDescription = "সমর্থন", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("সমর্থন (${PrayerCalculator.bnDigits(notice.supportCount)})")
                            }
                        }
                    }
                }
            }
        }
    }
}
