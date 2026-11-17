package com.manobseba.foundation.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.model.FoundationEvent
import com.manobseba.foundation.data.model.PublicRequest
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CommunityScreen(
    vm: MainViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var tabIdx by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("ক্যালেন্ডার", "ভোট/পোল", "চ্যাট", "নোটিশ", "স্বেচ্ছাসেবা", "মতামত", "আবেদন ফর্ম")

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (tabIdx in listOf(0, 1, 3, 4, 5)) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("community_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "নতুন যোগ করুন")
                }
            }
        }
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            ScrollableTabRow(selectedTabIndex = tabIdx, edgePadding = 12.dp) {
                tabs.forEachIndexed { idx, label ->
                    Tab(selected = tabIdx == idx, onClick = { tabIdx = idx }, text = { Text(label) })
                }
            }

            when (tabIdx) {
                0 -> EventsTab(vm)
                1 -> PollsTab(vm)
                2 -> ChatTab(vm)
                3 -> NoticesTab(vm)
                4 -> DutiesTab(vm)
                5 -> VoicesTab(vm)
                else -> PublicFormTab(vm)
            }
        }
    }

    if (showCreateDialog) {
        when (tabIdx) {
            0 -> AddEventDialog(onDismiss = { showCreateDialog = false }) { ev -> vm.saveEvent(ev); showCreateDialog = false }
            1 -> AddPollDialog(onDismiss = { showCreateDialog = false }) { q, d, opts -> vm.createPoll(q, d, opts); showCreateDialog = false }
            3 -> AddNoticeDialog(onDismiss = { showCreateDialog = false }) { t, b, u -> vm.publishNotice(t, b, u); showCreateDialog = false }
            4 -> AddDutyDialog(onDismiss = { showCreateDialog = false }) { t, d, a, due -> vm.saveDuty(t, d, a, due); showCreateDialog = false }
            else -> AddVoiceDialog(vm.currentDisplayName(), onDismiss = { showCreateDialog = false }) { n, l, r, b -> vm.submitVoice(n, l, r, b); showCreateDialog = false }
        }
    }
}

@Composable
private fun EventsTab(vm: MainViewModel) {
    val events by vm.events.collectAsStateWithLifecycle()
    val uid = vm.currentUserId()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("অনুষ্ঠান ও সভার ক্যালেন্ডার", "উপস্থিতি (RSVP) নিশ্চিত করুন") }
        items(events, key = { it.id }) { ev ->
            val date = Instant.ofEpochMilli(ev.eventDateMillis).atZone(ZoneId.of("Asia/Dhaka")).toLocalDate()
            val attending = uid in ev.attendeeUids
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(PrayerCalculator.formatGregorianBn(date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    StatusPill(ev.type, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(6.dp))
                Text(ev.title, style = MaterialTheme.typography.titleMedium)
                if (!ev.location.isNullOrBlank()) Text("স্থান: ${ev.location}", style = MaterialTheme.typography.bodySmall)
                if (!ev.description.isNullOrBlank()) Text(ev.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("উপস্থিত থাকবেন: ${PrayerCalculator.bnDigits(ev.attendeeUids.size)} জন", style = MaterialTheme.typography.labelMedium)
                    Button(onClick = { vm.toggleRsvp(ev) }) {
                        Text(if (attending) "নিশ্চিতকৃত" else "উপস্থিত থাকব")
                    }
                }
            }
        }
    }
}

