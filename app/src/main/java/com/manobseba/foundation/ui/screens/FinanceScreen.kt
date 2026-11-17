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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manobseba.foundation.data.model.FoundationTransaction
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.components.SectionHeader
import com.manobseba.foundation.ui.components.StatusPill
import com.manobseba.foundation.ui.viewmodel.MainViewModel
import com.manobseba.foundation.util.ExportShareHelper
import java.time.LocalDate

@Composable
fun FinanceScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val funds by vm.funds.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var tabIdx by remember { mutableIntStateOf(0) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddFundDialog by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf<FoundationTransaction?>(null) }

    val tabs = listOf("আয়-ব্যয়", "চাঁদা গ্রিড", "তহবিল খাত")

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tabIdx == 2) showAddFundDialog = true else showAddTxDialog = true
                },
                modifier = Modifier.testTag("finance_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন এন্ট্রি")
            }
        }
    ) { innerPad ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPad)) {
            SecondaryTabRow(selectedTabIndex = tabIdx) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = tabIdx == idx,
                        onClick = { tabIdx = idx },
                        text = { Text(label, maxLines = 1) },
                        modifier = Modifier.testTag("finance_tab_$idx")
                    )
                }
            }

            when (tabIdx) {
                0 -> LedgerTab(
                    transactions = transactions,
                    funds = funds,
                    isAdmin = vm.isCurrentUserAdmin(),
                    currentUid = vm.currentUserId(),
                    onDelete = { vm.deleteItem("transactions", it.id, "লেনদেন") },
                    onShareCsv = { ExportShareHelper.shareFinanceCsv(context, transactions, funds) },
                    onOpenReceipt = { selectedReceipt = it }
                )
                1 -> FeeGridTab(
                    members = members.filter { it.active },
                    transactions = transactions,
                    defaultFee = settings?.monthlyFee ?: 200.0,
                    funds = funds,
                    onRecordFee = { memberId, memberName, month, year, amount ->
                        val fId = funds.firstOrNull()?.id ?: "f1"
                        vm.addTransaction(
                            FoundationTransaction(
                                kind = "fee",
                                amount = amount,
                                fundId = fId,
                                memberId = memberId,
                                donorName = memberName,
                                method = "নগদ/বিকাশ",
                                category = "মাসিক চাঁদা",
                                description = "${PrayerCalculator.EN_MONTHS_BN[(month - 1).coerceIn(0, 11)]} ${PrayerCalculator.bnDigits(year)} চাঁদা",
                                dateStr = LocalDate.of(year, month, 1).toString(),
                                year = year,
                                month = month,
                                status = "paid"
                            )
                        )
                    }
                )
                else -> FundsTab(funds = funds, transactions = transactions)
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            funds = funds,
            onDismiss = { showAddTxDialog = false },
            onSave = { tx ->
                vm.addTransaction(tx)
                showAddTxDialog = false
            }
        )
    }

    if (showAddFundDialog) {
        AddFundDialog(
            onDismiss = { showAddFundDialog = false },
            onSave = { name, code, desc, budget ->
                vm.saveFund(name, code, desc, budget)
                showAddFundDialog = false
            }
        )
    }

    selectedReceipt?.let { tx ->
        val fundName = funds.firstOrNull { it.id == tx.fundId }?.name ?: tx.fundId
        AlertDialog(
            onDismissRequest = { selectedReceipt = null },
            title = { Text("ডিজিটাল রসিদ / ভাউচার") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("মানবসেবা ফাউন্ডেশন", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("রসিদ নম্বর: ${tx.receiptNo ?: tx.voucherNo ?: tx.id.take(8)}")
                    Text("তারিখ: ${tx.dateStr}")
                    Text("তহবিল খাত: $fundName")
                    Text("দাতা/সদস্য: ${tx.donorName ?: "সাধারণ"}")
                    Text("পরিমাণ: ${PrayerCalculator.formatTaka(tx.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!tx.description.isNullOrBlank()) Text("বিবরণ: ${tx.description}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val shareBody = "মানবসেবা ফাউন্ডেশন রসিদ\nনম্বর: ${tx.receiptNo ?: tx.id}\nদাতা: ${tx.donorName ?: "সদস্য"}\nখাত: $fundName\nপরিমাণ: ${PrayerCalculator.formatTaka(tx.amount)}\nতারিখ: ${tx.dateStr}"
                    ExportShareHelper.shareText(context, "ডিজিটাল রসিদ", shareBody)
                    selectedReceipt = null
                }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("শেয়ার")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReceipt = null }) { Text("বন্ধ") }
            }
        )
    }
}

