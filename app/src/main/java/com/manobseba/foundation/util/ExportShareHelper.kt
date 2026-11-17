package com.manobseba.foundation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.manobseba.foundation.data.model.FoundationTransaction
import com.manobseba.foundation.data.model.Fund
import com.manobseba.foundation.data.model.Member
import com.manobseba.foundation.domain.PrayerCalculator

object ExportShareHelper {
    fun dialPhone(context: Context, phone: String) {
        val cleaned = phone.trim().replace(" ", "")
        if (cleaned.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
        runCatching { context.startActivity(intent) }
    }

    fun openWhatsApp(context: Context, phone: String, message: String = "") {
        val digits = phone.filter { it.isDigit() }
        val intl = when {
            digits.startsWith("880") -> digits
            digits.startsWith("0") -> "88$digits"
            else -> digits
        }
        val uri = Uri.parse("https://wa.me/$intl?text=${Uri.encode(message)}")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    fun openUrl(context: Context, url: String) {
        if (!url.startsWith("http")) return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    fun copyText(context: Context, text: String, label: String = "মানবসেবা") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun shareFinanceCsv(
        context: Context,
        transactions: List<FoundationTransaction>,
        funds: List<Fund>
    ) {
        val fundMap = funds.associateBy({ it.id }, { it.name })
        val sb = StringBuilder()
        sb.appendLine("তারিখ,ধরন,তহবিল,দাতা/সদস্য,পরিমাণ(টাকা),মাধ্যম,রসিদ/ভাউচার,বিবরণ")
        transactions.forEach { tx ->
            val kindBn = when (tx.kind) {
                "donation" -> "অনুদান"
                "fee" -> "মাসিক চাঁদা"
                else -> "ব্যয়"
            }
            val fundName = fundMap[tx.fundId] ?: tx.fundId
            val ref = tx.receiptNo ?: tx.voucherNo ?: ""
            val desc = (tx.description ?: tx.category ?: "").replace(",", " ")
            val person = (tx.donorName ?: "").replace(",", " ")
            sb.appendLine("${tx.dateStr},$kindBn,$fundName,$person,${tx.amount.toLong()},${tx.method ?: ""},$ref,$desc")
        }
        shareText(context, "মানবসেবা ফাউন্ডেশন — আর্থিক প্রতিবেদন (CSV)", sb.toString())
    }

    fun shareMemberIdCard(context: Context, member: Member, roleLabel: String) {
        val cardText = buildString {
            appendLine("════════════════════")
            appendLine("মানবসেবা ফাউন্ডেশন — ডিজিটাল আইডি")
            appendLine("মানুষের পাশে, মানবতার পাশে")
            appendLine("────────────────────")
            appendLine("নাম: ${member.fullName}")
            appendLine("পদবী: $roleLabel")
            appendLine("সদস্য কোড: ${member.memberCode}")
            if (!member.district.isNullOrBlank()) appendLine("জেলা: ${member.district}")
            if (!member.bloodGroup.isNullOrBlank()) appendLine("রক্তের গ্রুপ: ${member.bloodGroup}")
            if (!member.phone.isNullOrBlank()) appendLine("ফোন: ${member.phone}")
            appendLine("স্ট্যাটাস: ${if (member.active) "সক্রিয় সদস্য" else "নিষ্ক্রিয়"}")
            appendLine("তৈরির তারিখ: ${PrayerCalculator.formatGregorianBn(java.time.LocalDate.now())}")
            appendLine("════════════════════")
        }
        shareText(context, "সদস্য ডিজিটাল আইডি কার্ড", cardText)
    }
}
