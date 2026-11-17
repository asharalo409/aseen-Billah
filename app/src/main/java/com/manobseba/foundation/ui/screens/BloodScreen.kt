package com.manobseba.foundation.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.model.BloodDonor
import com.manobseba.foundation.data.model.BloodRequest
import com.manobseba.foundation.data.model.VALID_BLOOD_GROUPS
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.EmptyStateBanner
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun BloodScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val requests by vm.bloodRequests.collectAsStateWithLifecycle()
    val donors by vm.bloodDonors.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var tabIdx by remember { mutableIntStateOf(0) }
    var groupFilter by remember { mutableStateOf("সব") }
    var showNewRequest by remember { mutableStateOf(false) }
    var showNewDonor by remember { mutableStateOf(false) }

    val filteredRequests = remember(requests, groupFilter) {
        if (groupFilter == "সব") requests else requests.filter { it.bloodGroup == groupFilter }
    }
    val filteredDonors = remember(donors, groupFilter) {
        if (groupFilter == "সব") donors else donors.filter { it.bloodGroup == groupFilter }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (tabIdx == 0) showNewRequest = true else showNewDonor = true },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White,
                modifier = Modifier.testTag("blood_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন রক্তের আবেদন বা দাতা")
            }
        }
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            SecondaryTabRow(selectedTabIndex = tabIdx) {
                Tab(selected = tabIdx == 0, onClick = { tabIdx = 0 }, text = { Text("২৪ ঘণ্টার আবেদন") })
                Tab(selected = tabIdx == 1, onClick = { tabIdx = 1 }, text = { Text("রক্তদাতা (${PrayerCalculator.bnDigits(donors.size)})") })
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    FoundationCard(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Bloodtype, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("জরুরি রক্ত ও অ্যাম্বুলেন্স সহায়তা", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                Text("তাৎক্ষণিক সাহায্যে ৯৯৯ বা হটলাইনে কল করুন", style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalButton(
                                onClick = { ExportShareHelper.dialPhone(context, settings?.hotline ?: "999") },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("হটলাইন")
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        (listOf("সব") + VALID_BLOOD_GROUPS).forEach { grp ->
                            FilterChip(selected = groupFilter == grp, onClick = { groupFilter = grp }, label = { Text(grp) })
                        }
                    }
                }

                if (tabIdx == 0) {
                    if (filteredRequests.isEmpty()) {
                        item { EmptyStateBanner(Icons.Default.Bloodtype, "কোনো রক্তের আবেদন নেই", "জরুরি প্রয়োজনে + বোতামে ট্যাপ করে আবেদন পোস্ট করুন") }
                    }
                    items(filteredRequests, key = { it.id }) { req ->
                        FoundationCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                StatusPill(req.bloodGroup, MaterialTheme.colorScheme.error, Color.White)
                                StatusPill(
                                    if (req.managed) "ম্যানেজ হয়েছে" else if (req.urgent) "অতি জরুরি" else "চলমান",
                                    if (req.managed) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.errorContainer,
                                    if (req.managed) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(req.hospital, style = MaterialTheme.typography.titleMedium)
                            Text("সমস্যা: ${req.patientProblem}", style = MaterialTheme.typography.bodyMedium)
                            Text("আবেদনকারী: ${req.applicantName} • ${req.district ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { ExportShareHelper.dialPhone(context, req.contact) },
                                    modifier = Modifier.weight(1f).testTag("blood_call_${req.id}")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("কল: ${req.contact}")
                                }
                                if (!req.managed) {
                                    FilledTonalButton(onClick = { vm.markBloodManaged(req.id) }) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("ম্যানেজ")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredDonors, key = { it.id }) { d ->
                        val daysSince = d.lastDonated?.let { raw ->
                            runCatching { ChronoUnit.DAYS.between(LocalDate.parse(raw), LocalDate.now()).toInt() }.getOrNull()
                        }
                        val eligible = daysSince == null || daysSince >= 90
                        FoundationCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(d.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text("${d.district}${d.upazila?.let { ", $it" } ?: ""} • মোট দান ${PrayerCalculator.bnDigits(d.totalDonations)} বার", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(d.bloodGroup, MaterialTheme.colorScheme.error, Color.White)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (eligible) "রক্তদানের জন্য প্রস্তুত (৯০+ দিন পূর্ণ)" else "শেষ দান ${PrayerCalculator.bnDigits(daysSince ?: 0)} দিন আগে",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (eligible) Color(0xFF087A43) else Color(0xFFD97706)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { ExportShareHelper.dialPhone(context, d.phone) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("কল করুন")
                                }
                                FilledTonalButton(
                                    onClick = { ExportShareHelper.openWhatsApp(context, d.whatsapp ?: d.phone, "আসসালামু আলাইকুম, জরুরি ${d.bloodGroup} রক্তের জন্য যোগাযোগ করছি।") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("হোয়াটসঅ্যাপ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewRequest) {
        NewBloodRequestDialog(
            onDismiss = { showNewRequest = false },
            onSave = { req ->
                vm.submitBloodRequest(req)
                showNewRequest = false
            }
        )
    }

    if (showNewDonor) {
        NewDonorDialog(
            defaultName = vm.currentDisplayName(),
            onDismiss = { showNewDonor = false },
            onSave = { d ->
                vm.registerBloodDonor(d)
                showNewDonor = false
            }
        )
    }
}

@Composable
private fun NewBloodRequestDialog(onDismiss: () -> Unit, onSave: (BloodRequest) -> Unit) {
    var group by remember { mutableStateOf("O+") }
    var hospital by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("ঢাকা") }
    var contact by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var applicant by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("জরুরি রক্তের আবেদন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VALID_BLOOD_GROUPS.forEach { g ->
                        FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g) })
                    }
                }
                OutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = { Text("হাসপাতালের নাম *") }, singleLine = true)
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, singleLine = true)
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("যোগাযোগ নম্বর *") }, singleLine = true)
                OutlinedTextField(value = problem, onValueChange = { problem = it }, label = { Text("রোগীর সমস্যা ও ব্যাগ সংখ্যা *") })
                OutlinedTextField(value = applicant, onValueChange = { applicant = it }, label = { Text("আবেদনকারীর নাম *") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hospital.isNotBlank() && contact.isNotBlank()) {
                        onSave(
                            BloodRequest(
                                bloodGroup = group,
                                hospital = hospital.trim(),
                                district = district.trim(),
                                contact = contact.trim(),
                                patientProblem = problem.ifBlank { "জরুরি রক্ত প্রয়োজন" },
                                applicantName = applicant.ifBlank { "আবেদনকারী" },
                                urgent = true,
                                managed = false
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("blood_request_submit_btn")
            ) { Text("পোস্ট করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun NewDonorDialog(defaultName: String, onDismiss: () -> Unit, onSave: (BloodDonor) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    var group by remember { mutableStateOf("O+") }
    var district by remember { mutableStateOf("ঢাকা") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("রক্তদাতা হিসেবে নিবন্ধন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("পূর্ণ নাম *") }, singleLine = true)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VALID_BLOOD_GROUPS.forEach { g ->
                        FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g) })
                    }
                }
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("মোবাইল নম্বর *") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    onSave(BloodDonor(fullName = name.trim(), bloodGroup = group, district = district.trim(), phone = phone.trim(), whatsapp = phone.trim(), available = true))
                }
            }) { Text("নিবন্ধন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