@Composable
private fun LedgerTab(
    transactions: List<FoundationTransaction>,
    funds: List<com.manobseba.foundation.data.model.Fund>,
    isAdmin: Boolean,
    currentUid: String,
    onDelete: (FoundationTransaction) -> Unit,
    onShareCsv: () -> Unit,
    onOpenReceipt: (FoundationTransaction) -> Unit
) {
    var kindFilter by remember { mutableStateOf("all") }
    val filtered = remember(transactions, kindFilter) {
        if (kindFilter == "all") transactions else transactions.filter { it.kind == kindFilter }
    }
    val fundMap = remember(funds) { funds.associateBy({ it.id }, { it.name }) }
    val totalIn = transactions.filter { it.kind != "expense" }.sumOf { it.amount }
    val totalOut = transactions.filter { it.kind == "expense" }.sumOf { it.amount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("মোট জমা", style = MaterialTheme.typography.labelMedium)
                        Text(PrayerCalculator.formatTaka(totalIn), style = MaterialTheme.typography.titleLarge, color = Color(0xFF087A43), fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("মোট ব্যয়", style = MaterialTheme.typography.labelMedium)
                        Text(PrayerCalculator.formatTaka(totalOut), style = MaterialTheme.typography.titleLarge, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("স্থিতি", style = MaterialTheme.typography.labelMedium)
                        Text(PrayerCalculator.formatTaka(totalIn - totalOut), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onShareCsv, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("সম্পূর্ণ খাতা CSV হিসেবে এক্সপোর্ট")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("all" to "সব", "donation" to "অনুদান", "fee" to "মাসিক চাঁদা", "expense" to "ব্যয়").forEach { (k, label) ->
                    FilterChip(selected = kindFilter == k, onClick = { kindFilter = k }, label = { Text(label) })
                }
            }
        }

        items(filtered, key = { it.id }) { tx ->
            val isExp = tx.kind == "expense"
            FoundationCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                text = when (tx.kind) {
                                    "donation" -> "অনুদান"
                                    "fee" -> "চাঁদা"
                                    else -> "ব্যয়"
                                },
                                containerColor = if (isExp) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                contentColor = if (isExp) Color(0xFF92400E) else Color(0xFF065F46)
                            )
                            Text(fundMap[tx.fundId] ?: "সাধারণ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(tx.donorName ?: tx.description ?: tx.category ?: "লেনদেন", fontWeight = FontWeight.SemiBold)
                        Text("${tx.dateStr} • ${tx.method ?: "হাতে হাতে"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (isExp) "- " else "+ ") + PrayerCalculator.formatTaka(tx.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isExp) Color(0xFFD97706) else Color(0xFF087A43)
                        )
                        Row {
                            IconButton(onClick = { onOpenReceipt(tx) }) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "রসিদ", modifier = Modifier.size(18.dp))
                            }
                            if (isAdmin || tx.userId == currentUid) {
                                IconButton(onClick = { onDelete(tx) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeGridTab(
    members: List<com.manobseba.foundation.data.model.Member>,
    transactions: List<FoundationTransaction>,
    defaultFee: Double,
    funds: List<com.manobseba.foundation.data.model.Fund>,
    onRecordFee: (String, String, Int, Int, Double) -> Unit
) {
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    val feeTxs = remember(transactions, year) {
        transactions.filter { it.kind == "fee" && (it.year == year || it.dateStr.startsWith("$year-")) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("মাসিক চাঁদা গ্রিড (${PrayerCalculator.bnDigits(year)})", "প্রতি মাসের চাঁদার অবস্থা — ট্যাপ করে পরিশোধ করুন", modifier = Modifier.weight(1f))
                Row {
                    TextButton(onClick = { year-- }) { Text("<") }
                    TextButton(onClick = { year++ }) { Text(">") }
                }
            }
        }

        items(members, key = { it.id }) { m ->
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${m.fullName} (${m.memberCode})", style = MaterialTheme.typography.titleSmall)
                    Text("৳${PrayerCalculator.bnDigits(defaultFee.toInt())}/মাস", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).forEach { mo ->
                        val paid = feeTxs.any {
                            (it.memberId == m.id || it.donorName == m.fullName) &&
                                (it.month == mo || it.dateStr.startsWith("$year-${mo.toString().padStart(2, '0')}"))
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (paid) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(enabled = !paid) {
                                onRecordFee(m.id, m.fullName, mo, year, defaultFee)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(PrayerCalculator.EN_MONTHS_BN[mo - 1].take(3), style = MaterialTheme.typography.labelSmall)
                                if (paid) {
                                    Icon(Icons.Default.Check, contentDescription = "পরিশোধিত", tint = Color(0xFF065F46), modifier = Modifier.size(14.dp))
                                } else {
                                    Text("বাকি", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FundsTab(
    funds: List<com.manobseba.foundation.data.model.Fund>,
    transactions: List<FoundationTransaction>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader("তহবিল ও খাতসমূহ", "প্রতিটি তহবিলের জমা, ব্যয় ও নিট স্থিতি") }
        items(funds, key = { it.id }) { fund ->
            val inc = transactions.filter { it.fundId == fund.id && it.kind != "expense" }.sumOf { it.amount }
            val exp = transactions.filter { it.fundId == fund.id && it.kind == "expense" }.sumOf { it.amount }
            FoundationCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fund.name, style = MaterialTheme.typography.titleMedium)
                    fund.code?.let { StatusPill(it, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                if (!fund.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(fund.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("জমা: ${PrayerCalculator.formatTaka(inc)}", color = Color(0xFF087A43), fontWeight = FontWeight.SemiBold)
                    Text("ব্যয়: ${PrayerCalculator.formatTaka(exp)}", color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                    Text("স্থিতি: ${PrayerCalculator.formatTaka(inc - exp)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    funds: List<com.manobseba.foundation.data.model.Fund>,
    onDismiss: () -> Unit,
    onSave: (FoundationTransaction) -> Unit
) {
    var kind by remember { mutableStateOf("donation") }
    var amountStr by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("bKash") }
    var fundId by remember { mutableStateOf(funds.firstOrNull()?.id ?: "f1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন আর্থিক এন্ট্রি") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("donation" to "অনুদান", "expense" to "ব্যয়", "fee" to "চাঁদা").forEach { (k, label) ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(label) })
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (kind == "expense") "খাত / প্রাপক" else "দাতা / সদস্যের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("মাধ্যম (bKash / নগদ / ব্যাংক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    funds.forEach { f ->
                        FilterChip(selected = fundId == f.id, onClick = { fundId = f.id }, label = { Text(f.name.take(14)) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val today = LocalDate.now()
                        onSave(
                            FoundationTransaction(
                                kind = kind,
                                amount = amt,
                                fundId = fundId,
                                donorName = personName.ifBlank { null },
                                method = method,
                                description = desc.ifBlank { null },
                                dateStr = today.toString(),
                                year = today.year,
                                month = today.monthValue,
                                receiptNo = if (kind != "expense") "MSF-${System.currentTimeMillis() % 100000}" else null,
                                voucherNo = if (kind == "expense") "V-${System.currentTimeMillis() % 100000}" else null,
                                status = if (kind == "fee") "paid" else null
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("tx_save_btn")
            ) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun AddFundDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf("50000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন তহবিল খাত") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("তহবিলের নাম *") }, singleLine = true)
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("সংক্ষিপ্ত কোড") }, singleLine = true)
                OutlinedTextField(value = budgetStr, onValueChange = { budgetStr = it }, label = { Text("বাজেট (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("বিবরণ") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name.trim(), code.trim(), desc.trim(), budgetStr.toDoubleOrNull() ?: 0.0) }) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
