package com.manobseba.foundation.data.remote

import android.content.Context
import android.util.Log
import com.manobseba.foundation.auth.AppUser
import com.manobseba.foundation.auth.SupabaseAuthHelper
import com.manobseba.foundation.data.SeedCatalog
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "SupabaseRepo"
private const val PREF_LOCAL_STORE = "manobseba_user_local_sync"

class FoundationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val localPrefs = appContext.getSharedPreferences(PREF_LOCAL_STORE, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _settings = MutableStateFlow<FoundationSettings?>(null)
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    private val _funds = MutableStateFlow<List<Fund>>(emptyList())
    private val _transactions = MutableStateFlow<List<FoundationTransaction>>(emptyList())
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    private val _bloodRequests = MutableStateFlow<List<BloodRequest>>(emptyList())
    private val _bloodDonors = MutableStateFlow<List<BloodDonor>>(emptyList())
    private val _reliefLocations = MutableStateFlow<List<ReliefLocation>>(emptyList())
    private val _fieldReports = MutableStateFlow<List<FieldReport>>(emptyList())
    private val _events = MutableStateFlow<List<FoundationEvent>>(emptyList())
    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    private val _polls = MutableStateFlow<List<Poll>>(emptyList())
    private val _voices = MutableStateFlow<List<VoiceTestimonial>>(emptyList())
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _duties = MutableStateFlow<List<VolunteerDuty>>(emptyList())
    private val _publicRequests = MutableStateFlow<List<PublicRequest>>(emptyList())
    private val _salatLogs = MutableStateFlow<List<SalatAmolLog>>(emptyList())
    private val _aiSessions = MutableStateFlow<List<AiChatSession>>(emptyList())
    private val _aiMessagesByChat = MutableStateFlow<Map<String, List<AiChatMessage>>>(emptyMap())

    fun observeSettings(): Flow<FoundationSettings?> = _settings
    fun observeMembers(): Flow<List<Member>> = _members.map { list ->
        list.sortedWith(compareBy({ it.rank ?: 100 }, { it.memberCode }))
    }
    fun observeFunds(): Flow<List<Fund>> = _funds
    fun observeTransactions(): Flow<List<FoundationTransaction>> = _transactions.map { list ->
        list.sortedByDescending { it.dateStr }
    }
    fun observeProjects(): Flow<List<Project>> = _projects
    fun observeBloodRequests(): Flow<List<BloodRequest>> = _bloodRequests.map { list ->
        list.sortedByDescending { it.createdAtIso.orEmpty() }
    }
    fun observeBloodDonors(): Flow<List<BloodDonor>> = _bloodDonors
    fun observeReliefLocations(): Flow<List<ReliefLocation>> = _reliefLocations
    fun observeFieldReports(): Flow<List<FieldReport>> = _fieldReports.map { list ->
        list.sortedByDescending { it.createdAtIso.orEmpty() }
    }
    fun observeEvents(): Flow<List<FoundationEvent>> = _events.map { list ->
        list.sortedBy { it.eventDateMillis }
    }
    fun observeNotices(): Flow<List<Notice>> = _notices.map { list ->
        list.sortedByDescending { it.createdAtIso.orEmpty() }
    }
    fun observePolls(): Flow<List<Poll>> = _polls.map { list ->
        list.sortedByDescending { it.createdAtIso.orEmpty() }
    }
    fun observeVoices(): Flow<List<VoiceTestimonial>> = _voices.map { list ->
        list.sortedByDescending { it.createdAtIso.orEmpty() }
    }
    fun observeChatMessages(): Flow<List<ChatMessage>> = _chatMessages
    fun observeDuties(): Flow<List<VolunteerDuty>> = _duties
    fun observePublicRequests(user: AppUser?): Flow<List<PublicRequest>> = _publicRequests
    fun observeUserProfile(uid: String?): Flow<UserProfile?> = _userProfile
    fun observeSalatAmolLogs(uid: String?): Flow<List<SalatAmolLog>> = _salatLogs
    fun observeAiSessions(uid: String?): Flow<List<AiChatSession>> = _aiSessions
    fun observeAiMessages(uid: String?, chatId: String?): Flow<List<AiChatMessage>> =
        _aiMessagesByChat.map { map -> if (chatId.isNullOrBlank()) emptyList() else map[chatId].orEmpty() }

    fun clearAll() {
        _userProfile.value = null
        _settings.value = null
        _members.value = emptyList()
        _funds.value = emptyList()
        _transactions.value = emptyList()
        _projects.value = emptyList()
        _bloodRequests.value = emptyList()
        _bloodDonors.value = emptyList()
        _reliefLocations.value = emptyList()
        _fieldReports.value = emptyList()
        _events.value = emptyList()
        _notices.value = emptyList()
        _polls.value = emptyList()
        _voices.value = emptyList()
        _chatMessages.value = emptyList()
        _duties.value = emptyList()
        _publicRequests.value = emptyList()
        _salatLogs.value = emptyList()
        _aiSessions.value = emptyList()
        _aiMessagesByChat.value = emptyMap()
    }

    suspend fun syncAll(user: AppUser): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase কনফিগার করা হয়নি"))
        }
        val uid = user.uid
        val localThemePalette = localPrefs.getString("theme_palette_$uid", "green") ?: "green"
        val localThemeBg = localPrefs.getString("theme_bg_$uid", "plain") ?: "plain"
        val localDarkMode = localPrefs.getBoolean("theme_dark_$uid", false)

        val remoteProfiles = fetchTableFirst(listOf("profiles", "users"), "id=eq.$uid") {
            SupabaseJsonMapper.toUserProfile(it)
        }
        val remoteProfile = remoteProfiles.firstOrNull()
        _userProfile.value = remoteProfile?.copy(
            themePalette = remoteProfile.themePalette ?: localThemePalette,
            themeBg = remoteProfile.themeBg ?: localThemeBg,
            darkMode = remoteProfile.darkMode ?: localDarkMode
        ) ?: UserProfile(
            id = uid,
            userId = uid,
            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "সদস্য",
            email = user.email,
            role = user.role,
            themePalette = localThemePalette,
            themeBg = localThemeBg,
            darkMode = localDarkMode
        )

        _settings.value = fetchTableFirst(listOf("settings", "foundation_settings")) {
            SupabaseJsonMapper.toSettings(it)
        }.firstOrNull() ?: SeedCatalog.defaultSettings(uid)

        _members.value = fetchTableFirst(listOf("members")) { SupabaseJsonMapper.toMember(it) }
        _funds.value = fetchTableFirst(listOf("funds", "fund_groups")) { SupabaseJsonMapper.toFund(it) }
        _transactions.value = fetchTableFirst(listOf("transactions", "ledger")) { SupabaseJsonMapper.toTransaction(it) }
        _projects.value = fetchTableFirst(listOf("projects")) { SupabaseJsonMapper.toProject(it) }
        _bloodRequests.value = fetchTableFirst(listOf("blood_requests")) { SupabaseJsonMapper.toBloodRequest(it) }
        _bloodDonors.value = fetchTableFirst(listOf("blood_donors")) { SupabaseJsonMapper.toBloodDonor(it) }
        _reliefLocations.value = fetchTableFirst(listOf("relief_locations", "relief_map")) { SupabaseJsonMapper.toReliefLocation(it) }
        _fieldReports.value = fetchTableFirst(listOf("field_reports", "works")) { SupabaseJsonMapper.toFieldReport(it) }
        _events.value = fetchTableFirst(listOf("events")) { SupabaseJsonMapper.toEvent(it) }
        _notices.value = fetchTableFirst(listOf("notices", "board_notices")) { SupabaseJsonMapper.toNotice(it) }
        _polls.value = fetchTableFirst(listOf("polls")) { SupabaseJsonMapper.toPoll(it) }
        _voices.value = fetchTableFirst(listOf("voices")) { SupabaseJsonMapper.toVoice(it) }
        _chatMessages.value = fetchTableFirst(listOf("chat_messages")) { SupabaseJsonMapper.toChatMessage(it) }
        _duties.value = fetchTableFirst(listOf("duties", "volunteers")) { SupabaseJsonMapper.toDuty(it) }
        _publicRequests.value = fetchTableFirst(listOf("public_requests")) { SupabaseJsonMapper.toPublicRequest(it) }

        val remoteSalat = fetchTableFirst(listOf("salat_amol"), "user_id=eq.$uid") {
            SupabaseJsonMapper.toSalatAmolLog(it)
        }
        _salatLogs.value = if (remoteSalat.isNotEmpty()) remoteSalat else loadLocalSalatLogs(uid)

        val remoteSessions = fetchTableFirst(listOf("ai_chats"), "user_id=eq.$uid") {
            SupabaseJsonMapper.toAiSession(it)
        }
        _aiSessions.value = if (remoteSessions.isNotEmpty()) remoteSessions else loadLocalAiSessions(uid)
        Result.success(Unit)
    }

    suspend fun updateUserTheme(uid: String, palette: String, bg: String, darkMode: Boolean): Result<Unit> = runCatchingSafe {
        localPrefs.edit()
            .putString("theme_palette_$uid", palette)
            .putString("theme_bg_$uid", bg)
            .putBoolean("theme_dark_$uid", darkMode)
            .apply()
        _userProfile.value = (_userProfile.value ?: UserProfile(id = uid, userId = uid)).copy(
            themePalette = palette,
            themeBg = bg,
            darkMode = darkMode
        )
        runCatching {
            patchRow("profiles", "id=eq.$uid", JSONObject().apply {
                put("theme_palette", palette)
                put("theme_bg", bg)
                put("dark_mode", darkMode)
            })
        }
    }

    suspend fun updateUserProfileInfo(uid: String, displayName: String, memberCode: String?): Result<Unit> = runCatchingSafe {
        val cleanName = displayName.trim().take(120).ifBlank { "সদস্য" }
        _userProfile.value = (_userProfile.value ?: UserProfile(id = uid, userId = uid)).copy(
            displayName = cleanName,
            memberCode = memberCode?.trim()?.ifBlank { null }
        )
        val payload = JSONObject().apply {
            put("full_name", cleanName)
            put("display_name", cleanName)
            if (!memberCode.isNullOrBlank()) put("member_code", memberCode.trim().take(40))
        }
        patchRow("profiles", "id=eq.$uid", payload)
    }

    suspend fun saveSalatAmol(uid: String, dayKey: String, prayers: List<String>, amolDone: List<String>, tasbihTotal: Int): Result<Unit> = runCatchingSafe {
        val updated = SalatAmolLog(
            id = dayKey,
            userId = uid,
            dateKey = dayKey,
            prayers = prayers.take(5),
            amolDone = amolDone.take(30),
            tasbihTotal = tasbihTotal.coerceIn(0, 10_000_000)
        )
        val nextList = (_salatLogs.value.filterNot { it.dateKey == dayKey } + updated).sortedBy { it.dateKey }
        _salatLogs.value = nextList
        saveLocalSalatLogs(uid, nextList)
    }

    suspend fun createAiSession(uid: String, title: String): Result<String> = runCatching {
        val id = "ai_${System.currentTimeMillis()}"
        val session = AiChatSession(id = id, userId = uid, title = title.take(120).ifBlank { "নতুন কথোপকথন" })
        val next = listOf(session) + _aiSessions.value
        _aiSessions.value = next
        saveLocalAiSessions(uid, next)
        id
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun addAiMessage(uid: String, chatId: String, sender: String, text: String, actionType: String? = null): Result<Unit> = runCatchingSafe {
        val msg = AiChatMessage(
            id = "m_${System.currentTimeMillis()}",
            userId = uid,
            sender = sender,
            text = text,
            actionType = actionType
        )
        val cur = _aiMessagesByChat.value[chatId].orEmpty()
        _aiMessagesByChat.value = _aiMessagesByChat.value + (chatId to (cur + msg))
    }

    suspend fun saveTransaction(tx: FoundationTransaction): Result<Unit> = runCatchingSafe {
        require(tx.amount > 0.0) { "টাকার পরিমাণ শূন্যের বেশি হতে হবে" }
        val payload = JSONObject().apply {
            put("user_id", tx.userId)
            put("kind", tx.kind)
            put("amount", tx.amount)
            put("fund_id", tx.fundId)
            tx.memberId?.let { put("member_id", it) }
            tx.donorName?.let { put("donor_name", it) }
            tx.method?.let { put("method", it) }
            tx.receiptNo?.let { put("receipt_no", it) }
            tx.voucherNo?.let { put("voucher_no", it) }
            tx.category?.let { put("category", it) }
            tx.description?.let { put("description", it) }
            put("date_str", tx.dateStr)
            tx.year?.let { put("year", it) }
            tx.month?.let { put("month", it) }
            tx.status?.let { put("status", it) }
        }
        val inserted = insertRow("transactions", payload)
        _transactions.value = listOf(SupabaseJsonMapper.toTransaction(inserted)) + _transactions.value
    }

    suspend fun deleteDocument(collection: String, docId: String): Result<Unit> = runCatchingSafe {
        deleteRow(collection, docId)
        when (collection) {
            "transactions" -> _transactions.value = _transactions.value.filterNot { it.id == docId }
            "members" -> _members.value = _members.value.filterNot { it.id == docId }
            "projects" -> _projects.value = _projects.value.filterNot { it.id == docId }
            "notices" -> _notices.value = _notices.value.filterNot { it.id == docId }
            "blood_requests" -> _bloodRequests.value = _bloodRequests.value.filterNot { it.id == docId }
        }
    }

    suspend fun saveMember(member: Member, isEdit: Boolean): Result<Unit> = runCatchingSafe {
        require(member.fullName.isNotBlank()) { "সদস্যের নাম লিখুন" }
        val payload = JSONObject().apply {
            put("full_name", member.fullName.take(120))
            member.phone?.let { put("phone", it) }
            member.district?.let { put("district", it) }
            member.bloodGroup?.let { put("blood_group", it) }
            put("role", member.role)
            member.roleLabel?.let { put("role_label", it) }
            member.rank?.let { put("rank", it) }
            put("member_code", member.memberCode)
            put("active", member.active)
            member.joinedDate?.let { put("joined_date", it) }
        }
        if (isEdit && member.id.isNotBlank()) {
            patchRow("members", "id=eq.${member.id}", payload)
            _members.value = _members.value.map { if (it.id == member.id) member else it }
        } else {
            val inserted = insertRow("members", payload)
            _members.value = _members.value + SupabaseJsonMapper.toMember(inserted)
        }
    }

    suspend fun saveFund(fund: Fund): Result<Unit> = runCatchingSafe {
        require(fund.name.isNotBlank()) { "খাতের নাম লিখুন" }
        val payload = JSONObject().apply {
            put("name", fund.name.take(120))
            fund.code?.let { put("code", it) }
            fund.description?.let { put("description", it) }
            fund.budget?.let { put("budget", it) }
        }
        val inserted = insertRow("funds", payload)
        _funds.value = _funds.value + SupabaseJsonMapper.toFund(inserted)
    }

    suspend fun saveProject(project: Project): Result<Unit> = runCatchingSafe {
        require(project.title.isNotBlank()) { "প্রকল্পের শিরোনাম লিখুন" }
        val payload = JSONObject().apply {
            put("title", project.title.take(200))
            put("category", project.category)
            project.area?.let { put("area", it) }
            project.description?.let { put("description", it) }
            put("goal", project.goal)
            put("fund_id", project.fundId)
            put("status", project.status)
        }
        val inserted = insertRow("projects", payload)
        _projects.value = listOf(SupabaseJsonMapper.toProject(inserted)) + _projects.value
    }

    suspend fun saveBloodRequest(req: BloodRequest): Result<Unit> = runCatchingSafe {
        require(req.hospital.isNotBlank() && req.contact.isNotBlank()) { "হাসপাতাল ও ফোন নম্বর লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", req.userId)
            put("blood_group", req.bloodGroup)
            put("hospital", req.hospital)
            req.district?.let { put("district", it) }
            req.upazila?.let { put("upazila", it) }
            put("contact", req.contact)
            put("patient_problem", req.patientProblem)
            req.neededByMillis?.let { put("needed_by_millis", it) }
            put("urgent", req.urgent)
            put("applicant_name", req.applicantName)
            req.note?.let { put("note", it) }
            put("managed", req.managed)
        }
        val inserted = insertRow("blood_requests", payload)
        _bloodRequests.value = listOf(SupabaseJsonMapper.toBloodRequest(inserted)) + _bloodRequests.value
    }

    suspend fun markBloodRequestManaged(requestId: String): Result<Unit> = runCatchingSafe {
        patchRow("blood_requests", "id=eq.$requestId", JSONObject().put("managed", true))
        _bloodRequests.value = _bloodRequests.value.map { if (it.id == requestId) it.copy(managed = true) else it }
    }

    suspend fun saveBloodDonor(donor: BloodDonor): Result<Unit> = runCatchingSafe {
        require(donor.fullName.isNotBlank() && donor.phone.isNotBlank()) { "রক্তদাতার নাম ও ফোন লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", donor.userId)
            put("full_name", donor.fullName)
            put("blood_group", donor.bloodGroup)
            put("district", donor.district)
            donor.upazila?.let { put("upazila", it) }
            put("phone", donor.phone)
            donor.whatsapp?.let { put("whatsapp", it) }
            put("available", donor.available)
            put("total_donations", donor.totalDonations)
            donor.lastDonated?.let { put("last_donated", it) }
        }
        val inserted = insertRow("blood_donors", payload)
        _bloodDonors.value = listOf(SupabaseJsonMapper.toBloodDonor(inserted)) + _bloodDonors.value
    }

    suspend fun saveReliefLocation(loc: ReliefLocation): Result<Unit> = runCatchingSafe {
        require(loc.title.isNotBlank()) { "ক্যাম্পের নাম লিখুন" }
        val payload = JSONObject().apply {
            put("title", loc.title)
            put("division", loc.division)
            put("district", loc.district)
            loc.area?.let { put("area", it) }
            put("disaster_type", loc.disasterType)
            put("families_helped", loc.familiesHelped)
            put("amount_distributed", loc.amountDistributed)
            loc.distributedDate?.let { put("distributed_date", it) }
            put("lat", loc.lat)
            put("lng", loc.lng)
        }
        val inserted = insertRow("relief_locations", payload)
        _reliefLocations.value = listOf(SupabaseJsonMapper.toReliefLocation(inserted)) + _reliefLocations.value
    }

    suspend fun saveFieldReport(report: FieldReport): Result<Unit> = runCatchingSafe {
        require(report.title.isNotBlank()) { "রিপোর্টের শিরোনাম লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", report.userId)
            put("title", report.title)
            put("category", report.category)
            report.location?.let { put("location", it) }
            report.body?.let { put("body", it) }
            put("spent", report.spent)
            put("beneficiaries", report.beneficiaries)
            put("status", report.status)
            put("author_name", report.authorName)
            put("reaction_count", report.reactionCount)
        }
        val inserted = insertRow("field_reports", payload)
        _fieldReports.value = listOf(SupabaseJsonMapper.toFieldReport(inserted)) + _fieldReports.value
    }

    suspend fun reactFieldReport(reportId: String, nextCount: Int): Result<Unit> = runCatchingSafe {
        patchRow("field_reports", "id=eq.$reportId", JSONObject().put("reaction_count", nextCount))
        _fieldReports.value = _fieldReports.value.map { if (it.id == reportId) it.copy(reactionCount = nextCount) else it }
    }

    suspend fun saveEvent(event: FoundationEvent): Result<Unit> = runCatchingSafe {
        require(event.title.isNotBlank()) { "ইভেন্টের নাম লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", event.userId)
            put("title", event.title)
            put("type", event.type)
            put("event_date_millis", event.eventDateMillis)
            event.location?.let { put("location", it) }
            event.description?.let { put("description", it) }
            put("attendee_uids", JSONArray(event.attendeeUids))
        }
        val inserted = insertRow("events", payload)
        _events.value = _events.value + SupabaseJsonMapper.toEvent(inserted)
    }

    suspend fun toggleEventRsvp(event: FoundationEvent, uid: String): Result<Unit> = runCatchingSafe {
        val nextList = if (uid in event.attendeeUids) event.attendeeUids - uid else (event.attendeeUids + uid).take(100)
        patchRow("events", "id=eq.${event.id}", JSONObject().put("attendee_uids", JSONArray(nextList)))
        _events.value = _events.value.map { if (it.id == event.id) it.copy(attendeeUids = nextList) else it }
    }

    suspend fun saveNotice(notice: Notice): Result<Unit> = runCatchingSafe {
        require(notice.title.isNotBlank() && notice.body.isNotBlank()) { "শিরোনাম ও বিবরণ লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", notice.userId)
            put("title", notice.title)
            put("body", notice.body)
            put("urgent", notice.urgent)
            put("like_count", notice.likeCount)
            put("support_count", notice.supportCount)
        }
        val inserted = insertRow("notices", payload)
        _notices.value = listOf(SupabaseJsonMapper.toNotice(inserted)) + _notices.value
    }

    suspend fun reactNotice(notice: Notice, isLike: Boolean): Result<Unit> = runCatchingSafe {
        val nextLike = if (isLike) notice.likeCount + 1 else notice.likeCount
        val nextSup = if (!isLike) notice.supportCount + 1 else notice.supportCount
        val payload = JSONObject().apply {
            if (isLike) put("like_count", nextLike) else put("support_count", nextSup)
        }
        patchRow("notices", "id=eq.${notice.id}", payload)
        _notices.value = _notices.value.map {
            if (it.id == notice.id) it.copy(likeCount = nextLike, supportCount = nextSup) else it
        }
    }

    suspend fun createPoll(poll: Poll): Result<Unit> = runCatchingSafe {
        require(poll.question.isNotBlank() && poll.options.size >= 2) { "প্রশ্ন ও কমপক্ষে ২টি অপশন দিন" }
        val payload = JSONObject().apply {
            put("user_id", poll.userId)
            put("question", poll.question)
            poll.description?.let { put("description", it) }
            put("options", JSONArray(poll.options))
            put("vote_counts", JSONArray(poll.voteCounts))
            put("closed", poll.closed)
        }
        val inserted = insertRow("polls", payload)
        _polls.value = listOf(SupabaseJsonMapper.toPoll(inserted)) + _polls.value
    }

    suspend fun getMyVote(pollId: String, uid: String): Int? = withContext(Dispatchers.IO) {
        val localKey = "poll_vote_${pollId}_$uid"
        if (localPrefs.contains(localKey)) localPrefs.getInt(localKey, -1).takeIf { it >= 0 } else null
    }

    suspend fun castPollVote(poll: Poll, uid: String, optionIdx: Int): Result<Unit> = runCatchingSafe {
        require(!poll.closed) { "এই ভোটটি বন্ধ হয়ে গেছে" }
        val localKey = "poll_vote_${poll.id}_$uid"
        val prevIdx = if (localPrefs.contains(localKey)) localPrefs.getInt(localKey, -1) else -1
        if (prevIdx == optionIdx) return@runCatchingSafe

        val nextCounts = poll.voteCounts.toMutableList()
        while (nextCounts.size < poll.options.size) nextCounts.add(0)
        if (prevIdx in nextCounts.indices) nextCounts[prevIdx] = (nextCounts[prevIdx] - 1).coerceAtLeast(0)
        if (optionIdx in nextCounts.indices) nextCounts[optionIdx] = nextCounts[optionIdx] + 1

        patchRow("polls", "id=eq.${poll.id}", JSONObject().put("vote_counts", JSONArray(nextCounts)))
        localPrefs.edit().putInt(localKey, optionIdx).apply()
        _polls.value = _polls.value.map { if (it.id == poll.id) it.copy(voteCounts = nextCounts) else it }
    }

    suspend fun sendChatMessage(msg: ChatMessage): Result<Unit> = runCatchingSafe {
        require(msg.body.isNotBlank()) { "বার্তা খালি রাখা যাবে না" }
        val payload = JSONObject().apply {
            put("user_id", msg.userId)
            put("channel", msg.channel)
            put("sender_name", msg.senderName)
            msg.senderRole?.let { put("sender_role", it) }
            put("body", msg.body)
        }
        val inserted = insertRow("chat_messages", payload)
        _chatMessages.value = _chatMessages.value + SupabaseJsonMapper.toChatMessage(inserted)
    }

    suspend fun saveVoice(voice: VoiceTestimonial): Result<Unit> = runCatchingSafe {
        require(voice.name.isNotBlank() && voice.body.isNotBlank()) { "নাম ও মন্তব্য লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", voice.userId)
            put("name", voice.name)
            put("label", voice.label)
            put("rating", voice.rating)
            put("body", voice.body)
            put("status", voice.status)
        }
        val inserted = insertRow("voices", payload)
        _voices.value = listOf(SupabaseJsonMapper.toVoice(inserted)) + _voices.value
    }

    suspend fun updateVoiceStatus(voiceId: String, status: String): Result<Unit> = runCatchingSafe {
        patchRow("voices", "id=eq.$voiceId", JSONObject().put("status", status))
        _voices.value = _voices.value.map { if (it.id == voiceId) it.copy(status = status) else it }
    }

    suspend fun saveDuty(duty: VolunteerDuty): Result<Unit> = runCatchingSafe {
        require(duty.title.isNotBlank()) { "কাজের শিরোনাম লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", duty.userId)
            put("title", duty.title)
            duty.description?.let { put("description", it) }
            put("assigned_to_name", duty.assignedToName)
            duty.dueOn?.let { put("due_on", it) }
            put("status", duty.status)
        }
        val inserted = insertRow("duties", payload)
        _duties.value = listOf(SupabaseJsonMapper.toDuty(inserted)) + _duties.value
    }

    suspend fun updateDutyStatus(dutyId: String, status: String): Result<Unit> = runCatchingSafe {
        patchRow("duties", "id=eq.$dutyId", JSONObject().put("status", status))
        _duties.value = _duties.value.map { if (it.id == dutyId) it.copy(status = status) else it }
    }

    suspend fun submitPublicRequest(req: PublicRequest): Result<Unit> = runCatchingSafe {
        require(req.name.isNotBlank()) { "আপনার নাম লিখুন" }
        val payload = JSONObject().apply {
            put("user_id", req.userId)
            put("kind", req.kind)
            put("name", req.name)
            req.phone?.let { put("phone", it) }
            req.email?.let { put("email", it) }
            req.district?.let { put("district", it) }
            req.bloodGroup?.let { put("blood_group", it) }
            req.aidType?.let { put("aid_type", it) }
            req.amountNeeded?.let { put("amount_needed", it) }
            req.subject?.let { put("subject", it) }
            req.details?.let { put("details", it) }
            put("status", "pending")
        }
        val inserted = insertRow("public_requests", payload)
        _publicRequests.value = listOf(SupabaseJsonMapper.toPublicRequest(inserted)) + _publicRequests.value
    }

    suspend fun updatePublicRequestStatus(reqId: String, status: String): Result<Unit> = runCatchingSafe {
        patchRow("public_requests", "id=eq.$reqId", JSONObject().put("status", status))
        _publicRequests.value = _publicRequests.value.map { if (it.id == reqId) it.copy(status = status) else it }
    }

    suspend fun updateFoundationSettings(settings: FoundationSettings): Result<Unit> = runCatchingSafe {
        val payload = JSONObject().apply {
            put("org_name", settings.orgName)
            settings.slogan?.let { put("slogan", it) }
            settings.hotline?.let { put("hotline", it) }
            settings.bankDetails?.let { put("bank_details", it) }
            settings.goalTitle?.let { put("goal_title", it) }
            put("target_amount", settings.targetAmount)
            settings.collectedAmount?.let { put("collected_amount", it) }
            settings.monthlyFee?.let { put("monthly_fee", it) }
            settings.joinCode?.let { put("registration_code", it) }
            settings.disabledFeatures?.let { put("disabled_features", JSONArray(it)) }
        }
        val docId = settings.id.ifBlank { "main" }
        patchRow("settings", "id=eq.$docId", payload)
        _settings.value = settings
    }

    private suspend fun <T> fetchTableFirst(
        candidates: List<String>,
        query: String = "select=*",
        mapper: (JSONObject) -> T
    ): List<T> {
        val q = if ("select=" in query) query else "select=*&$query"
        for (table in candidates) {
            val res = restRequest("GET", "/rest/v1/$table?$q", null)
            if (res.first in 200..299) {
                return runCatching { SupabaseJsonMapper.parseArray(res.second, mapper) }.getOrDefault(emptyList())
            }
        }
        return emptyList()
    }

    private suspend fun insertRow(table: String, body: JSONObject): JSONObject {
        val (code, raw) = restRequest(
            method = "POST",
            path = "/rest/v1/$table",
            jsonBody = body.toString(),
            prefer = "return=representation"
        )
        if (code !in 200..299) {
            throw IllegalStateException(parsePostgrestError(raw, "ডেটা সংরক্ষণ ব্যর্থ (HTTP $code)"))
        }
        val arr = runCatching { JSONArray(raw) }.getOrNull()
        return arr?.optJSONObject(0) ?: body
    }

    private suspend fun patchRow(table: String, filter: String, body: JSONObject) {
        val (code, raw) = restRequest(
            method = "PATCH",
            path = "/rest/v1/$table?$filter",
            jsonBody = body.toString(),
            prefer = "return=representation"
        )
        if (code !in 200..299) {
            throw IllegalStateException(parsePostgrestError(raw, "আপডেট ব্যর্থ (HTTP $code)"))
        }
    }

    private suspend fun deleteRow(table: String, docId: String) {
        val (code, raw) = restRequest("DELETE", "/rest/v1/$table?id=eq.$docId", null)
        if (code !in 200..299) {
            throw IllegalStateException(parsePostgrestError(raw, "মুছে ফেলা যায়নি (HTTP $code)"))
        }
    }

    private suspend fun restRequest(
        method: String,
        path: String,
        jsonBody: String?,
        prefer: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val base = SupabaseAuthHelper.baseUrl()
        val key = SupabaseAuthHelper.anonKey()
        val token = SupabaseAuthHelper.getValidAccessToken(appContext) ?: key
        val conn = (URL("$base$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", key)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            if (jsonBody != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
        }
        try {
            if (jsonBody != null) {
                conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299 && code != 404) {
                Log.w(TAG, "$method $path -> $code: $raw")
            }
            code to raw
        } finally {
            conn.disconnect()
        }
    }

    private fun parsePostgrestError(raw: String, fallback: String): String {
        val obj = runCatching { JSONObject(raw) }.getOrNull()
        return obj?.optString("message")?.ifBlank { null }
            ?: obj?.optString("hint")?.ifBlank { null }
            ?: fallback
    }

    private fun loadLocalSalatLogs(uid: String): List<SalatAmolLog> {
        val raw = localPrefs.getString("salat_logs_$uid", null) ?: return emptyList()
        return runCatching { SupabaseJsonMapper.parseArray(raw) { SupabaseJsonMapper.toSalatAmolLog(it) } }.getOrDefault(emptyList())
    }

    private fun saveLocalSalatLogs(uid: String, list: List<SalatAmolLog>) {
        val arr = JSONArray()
        list.forEach { log ->
            arr.put(JSONObject().apply {
                put("id", log.dateKey)
                put("user_id", uid)
                put("date_key", log.dateKey)
                put("prayers", JSONArray(log.prayers))
                put("amol_done", JSONArray(log.amolDone))
                put("tasbih_total", log.tasbihTotal)
            })
        }
        localPrefs.edit().putString("salat_logs_$uid", arr.toString()).apply()
    }

    private fun loadLocalAiSessions(uid: String): List<AiChatSession> {
        val raw = localPrefs.getString("ai_sessions_$uid", null) ?: return emptyList()
        return runCatching { SupabaseJsonMapper.parseArray(raw) { SupabaseJsonMapper.toAiSession(it) } }.getOrDefault(emptyList())
    }

    private fun saveLocalAiSessions(uid: String, list: List<AiChatSession>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("user_id", uid)
                put("title", s.title)
            })
        }
        localPrefs.edit().putString("ai_sessions_$uid", arr.toString()).apply()
    }

    private suspend inline fun runCatchingSafe(crossinline block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
