package com.manobseba.foundation.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val role: String = "member",
    val memberCode: String? = null,
    val themePalette: String? = "green",
    val themeBg: String? = "plain",
    val darkMode: Boolean? = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName.take(120).ifBlank { "সদস্য" })
        email?.takeIf { it.isNotBlank() }?.let { put("email", it.take(160)) }
        put("role", role)
        memberCode?.takeIf { it.isNotBlank() }?.let { put("memberCode", it.take(40)) }
        themePalette?.takeIf { it.isNotBlank() }?.let { put("themePalette", it.take(30)) }
        themeBg?.takeIf { it.isNotBlank() }?.let { put("themeBg", it.take(30)) }
        darkMode?.let { put("darkMode", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class SalatAmolLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val prayers: List<String> = emptyList(),
    val amolDone: List<String> = emptyList(),
    val tasbihTotal: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "dateKey" to dateKey.take(20),
        "prayers" to prayers.take(5),
        "amolDone" to amolDone.take(30),
        "tasbihTotal" to tasbihTotal.coerceIn(0, 10_000_000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AiChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(160).ifBlank { "নতুন কথোপকথন" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AiChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "user",
    val text: String = "",
    val actionType: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("sender", if (sender == "assistant") "assistant" else "user")
        put("text", text.take(8000).ifBlank { "…" })
        actionType?.takeIf { it.isNotBlank() }?.let { put("actionType", it.take(80)) }
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class FoundationSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val orgName: String = "মানবসেবা ফাউন্ডেশন",
    val slogan: String? = "মানুষের পাশে, মানবতার পাশে",
    val hotline: String? = "01700-000000",
    val bankDetails: String? = "বিকাশ / নগদ: 01700-000000\nব্যাংক অ্যাকাউন্ট: মানবসেবা ফাউন্ডেশন",
    val goalTitle: String? = "তহবিল সংগ্রহের লক্ষ্যমাত্রা",
    val targetAmount: Double = 200000.0,
    val collectedAmount: Double? = 120000.0,
    val monthlyFee: Double? = 200.0,
    val joinCode: String? = "MSF2026",
    val disabledFeatures: List<String>? = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("orgName", orgName.take(120).ifBlank { "মানবসেবা ফাউন্ডেশন" })
        slogan?.let { put("slogan", it.take(200)) }
        hotline?.let { put("hotline", it.take(40)) }
        bankDetails?.let { put("bankDetails", it.take(600)) }
        goalTitle?.let { put("goalTitle", it.take(120)) }
        put("targetAmount", targetAmount.coerceIn(0.0, 1_000_000_000.0))
        collectedAmount?.let { put("collectedAmount", it.coerceAtLeast(0.0)) }
        monthlyFee?.let { put("monthlyFee", it.coerceAtLeast(0.0)) }
        joinCode?.let { put("joinCode", it.take(40)) }
        disabledFeatures?.let { put("disabledFeatures", it.take(30)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Member(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val fullName: String = "",
    val phone: String? = null,
    val district: String? = null,
    val bloodGroup: String? = null,
    val role: String = "member",
    val roleLabel: String? = null,
    val rank: Int? = 100,
    val memberCode: String = "",
    val active: Boolean = true,
    val joinedDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("fullName", fullName.take(120).ifBlank { "সদস্য" })
        phone?.takeIf { it.isNotBlank() }?.let { put("phone", it.take(40)) }
        district?.takeIf { it.isNotBlank() }?.let { put("district", it.take(60)) }
        bloodGroup?.takeIf { it.isNotBlank() }?.let { put("bloodGroup", it.take(10)) }
        put("role", role.take(40).ifBlank { "member" })
        roleLabel?.takeIf { it.isNotBlank() }?.let { put("roleLabel", it.take(60)) }
        rank?.let { put("rank", it.coerceIn(1, 999)) }
        put("memberCode", memberCode.take(40).ifBlank { "MSF-NEW" })
        put("active", active)
        joinedDate?.takeIf { it.isNotBlank() }?.let { put("joinedDate", it.take(30)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Fund(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val name: String = "",
    val code: String? = null,
    val description: String? = null,
    val budget: Double? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("name", name.take(120).ifBlank { "সাধারণ তহবিল" })
        code?.takeIf { it.isNotBlank() }?.let { put("code", it.take(40)) }
        description?.takeIf { it.isNotBlank() }?.let { put("description", it.take(500)) }
        budget?.let { put("budget", it.coerceAtLeast(0.0)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class FoundationTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val kind: String = "donation", // donation | expense | fee
    val amount: Double = 0.0,
    val fundId: String = "f1",
    val memberId: String? = null,
    val donorName: String? = null,
    val method: String? = null,
    val receiptNo: String? = null,
    val voucherNo: String? = null,
    val category: String? = null,
    val description: String? = null,
    val dateStr: String = "",
    val year: Int? = null,
    val month: Int? = null,
    val status: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("kind", if (kind in listOf("donation", "expense", "fee")) kind else "donation")
        put("amount", amount.coerceIn(1.0, 100_000_000.0))
        put("fundId", fundId.take(128).ifBlank { "f1" })
        memberId?.takeIf { it.isNotBlank() }?.let { put("memberId", it.take(128)) }
        donorName?.takeIf { it.isNotBlank() }?.let { put("donorName", it.take(120)) }
        method?.takeIf { it.isNotBlank() }?.let { put("method", it.take(60)) }
        receiptNo?.takeIf { it.isNotBlank() }?.let { put("receiptNo", it.take(60)) }
        voucherNo?.takeIf { it.isNotBlank() }?.let { put("voucherNo", it.take(60)) }
        category?.takeIf { it.isNotBlank() }?.let { put("category", it.take(60)) }
        description?.takeIf { it.isNotBlank() }?.let { put("description", it.take(500)) }
        put("dateStr", dateStr.take(30).ifBlank { "2026-11-16" })
        year?.let { put("year", it.coerceIn(2000, 2100)) }
        month?.let { put("month", it.coerceIn(1, 12)) }
        status?.takeIf { it.isNotBlank() }?.let { put("status", it.take(30)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val category: String = "flood",
    val area: String? = null,
    val description: String? = null,
    val goal: Double = 0.0,
    val fundId: String = "f1",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("title", title.take(200).ifBlank { "নতুন প্রকল্প" })
        put("category", category.take(60).ifBlank { "relief" })
        area?.takeIf { it.isNotBlank() }?.let { put("area", it.take(120)) }
        description?.takeIf { it.isNotBlank() }?.let { put("description", it.take(1000)) }
        put("goal", goal.coerceIn(0.0, 1_000_000_000.0))
        put("fundId", fundId.take(128).ifBlank { "f1" })
        put("status", if (status in listOf("active", "completed", "paused")) status else "active")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class BloodRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val bloodGroup: String = "O+",
    val hospital: String = "",
    val district: String? = null,
    val upazila: String? = null,
    val contact: String = "",
    val patientProblem: String = "",
    val neededByMillis: Long? = null,
    val urgent: Boolean = true,
    val applicantName: String = "",
    val note: String? = null,
    val managed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("bloodGroup", if (bloodGroup in VALID_BLOOD_GROUPS) bloodGroup else "O+")
        put("hospital", hospital.take(160).ifBlank { "হাসপাতাল" })
        district?.takeIf { it.isNotBlank() }?.let { put("district", it.take(60)) }
        upazila?.takeIf { it.isNotBlank() }?.let { put("upazila", it.take(60)) }
        put("contact", contact.take(40).ifBlank { "01700000000" })
        put("patientProblem", patientProblem.take(300).ifBlank { "জরুরি রক্ত প্রয়োজন" })
        neededByMillis?.let { put("neededByMillis", it) }
        put("urgent", urgent)
        put("applicantName", applicantName.take(120).ifBlank { "আবেদনকারী" })
        note?.takeIf { it.isNotBlank() }?.let { put("note", it.take(500)) }
        put("managed", managed)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class BloodDonor(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val fullName: String = "",
    val bloodGroup: String = "O+",
    val district: String = "ঢাকা",
    val upazila: String? = null,
    val phone: String = "",
    val whatsapp: String? = null,
    val available: Boolean = true,
    val totalDonations: Int = 0,
    val lastDonated: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("fullName", fullName.take(120).ifBlank { "রক্তদাতা" })
        put("bloodGroup", if (bloodGroup in VALID_BLOOD_GROUPS) bloodGroup else "O+")
        put("district", district.take(60).ifBlank { "ঢাকা" })
        upazila?.takeIf { it.isNotBlank() }?.let { put("upazila", it.take(60)) }
        put("phone", phone.take(40).ifBlank { "01700000000" })
        whatsapp?.takeIf { it.isNotBlank() }?.let { put("whatsapp", it.take(40)) }
        put("available", available)
        put("totalDonations", totalDonations.coerceIn(0, 500))
        lastDonated?.takeIf { it.isNotBlank() }?.let { put("lastDonated", it.take(30)) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class ReliefLocation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val division: String = "ঢাকা বিভাগ",
    val district: String = "ঢাকা",
    val area: String? = null,
    val disasterType: String = "বন্যা",
    val familiesHelped: Int = 0,
    val amountDistributed: Double = 0.0,
    val distributedDate: String? = null,
    val lat: Double = 23.81,
    val lng: Double = 90.41,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("title", title.take(180).ifBlank { "ত্রাণ ক্যাম্প" })
        put("division", division.take(60).ifBlank { "ঢাকা বিভাগ" })
        put("district", district.take(60).ifBlank { "ঢাকা" })
        area?.takeIf { it.isNotBlank() }?.let { put("area", it.take(120)) }
        put("disasterType", disasterType.take(60).ifBlank { "বন্যা" })
        put("familiesHelped", familiesHelped.coerceAtLeast(0))
        put("amountDistributed", amountDistributed.coerceAtLeast(0.0))
        distributedDate?.takeIf { it.isNotBlank() }?.let { put("distributedDate", it.take(30)) }
        put("lat", lat.coerceIn(20.0, 27.0))
        put("lng", lng.coerceIn(87.5, 93.0))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class FieldReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val category: String = "ত্রাণ",
    val location: String? = null,
    val body: String? = null,
    val spent: Double = 0.0,
    val beneficiaries: Int = 0,
    val status: String = "pending",
    val authorName: String = "",
    val proofUrl: String? = null,
    val videoUrl: String? = null,
    val reactionCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("title", title.take(200).ifBlank { "মাঠ পর্যায়ের রিপোর্ট" })
        put("category", category.take(60).ifBlank { "ত্রাণ" })
        location?.takeIf { it.isNotBlank() }?.let { put("location", it.take(120)) }
        body?.takeIf { it.isNotBlank() }?.let { put("body", it.take(2000)) }
        put("spent", spent.coerceAtLeast(0.0))
        put("beneficiaries", beneficiaries.coerceAtLeast(0))
        put("status", if (status == "verified") "verified" else "pending")
        put("authorName", authorName.take(120).ifBlank { "স্বেচ্ছাসেবক" })
        proofUrl?.takeIf { it.isNotBlank() }?.let { put("proofUrl", it.take(500)) }
        videoUrl?.takeIf { it.isNotBlank() }?.let { put("videoUrl", it.take(500)) }
        put("reactionCount", reactionCount.coerceAtLeast(0))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class FoundationEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val type: String = "meeting",
    val eventDateMillis: Long = 0L,
    val location: String? = null,
    val description: String? = null,
    val attendeeUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("title", title.take(200).ifBlank { "অনুষ্ঠান" })
        put("type", if (type in listOf("urgent", "meeting", "plan", "field", "other")) type else "other")
        put("eventDateMillis", eventDateMillis)
        location?.takeIf { it.isNotBlank() }?.let { put("location", it.take(160)) }
        description?.takeIf { it.isNotBlank() }?.let { put("description", it.take(1000)) }
        put("attendeeUids", attendeeUids.take(100))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val body: String = "",
    val urgent: Boolean = false,
    val likeCount: Int = 0,
    val supportCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "visibility" to "foundation",
        "title" to title.take(200).ifBlank { "নোটিশ" },
        "body" to body.take(2000).ifBlank { "বিজ্ঞপ্তি" },
        "urgent" to urgent,
        "likeCount" to likeCount.coerceAtLeast(0),
        "supportCount" to supportCount.coerceAtLeast(0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Poll(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val question: String = "",
    val description: String? = null,
    val options: List<String> = emptyList(),
    val voteCounts: List<Int> = emptyList(),
    val closed: Boolean = false,
    val closesAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        val safeOpts = options.take(6).ifEmpty { listOf("হ্যাঁ", "না") }
        val safeCounts = List(safeOpts.size) { i -> voteCounts.getOrElse(i) { 0 }.coerceAtLeast(0) }
        put("userId", userId)
        put("visibility", "foundation")
        put("question", question.take(300).ifBlank { "ভোট প্রশ্ন" })
        description?.takeIf { it.isNotBlank() }?.let { put("description", it.take(600)) }
        put("options", safeOpts)
        put("voteCounts", safeCounts)
        put("closed", closed)
        closesAtMillis?.let { put("closesAtMillis", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class PollVote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pollId: String = "",
    val optionIdx: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "pollId" to pollId,
        "optionIdx" to optionIdx.coerceIn(0, 5),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class VoiceTestimonial(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val name: String = "",
    val label: String = "দাতা",
    val rating: Int = 5,
    val body: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "visibility" to "foundation",
        "name" to name.take(120).ifBlank { "অতিথি" },
        "label" to label.take(60).ifBlank { "শুভাকাঙ্ক্ষী" },
        "rating" to rating.coerceIn(1, 5),
        "body" to body.take(500).ifBlank { "উত্তম উদ্যোগ" },
        "status" to if (status in listOf("pending", "approved", "rejected")) status else "pending",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val channel: String = "general",
    val senderName: String = "",
    val senderRole: String? = null,
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("channel", channel.take(80).ifBlank { "general" })
        put("senderName", senderName.take(120).ifBlank { "সদস্য" })
        senderRole?.takeIf { it.isNotBlank() }?.let { put("senderRole", it.take(60)) }
        put("body", body.take(2000).ifBlank { "…" })
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class VolunteerDuty(
    @DocumentId val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val description: String? = null,
    val assignedToName: String = "",
    val dueOn: String? = null,
    val status: String = "todo",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("visibility", "foundation")
        put("title", title.take(200).ifBlank { "দায়িত্ব" })
        description?.takeIf { it.isNotBlank() }?.let { put("description", it.take(1000)) }
        put("assignedToName", assignedToName.take(120).ifBlank { "স্বেচ্ছাসেবক" })
        dueOn?.takeIf { it.isNotBlank() }?.let { put("dueOn", it.take(30)) }
        put("status", if (status in listOf("todo", "doing", "done")) status else "todo")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class PublicRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "aid", // aid | contact | member_apply
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val district: String? = null,
    val bloodGroup: String? = null,
    val aidType: String? = null,
    val amountNeeded: Double? = null,
    val subject: String? = null,
    val details: String? = null,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("kind", if (kind in listOf("aid", "contact", "member_apply")) kind else "aid")
        put("name", name.take(120).ifBlank { "আবেদনকারী" })
        phone?.takeIf { it.isNotBlank() }?.let { put("phone", it.take(40)) }
        email?.takeIf { it.isNotBlank() }?.let { put("email", it.take(160)) }
        district?.takeIf { it.isNotBlank() }?.let { put("district", it.take(60)) }
        bloodGroup?.takeIf { it.isNotBlank() }?.let { put("bloodGroup", it.take(10)) }
        aidType?.takeIf { it.isNotBlank() }?.let { put("aidType", it.take(80)) }
        amountNeeded?.let { put("amountNeeded", it.coerceAtLeast(0.0)) }
        subject?.takeIf { it.isNotBlank() }?.let { put("subject", it.take(160)) }
        details?.takeIf { it.isNotBlank() }?.let { put("details", it.take(2000)) }
        put("status", "pending")
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

val VALID_BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
