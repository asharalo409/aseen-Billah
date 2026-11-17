package com.manobseba.foundation.data.model

import androidx.annotation.Keep

@Keep
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val role: String = "member",
    val memberCode: String? = null,
    val themePalette: String? = "green",
    val themeBg: String? = "plain",
    val darkMode: Boolean? = false,
    val createdAtIso: String? = null
)

@Keep
data class SalatAmolLog(
    val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val prayers: List<String> = emptyList(),
    val amolDone: List<String> = emptyList(),
    val tasbihTotal: Int = 0
)

@Keep
data class AiChatSession(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val createdAtIso: String? = null
)

@Keep
data class AiChatMessage(
    val id: String = "",
    val userId: String = "",
    val sender: String = "user",
    val text: String = "",
    val actionType: String? = null,
    val createdAtIso: String? = null
)

@Keep
data class FoundationSettings(
    val id: String = "",
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
    val disabledFeatures: List<String>? = emptyList()
)

@Keep
data class Member(
    val id: String = "",
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
    val joinedDate: String? = null
)

@Keep
data class Fund(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val name: String = "",
    val code: String? = null,
    val description: String? = null,
    val budget: Double? = null
)

@Keep
data class FoundationTransaction(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val kind: String = "donation",
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
    val status: String? = null
)

@Keep
data class Project(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val category: String = "flood",
    val area: String? = null,
    val description: String? = null,
    val goal: Double = 0.0,
    val fundId: String = "f1",
    val status: String = "active"
)

@Keep
data class BloodRequest(
    val id: String = "",
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
    val createdAtIso: String? = null
)

@Keep
data class BloodDonor(
    val id: String = "",
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
    val lastDonated: String? = null
)

@Keep
data class ReliefLocation(
    val id: String = "",
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
    val lng: Double = 90.41
)

@Keep
data class FieldReport(
    val id: String = "",
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
    val createdAtIso: String? = null
)

@Keep
data class FoundationEvent(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val type: String = "meeting",
    val eventDateMillis: Long = 0L,
    val location: String? = null,
    val description: String? = null,
    val attendeeUids: List<String> = emptyList()
)

@Keep
data class Notice(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val body: String = "",
    val urgent: Boolean = false,
    val likeCount: Int = 0,
    val supportCount: Int = 0,
    val createdAtIso: String? = null
)

@Keep
data class Poll(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val question: String = "",
    val description: String? = null,
    val options: List<String> = emptyList(),
    val voteCounts: List<Int> = emptyList(),
    val closed: Boolean = false,
    val closesAtMillis: Long? = null,
    val createdAtIso: String? = null
)

@Keep
data class PollVote(
    val id: String = "",
    val userId: String = "",
    val pollId: String = "",
    val optionIdx: Int = 0
)

@Keep
data class VoiceTestimonial(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val name: String = "",
    val label: String = "দাতা",
    val rating: Int = 5,
    val body: String = "",
    val status: String = "pending",
    val createdAtIso: String? = null
)

@Keep
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val channel: String = "general",
    val senderName: String = "",
    val senderRole: String? = null,
    val body: String = "",
    val createdAtIso: String? = null
)

@Keep
data class VolunteerDuty(
    val id: String = "",
    val userId: String = "",
    val visibility: String = "foundation",
    val title: String = "",
    val description: String? = null,
    val assignedToName: String = "",
    val dueOn: String? = null,
    val status: String = "todo"
)

@Keep
data class PublicRequest(
    val id: String = "",
    val userId: String = "",
    val kind: String = "aid",
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
    val createdAtIso: String? = null
)

val VALID_BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
