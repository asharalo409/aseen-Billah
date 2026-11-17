package com.manobseba.foundation.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.DistrictsData
import com.manobseba.foundation.data.model.FieldReport
import com.manobseba.foundation.data.model.Project
import com.manobseba.foundation.data.model.ReliefLocation
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
fun ProjectsWorksMapScreen(
    vm: MainViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val funds by vm.funds.collectAsStateWithLifecycle()
    val reports by vm.fieldReports.collectAsStateWithLifecycle()
    val locations by vm.reliefLocations.collectAsStateWithLifecycle()

    var tabIdx by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("projects_works_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন যোগ করুন")
            }
        }
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            SecondaryTabRow(selectedTabIndex = tabIdx) {
                Tab(selected = tabIdx == 0, onClick = { tabIdx = 0 }, text = { Text("প্রকল্প") })
                Tab(selected = tabIdx == 1, onClick = { tabIdx = 1 }, text = { Text("মাঠের কাজ") })
                Tab(selected = tabIdx == 2, onClick = { tabIdx = 2 }, text = { Text("ত্রাণ মানচিত্র") })
            }

            when (tabIdx) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SectionHeader("চলমান মানবিক প্রকল্প", "লক্ষ্যমাত্রা ও তহবিল অগ্রগতি") }
                    items(projects, key = { it.id }) { proj ->
                        val raised = transactions.filter { it.fundId == proj.fundId && it.kind != "expense" }.sumOf { it.amount }
                        val ratio = (raised / proj.goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                        FoundationCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatusPill(proj.area ?: "সারাদেশ", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                StatusPill(if (proj.status == "active") "চলমান" else proj.status, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(proj.title, style = MaterialTheme.typography.titleMedium)
                            if (!proj.description.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(proj.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("সংগৃহীত: ${PrayerCalculator.formatTaka(raised)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("লক্ষ্য: ${PrayerCalculator.formatTaka(proj.goal)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SectionHeader("মাঠ পর্যায়ের কাজের রিপোর্ট", "যাচাইকৃত মানবিক সহায়তা ও ব্যয়") }
                    items(reports, key = { it.id }) { rep ->
                        FoundationCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rep.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusPill(
                                    if (rep.status == "verified") "যাচাইকৃত" else "অপেক্ষমাণ",
                                    if (rep.status == "verified") Color(0xFFD1FAE5) else MaterialTheme.colorScheme.secondaryContainer,
                                    if (rep.status == "verified") Color(0xFF065F46) else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (!rep.body.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(rep.body, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${rep.location ?: "মাঠ পর্যায়"} • ব্যয় ${PrayerCalculator.formatTaka(rep.spent)} • উপকারভোগী ${PrayerCalculator.bnDigits(rep.beneficiaries)} জন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = { vm.reactFieldReport(rep) }) {
                                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("কৃতজ্ঞতা (${PrayerCalculator.bnDigits(rep.reactionCount)})")
                            }
                        }
                    }
                }
                else -> ReliefMapTab(locations = locations)
            }
        }
    }

    if (showAddDialog) {
        when (tabIdx) {
            0 -> AddProjectModal(funds.firstOrNull()?.id ?: "f1", onDismiss = { showAddDialog = false }) { p ->
                vm.saveProject(p); showAddDialog = false
            }
            1 -> AddReportModal(onDismiss = { showAddDialog = false }) { r ->
                vm.saveFieldReport(r); showAddDialog = false
            }
            else -> AddReliefModal(onDismiss = { showAddDialog = false }) { loc ->
                vm.saveReliefLocation(loc); showAddDialog = false
            }
        }
    }
}

@Composable
private fun ReliefMapTab(locations: List<ReliefLocation>) {
    val totalFamilies = locations.sumOf { it.familiesHelped }
    val totalAmount = locations.sumOf { it.amountDistributed }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("বাংলাদেশ ত্রাণ মানচিত্র", "বিভাগ ও জেলাভিত্তিক ত্রাণ কেন্দ্র") }
        item {
            FoundationCard {
                Text(
                    "মোট ${PrayerCalculator.bnDigits(locations.size)}টি এলাকা • ${PrayerCalculator.bnDigits(totalFamilies)} পরিবার • ${PrayerCalculator.formatTaka(totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                val primary = MaterialTheme.colorScheme.primary
                val pinColor = Color(0xFFDC2626)
                Canvas(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                    // Bounds of Bangladesh: lat 20.6..26.7, lng 88.0..92.7
                    fun mapX(lng: Double) = ((lng - 87.9) / (92.8 - 87.9)).toFloat().coerceIn(0.05f, 0.95f) * size.width
                    fun mapY(lat: Double) = (1f - ((lat - 20.5) / (26.75 - 20.5)).toFloat().coerceIn(0.05f, 0.95f)) * size.height

                    // Stylized Bangladesh boundary silhouette
                    val outline = listOf(
                        26.62 to 88.35, 26.35 to 89.45, 25.85 to 89.88, 25.15 to 90.00,
                        25.15 to 92.20, 24.40 to 92.45, 23.70 to 91.25, 22.90 to 91.50,
                        21.85 to 92.60, 20.80 to 92.32, 22.00 to 91.85, 22.45 to 90.75,
                        21.90 to 89.90, 22.10 to 89.05, 23.25 to 88.65, 24.45 to 88.15, 25.80 to 88.15
                    )
                    val path = Path()
                    outline.forEachIndexed { idx, (lat, lng) ->
                        val pt = Offset(mapX(lng), mapY(lat))
                        if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    path.close()
                    drawPath(path, color = primary.copy(alpha = 0.14f))
                    drawPath(path, color = primary, style = Stroke(width = 2.dp.toPx()))

                    locations.forEach { loc ->
                        val pt = Offset(mapX(loc.lng), mapY(loc.lat))
                        drawCircle(pinColor.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = pt)
                        drawCircle(pinColor, radius = 6.dp.toPx(), center = pt)
                    }
                }
            }
        }
        items(locations, key = { it.id }) { loc ->
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(loc.title, style = MaterialTheme.typography.titleSmall)
                            Text("${loc.district}, ${loc.division} • ${loc.area ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusPill(loc.disasterType, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.height(6.dp))
                Text("${PrayerCalculator.bnDigits(loc.familiesHelped)} পরিবার • বিতরণ: ${PrayerCalculator.formatTaka(loc.amountDistributed)}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AddProjectModal(defaultFundId: String, onDismiss: () -> Unit, onSave: (Project) -> Unit) {
    var title by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var goalStr by remember { mutableStateOf("50000") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন প্রকল্প") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("প্রকল্পের নাম *") })
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("এলাকা") })
                OutlinedTextField(value = goalStr, onValueChange = { goalStr = it }, label = { Text("লক্ষ্যমাত্রা (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("বিবরণ") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onSave(Project(title = title.trim(), area = area.trim().ifBlank { "সারাদেশ" }, goal = goalStr.toDoubleOrNull() ?: 50000.0, fundId = defaultFundId, description = desc.trim()))
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddReportModal(onDismiss: () -> Unit, onSave: (FieldReport) -> Unit) {
    var title by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf("") }
    var spentStr by remember { mutableStateOf("0") }
    var benStr by remember { mutableStateOf("20") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন কাজের রিপোর্ট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("শিরোনাম *") })
                OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("স্থান") })
                OutlinedTextField(value = spentStr, onValueChange = { spentStr = it }, label = { Text("ব্যয় (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = benStr, onValueChange = { benStr = it }, label = { Text("উপকারভোগী সংখ্যা") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("বিস্তারিত বিবরণ") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onSave(FieldReport(title = title.trim(), location = loc.trim(), spent = spentStr.toDoubleOrNull() ?: 0.0, beneficiaries = benStr.toIntOrNull() ?: 0, body = body.trim(), status = "verified"))
            }) { Text("জমা দিন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddReliefModal(onDismiss: () -> Unit, onSave: (ReliefLocation) -> Unit) {
    var title by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("ঢাকা") }
    var disaster by remember { mutableStateOf("বন্যা") }
    var famStr by remember { mutableStateOf("50") }
    var amtStr by remember { mutableStateOf("25000") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ত্রাণ মানচিত্রে নতুন কেন্দ্র") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("ক্যাম্পের নাম *") })
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলার নাম (যেমন: সিলেট)") })
                OutlinedTextField(value = disaster, onValueChange = { disaster = it }, label = { Text("দুর্যোগের ধরন (বন্যা/শীত)") })
                OutlinedTextField(value = famStr, onValueChange = { famStr = it }, label = { Text("উপকৃত পরিবার") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = amtStr, onValueChange = { amtStr = it }, label = { Text("বিতরণকৃত অর্থ (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    val coord = DistrictsData.findByName(district.trim())
                    onSave(ReliefLocation(title = title.trim(), division = coord.division, district = coord.name, disasterType = disaster.trim().ifBlank { "ত্রাণ" }, familiesHelped = famStr.toIntOrNull() ?: 0, amountDistributed = amtStr.toDoubleOrNull() ?: 0.0, distributedDate = LocalDate.now().toString(), lat = coord.lat, lng = coord.lng))
                }
            }) { Text("যোগ করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
