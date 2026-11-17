package com.manobseba.foundation.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.viewmodel.AiTaskState
import com.manobseba.foundation.ui.viewmodel.MainViewModel

@Composable
fun AiHubScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("AI চ্যাট", "হাতে লেখা খাতা", "পুরোনো সদস্য")

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, maxLines = 1) },
                    modifier = Modifier.testTag("ai_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> AiChatTab(vm)
            1 -> HandwrittenLedgerTab(vm, context)
            else -> LegacyImportTab(vm, context)
        }
    }
}

@Composable
private fun AiChatTab(vm: MainViewModel) {
    val context = LocalContext.current
    val sessions by vm.aiSessions.collectAsStateWithLifecycle()
    val activeSessionId by vm.activeAiChatId.collectAsStateWithLifecycle()
    val remoteMessages by vm.aiMessages.collectAsStateWithLifecycle()
    val pendingBubbles by vm.chatPendingBubbles.collectAsStateWithLifecycle()
    val chatStatus by vm.aiChatStatus.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) attachedBitmap = decodeBitmapFromUri(context, uri)
    }

    val quickPrompts = listOf(
        "আব্দুর রহমান থেকে ১,০০০ টাকা সাধারণ অনুদান যোগ করো",
        "আজকের ত্রাণের জন্য ৫,০০০ টাকা খরচ এন্ট্রি করো",
        "আগামী শুক্রবার জরুরি সভার নোটিশ লিখে দাও",
        "আমাদের ফাউন্ডেশনের আর্থিক সারসংক্ষেপ দাও"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilledTonalButton(onClick = { vm.startNewAiSession() }, modifier = Modifier.testTag("ai_new_chat_btn")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন চ্যাট")
                }
            }
            items(sessions, key = { it.id }) { session ->
                FilterChip(
                    selected = session.id == activeSessionId,
                    onClick = { vm.selectAiSession(session.id) },
                    label = { Text(session.title.take(18)) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (remoteMessages.isEmpty() && pendingBubbles.isEmpty()) {
                item {
                    FoundationCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("মানবসেবা AI সহকারী", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "বাংলায় যেকোনো অনুদান, চাঁদা, খরচ, নোটিশ বা হিসাব জানতে লিখুন অথবা ছবি যুক্ত করুন।",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        quickPrompts.forEach { qp ->
                            FilledTonalButton(
                                onClick = { vm.sendAiMessage(qp) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Text(qp, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            items(remoteMessages, key = { it.id }) { m ->
                val isUser = m.sender == "user"
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.88f)
                    ) {
                        Text(
                            text = m.text,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            items(pendingBubbles.filter { b -> remoteMessages.none { it.text == b.text && it.sender == b.sender } }, key = { it.id }) { b ->
                val isUser = b.sender == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.90f)
                    ) {
                        Text(
                            text = b.text,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    b.proposal?.let { prop ->
                        Spacer(Modifier.height(6.dp))
                        FoundationCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                            Text(prop.title.ifBlank { "অ্যাকশন প্রস্তাব" }, style = MaterialTheme.typography.titleSmall)
                            if (prop.amount > 0) {
                                Text("পরিমাণ: ${PrayerCalculator.formatTaka(prop.amount)}", fontWeight = FontWeight.Bold)
                            }
                            if (prop.summary.isNotBlank()) {
                                Text(prop.summary, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            if (b.executed) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("সংরক্ষিত হয়েছে", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Button(
                                    onClick = { vm.confirmAiProposal(b.id, prop) },
                                    modifier = Modifier.testTag("ai_confirm_proposal_btn")
                                ) {
                                    Text("নিশ্চিত করে ডাটাবেসে সংরক্ষণ করুন")
                                }
                            }
                        }
                    }
                }
            }

            if (chatStatus is AiTaskState.Loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("মানবসেবা AI চিন্তা করছে...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (chatStatus is AiTaskState.Error) {
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = (chatStatus as AiTaskState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        attachedBitmap?.let { bmp ->
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(bmp.asImageBitmap(), contentDescription = "সংযুক্ত ছবি", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(8.dp))
                Text("ছবি সংযুক্ত হয়েছে", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { attachedBitmap = null }) { Text("সরান") }
            }
        }

        Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.testTag("ai_attach_image_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "ছবি সংযুক্ত করুন")
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("বাংলায় নির্দেশ বা প্রশ্ন লিখুন...") },
                    maxLines = 3,
                    modifier = Modifier.weight(1f).testTag("ai_chat_input")
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        val msg = input.trim()
                        val bmp = attachedBitmap
                        if (msg.isNotBlank() || bmp != null) {
                            input = ""
                            attachedBitmap = null
                            vm.sendAiMessage(msg, bmp)
                        }
                    },
                    modifier = Modifier.testTag("ai_chat_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "পাঠান", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun HandwrittenLedgerTab(vm: MainViewModel, context: android.content.Context) {
    val scanState by vm.ledgerScanState.collectAsStateWithLifecycle()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) previewBitmap = decodeBitmapFromUri(context, uri)
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) previewBitmap = bmp
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("হাতে লেখা খাতা রিডার (AI Vision)", "খাতার ছবি থেকে স্বয়ংক্রিয়ভাবে আয়-ব্যয় পড়ুন")
        }
        item {
            FoundationCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { cameraCapture.launch(null) },
                        modifier = Modifier.weight(1f).testTag("ledger_camera_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("ক্যামেরা")
                    }
                    FilledTonalButton(
                        onClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f).testTag("ledger_gallery_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("গ্যালারি")
                    }
                }
                previewBitmap?.let { bmp ->
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "খাতার ছবি",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.analyzeLedgerPhoto(bmp) },
                        enabled = scanState !is AiTaskState.Loading,
                        modifier = Modifier.fillMaxWidth().testTag("ledger_analyze_btn")
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (scanState is AiTaskState.Loading) "বিশ্লেষণ চলছে..." else "খাতা স্ক্যান ও বিশ্লেষণ করুন")
                    }
                }
            }
        }

        when (val st = scanState) {
            is AiTaskState.Loading -> item {
                FoundationCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("মানবসেবা AI খাতার লেখা পড়ছে...")
                    }
                }
            }
            is AiTaskState.Error -> item {
                FoundationCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(st.message, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            is AiTaskState.Success -> {
                val res = st.data
                item {
                    FoundationCard {
                        Text(res.summary.ifBlank { "শনাক্তকৃত খাতার এন্ট্রি" }, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        res.items.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${entry.personName} (${entry.entryKind})", fontWeight = FontWeight.SemiBold)
                                    Text("${entry.fundName.ifBlank { "সাধারণ" }} • ${entry.dateStr}", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(PrayerCalculator.formatTaka(entry.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    StatusPill(entry.confidence, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                        if (res.items.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { vm.commitScannedLedgerEntries(res.items) },
                                modifier = Modifier.fillMaxWidth().testTag("ledger_commit_btn")
                            ) {
                                Text("সব ${PrayerCalculator.bnDigits(res.items.size)}টি এন্ট্রি ডাটাবেসে সংরক্ষণ করুন")
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun LegacyImportTab(vm: MainViewModel, context: android.content.Context) {
    val parseState by vm.legacyParseState.collectAsStateWithLifecycle()
    var rawText by remember { mutableStateOf("") }
    var sheetBmp by remember { mutableStateOf<Bitmap?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) sheetBmp = decodeBitmapFromUri(context, uri)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("পুরোনো সদস্য ও জের আমদানি", "খাতা, এক্সেল বা নোট পেস্ট করে সদস্য যুক্ত করুন")
        }
        item {
            FoundationCard {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("সদস্যদের নাম, ফোন ও পূর্বের জমার তালিকা পেস্ট করুন") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("legacy_raw_input")
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (sheetBmp == null) "ছবি যোগ করুন" else "ছবি যুক্ত হয়েছে")
                    }
                    Button(
                        onClick = { vm.parseLegacyData(rawText, sheetBmp) },
                        enabled = (rawText.isNotBlank() || sheetBmp != null) && parseState !is AiTaskState.Loading,
                        modifier = Modifier.weight(1f).testTag("legacy_parse_btn")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("বিশ্লেষণ")
                    }
                }
            }
        }

        when (val st = parseState) {
            is AiTaskState.Loading -> item {
                FoundationCard { Text("পুরোনো সদস্য তথ্য পার্স হচ্ছে...") }
            }
            is AiTaskState.Error -> item {
                FoundationCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(st.message, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            is AiTaskState.Success -> {
                val res = st.data
                item {
                    FoundationCard {
                        Text(res.summary.ifBlank { "শনাক্তকৃত সদস্য তালিকা" }, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        res.members.forEach { m ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(m.fullName, fontWeight = FontWeight.SemiBold)
                                    Text("${m.phone} • ${m.district} • ${m.bloodGroup}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(PrayerCalculator.formatTaka(m.openingBalance), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (res.members.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { vm.commitLegacyMembers(res) },
                                modifier = Modifier.fillMaxWidth().testTag("legacy_commit_btn")
                            ) {
                                Text("সব ${PrayerCalculator.bnDigits(res.members.size)} জন সদস্য সংরক্ষণ করুন")
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

private fun decodeBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}
