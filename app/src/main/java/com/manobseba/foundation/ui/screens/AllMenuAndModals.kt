package com.manobseba.foundation.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.model.FoundationTransaction
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.navigation.AppRoute
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import java.time.LocalDate

private data class MenuShortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: AppRoute
)

@Composable
fun AllMenuScreen(
    vm: MainViewModel,
    onNavigate: (AppRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val members by vm.members.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val notices by vm.notices.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val shortcuts = listOf(
        MenuShortcut("এক্সিকিউটিভ ওভারভিউ", "KPI, গ্রাফ ও স্বচ্ছতা অনুপাত", Icons.Default.Analytics, AppRoute.Overview),
        MenuShortcut("মানবসেবা AI হাব", "চ্যাট, খাতা স্ক্যানার ও লিগ্যাসি আমদানি", Icons.Default.AutoAwesome, AppRoute.AiHub),
        MenuShortcut("আয়-ব্যয় ও চাঁদা গ্রিড", "তহবিল, রসিদ ও ১২ মাসের চাঁদা", Icons.Default.AccountBalance, AppRoute.Finance),
        MenuShortcut("রক্তদান ও SOS", "২৪ ঘণ্টার আবেদন ও রক্তদাতা", Icons.Default.Bloodtype, AppRoute.Blood),
        MenuShortcut("প্রকল্প, কাজ ও ত্রাণ মানচিত্র", "বাংলাদেশ ত্রাণ ম্যাপ ও রিপোর্ট", Icons.Default.Map, AppRoute.ProjectsWorksMap),
        MenuShortcut("ইসলামিক কর্নার", "নামাজ, কিবলা, তসবিহ, আমল, জাকাত, কুরআন, হাদিস", Icons.Default.Mosque, AppRoute.Islamic),
        MenuShortcut("আবহাওয়া ও নিরাপত্তা", "লাইভ তাপমাত্রা, AQI ও ৭ দিনের পূর্বাভাস", Icons.Default.WbSunny, AppRoute.Weather),
        MenuShortcut("সদস্য, থিম ও অ্যাপ লক", "সদস্য তালিকা, কমিটি, থিম স্টুডিও", Icons.Default.Group, AppRoute.Members),
        MenuShortcut("কমিউনিটি, পোল ও ক্যালেন্ডার", "ভোট, চ্যাট, নোটিশ, স্বেচ্ছাসেবা, ফর্ম", Icons.Default.Forum, AppRoute.Community),
        MenuShortcut("আমার ড্যাশবোর্ড", "ব্যক্তিগত প্রোফাইল, চাঁদা ও লগআউট", Icons.Default.Person, AppRoute.MyDashboard),
        MenuShortcut("অ্যাডমিন প্যানেল", "সেটিংস, লক্ষ্যমাত্রা ও আবেদন অনুমোদন", Icons.Default.AdminPanelSettings, AppRoute.Admin)
    )

    val q = query.trim()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("সকল সেবা ও মেনু", "সার্চ করুন অথবা যেকোনো বিভাগ খুলুন")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("সদস্য, প্রকল্প, নোটিশ বা মেনু খুঁজুন...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("global_search_input")
            )
        }

        if (q.isNotEmpty()) {
            val matchedMembers = members.filter { it.fullName.contains(q, true) || it.memberCode.contains(q, true) }
            val matchedProjects = projects.filter { it.title.contains(q, true) }
            val matchedNotices = notices.filter { it.title.contains(q, true) || it.body.contains(q, true) }
            item {
                FoundationCard {
                    Text("সার্চ ফলাফল: সদস্য (${PrayerCalculator.bnDigits(matchedMembers.size)}), প্রকল্প (${PrayerCalculator.bnDigits(matchedProjects.size)}), নোটিশ (${PrayerCalculator.bnDigits(matchedNotices.size)})", style = MaterialTheme.typography.titleSmall)
                    matchedMembers.take(3).forEach { m -> Text("• সদস্য: ${m.fullName} (${m.memberCode})", modifier = Modifier.clickable { onNavigate(AppRoute.Members) }.padding(vertical = 3.dp)) }
                    matchedProjects.take(3).forEach { p -> Text("• প্রকল্প: ${p.title}", modifier = Modifier.clickable { onNavigate(AppRoute.ProjectsWorksMap) }.padding(vertical = 3.dp)) }
                    matchedNotices.take(3).forEach { n -> Text("• নোটিশ: ${n.title}", modifier = Modifier.clickable { onNavigate(AppRoute.Community) }.padding(vertical = 3.dp)) }
                }
            }
        }

        val shownShortcuts = if (q.isEmpty()) shortcuts else shortcuts.filter { it.title.contains(q, true) || it.subtitle.contains(q, true) }
        shownShortcuts.forEach { item ->
            item {
                FoundationCard(modifier = Modifier.clickable { onNavigate(item.route) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonateModalDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val funds by vm.funds.collectAsStateWithLifecycle()
    var amountStr by remember { mutableStateOf("500") }
    var method by remember { mutableStateOf("bKash") }
    var donorName by remember { mutableStateOf(vm.currentDisplayName()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("মানবসেবা ফাউন্ডেশনে দান করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(settings?.bankDetails ?: "বিকাশ / নগদ: 01700-000000", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        FilledTonalButton(onClick = { ExportShareHelper.copyText(context, settings?.hotline ?: "01700000000") }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("নম্বর কপি করুন")
                        }
                    }
                }
                OutlinedTextField(value = donorName, onValueChange = { donorName = it }, label = { Text("দাতার নাম") }, singleLine = true)
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("দানের পরিমাণ (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("মাধ্যম (bKash / নগদ / ব্যাংক)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val today = LocalDate.now()
                        vm.addTransaction(
                            FoundationTransaction(
                                kind = "donation",
                                amount = amt,
                                fundId = funds.firstOrNull()?.id ?: "f1",
                                donorName = donorName.trim().ifBlank { "দাতা" },
                                method = method.trim(),
                                category = "অনলাইন/সরাসরি দান",
                                receiptNo = "MSF-${System.currentTimeMillis() % 100000}",
                                dateStr = today.toString(),
                                year = today.year,
                                month = today.monthValue
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("donate_confirm_btn")
            ) { Text("দান নিশ্চিত করুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বন্ধ") } }
    )
}
