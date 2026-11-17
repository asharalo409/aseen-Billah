package com.manobseba.foundation.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.manobseba.foundation.R
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
import com.manobseba.foundation.data.model.PollVote
import com.manobseba.foundation.data.model.Project
import com.manobseba.foundation.data.model.PublicRequest
import com.manobseba.foundation.data.model.ReliefLocation
import com.manobseba.foundation.data.model.SalatAmolLog
import com.manobseba.foundation.data.model.UserProfile
import com.manobseba.foundation.data.model.VoiceTestimonial
import com.manobseba.foundation.data.model.VolunteerDuty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FoundationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
    )

    private inline fun <reified T> observeFoundationCollection(collection: String): Flow<List<T>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection(collection)
            .whereEqualTo("visibility", "foundation")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, collection)
            }
    }

    fun observeSettings(): Flow<FoundationSettings?> =
        observeFoundationCollection<FoundationSettings>("foundation_settings")
            .map { it.firstOrNull() }

    fun observeMembers(): Flow<List<Member>> = observeFoundationCollection<Member>("members")
        .map { list -> list.sortedWith(compareBy({ it.rank ?: 100 }, { it.memberCode })) }

    fun observeFunds(): Flow<List<Fund>> = observeFoundationCollection<Fund>("funds")

    fun observeTransactions(): Flow<List<FoundationTransaction>> =
        observeFoundationCollection<FoundationTransaction>("transactions")
            .map { list -> list.sortedByDescending { it.dateStr } }

    fun observeProjects(): Flow<List<Project>> = observeFoundationCollection<Project>("projects")

    fun observeBloodRequests(): Flow<List<BloodRequest>> =
        observeFoundationCollection<BloodRequest>("blood_requests")
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

    fun observeBloodDonors(): Flow<List<BloodDonor>> =
        observeFoundationCollection<BloodDonor>("blood_donors")

    fun observeReliefLocations(): Flow<List<ReliefLocation>> =
        observeFoundationCollection<ReliefLocation>("relief_locations")

    fun observeFieldReports(): Flow<List<FieldReport>> =
        observeFoundationCollection<FieldReport>("field_reports")
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

    fun observeEvents(): Flow<List<FoundationEvent>> =
        observeFoundationCollection<FoundationEvent>("events")
            .map { list -> list.sortedBy { it.eventDateMillis } }

    fun observeNotices(): Flow<List<Notice>> =
        observeFoundationCollection<Notice>("notices")
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

    fun observePolls(): Flow<List<Poll>> =
        observeFoundationCollection<Poll>("polls")
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

    fun observeVoices(): Flow<List<VoiceTestimonial>> =
        observeFoundationCollection<VoiceTestimonial>("voices")
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

    fun observeChatMessages(): Flow<List<ChatMessage>> =
        observeFoundationCollection<ChatMessage>("chat_messages")
            .map { list -> list.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE } }

    fun observeDuties(): Flow<List<VolunteerDuty>> =
        observeFoundationCollection<VolunteerDuty>("duties")

    fun observePublicRequests(user: FirebaseUser?): Flow<List<PublicRequest>> {
        if (user == null) return flowOf(emptyList())
        val isBootstrapAdmin = user.isEmailVerified && user.email == SeedCatalog.BOOTSTRAP_ADMIN_EMAIL
        val query: Query = if (isBootstrapAdmin) {
            db.collection("public_requests")
        } else {
            db.collection("public_requests").whereEqualTo("userId", user.uid)
        }
        return query.snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(PublicRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, "public_requests")
            }
    }

    fun observeUserProfile(uid: String?): Flow<UserProfile?> {
        if (uid.isNullOrBlank()) return flowOf(null)
        return db.collection("users").document(uid)
            .snapshots(MetadataChanges.INCLUDE)
            .map { doc -> doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.GET, "users/$uid")
            }
    }

    fun observeSalatAmolLogs(uid: String?): Flow<List<SalatAmolLog>> {
        if (uid.isNullOrBlank()) return flowOf(emptyList())
        return db.collection("users").document(uid).collection("salat_amol")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(SalatAmolLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, "users/$uid/salat_amol")
            }
    }

    fun observeAiSessions(uid: String?): Flow<List<AiChatSession>> {
        if (uid.isNullOrBlank()) return flowOf(emptyList())
        return db.collection("users").document(uid).collection("ai_chats")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(AiChatSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, "users/$uid/ai_chats")
            }
    }

    fun observeAiMessages(uid: String?, chatId: String?): Flow<List<AiChatMessage>> {
        if (uid.isNullOrBlank() || chatId.isNullOrBlank()) return flowOf(emptyList())
        return db.collection("users").document(uid).collection("ai_chats").document(chatId).collection("messages")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(AiChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, "users/$uid/ai_chats/$chatId/messages")
            }
    }

    suspend fun ensureUserProfileAndStarterSeed(user: FirebaseUser): Result<Unit> = runCatchingSafe {
        val uid = user.uid
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        if (!snap.exists()) {
            val isBootstrap = user.isEmailVerified && user.email == SeedCatalog.BOOTSTRAP_ADMIN_EMAIL
            val profile = UserProfile(
                userId = uid,
                displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "সদস্য",
                email = user.email,
                role = if (isBootstrap) "admin" else "member",
                themePalette = "green",
                themeBg = "plain",
                darkMode = false
            )
            userRef.set(profile.toCreateMap()).await()
        }

        val hasFunds = !db.collection("funds").whereEqualTo("visibility", "foundation").limit(1).get().await().isEmpty
        if (!hasFunds) {
            seedStarterData(uid)
        }
    }

    private suspend fun seedStarterData(uid: String) {
        val batch = db.batch()
        batch.set(db.collection("foundation_settings").document("main"), SeedCatalog.defaultSettings(uid).toCreateMap())
        SeedCatalog.seedFunds(uid).forEach { batch.set(db.collection("funds").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedMembers(uid).forEach { batch.set(db.collection("members").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedProjects(uid).forEach { batch.set(db.collection("projects").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedTransactions(uid).forEach { batch.set(db.collection("transactions").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedReliefLocations(uid).forEach { batch.set(db.collection("relief_locations").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedFieldReports(uid).forEach { batch.set(db.collection("field_reports").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedNotices(uid).forEach { batch.set(db.collection("notices").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedBloodRequests(uid).forEach { batch.set(db.collection("blood_requests").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedBloodDonors(uid).forEach { batch.set(db.collection("blood_donors").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedEvents(uid).forEach { batch.set(db.collection("events").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedPolls(uid).forEach { batch.set(db.collection("polls").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedVoices(uid).forEach { batch.set(db.collection("voices").document(it.id), it.toCreateMap()) }
        SeedCatalog.seedDuties(uid).forEach { batch.set(db.collection("duties").document(it.id), it.toCreateMap()) }
        batch.commit().await()
    }

    suspend fun updateUserTheme(uid: String, palette: String, bg: String, darkMode: Boolean): Result<Unit> = runCatchingSafe {
        db.collection("users").document(uid).update(
            mapOf(
                "themePalette" to palette,
                "themeBg" to bg,
                "darkMode" to darkMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateUserProfileInfo(uid: String, displayName: String, memberCode: String?): Result<Unit> = runCatchingSafe {
        val map = buildMap<String, Any> {
            put("displayName", displayName.trim().take(120).ifBlank { "সদস্য" })
            if (!memberCode.isNullOrBlank()) put("memberCode", memberCode.trim().take(40))
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.collection("users").document(uid).update(map).await()
    }

    suspend fun saveSalatAmol(uid: String, dayKey: String, prayers: List<String>, amolDone: List<String>, tasbihTotal: Int): Result<Unit> = runCatchingSafe {
        val ref = db.collection("users").document(uid).collection("salat_amol").document(dayKey)
        val exists = ref.get().await().exists()
        if (exists) {
            ref.update(
                mapOf(
                    "prayers" to prayers.take(5),
                    "amolDone" to amolDone.take(30),
                    "tasbihTotal" to tasbihTotal.coerceIn(0, 10_000_000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val log = SalatAmolLog(
                userId = uid,
                dateKey = dayKey,
                prayers = prayers,
                amolDone = amolDone,
                tasbihTotal = tasbihTotal
            )
            ref.set(log.toCreateMap()).await()
        }
    }

    suspend fun createAiSession(uid: String, title: String): Result<String> = runCatching {
        val ref = db.collection("users").document(uid).collection("ai_chats").document()
        val session = AiChatSession(userId = uid, title = title)
        ref.set(session.toCreateMap()).await()
        ref.id
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun addAiMessage(uid: String, chatId: String, sender: String, text: String, actionType: String? = null): Result<Unit> = runCatchingSafe {
        val msg = AiChatMessage(userId = uid, sender = sender, text = text, actionType = actionType)
        db.collection("users").document(uid).collection("ai_chats").document(chatId)
            .collection("messages").add(msg.toCreateMap()).await()
    }

    suspend fun saveTransaction(tx: FoundationTransaction): Result<Unit> = runCatchingSafe {
        require(tx.amount > 0.0) { "টাকার পরিমাণ শূন্যের বেশি হতে হবে" }
        val docId = tx.id.ifBlank { db.collection("transactions").document().id }
        db.collection("transactions").document(docId).set(tx.toCreateMap()).await()
    }

    suspend fun deleteDocument(collection: String, docId: String): Result<Unit> = runCatchingSafe {
        db.collection(collection).document(docId).delete().await()
    }

    suspend fun saveMember(member: Member, isEdit: Boolean): Result<Unit> = runCatchingSafe {
        require(member.fullName.isNotBlank()) { "সদস্যের নাম লিখুন" }
        if (isEdit && member.id.isNotBlank()) {
            val updates = buildMap<String, Any> {
                put("fullName", member.fullName.take(120))
                member.phone?.let { put("phone", it.take(40)) }
                member.district?.let { put("district", it.take(60)) }
                member.bloodGroup?.let { put("bloodGroup", it.take(10)) }
                put("role", member.role.take(40))
                member.roleLabel?.let { put("roleLabel", it.take(60)) }
                member.rank?.let { put("rank", it.coerceIn(1, 999)) }
                put("memberCode", member.memberCode.take(40))
                put("active", member.active)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("members").document(member.id).update(updates).await()
        } else {
            val docId = member.id.ifBlank { db.collection("members").document().id }
            db.collection("members").document(docId).set(member.toCreateMap()).await()
        }
    }

    suspend fun saveFund(fund: Fund): Result<Unit> = runCatchingSafe {
        require(fund.name.isNotBlank()) { "খাতের নাম লিখুন" }
        val docId = fund.id.ifBlank { db.collection("funds").document().id }
        db.collection("funds").document(docId).set(fund.toCreateMap()).await()
    }

    suspend fun saveProject(project: Project): Result<Unit> = runCatchingSafe {
        require(project.title.isNotBlank()) { "প্রকল্পের শিরোনাম লিখুন" }
        val docId = project.id.ifBlank { db.collection("projects").document().id }
        db.collection("projects").document(docId).set(project.toCreateMap()).await()
    }

    suspend fun saveBloodRequest(req: BloodRequest): Result<Unit> = runCatchingSafe {
        require(req.hospital.isNotBlank() && req.contact.isNotBlank()) { "হাসপাতাল ও ফোন নম্বর লিখুন" }
        val docId = req.id.ifBlank { db.collection("blood_requests").document().id }
        db.collection("blood_requests").document(docId).set(req.toCreateMap()).await()
    }

    suspend fun markBloodRequestManaged(requestId: String): Result<Unit> = runCatchingSafe {
        db.collection("blood_requests").document(requestId).update(
            mapOf("managed" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun saveBloodDonor(donor: BloodDonor): Result<Unit> = runCatchingSafe {
        require(donor.fullName.isNotBlank() && donor.phone.isNotBlank()) { "রক্তদাতার নাম ও ফোন লিখুন" }
        val docId = donor.id.ifBlank { db.collection("blood_donors").document().id }
        db.collection("blood_donors").document(docId).set(donor.toCreateMap()).await()
    }

    suspend fun saveReliefLocation(loc: ReliefLocation): Result<Unit> = runCatchingSafe {
        require(loc.title.isNotBlank()) { "ক্যাম্পের নাম লিখুন" }
        val docId = loc.id.ifBlank { db.collection("relief_locations").document().id }
        db.collection("relief_locations").document(docId).set(loc.toCreateMap()).await()
    }

    suspend fun saveFieldReport(report: FieldReport): Result<Unit> = runCatchingSafe {
        require(report.title.isNotBlank()) { "রিপোর্টের শিরোনাম লিখুন" }
        val docId = report.id.ifBlank { db.collection("field_reports").document().id }
        db.collection("field_reports").document(docId).set(report.toCreateMap()).await()
    }

    suspend fun reactFieldReport(reportId: String, nextCount: Int): Result<Unit> = runCatchingSafe {
        db.collection("field_reports").document(reportId).update(
            mapOf("reactionCount" to nextCount, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun saveEvent(event: FoundationEvent): Result<Unit> = runCatchingSafe {
        require(event.title.isNotBlank()) { "ইভেন্টের নাম লিখুন" }
        val docId = event.id.ifBlank { db.collection("events").document().id }
        db.collection("events").document(docId).set(event.toCreateMap()).await()
    }

    suspend fun toggleEventRsvp(event: FoundationEvent, uid: String): Result<Unit> = runCatchingSafe {
        val nextList = if (uid in event.attendeeUids) event.attendeeUids - uid else (event.attendeeUids + uid).take(100)
        db.collection("events").document(event.id).update(
            mapOf("attendeeUids" to nextList, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun saveNotice(notice: Notice): Result<Unit> = runCatchingSafe {
        require(notice.title.isNotBlank() && notice.body.isNotBlank()) { "শিরোনাম ও বিবরণ লিখুন" }
        val docId = notice.id.ifBlank { db.collection("notices").document().id }
        db.collection("notices").document(docId).set(notice.toCreateMap()).await()
    }

    suspend fun reactNotice(notice: Notice, isLike: Boolean): Result<Unit> = runCatchingSafe {
        val updates = if (isLike) {
            mapOf("likeCount" to notice.likeCount + 1, "updatedAt" to FieldValue.serverTimestamp())
        } else {
            mapOf("supportCount" to notice.supportCount + 1, "updatedAt" to FieldValue.serverTimestamp())
        }
        db.collection("notices").document(notice.id).update(updates).await()
    }

    suspend fun createPoll(poll: Poll): Result<Unit> = runCatchingSafe {
        require(poll.question.isNotBlank() && poll.options.size >= 2) { "প্রশ্ন ও কমপক্ষে ২টি অপশন দিন" }
        val docId = poll.id.ifBlank { db.collection("polls").document().id }
        db.collection("polls").document(docId).set(poll.toCreateMap()).await()
    }

    suspend fun getMyVote(pollId: String, uid: String): Int? {
        return runCatching {
            val doc = db.collection("polls").document(pollId).collection("votes").document(uid).get().await()
            if (doc.exists()) doc.getLong("optionIdx")?.toInt() else null
        }.getOrNull()
    }

    suspend fun castPollVote(poll: Poll, uid: String, optionIdx: Int): Result<Unit> = runCatchingSafe {
        require(!poll.closed) { "এই ভোটটি বন্ধ হয়ে গেছে" }
        val pollRef = db.collection("polls").document(poll.id)
        val voteRef = pollRef.collection("votes").document(uid)
        val existingVote = voteRef.get().await()
        val prevIdx = if (existingVote.exists()) existingVote.getLong("optionIdx")?.toInt() else null
        if (prevIdx == optionIdx) return@runCatchingSafe

        val nextCounts = poll.voteCounts.toMutableList()
        while (nextCounts.size < poll.options.size) nextCounts.add(0)
        if (prevIdx != null && prevIdx in nextCounts.indices) {
            nextCounts[prevIdx] = (nextCounts[prevIdx] - 1).coerceAtLeast(0)
        }
        if (optionIdx in nextCounts.indices) {
            nextCounts[optionIdx] = nextCounts[optionIdx] + 1
        }

        val batch = db.batch()
        if (existingVote.exists()) {
            batch.update(voteRef, mapOf("optionIdx" to optionIdx, "updatedAt" to FieldValue.serverTimestamp()))
        } else {
            val vote = PollVote(userId = uid, pollId = poll.id, optionIdx = optionIdx)
            batch.set(voteRef, vote.toCreateMap())
        }
        batch.update(pollRef, mapOf("voteCounts" to nextCounts, "updatedAt" to FieldValue.serverTimestamp()))
        batch.commit().await()
    }

    suspend fun sendChatMessage(msg: ChatMessage): Result<Unit> = runCatchingSafe {
        require(msg.body.isNotBlank()) { "বার্তা খালি রাখা যাবে না" }
        val docId = msg.id.ifBlank { db.collection("chat_messages").document().id }
        db.collection("chat_messages").document(docId).set(msg.toCreateMap()).await()
    }

    suspend fun saveVoice(voice: VoiceTestimonial): Result<Unit> = runCatchingSafe {
        require(voice.name.isNotBlank() && voice.body.isNotBlank()) { "নাম ও মন্তব্য লিখুন" }
        val docId = voice.id.ifBlank { db.collection("voices").document().id }
        db.collection("voices").document(docId).set(voice.toCreateMap()).await()
    }

    suspend fun updateVoiceStatus(voiceId: String, status: String): Result<Unit> = runCatchingSafe {
        db.collection("voices").document(voiceId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun saveDuty(duty: VolunteerDuty): Result<Unit> = runCatchingSafe {
        require(duty.title.isNotBlank()) { "কাজের শিরোনাম লিখুন" }
        val docId = duty.id.ifBlank { db.collection("duties").document().id }
        db.collection("duties").document(docId).set(duty.toCreateMap()).await()
    }

    suspend fun updateDutyStatus(dutyId: String, status: String): Result<Unit> = runCatchingSafe {
        db.collection("duties").document(dutyId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun submitPublicRequest(req: PublicRequest): Result<Unit> = runCatchingSafe {
        require(req.name.isNotBlank()) { "আপনার নাম লিখুন" }
        val docId = req.id.ifBlank { db.collection("public_requests").document().id }
        db.collection("public_requests").document(docId).set(req.toCreateMap()).await()
    }

    suspend fun updatePublicRequestStatus(reqId: String, status: String): Result<Unit> = runCatchingSafe {
        db.collection("public_requests").document(reqId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun updateFoundationSettings(settings: FoundationSettings): Result<Unit> = runCatchingSafe {
        val docId = settings.id.ifBlank { "main" }
        db.collection("foundation_settings").document(docId).update(
            buildMap {
                put("orgName", settings.orgName.take(120))
                settings.slogan?.let { put("slogan", it.take(200)) }
                settings.hotline?.let { put("hotline", it.take(40)) }
                settings.bankDetails?.let { put("bankDetails", it.take(600)) }
                settings.goalTitle?.let { put("goalTitle", it.take(120)) }
                put("targetAmount", settings.targetAmount.coerceIn(0.0, 1_000_000_000.0))
                settings.collectedAmount?.let { put("collectedAmount", it.coerceAtLeast(0.0)) }
                settings.monthlyFee?.let { put("monthlyFee", it.coerceAtLeast(0.0)) }
                settings.joinCode?.let { put("joinCode", it.take(40)) }
                settings.disabledFeatures?.let { put("disabledFeatures", it.take(30)) }
                put("updatedAt", FieldValue.serverTimestamp())
            }
        ).await()
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
