package com.manobseba.foundation.data.remote

import com.manobseba.foundation.data.model.AiChatMessage
import com.manobseba.foundation.data.model.AiChatSession
import com.manobseba.foundation.data.model.BloodDonor
import com.manobseba.foundation.data.model.BloodRequest
import com.manobseba.foundation.data.model.ChatMessage
import com.manobseba.foundation.data.model.FieldReport
import com.manobseba.foundation.data.model.FoundationEvent
import com.manobseba.foundation.data.model.FoundationSettings
import com.manobseba.foundation.data.model.FoundationTransaction
import com.manobseba.foundation.data.model.Fund
import com.manobseba.foundation.data.model.Member
import com.manobseba.foundation.data.model.Notice
import com.manobseba.foundation.data.model.Poll
import com.manobseba.foundation.data.model.Project
import com.manobseba.foundation.data.model.PublicRequest
import com.manobseba.foundation.data.model.ReliefLocation
import com.manobseba.foundation.data.model.SalatAmolLog
import com.manobseba.foundation.data.model.UserProfile
import com.manobseba.foundation.data.model.VoiceTestimonial
import com.manobseba.foundation.data.model.VolunteerDuty
import org.json.JSONArray
import org.json.JSONObject

internal object SupabaseJsonMapper {
    private fun JSONObject.str(vararg keys: String): String {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val v = optString(k, "").trim()
                if (v.isNotEmpty() && v != "null") return v
            }
        }
        return ""
    }

    private fun JSONObject.strOrNull(vararg keys: String): String? = str(*keys).ifBlank { null }

    private fun JSONObject.dbl(default: Double = 0.0, vararg keys: String): Double {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val v = optDouble(k, Double.NaN)
                if (!v.isNaN()) return v
            }
        }
        return default
    }

    private fun JSONObject.dblOrNull(vararg keys: String): Double? {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val v = optDouble(k, Double.NaN)
                if (!v.isNaN()) return v
            }
        }
        return null
    }

    private fun JSONObject.intVal(default: Int = 0, vararg keys: String): Int {
        for (k in keys) {
            if (has(k) && !isNull(k)) return optInt(k, default)
        }
        return default
    }

    private fun JSONObject.longOrNull(vararg keys: String): Long? {
        for (k in keys) {
            if (has(k) && !isNull(k)) return optLong(k)
        }
        return null
    }

    private fun JSONObject.boolVal(default: Boolean = false, vararg keys: String): Boolean {
        for (k in keys) {
            if (has(k) && !isNull(k)) return optBoolean(k, default)
        }
        return default
    }

    private fun JSONObject.strList(vararg keys: String): List<String> {
        for (k in keys) {
            val arr = optJSONArray(k) ?: continue
            return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        }
        return emptyList()
    }

    private fun JSONObject.intList(vararg keys: String): List<Int> {
        for (k in keys) {
            val arr = optJSONArray(k) ?: continue
            return (0 until arr.length()).map { arr.optInt(it, 0) }
        }
        return emptyList()
    }

    fun toUserProfile(o: JSONObject) = UserProfile(
        id = o.str("id"),
        userId = o.str("user_id", "userId", "id"),
        displayName = o.str("display_name", "displayName", "full_name", "name").ifBlank { "সদস্য" },
        email = o.strOrNull("email"),
        role = o.str("role").ifBlank { "member" },
        memberCode = o.strOrNull("member_code", "memberCode"),
        themePalette = o.strOrNull("theme_palette", "themePalette") ?: "green",
        themeBg = o.strOrNull("theme_bg", "themeBg") ?: "plain",
        darkMode = o.boolVal(false, "dark_mode", "darkMode"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toSettings(o: JSONObject) = FoundationSettings(
        id = o.str("id").ifBlank { "main" },
        userId = o.str("user_id", "userId"),
        orgName = o.str("org_name", "orgName", "foundation_name").ifBlank { "মানবসেবা ফাউন্ডেশন" },
        slogan = o.strOrNull("slogan"),
        hotline = o.strOrNull("hotline"),
        bankDetails = o.strOrNull("bank_details", "bankDetails"),
        goalTitle = o.strOrNull("goal_title", "goalTitle"),
        targetAmount = o.dbl(200000.0, "target_amount", "targetAmount", "goal_amount"),
        collectedAmount = o.dblOrNull("collected_amount", "collectedAmount"),
        monthlyFee = o.dblOrNull("monthly_fee", "monthlyFee"),
        joinCode = o.strOrNull("registration_code", "join_code", "joinCode"),
        disabledFeatures = o.strList("disabled_features", "disabledFeatures")
    )

    fun toMember(o: JSONObject) = Member(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        fullName = o.str("full_name", "fullName", "name").ifBlank { "সদস্য" },
        phone = o.strOrNull("phone", "mobile"),
        district = o.strOrNull("district"),
        bloodGroup = o.strOrNull("blood_group", "bloodGroup"),
        role = o.str("role").ifBlank { "member" },
        roleLabel = o.strOrNull("role_label", "roleLabel", "designation"),
        rank = o.intVal(100, "rank"),
        memberCode = o.str("member_code", "memberCode", "code"),
        active = o.boolVal(true, "active", "is_active"),
        joinedDate = o.strOrNull("joined_date", "joinedDate", "created_at")?.take(10)
    )

    fun toFund(o: JSONObject) = Fund(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        name = o.str("name", "title").ifBlank { "সাধারণ তহবিল" },
        code = o.strOrNull("code"),
        description = o.strOrNull("description"),
        budget = o.dblOrNull("budget", "target")
    )

    fun toTransaction(o: JSONObject) = FoundationTransaction(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        kind = o.str("kind", "type", "tx_type").ifBlank { "donation" },
        amount = o.dbl(0.0, "amount", "total"),
        fundId = o.str("fund_id", "fundId").ifBlank { "f1" },
        memberId = o.strOrNull("member_id", "memberId"),
        donorName = o.strOrNull("donor_name", "donorName", "person_name", "name"),
        method = o.strOrNull("method", "payment_method"),
        receiptNo = o.strOrNull("receipt_no", "receiptNo"),
        voucherNo = o.strOrNull("voucher_no", "voucherNo"),
        category = o.strOrNull("category"),
        description = o.strOrNull("description", "note"),
        dateStr = o.str("date_str", "dateStr", "date", "created_at").take(10),
        year = o.intVal(0, "year").takeIf { it > 0 },
        month = o.intVal(0, "month").takeIf { it > 0 },
        status = o.strOrNull("status")
    )

    fun toProject(o: JSONObject) = Project(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title", "name"),
        category = o.str("category").ifBlank { "flood" },
        area = o.strOrNull("area", "location"),
        description = o.strOrNull("description"),
        goal = o.dbl(0.0, "goal", "target_amount", "budget"),
        fundId = o.str("fund_id", "fundId").ifBlank { "f1" },
        status = o.str("status").ifBlank { "active" }
    )

    fun toBloodRequest(o: JSONObject) = BloodRequest(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        bloodGroup = o.str("blood_group", "bloodGroup").ifBlank { "O+" },
        hospital = o.str("hospital", "location"),
        district = o.strOrNull("district"),
        upazila = o.strOrNull("upazila"),
        contact = o.str("contact", "phone"),
        patientProblem = o.str("patient_problem", "patientProblem", "problem", "reason"),
        neededByMillis = o.longOrNull("needed_by_millis", "neededByMillis"),
        urgent = o.boolVal(true, "urgent", "is_urgent"),
        applicantName = o.str("applicant_name", "applicantName", "patient_name"),
        note = o.strOrNull("note"),
        managed = o.boolVal(false, "managed", "is_managed"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toBloodDonor(o: JSONObject) = BloodDonor(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        fullName = o.str("full_name", "fullName", "name"),
        bloodGroup = o.str("blood_group", "bloodGroup").ifBlank { "O+" },
        district = o.str("district").ifBlank { "ঢাকা" },
        upazila = o.strOrNull("upazila"),
        phone = o.str("phone", "contact"),
        whatsapp = o.strOrNull("whatsapp"),
        available = o.boolVal(true, "available", "is_available"),
        totalDonations = o.intVal(0, "total_donations", "totalDonations"),
        lastDonated = o.strOrNull("last_donated", "lastDonated")
    )

    fun toReliefLocation(o: JSONObject) = ReliefLocation(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title", "name"),
        division = o.str("division").ifBlank { "ঢাকা বিভাগ" },
        district = o.str("district").ifBlank { "ঢাকা" },
        area = o.strOrNull("area"),
        disasterType = o.str("disaster_type", "disasterType").ifBlank { "বন্যা" },
        familiesHelped = o.intVal(0, "families_helped", "familiesHelped"),
        amountDistributed = o.dbl(0.0, "amount_distributed", "amountDistributed"),
        distributedDate = o.strOrNull("distributed_date", "distributedDate"),
        lat = o.dbl(23.81, "lat", "latitude"),
        lng = o.dbl(90.41, "lng", "lon", "longitude")
    )

    fun toFieldReport(o: JSONObject) = FieldReport(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title"),
        category = o.str("category").ifBlank { "ত্রাণ" },
        location = o.strOrNull("location", "district"),
        body = o.strOrNull("body", "description"),
        spent = o.dbl(0.0, "spent", "amount"),
        beneficiaries = o.intVal(0, "beneficiaries"),
        status = o.str("status").ifBlank { "pending" },
        authorName = o.str("author_name", "authorName").ifBlank { "স্বেচ্ছাসেবক" },
        proofUrl = o.strOrNull("proof_url", "proofUrl"),
        videoUrl = o.strOrNull("video_url", "videoUrl"),
        reactionCount = o.intVal(0, "reaction_count", "reactionCount"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toEvent(o: JSONObject) = FoundationEvent(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title"),
        type = o.str("type").ifBlank { "meeting" },
        eventDateMillis = o.longOrNull("event_date_millis", "eventDateMillis") ?: 0L,
        location = o.strOrNull("location"),
        description = o.strOrNull("description"),
        attendeeUids = o.strList("attendee_uids", "attendeeUids")
    )

    fun toNotice(o: JSONObject) = Notice(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title"),
        body = o.str("body", "content"),
        urgent = o.boolVal(false, "urgent", "is_urgent"),
        likeCount = o.intVal(0, "like_count", "likeCount"),
        supportCount = o.intVal(0, "support_count", "supportCount"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toPoll(o: JSONObject) = Poll(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        question = o.str("question", "title"),
        description = o.strOrNull("description"),
        options = o.strList("options"),
        voteCounts = o.intList("vote_counts", "voteCounts"),
        closed = o.boolVal(false, "closed", "is_closed"),
        closesAtMillis = o.longOrNull("closes_at_millis", "closesAtMillis"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toVoice(o: JSONObject) = VoiceTestimonial(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        name = o.str("name", "author"),
        label = o.str("label").ifBlank { "শুভাকাঙ্ক্ষী" },
        rating = o.intVal(5, "rating"),
        body = o.str("body", "message"),
        status = o.str("status").ifBlank { "approved" },
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toChatMessage(o: JSONObject) = ChatMessage(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        channel = o.str("channel").ifBlank { "general" },
        senderName = o.str("sender_name", "senderName").ifBlank { "সদস্য" },
        senderRole = o.strOrNull("sender_role", "senderRole"),
        body = o.str("body", "message", "text"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toDuty(o: JSONObject) = VolunteerDuty(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title"),
        description = o.strOrNull("description"),
        assignedToName = o.str("assigned_to_name", "assignedToName", "assignee"),
        dueOn = o.strOrNull("due_on", "dueOn"),
        status = o.str("status").ifBlank { "todo" }
    )

    fun toPublicRequest(o: JSONObject) = PublicRequest(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        kind = o.str("kind", "type").ifBlank { "aid" },
        name = o.str("name"),
        phone = o.strOrNull("phone"),
        email = o.strOrNull("email"),
        district = o.strOrNull("district"),
        bloodGroup = o.strOrNull("blood_group", "bloodGroup"),
        aidType = o.strOrNull("aid_type", "aidType"),
        amountNeeded = o.dblOrNull("amount_needed", "amountNeeded"),
        subject = o.strOrNull("subject"),
        details = o.strOrNull("details", "message"),
        status = o.str("status").ifBlank { "pending" },
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toSalatAmolLog(o: JSONObject) = SalatAmolLog(
        id = o.str("id").ifBlank { o.str("date_key", "dateKey") },
        userId = o.str("user_id", "userId"),
        dateKey = o.str("date_key", "dateKey"),
        prayers = o.strList("prayers"),
        amolDone = o.strList("amol_done", "amolDone"),
        tasbihTotal = o.intVal(0, "tasbih_total", "tasbihTotal")
    )

    fun toAiSession(o: JSONObject) = AiChatSession(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        title = o.str("title").ifBlank { "নতুন কথোপকথন" },
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    fun toAiMessage(o: JSONObject) = AiChatMessage(
        id = o.str("id"),
        userId = o.str("user_id", "userId"),
        sender = o.str("sender").ifBlank { "user" },
        text = o.str("text", "content"),
        actionType = o.strOrNull("action_type", "actionType"),
        createdAtIso = o.strOrNull("created_at", "createdAt")
    )

    inline fun <T> parseArray(raw: String, mapper: (JSONObject) -> T): List<T> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(mapper)
        }
    }
}