@Composable
private fun PollsTab(vm: MainViewModel) {
    val polls by vm.polls.collectAsStateWithLifecycle()
    val myVotes = remember { mutableStateMapOf<String, Int?>() }

    LaunchedEffect(polls) {
        polls.forEach { p ->
            myVotes[p.id] = vm.getMyPollVote(p.id)
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("গোপন ভোট ও মতামত পোল", "আপনার ভোট সম্পূর্ণ গোপন থাকে") }
        items(polls, key = { it.id }) { poll ->
            val totalVotes = poll.voteCounts.sum().coerceAtLeast(1)
            val chosen = myVotes[poll.id]
            FoundationCard {
                Text(poll.question, style = MaterialTheme.typography.titleMedium)
                if (!poll.description.isNullOrBlank()) Text(poll.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                poll.options.forEachIndexed { idx, opt ->
                    val cnt = poll.voteCounts.getOrElse(idx) { 0 }
                    val frac = cnt.toFloat() / totalVotes
                    val isSelected = chosen == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !poll.closed) {
                                myVotes[poll.id] = idx
                                vm.voteInPoll(poll, idx)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(opt, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                Text("${PrayerCalculator.bnDigits(cnt)} ভোট (${PrayerCalculator.bnDigits((frac * 100).toInt())}%)", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTab(vm: MainViewModel) {
    val messages by vm.chatMessages.collectAsStateWithLifecycle()
    val funds by vm.funds.collectAsStateWithLifecycle()
    var channel by remember { mutableStateOf("general") }
    var text by remember { mutableStateOf("") }
    val uid = vm.currentUserId()

    val channels = remember(funds) { listOf("general" to "কেন্দ্রীয় আলোচনা") + funds.map { it.id to it.name } }
    val shown = remember(messages, channel) { messages.filter { it.channel == channel } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { (id, label) ->
                FilterChip(selected = channel == id, onClick = { channel = id }, label = { Text(label) })
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { m ->
                val isMine = m.userId == uid
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.84f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "${m.senderName}${m.senderRole?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMine) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.primary
                            )
                            Text(m.body, color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("বার্তা লিখুন...") }, singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = {
                    val body = text.trim()
                    if (body.isNotEmpty()) {
                        text = ""
                        vm.sendCommunityMessage(channel, body)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "পাঠান", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun NoticesTab(vm: MainViewModel) {
    val notices by vm.notices.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("নোটিশ বোর্ড", "সংগঠনের ঘোষণা ও জরুরি নির্দেশনা") }
        items(notices, key = { it.id }) { n ->
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(n.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (n.urgent) StatusPill("জরুরি", MaterialTheme.colorScheme.error, Color.White)
                }
                Spacer(Modifier.height(6.dp))
                Text(n.body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { vm.reactNotice(n, true) }) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("পছন্দ (${PrayerCalculator.bnDigits(n.likeCount)})")
                    }
                    FilledTonalButton(onClick = { vm.reactNotice(n, false) }) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("সমর্থন (${PrayerCalculator.bnDigits(n.supportCount)})")
                    }
                }
            }
        }
    }
}

@Composable
private fun DutiesTab(vm: MainViewModel) {
    val duties by vm.duties.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("স্বেচ্ছাসেবক দায়িত্ব বণ্টন", "ট্যাপ করে কাজের অগ্রগতি আপডেট করুন") }
        items(duties, key = { it.id }) { d ->
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(d.title, style = MaterialTheme.typography.titleMedium)
                        Text("দায়িত্বপ্রাপ্ত: ${d.assignedToName}${d.dueOn?.let { " • সময়: $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val nextStatus = when (d.status) {
                        "todo" -> "doing"
                        "doing" -> "done"
                        else -> "todo"
                    }
                    val label = when (d.status) {
                        "done" -> "সম্পন্ন"
                        "doing" -> "চলমান"
                        else -> "বাকি"
                    }
                    FilledTonalButton(onClick = { vm.updateDutyStatus(d.id, nextStatus) }) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoicesTab(vm: MainViewModel) {
    val voices by vm.voices.collectAsStateWithLifecycle()
    val shown = voices.filter { it.status == "approved" || vm.isCurrentUserAdmin() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("মানুষের কথা (Voices)", "দাতা, সদস্য ও উপকারভোগীদের অনুভূতি") }
        items(shown, key = { it.id }) { v ->
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${v.name} • ${v.label}", style = MaterialTheme.typography.titleSmall)
                    Row {
                        repeat(v.rating.coerceIn(1, 5)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(v.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PublicFormTab(vm: MainViewModel) {
    var kind by remember { mutableStateOf("aid") }
    var name by remember { mutableStateOf(vm.currentDisplayName()) }
    var phone by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("ঢাকা") }
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            FoundationCard {
                Text("সহায়তা, যোগাযোগ ও সদস্য আবেদন", style = MaterialTheme.typography.titleLarge)
                Text("আপনার ব্যক্তিগত তথ্য শুধুমাত্র আপনি ও অনুমোদিত অ্যাডমিন দেখতে পাবেন।", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("aid" to "সহায়তা আবেদন", "member_apply" to "সদস্য হতে চাই", "contact" to "যোগাযোগ").forEach { (k, l) ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(l) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("আপনার নাম *") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("মোবাইল নম্বর *") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("জেলা") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("বিষয়") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("বিস্তারিত বিবরণ") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            vm.submitPublicRequest(
                                PublicRequest(
                                    kind = kind,
                                    name = name.trim(),
                                    phone = phone.trim().ifBlank { null },
                                    district = district.trim().ifBlank { null },
                                    subject = subject.trim().ifBlank { null },
                                    details = details.trim().ifBlank { null }
                                )
                            )
                            phone = ""; subject = ""; details = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("public_request_submit_btn")
                ) { Text("আবেদন জমা দিন") }
            }
        }
    }
}

@Composable
fun AdminScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val requests by vm.publicRequests.collectAsStateWithLifecycle()
    val voices by vm.voices.collectAsStateWithLifecycle()

    var orgName by remember(settings?.orgName) { mutableStateOf(settings?.orgName ?: "মানবসেবা ফাউন্ডেশন") }
    var slogan by remember(settings?.slogan) { mutableStateOf(settings?.slogan ?: "মানুষের পাশে, মানবতার পাশে") }
    var hotline by remember(settings?.hotline) { mutableStateOf(settings?.hotline ?: "01700-000000") }
    var bankDetails by remember(settings?.bankDetails) { mutableStateOf(settings?.bankDetails ?: "") }
    var targetStr by remember(settings?.targetAmount) { mutableStateOf((settings?.targetAmount ?: 200000.0).toLong().toString()) }
    var feeStr by remember(settings?.monthlyFee) { mutableStateOf((settings?.monthlyFee ?: 200.0).toLong().toString()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("অ্যাডমিন কন্ট্রোল প্যানেল", "সংগঠন কনফিগারেশন ও আবেদন অনুমোদন") }

        item {
            FoundationCard {
                Text("সংগঠনের তথ্য ও লক্ষ্যমাত্রা", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("সংগঠনের নাম") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = slogan, onValueChange = { slogan = it }, label = { Text("স্লোগান") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hotline, onValueChange = { hotline = it }, label = { Text("জরুরি হটলাইন") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bankDetails, onValueChange = { bankDetails = it }, label = { Text("ব্যাংক ও বিকাশ তথ্য") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = targetStr, onValueChange = { targetStr = it }, label = { Text("তহবিল লক্ষ্য (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = feeStr, onValueChange = { feeStr = it }, label = { Text("মাসিক চাঁদা (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        settings?.let { cur ->
                            vm.updateFoundationSettings(
                                cur.copy(
                                    orgName = orgName.trim(),
                                    slogan = slogan.trim(),
                                    hotline = hotline.trim(),
                                    bankDetails = bankDetails.trim(),
                                    targetAmount = targetStr.toDoubleOrNull() ?: cur.targetAmount,
                                    monthlyFee = feeStr.toDoubleOrNull() ?: cur.monthlyFee
                                )
                            )
                        }
                    },
                    enabled = settings != null,
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_settings_btn")
                ) { Text("সেটিংস সংরক্ষণ করুন") }
            }
        }

        item {
            SectionHeader("আবেদন সমূহ (${PrayerCalculator.bnDigits(requests.size)})", "সহায়তা, সদস্যপদ ও যোগাযোগ")
        }
        items(requests, key = { it.id }) { req ->
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${req.name} (${req.kind})", style = MaterialTheme.typography.titleSmall)
                    StatusPill(req.status, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                req.phone?.let { Text("ফোন: $it", style = MaterialTheme.typography.bodySmall) }
                req.details?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (vm.isCurrentUserAdmin() && req.status == "pending") {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.updatePublicRequestStatus(req.id, "approved") }) { Text("অনুমোদন") }
                        FilledTonalButton(onClick = { vm.updatePublicRequestStatus(req.id, "rejected") }) { Text("বাতিল") }
                    }
                }
            }
        }

        val pendingVoices = voices.filter { it.status == "pending" }
        if (pendingVoices.isNotEmpty() && vm.isCurrentUserAdmin()) {
            item { SectionHeader("মতামত অনুমোদন") }
            items(pendingVoices, key = { it.id }) { v ->
                FoundationCard {
                    Text("${v.name}: ${v.body}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { vm.updateVoiceStatus(v.id, "approved") }) { Text("প্রকাশ করুন") }
                        FilledTonalButton(onClick = { vm.updateVoiceStatus(v.id, "rejected") }) { Text("বাতিল") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEventDialog(onDismiss: () -> Unit, onSave: (FoundationEvent) -> Unit) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var daysAhead by remember { mutableStateOf("2") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন ইভেন্ট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("ইভেন্টের নাম *") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("স্থান") })
                OutlinedTextField(value = daysAhead, onValueChange = { daysAhead = it }, label = { Text("কত দিন পর?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("বিবরণ") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    val millis = System.currentTimeMillis() + (daysAhead.toLongOrNull() ?: 1L) * 86400_000L
                    onSave(FoundationEvent(title = title.trim(), type = "meeting", eventDateMillis = millis, location = location.trim(), description = desc.trim()))
                }
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddPollDialog(onDismiss: () -> Unit, onSave: (String, String, List<String>) -> Unit) {
    var q by remember { mutableStateOf("") }
    var optsText by remember { mutableStateOf("হ্যাঁ\nনা") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন গোপন ভোট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("ভোটের প্রশ্ন *") })
                OutlinedTextField(value = optsText, onValueChange = { optsText = it }, label = { Text("অপশনসমূহ (প্রতি লাইনে ১টি)") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { if (q.isNotBlank()) onSave(q.trim(), "", optsText.lines()) }) { Text("প্রকাশ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddNoticeDialog(onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন অফিসিয়াল নোটিশ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("শিরোনাম *") })
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("বিস্তারিত *") }, minLines = 3)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = urgent, onCheckedChange = { urgent = it })
                    Text("জরুরি নোটিশ")
                }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank() && body.isNotBlank()) onSave(title.trim(), body.trim(), urgent) }) { Text("প্রকাশ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddDutyDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("স্বেচ্ছাসেবক দায়িত্ব") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("কাজের নাম *") })
                OutlinedTextField(value = assignee, onValueChange = { assignee = it }, label = { Text("দায়িত্বপ্রাপ্ত সদস্য *") })
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(title.trim(), "", assignee.ifBlank { "স্বেচ্ছাসেবক" }, LocalDate.now().plusDays(2).toString()) }) { Text("যোগ করুন") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddVoiceDialog(defaultName: String, onDismiss: () -> Unit, onSave: (String, String, Int, String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("আপনার মতামত লিখুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("আপনার নাম *") })
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("মতামত *") }, minLines = 3)
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank() && body.isNotBlank()) onSave(name.trim(), "সদস্য/শুভাকাঙ্ক্ষী", 5, body.trim()) }) { Text("জমা দিন") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
