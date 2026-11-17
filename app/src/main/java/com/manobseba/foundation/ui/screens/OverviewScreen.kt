package com.manobseba.foundation.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.GoalProgressCard
import com.manobseba.foundation.ui.components.MetricBadgeCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import java.time.YearMonth

@Composable
fun OverviewScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val funds by vm.funds.collectAsStateWithLifecycle()
    val relief by vm.reliefLocations.collectAsStateWithLifecycle()
    val blood by vm.bloodRequests.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var rangeFilter by remember { mutableStateOf("all") } // all | month | quarter
    var selectedFundId by remember { mutableStateOf<String?>(null) }

    val filteredTx = remember(transactions, rangeFilter, selectedFundId) {
        val ymNow = YearMonth.now()
        val cutoffStr = when (rangeFilter) {
            "month" -> ymNow.toString()
            "quarter" -> ymNow.minusMonths(2).toString()
            else -> ""
        }
        transactions.filter { tx ->
            val okFund = selectedFundId == null || tx.fundId == selectedFundId
            val okDate = cutoffStr.isEmpty() || tx.dateStr >= cutoffStr
            okFund && okDate
        }
    }

    val income = filteredTx.filter { it.kind != "expense" }.sumOf { it.amount }
    val expense = filteredTx.filter { it.kind == "expense" }.sumOf { it.amount }
    val families = relief.sumOf { it.familiesHelped }
    val reliefAmount = relief.sumOf { it.amountDistributed }

    val monthlyBars = remember(filteredTx) {
        val nowYm = YearMonth.now()
        (5 downTo 0).map { offset ->
            val ym = nowYm.minusMonths(offset.toLong())
            val key = ym.toString()
            val inc = filteredTx.filter { it.dateStr.startsWith(key) && it.kind != "expense" }.sumOf { it.amount }
            val exp = filteredTx.filter { it.dateStr.startsWith(key) && it.kind == "expense" }.sumOf { it.amount }
            Triple(PrayerCalculator.EN_MONTHS_BN[(ym.monthValue - 1).coerceIn(0, 11)].take(4), inc, exp)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("এক্সিকিউটিভ ওভারভিউ", "কৌশলগত বিশ্লেষণ ও স্বচ্ছতা", modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = { ExportShareHelper.shareFinanceCsv(context, filteredTx, funds) },
                    modifier = Modifier.testTag("overview_export_csv_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "এক্সপোর্ট", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CSV")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "সব সময়", "quarter" to "৩ মাস", "month" to "এই মাস").forEach { (k, label) ->
                    FilterChip(
                        selected = rangeFilter == k,
                        onClick = { rangeFilter = k },
                        label = { Text(label) }
                    )
                }
            }
            if (funds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    FilterChip(
                        selected = selectedFundId == null,
                        onClick = { selectedFundId = null },
                        label = { Text("সব তহবিল") }
                    )
                    funds.take(3).forEach { f ->
                        FilterChip(
                            selected = selectedFundId == f.id,
                            onClick = { selectedFundId = if (selectedFundId == f.id) null else f.id },
                            label = { Text(f.name.take(12)) }
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBadgeCard("মোট আয়", PrayerCalculator.formatTaka(income), Icons.Default.AccountBalance, Color(0xFF087A43), Modifier.weight(1f))
                MetricBadgeCard("মোট ব্যয়", PrayerCalculator.formatTaka(expense), Icons.Default.Analytics, Color(0xFFD97706), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBadgeCard("ত্রাণ পরিবার", "${PrayerCalculator.bnDigits(families)}টি", Icons.Default.Map, Color(0xFF0284C7), Modifier.weight(1f), caption = PrayerCalculator.formatTaka(reliefAmount))
                MetricBadgeCard("রক্ত আবেদন", "${PrayerCalculator.bnDigits(blood.size)}টি", Icons.Default.Bloodtype, Color(0xFFE11D48), Modifier.weight(1f))
            }
        }

        item {
            GoalProgressCard(
                title = settings?.goalTitle ?: "বার্ষিক লক্ষ্যমাত্রা",
                collected = income,
                target = settings?.targetAmount ?: 200000.0
            )
        }

        // Monthly Income vs Expense Canvas Bar Chart
        item {
            FoundationCard {
                Text("৬ মাসের আয় বনাম ব্যয় তুলনা", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF087A43)))
                        Spacer(Modifier.width(4.dp))
                        Text("আয়", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFD97706)))
                        Spacer(Modifier.width(4.dp))
                        Text("ব্যয়", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val maxVal = monthlyBars.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1000.0) ?: 1000.0
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    val slotW = size.width / monthlyBars.size.coerceAtLeast(1)
                    val barW = (slotW * 0.28f).coerceAtLeast(12f)
                    val maxH = size.height - 10.dp.toPx()
                    monthlyBars.forEachIndexed { i, (_, inc, exp) ->
                        val cx = i * slotW + slotW / 2f
                        val incH = ((inc / maxVal) * maxH).toFloat().coerceAtLeast(4f)
                        val expH = ((exp / maxVal) * maxH).toFloat().coerceAtLeast(4f)
                        drawRoundRect(
                            color = Color(0xFF087A43),
                            topLeft = Offset(cx - barW - 2f, size.height - incH),
                            size = Size(barW, incH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color(0xFFD97706),
                            topLeft = Offset(cx + 2f, size.height - expH),
                            size = Size(barW, expH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    monthlyBars.forEach { (label, _, _) ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // District Impact & Transparency Ratios
        item {
            FoundationCard {
                Text("স্বচ্ছতা অনুপাত ও জেলা কার্যক্রম", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val utilPct = if (income > 0) ((expense / income) * 100).toInt() else 0
                val netPct = (100 - utilPct).coerceAtLeast(0)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ত্রাণ ও প্রকল্প ব্যয় হার", style = MaterialTheme.typography.bodyMedium)
                    Text("${PrayerCalculator.bnDigits(utilPct)}%", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("তহবিল সংরক্ষণ অনুপাত", style = MaterialTheme.typography.bodyMedium)
                    Text("${PrayerCalculator.bnDigits(netPct)}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (relief.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("ত্রাণপ্রাপ্ত জেলাসমূহ", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    relief.forEach { loc ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${loc.district} (${loc.disasterType})", style = MaterialTheme.typography.bodySmall)
                            Text("${PrayerCalculator.bnDigits(loc.familiesHelped)} পরিবার • ${PrayerCalculator.formatTaka(loc.amountDistributed)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
