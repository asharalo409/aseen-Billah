package com.manobseba.foundation.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.R
import com.manobseba.foundation.data.SeedCatalog
import com.manobseba.foundation.data.model.Member
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.theme.FOUNDATION_PALETTES
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MembersScreen(
    vm: MainViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val members by vm.members.collectAsStateWithLifecycle()
    val profile by vm.userProfile.collectAsStateWithLifecycle()

    var tabIdx by remember(initialTab) { mutableIntStateOf(initialTab) }
    var search by remember { mutableStateOf("") }
    var showAddMember by remember { mutableStateOf(false) }

    val filteredMembers = remember(members, search) {
        val q = search.trim()
        if (q.isEmpty()) members
        else members.filter { it.fullName.contains(q, true) || it.memberCode.contains(q, true) || (it.district ?: "").contains(q, true) }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (tabIdx <= 1) {
                FloatingActionButton(
                    onClick = { showAddMember = true },
                    modifier = Modifier.testTag("members_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "নতুন সদস্য")
                }
            }
        }
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            ScrollableTabRow(selectedTabIndex = tabIdx, edgePadding = 12.dp) {
                listOf("সদস্য তালিকা", "কমিটি", "দায়িত্ব গাইড", "থিম স্টুডিও", "অ্যাপ লক").forEachIndexed { idx, label ->
                    Tab(selected = tabIdx == idx, onClick = { tabIdx = idx }, text = { Text(label) })
                }
            }

            when (tabIdx) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            label = { Text("নাম, কোড বা জেলা দিয়ে খুঁজুন...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("members_search_input")
                        )
                    }
                    items(filteredMembers, key = { it.id }) { m ->
                        val roleTitle = m.roleLabel ?: SeedCatalog.roleLabel(m.role)
                        FoundationCard {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(m.fullName.take(1), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text("$roleTitle • ${m.memberCode} • ${m.district ?: "সারাদেশ"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!m.bloodGroup.isNullOrBlank()) {
                                        Text("রক্তের গ্রুপ: ${m.bloodGroup}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(
                                        if (m.active) "সক্রিয়" else "নিষ্ক্রিয়",
                                        if (m.active) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.surfaceVariant,
                                        if (m.active) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row {
                                        m.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                            IconButton(onClick = { ExportShareHelper.dialPhone(context, phone) }) {
                                                Icon(Icons.Default.Call, contentDescription = "কল", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        IconButton(onClick = { ExportShareHelper.shareMemberIdCard(context, m, roleTitle) }) {
                                            Icon(Icons.Default.Badge, contentDescription = "আইডি কার্ড শেয়ার", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SectionHeader("কার্যনির্বাহী কমিটি", "সংগঠনের দায়িত্বশীল নেতৃত্ব") }
                    items(members.filter { it.active && (it.rank ?: 100) < 20 }, key = { it.id }) { m ->
                        FoundationCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(m.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text(m.roleLabel ?: SeedCatalog.roleLabel(m.role), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                }
                                StatusPill(m.memberCode, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                2 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SectionHeader("পদবী ও দায়িত্ব নির্দেশিকা", "প্রতিটি পদের অধিকার ও কর্তব্য") }
                    items(SeedCatalog.ROLES, key = { it.key }) { role ->
                        FoundationCard {
                            Text("${role.label} (র‍্যাংক ${PrayerCalculator.bnDigits(role.rank)})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            role.duties.forEach { duty ->
                                Text("• $duty", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                3 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val curPalette = profile?.themePalette ?: "green"
                        val curBg = profile?.themeBg ?: "plain"
                        val curDark = profile?.darkMode ?: false
                        FoundationCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("থিম স্টুডিও", style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("ডার্ক মোড (রাতের থিম)", style = MaterialTheme.typography.titleSmall)
                                Switch(checked = curDark, onCheckedChange = { vm.setThemePreference(curPalette, curBg, it) })
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("রঙের প্যালেট নির্বাচন করুন", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            FOUNDATION_PALETTES.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.setThemePreference(p.key, curBg, curDark) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(p.primary))
                                    Spacer(Modifier.width(12.dp))
                                    Text(p.labelBn, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    if (curPalette == p.key) {
                                        StatusPill("সক্রিয়", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("ব্যাকগ্রাউন্ড স্টাইল", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                listOf("plain" to "সাধারণ", "islamic" to "ইসলামিক প্যাটার্ন", "gradient" to "গ্রেডিয়েন্ট", "soft" to "হালকা আভা").forEach { (k, label) ->
                                    FilterChip(selected = curBg == k, onClick = { vm.setThemePreference(curPalette, k, curDark) }, label = { Text(label) })
                                }
                            }
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item { AppLockSettingsContent(lockManager = vm.lockManager, onToast = { vm.showToast(it) }) }
                }
            }
        }
    }

    if (showAddMember) {
        AddMemberDialog(
            onDismiss = { showAddMember = false },
            onSave = { m -> vm.saveMember(m, false); showAddMember = false }
        )
    }
}

@Composable
fun MyDashboardScreen(
    vm: MainViewModel,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by vm.userProfile.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val user = vm.currentUser()

    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: vm.currentDisplayName()) }
    var memberCode by remember(profile?.memberCode) { mutableStateOf(profile?.memberCode ?: "") }

    val myTxs = remember(transactions, user?.uid, profile?.displayName) {
        transactions.filter { it.userId == user?.uid || (!profile?.displayName.isNullOrBlank() && it.donorName == profile?.displayName) }
    }
    val myTotal = myTxs.filter { it.kind != "expense" }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FoundationCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(vm.currentDisplayName(), style = MaterialTheme.typography.titleLarge)
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        StatusPill(SeedCatalog.roleLabel(vm.currentRole()), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        item {
            FoundationCard {
                Text("আমার মোট অবদান: ${PrayerCalculator.formatTaka(myTotal)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("রেকর্ড সংখ্যা: ${PrayerCalculator.bnDigits(myTxs.size)}টি লেনদেন", style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            FoundationCard {
                Text("প্রোফাইল ও সদস্য কোড লিংক", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("পূর্ণ নাম") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = memberCode, onValueChange = { memberCode = it }, label = { Text("সদস্য কোড (যেমন: MSF-001)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.updateProfileNameAndCode(displayName, memberCode) }, modifier = Modifier.fillMaxWidth()) {
                    Text("প্রোফাইল সংরক্ষণ করুন")
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    vm.signOut()
                    onSignedOut()
                },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out_button))
            }
        }
    }
}

@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onSave: (Member) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("ঢাকা") }
    var blood by remember { mutableStateOf("O+") }
    var code by remember { mutableStateOf("MSF-${(10..99).random()}") }
    var roleKey by remember { mutableStateOf("member") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন সদস্য যোগ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("পূর্ণ নাম *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("ফোন নম্বর") }, singleLine = true)
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, singleLine = true)
                OutlinedTextField(value = blood, onValueChange = { blood = it }, label = { Text("রক্তের গ্রুপ") }, singleLine = true)
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("সদস্য কোড") }, singleLine = true)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SeedCatalog.ROLES.filter { it.key != "admin" }.forEach { r ->
                        FilterChip(selected = roleKey == r.key, onClick = { roleKey = r.key }, label = { Text(r.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val rDef = SeedCatalog.ROLES.firstOrNull { it.key == roleKey }
                    onSave(
                        Member(
                            fullName = name.trim(),
                            phone = phone.trim().ifBlank { null },
                            district = district.trim().ifBlank { null },
                            bloodGroup = blood.trim().ifBlank { null },
                            role = roleKey,
                            roleLabel = rDef?.label,
                            rank = rDef?.rank ?: 100,
                            memberCode = code.trim(),
                            active = true,
                            joinedDate = LocalDate.now().toString()
                        )
                    )
                }
            }) { Text("যোগ করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
