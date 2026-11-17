package com.manobseba.foundation.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.manobseba.foundation.data.DistrictsData
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
import com.manobseba.foundation.data.remote.AiActionProposal
import com.manobseba.foundation.data.remote.AiRepository
import com.manobseba.foundation.data.remote.ExternalContentClient
import com.manobseba.foundation.data.remote.FoundationRepository
import com.manobseba.foundation.data.remote.HadithBookOffline
import com.manobseba.foundation.data.remote.ParsedLedgerEntry
import com.manobseba.foundation.data.remote.ParsedLedgerResult
import com.manobseba.foundation.data.remote.ParsedLegacyResult
import com.manobseba.foundation.data.remote.QuranAyahItem
import com.manobseba.foundation.data.remote.WeatherReport
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.domain.PrayerLoc
import com.manobseba.foundation.security.AppLockManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.YearMonth

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class SignedIn(val user: FirebaseUser) : AuthUiState
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data object Loading : AiTaskState<Nothing>
    data class Streaming(val text: String) : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

data class LocalChatBubble(
    val id: String,
    val sender: String,
    val text: String,
    val proposal: AiActionProposal? = null,
    val executed: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FoundationRepository(app)
    private val aiRepo = AiRepository()
    private val extClient = ExternalContentClient(app)
    val lockManager = AppLockManager(app)
    private val localPrefs = app.getSharedPreferences("manobseba_local", Context.MODE_PRIVATE)

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthUiState>(
        auth.currentUser?.let { AuthUiState.SignedIn(it) } ?: AuthUiState.SignedOut
    )
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isLocked = MutableStateFlow(lockManager.isLockEnabled())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Live Firestore streams (populated only while authenticated)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _settings = MutableStateFlow<FoundationSettings?>(null)
    val settings: StateFlow<FoundationSettings?> = _settings.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _funds = MutableStateFlow<List<Fund>>(emptyList())
    val funds: StateFlow<List<Fund>> = _funds.asStateFlow()

    private val _transactions = MutableStateFlow<List<FoundationTransaction>>(emptyList())
    val transactions: StateFlow<List<FoundationTransaction>> = _transactions.asStateFlow()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _bloodRequests = MutableStateFlow<List<BloodRequest>>(emptyList())
    val bloodRequests: StateFlow<List<BloodRequest>> = _bloodRequests.asStateFlow()

    private val _bloodDonors = MutableStateFlow<List<BloodDonor>>(emptyList())
    val bloodDonors: StateFlow<List<BloodDonor>> = _bloodDonors.asStateFlow()

    private val _reliefLocations = MutableStateFlow<List<ReliefLocation>>(emptyList())
    val reliefLocations: StateFlow<List<ReliefLocation>> = _reliefLocations.asStateFlow()

    private val _fieldReports = MutableStateFlow<List<FieldReport>>(emptyList())
    val fieldReports: StateFlow<List<FieldReport>> = _fieldReports.asStateFlow()

    private val _events = MutableStateFlow<List<FoundationEvent>>(emptyList())
    val events: StateFlow<List<FoundationEvent>> = _events.asStateFlow()

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    private val _polls = MutableStateFlow<List<Poll>>(emptyList())
    val polls: StateFlow<List<Poll>> = _polls.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceTestimonial>>(emptyList())
    val voices: StateFlow<List<VoiceTestimonial>> = _voices.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _duties = MutableStateFlow<List<VolunteerDuty>>(emptyList())
    val duties: StateFlow<List<VolunteerDuty>> = _duties.asStateFlow()

    private val _publicRequests = MutableStateFlow<List<PublicRequest>>(emptyList())
    val publicRequests: StateFlow<List<PublicRequest>> = _publicRequests.asStateFlow()

    private val _salatLogs = MutableStateFlow<List<SalatAmolLog>>(emptyList())
    val salatLogs: StateFlow<List<SalatAmolLog>> = _salatLogs.asStateFlow()

    private val _aiSessions = MutableStateFlow<List<AiChatSession>>(emptyList())
    val aiSessions: StateFlow<List<AiChatSession>> = _aiSessions.asStateFlow()

    private val _activeAiChatId = MutableStateFlow<String?>(null)
    val activeAiChatId: StateFlow<String?> = _activeAiChatId.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    // AI feature states
    private val _chatPendingBubbles = MutableStateFlow<List<LocalChatBubble>>(emptyList())
    val chatPendingBubbles: StateFlow<List<LocalChatBubble>> = _chatPendingBubbles.asStateFlow()

    private val _aiChatStatus = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val aiChatStatus: StateFlow<AiTaskState<String>> = _aiChatStatus.asStateFlow()

    private val _aiHealthSummary = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val aiHealthSummary: StateFlow<AiTaskState<String>> = _aiHealthSummary.asStateFlow()

    private val _ledgerScanState = MutableStateFlow<AiTaskState<ParsedLedgerResult>>(AiTaskState.Idle)
    val ledgerScanState: StateFlow<AiTaskState<ParsedLedgerResult>> = _ledgerScanState.asStateFlow()

    private val _legacyParseState = MutableStateFlow<AiTaskState<ParsedLegacyResult>>(AiTaskState.Idle)
    val legacyParseState: StateFlow<AiTaskState<ParsedLegacyResult>> = _legacyParseState.asStateFlow()

    // Location, Weather, Islamic state
    private val _selectedDistrict = MutableStateFlow(localPrefs.getString("selected_district", "ঢাকা") ?: "ঢাকা")
    val selectedDistrict: StateFlow<String> = _selectedDistrict.asStateFlow()

    val prayerLoc: StateFlow<PrayerLoc> = _selectedDistrict
        .combine(_selectedDistrict) { d, _ -> PrayerCalculator.districtLoc(d) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PrayerCalculator.districtLoc("ঢাকা"))

    private val _asrFactor = MutableStateFlow(localPrefs.getInt("asr_factor", 2))
    val asrFactor: StateFlow<Int> = _asrFactor.asStateFlow()

    private val _weather = MutableStateFlow<WeatherReport?>(null)
    val weather: StateFlow<WeatherReport?> = _weather.asStateFlow()

    private val _weatherBusy = MutableStateFlow(false)
    val weatherBusy: StateFlow<Boolean> = _weatherBusy.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    private val _surahItems = MutableStateFlow<List<QuranAyahItem>>(emptyList())
    val surahItems: StateFlow<List<QuranAyahItem>> = _surahItems.asStateFlow()

    private val _surahBusy = MutableStateFlow(false)
    val surahBusy: StateFlow<Boolean> = _surahBusy.asStateFlow()

    private val _savedHadithBooks = MutableStateFlow(extClient.getSavedHadithBookIds())
    val savedHadithBooks: StateFlow<List<String>> = _savedHadithBooks.asStateFlow()

    private val _hadithBusyId = MutableStateFlow<String?>(null)
    val hadithBusyId: StateFlow<String?> = _hadithBusyId.asStateFlow()

    private val _customAmolItems = MutableStateFlow(
        localPrefs.getString("custom_amol", null)?.split("||")?.filter { it.isNotBlank() }
            ?: SeedCatalog.DEFAULT_AMOL_ITEMS
    )
    val customAmolItems: StateFlow<List<String>> = _customAmolItems.asStateFlow()

    private val _tasbihCount = MutableStateFlow(localPrefs.getInt("tasbih_cur", 0))
    val tasbihCount: StateFlow<Int> = _tasbihCount.asStateFlow()

    private val _tasbihRounds = MutableStateFlow(localPrefs.getInt("tasbih_rounds", 0))
    val tasbihRounds: StateFlow<Int> = _tasbihRounds.asStateFlow()

    private val _tasbihTotal = MutableStateFlow(localPrefs.getInt("tasbih_total", 0))
    val tasbihTotal: StateFlow<Int> = _tasbihTotal.asStateFlow()

    private val _tasbihTarget = MutableStateFlow(localPrefs.getInt("tasbih_target", 33))
    val tasbihTarget: StateFlow<Int> = _tasbihTarget.asStateFlow()

    private var listenerJobs = mutableListOf<Job>()
    private var aiMsgJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            cancelListeners()
            _authState.value = AuthUiState.SignedOut
        } else {
            _authState.value = AuthUiState.SignedIn(user)
            startAuthenticatedSync(user)
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.let { startAuthenticatedSync(it) }
        val loc = DistrictsData.findByName(_selectedDistrict.value)
        _weather.value = extClient.getCachedWeather(loc.lat, loc.lng)
        refreshWeather(force = false)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        cancelListeners()
        super.onCleared()
    }

    private fun cancelListeners() {
        listenerJobs.forEach { it.cancel() }
        listenerJobs.clear()
        aiMsgJob?.cancel()
        aiMsgJob = null
    }

    private fun startAuthenticatedSync(user: FirebaseUser) {
        cancelListeners()
        viewModelScope.launch {
            repo.ensureUserProfileAndStarterSeed(user)
                .onFailure { showToast("ডাটাবেস সিঙ্ক সতর্কতা: ${it.localizedMessage ?: "পুনরায় চেষ্টা করুন"}") }
        }
        val uid = user.uid
        listenerJobs += viewModelScope.launch { repo.observeUserProfile(uid).collect { _userProfile.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeSettings().collect { _settings.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeMembers().collect { _members.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeFunds().collect { _funds.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeTransactions().collect { _transactions.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeProjects().collect { _projects.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeBloodRequests().collect { _bloodRequests.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeBloodDonors().collect { _bloodDonors.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeReliefLocations().collect { _reliefLocations.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeFieldReports().collect { _fieldReports.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeEvents().collect { _events.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeNotices().collect { _notices.value = it } }
        listenerJobs += viewModelScope.launch { repo.observePolls().collect { _polls.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeVoices().collect { _voices.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeChatMessages().collect { _chatMessages.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeDuties().collect { _duties.value = it } }
        listenerJobs += viewModelScope.launch { repo.observePublicRequests(user).collect { _publicRequests.value = it } }
        listenerJobs += viewModelScope.launch { repo.observeSalatAmolLogs(uid).collect { _salatLogs.value = it } }
        listenerJobs += viewModelScope.launch {
            repo.observeAiSessions(uid).collect { sessions ->
                _aiSessions.value = sessions
                if (_activeAiChatId.value == null && sessions.isNotEmpty()) {
                    selectAiSession(sessions.first().id)
                }
            }
        }
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun currentUserId(): String = auth.currentUser?.uid.orEmpty()

    fun currentDisplayName(): String =
        _userProfile.value?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "সদস্য"

    fun currentRole(): String {
        val u = auth.currentUser
        if (u != null && u.isEmailVerified && u.email == SeedCatalog.BOOTSTRAP_ADMIN_EMAIL) return "admin"
        return _userProfile.value?.role ?: "member"
    }

    fun isCurrentUserAdmin(): Boolean = currentRole() == "admin"

    fun hasPerm(perm: String): Boolean = SeedCatalog.hasPerm(currentRole(), true, perm)

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockNowIfEnabled() {
        if (lockManager.isLockEnabled()) _isLocked.value = true
    }

    fun setThemePreference(palette: String, bg: String, darkMode: Boolean) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.updateUserTheme(uid, palette, bg, darkMode)
                .onFailure { showToast("থিম সংরক্ষণ করা যায়নি") }
        }
    }

    fun updateProfileNameAndCode(displayName: String, memberCode: String?) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.updateUserProfileInfo(uid, displayName, memberCode)
                .onSuccess { showToast("প্রোফাইল আপডেট হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "প্রোফাইল আপডেট ব্যর্থ") }
        }
    }

    // --- Finance & Transactions ---
    fun addTransaction(tx: FoundationTransaction) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveTransaction(tx.copy(userId = uid))
                .onSuccess { showToast("আর্থিক এন্ট্রি সংরক্ষিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "সংরক্ষণ ব্যর্থ") }
        }
    }

    fun deleteItem(collection: String, id: String, label: String) {
        viewModelScope.launch {
            repo.deleteDocument(collection, id)
                .onSuccess { showToast("$label মুছে ফেলা হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "মোছা যায়নি") }
        }
    }

    // --- Members & Funds ---
    fun saveMember(member: Member, isEdit: Boolean) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveMember(member.copy(userId = uid), isEdit)
                .onSuccess { showToast("সদস্যের তথ্য সংরক্ষিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "সদস্য সংরক্ষণ ব্যর্থ") }
        }
    }

    fun saveFund(name: String, code: String, description: String, budget: Double) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveFund(Fund(userId = uid, name = name, code = code, description = description, budget = budget))
                .onSuccess { showToast("নতুন তহবিল খাত তৈরি হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "তহবিল তৈরি ব্যর্থ") }
        }
    }

    fun saveProject(project: Project) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveProject(project.copy(userId = uid))
                .onSuccess { showToast("প্রকল্প সংরক্ষিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "প্রকল্প সংরক্ষণ ব্যর্থ") }
        }
    }

    // --- Blood SOS ---
    fun submitBloodRequest(req: BloodRequest) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveBloodRequest(req.copy(userId = uid))
                .onSuccess { showToast("জরুরি রক্তের আবেদন প্রকাশিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "আবেদন ব্যর্থ") }
        }
    }

    fun markBloodManaged(requestId: String) {
        viewModelScope.launch {
            repo.markBloodRequestManaged(requestId)
                .onSuccess { showToast("রক্ত ম্যানেজ হয়েছে হিসেবে চিহ্নিত") }
                .onFailure { showToast(it.localizedMessage ?: "আপডেট ব্যর্থ") }
        }
    }

    fun registerBloodDonor(donor: BloodDonor) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveBloodDonor(donor.copy(userId = uid))
                .onSuccess { showToast("রক্তদাতা হিসেবে নিবন্ধিত হয়েছেন") }
                .onFailure { showToast(it.localizedMessage ?: "নিবন্ধন ব্যর্থ") }
        }
    }

    // --- Relief Map & Field Reports ---
    fun saveReliefLocation(loc: ReliefLocation) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveReliefLocation(loc.copy(userId = uid))
                .onSuccess { showToast("ত্রাণ কেন্দ্র মানচিত্রে যোগ হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "যোগ ব্যর্থ") }
        }
    }

    fun saveFieldReport(report: FieldReport) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveFieldReport(report.copy(userId = uid, authorName = currentDisplayName()))
                .onSuccess { showToast("কার্যক্রম রিপোর্ট জমা হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "জমা ব্যর্থ") }
        }
    }

    fun reactFieldReport(report: FieldReport) {
        viewModelScope.launch {
            repo.reactFieldReport(report.id, report.reactionCount + 1)
        }
    }

    // --- Events, Notices, Polls, Chat, Voices, Duties, Public Requests ---
    fun saveEvent(event: FoundationEvent) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveEvent(event.copy(userId = uid))
                .onSuccess { showToast("ইভেন্ট ক্যালেন্ডারে যুক্ত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "ইভেন্ট সংরক্ষণ ব্যর্থ") }
        }
    }

    fun toggleRsvp(event: FoundationEvent) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.toggleEventRsvp(event, uid)
                .onSuccess { showToast(if (uid in event.attendeeUids) "উপস্থিতি বাতিল" else "উপস্থিত থাকব নিশ্চিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "আপডেট ব্যর্থ") }
        }
    }

    fun publishNotice(title: String, body: String, urgent: Boolean) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveNotice(Notice(userId = uid, title = title, body = body, urgent = urgent))
                .onSuccess { showToast("অফিসিয়াল নোটিশ প্রকাশিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "নোটিশ প্রকাশ ব্যর্থ") }
        }
    }

    fun reactNotice(notice: Notice, isLike: Boolean) {
        viewModelScope.launch { repo.reactNotice(notice, isLike) }
    }

    fun createPoll(question: String, description: String, options: List<String>) {
        val uid = currentUserId().ifBlank { return }
        val cleanOpts = options.map { it.trim() }.filter { it.isNotBlank() }.take(6)
        if (cleanOpts.size < 2) {
            showToast("কমপক্ষে ২টি অপশন দিন")
            return
        }
        viewModelScope.launch {
            repo.createPoll(
                Poll(
                    userId = uid,
                    question = question,
                    description = description.takeIf { it.isNotBlank() },
                    options = cleanOpts,
                    voteCounts = List(cleanOpts.size) { 0 },
                    closed = false
                )
            ).onSuccess { showToast("নতুন ভোট চালু হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "ভোট তৈরি ব্যর্থ") }
        }
    }

    fun voteInPoll(poll: Poll, optionIdx: Int) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.castPollVote(poll, uid, optionIdx)
                .onSuccess { showToast("আপনার গোপন ভোট গৃহীত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "ভোট দিতে সমস্যা হয়েছে") }
        }
    }

    suspend fun getMyPollVote(pollId: String): Int? {
        val uid = currentUserId().ifBlank { return null }
        return repo.getMyVote(pollId, uid)
    }

    fun sendCommunityMessage(channel: String, body: String) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.sendChatMessage(
                ChatMessage(
                    userId = uid,
                    channel = channel,
                    senderName = currentDisplayName(),
                    senderRole = SeedCatalog.roleLabel(currentRole()),
                    body = body
                )
            ).onFailure { showToast(it.localizedMessage ?: "বার্তা পাঠানো যায়নি") }
        }
    }

    fun submitVoice(name: String, label: String, rating: Int, body: String) {
        val uid = currentUserId().ifBlank { return }
        val status = if (isCurrentUserAdmin()) "approved" else "pending"
        viewModelScope.launch {
            repo.saveVoice(VoiceTestimonial(userId = uid, name = name, label = label, rating = rating, body = body, status = status))
                .onSuccess { showToast("আপনার মতামত জমা হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "জমা ব্যর্থ") }
        }
    }

    fun updateVoiceStatus(voiceId: String, status: String) {
        viewModelScope.launch {
            repo.updateVoiceStatus(voiceId, status)
                .onSuccess { showToast("মতামত আপডেট হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "আপডেট ব্যর্থ") }
        }
    }

    fun saveDuty(title: String, desc: String, assignedTo: String, dueOn: String) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.saveDuty(VolunteerDuty(userId = uid, title = title, description = desc, assignedToName = assignedTo, dueOn = dueOn, status = "todo"))
                .onSuccess { showToast("স্বেচ্ছাসেবক দায়িত্ব যুক্ত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "দায়িত্ব সংরক্ষণ ব্যর্থ") }
        }
    }

    fun updateDutyStatus(dutyId: String, status: String) {
        viewModelScope.launch { repo.updateDutyStatus(dutyId, status) }
    }

    fun submitPublicRequest(req: PublicRequest) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            repo.submitPublicRequest(req.copy(userId = uid))
                .onSuccess { showToast("আপনার আবেদন নিরাপদভাবে জমা হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "আবেদন জমা ব্যর্থ") }
        }
    }

    fun updatePublicRequestStatus(reqId: String, status: String) {
        viewModelScope.launch {
            repo.updatePublicRequestStatus(reqId, status)
                .onSuccess { showToast("আবেদনের অবস্থা আপডেট হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "আপডেট ব্যর্থ") }
        }
    }

    fun updateFoundationSettings(updated: FoundationSettings) {
        viewModelScope.launch {
            repo.updateFoundationSettings(updated)
                .onSuccess { showToast("সংগঠনের সেটিংস সংরক্ষিত হয়েছে") }
                .onFailure { showToast(it.localizedMessage ?: "সেটিংস সংরক্ষণ ব্যর্থ") }
        }
    }

    // --- Manobseba AI Hub ---
    fun selectAiSession(chatId: String) {
        val uid = currentUserId().ifBlank { return }
        _activeAiChatId.value = chatId
        _chatPendingBubbles.value = emptyList()
        aiMsgJob?.cancel()
        aiMsgJob = viewModelScope.launch {
            repo.observeAiMessages(uid, chatId).collect { _aiMessages.value = it }
        }
    }

    fun startNewAiSession() {
        _activeAiChatId.value = null
        _aiMessages.value = emptyList()
        _chatPendingBubbles.value = emptyList()
        _aiChatStatus.value = AiTaskState.Idle
    }

    fun sendAiMessage(userText: String, bitmap: Bitmap? = null) {
        val uid = currentUserId().ifBlank { return }
        if (userText.isBlank() && bitmap == null) return
        val prompt = userText.ifBlank { "এই ছবিটি বিশ্লেষণ করে মানবসেবা ফাউন্ডেশনের জন্য প্রাসঙ্গিক তথ্য দিন।" }
        val userBubble = LocalChatBubble("u_${System.currentTimeMillis()}", "user", prompt)
        _chatPendingBubbles.value = _chatPendingBubbles.value + userBubble
        _aiChatStatus.value = AiTaskState.Loading

        viewModelScope.launch {
            try {
                var chatId = _activeAiChatId.value
                if (chatId == null) {
                    chatId = repo.createAiSession(uid, prompt.take(60)).getOrNull()
                    if (chatId != null) {
                        _activeAiChatId.value = chatId
                        selectAiSession(chatId)
                    }
                }
                if (chatId != null) {
                    repo.addAiMessage(uid, chatId, "user", prompt)
                }

                val reply = aiRepo.sendStructuredChat(prompt, buildFoundationAiContext(), bitmap)
                val assistantBubble = LocalChatBubble(
                    id = "a_${System.currentTimeMillis()}",
                    sender = "assistant",
                    text = reply.replyText,
                    proposal = reply.proposal
                )
                _chatPendingBubbles.value = _chatPendingBubbles.value + assistantBubble
                if (chatId != null) {
                    repo.addAiMessage(uid, chatId, "assistant", reply.replyText, reply.proposal?.actionType)
                }
                _aiChatStatus.value = AiTaskState.Success(reply.replyText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiChatStatus.value = AiTaskState.Error(e.localizedMessage ?: "মানবসেবা AI থেকে উত্তর আনা যায়নি।")
            }
        }
    }

    fun confirmAiProposal(bubbleId: String, proposal: AiActionProposal) {
        val uid = currentUserId().ifBlank { return }
        val today = LocalDate.now()
        val matchingFund = _funds.value.firstOrNull {
            proposal.fundName.isNotBlank() && it.name.contains(proposal.fundName)
        } ?: _funds.value.firstOrNull()
        val fundId = matchingFund?.id ?: "f1"

        viewModelScope.launch {
            val res = when (proposal.actionType) {
                "record_donation", "record_expense", "record_fee" -> {
                    val kind = when (proposal.actionType) {
                        "record_expense" -> "expense"
                        "record_fee" -> "fee"
                        else -> "donation"
                    }
                    repo.saveTransaction(
                        FoundationTransaction(
                            userId = uid,
                            kind = kind,
                            amount = proposal.amount.coerceAtLeast(1.0),
                            fundId = fundId,
                            donorName = proposal.donorOrMemberName.ifBlank { currentDisplayName() },
                            method = proposal.method.ifBlank { "হাতে হাতে" },
                            category = proposal.category.ifBlank { proposal.title },
                            description = proposal.description.ifBlank { proposal.summary },
                            dateStr = today.toString(),
                            year = today.year,
                            month = today.monthValue,
                            receiptNo = if (kind == "donation") "AI-${System.currentTimeMillis() % 100000}" else null,
                            voucherNo = if (kind == "expense") "AV-${System.currentTimeMillis() % 100000}" else null,
                            status = if (kind == "fee") "paid" else null
                        )
                    )
                }
                "draft_notice" -> {
                    repo.saveNotice(
                        Notice(
                            userId = uid,
                            title = proposal.title.ifBlank { "অফিসিয়াল নোটিশ" },
                            body = proposal.description.ifBlank { proposal.summary },
                            urgent = proposal.urgent
                        )
                    )
                }
                "draft_project" -> {
                    repo.saveProject(
                        Project(
                            userId = uid,
                            title = proposal.title.ifBlank { "নতুন প্রকল্প" },
                            category = proposal.category.ifBlank { "relief" },
                            description = proposal.description.ifBlank { proposal.summary },
                            goal = proposal.amount.coerceAtLeast(10_000.0),
                            fundId = fundId,
                            status = "active"
                        )
                    )
                }
                else -> Result.failure(IllegalArgumentException("অজানা অ্যাকশন"))
            }

            res.onSuccess {
                _chatPendingBubbles.value = _chatPendingBubbles.value.map {
                    if (it.id == bubbleId) it.copy(executed = true) else it
                }
                showToast("AI প্রস্তাব ডাটাবেসে সংরক্ষিত হয়েছে")
            }.onFailure {
                showToast(it.localizedMessage ?: "প্রস্তাব সংরক্ষণ ব্যর্থ")
            }
        }
    }

    fun generateExecutiveHealthSummary() {
        _aiHealthSummary.value = AiTaskState.Loading
        val sb = StringBuilder()
        viewModelScope.launch {
            try {
                withTimeout(60_000L) {
                    aiRepo.streamFoundationSummary(buildFoundationAiContext()).collect { chunk ->
                        sb.append(chunk)
                        _aiHealthSummary.value = AiTaskState.Streaming(sb.toString())
                    }
                }
                val finalStr = sb.toString().trim()
                _aiHealthSummary.value = if (finalStr.isNotBlank()) {
                    AiTaskState.Success(finalStr)
                } else {
                    AiTaskState.Error("AI কোনো সারসংক্ষেপ তৈরি করেনি।")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiHealthSummary.value = AiTaskState.Error(e.localizedMessage ?: "AI বিশ্লেষণ ব্যর্থ হয়েছে।")
            }
        }
    }

    fun analyzeLedgerPhoto(bitmap: Bitmap) {
        _ledgerScanState.value = AiTaskState.Loading
        viewModelScope.launch {
            try {
                val mText = _members.value.joinToString(", ") { "${it.fullName} (${it.memberCode})" }
                val fText = _funds.value.joinToString(", ") { it.name }
                val parsed = aiRepo.analyzeHandwrittenLedger(bitmap, mText, fText)
                _ledgerScanState.value = AiTaskState.Success(parsed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ledgerScanState.value = AiTaskState.Error(e.localizedMessage ?: "খাতার ছবি বিশ্লেষণ ব্যর্থ হয়েছে।")
            }
        }
    }

    fun commitScannedLedgerEntries(entries: List<ParsedLedgerEntry>) {
        val uid = currentUserId().ifBlank { return }
        val today = LocalDate.now()
        viewModelScope.launch {
            var okCount = 0
            entries.filter { it.amount > 0 }.forEach { entry ->
                val fundId = _funds.value.firstOrNull {
                    entry.fundName.isNotBlank() && it.name.contains(entry.fundName)
                }?.id ?: _funds.value.firstOrNull()?.id ?: "f1"
                val memberId = _members.value.firstOrNull {
                    it.memberCode == entry.memberCode || it.fullName.contains(entry.personName)
                }?.id
                val tx = FoundationTransaction(
                    userId = uid,
                    kind = entry.entryKind.takeIf { it in listOf("donation", "expense", "fee") } ?: "fee",
                    amount = entry.amount,
                    fundId = fundId,
                    memberId = memberId,
                    donorName = entry.personName.ifBlank { "সদস্য" },
                    method = "হাতে লেখা খাতা (AI)",
                    description = entry.note.ifBlank { "হাতে লেখা খাতা থেকে সংরক্ষিত" },
                    dateStr = entry.dateStr.takeIf { it.length >= 8 } ?: today.toString(),
                    year = today.year,
                    month = today.monthValue,
                    status = if (entry.entryKind == "fee") "paid" else null
                )
                if (repo.saveTransaction(tx).isSuccess) okCount++
            }
            showToast("${PrayerCalculator.bnDigits(okCount)}টি খাতার এন্ট্রি সংরক্ষিত হয়েছে")
            _ledgerScanState.value = AiTaskState.Idle
        }
    }

    fun parseLegacyData(rawText: String, bitmap: Bitmap?) {
        _legacyParseState.value = AiTaskState.Loading
        viewModelScope.launch {
            try {
                val res = aiRepo.parseLegacyMembers(rawText, bitmap)
                _legacyParseState.value = AiTaskState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _legacyParseState.value = AiTaskState.Error(e.localizedMessage ?: "পুরোনো তথ্য পার্স করা যায়নি।")
            }
        }
    }

    fun commitLegacyMembers(res: ParsedLegacyResult) {
        val uid = currentUserId().ifBlank { return }
        viewModelScope.launch {
            var count = 0
            val today = LocalDate.now().toString()
            val defaultFundId = _funds.value.firstOrNull()?.id ?: "f1"
            res.members.forEachIndexed { idx, lm ->
                val code = lm.memberCode.ifBlank { "MSF-L${100 + idx}" }
                val member = Member(
                    userId = uid,
                    fullName = lm.fullName.ifBlank { "পুরোনো সদস্য" },
                    phone = lm.phone.takeIf { it.isNotBlank() },
                    district = lm.district.takeIf { it.isNotBlank() },
                    bloodGroup = lm.bloodGroup.takeIf { it.isNotBlank() },
                    role = "member",
                    roleLabel = lm.roleLabel.ifBlank { "সদস্য" },
                    memberCode = code,
                    active = true,
                    joinedDate = today
                )
                if (repo.saveMember(member, isEdit = false).isSuccess) {
                    count++
                    if (lm.openingBalance > 0) {
                        repo.saveTransaction(
                            FoundationTransaction(
                                userId = uid,
                                kind = "donation",
                                amount = lm.openingBalance,
                                fundId = defaultFundId,
                                donorName = lm.fullName,
                                method = "পূর্বের জমা (লিগ্যাসি)",
                                description = "পূর্বের সদস্য জের আমদানি",
                                dateStr = today,
                                year = YearMonth.now().year,
                                month = YearMonth.now().monthValue
                            )
                        )
                    }
                }
            }
            showToast("${PrayerCalculator.bnDigits(count)} জন সদস্য ও পূর্বের জের সংরক্ষিত হয়েছে")
            _legacyParseState.value = AiTaskState.Idle
        }
    }

    private fun buildFoundationAiContext(): String {
        val txs = _transactions.value
        val income = txs.filter { it.kind != "expense" }.sumOf { it.amount }
        val expense = txs.filter { it.kind == "expense" }.sumOf { it.amount }
        val activeMembers = _members.value.count { it.active }
        val activeBlood = _bloodRequests.value.count { !it.managed }
        val fundNames = _funds.value.joinToString(", ") { it.name }
        return """
            সংগঠন: ${_settings.value?.orgName ?: "মানবসেবা ফাউন্ডেশন"}
            মোট আয় (অনুদান+চাঁদা): ${income.toLong()} টাকা
            মোট ব্যয়: ${expense.toLong()} টাকা
            বর্তমান স্থিতি: ${(income - expense).toLong()} টাকা
            সক্রিয় সদস্য: $activeMembers জন, চলমান প্রকল্প: ${_projects.value.size}টি, জরুরি রক্তের আবেদন: ${activeBlood}টি
            তহবিল খাতসমূহ: $fundNames
        """.trimIndent()
    }

    // --- Weather, Location & Islamic ---
    fun selectDistrict(districtName: String) {
        _selectedDistrict.value = districtName
        localPrefs.edit().putString("selected_district", districtName).apply()
        refreshWeather(force = true)
    }

    fun setAsrSchool(factor: Int) {
        _asrFactor.value = factor
        localPrefs.edit().putInt("asr_factor", factor).apply()
    }

    fun refreshWeather(force: Boolean = true) {
        val d = DistrictsData.findByName(_selectedDistrict.value)
        viewModelScope.launch {
            _weatherBusy.value = true
            _weatherError.value = null
            extClient.fetchWeather(d.name, d.lat, d.lng, forceRefresh = force)
                .onSuccess { _weather.value = it }
                .onFailure { _weatherError.value = "আবহাওয়ার লাইভ তথ্য আনা যায়নি; সর্বশেষ সংরক্ষিত তথ্য দেখানো হচ্ছে।" }
            _weatherBusy.value = false
        }
    }

    fun loadSurah(surahNumber: Int) {
        viewModelScope.launch {
            _surahBusy.value = true
            extClient.fetchSurah(surahNumber)
                .onSuccess { _surahItems.value = it }
                .onFailure { showToast("সূরা লোড করা যায়নি, ইন্টারনেট সংযোগ পরীক্ষা করুন") }
            _surahBusy.value = false
        }
    }

    fun getCachedHadithBook(bookId: String): HadithBookOffline? = extClient.getCachedHadithBook(bookId)

    fun downloadHadithBook(bookId: String) {
        viewModelScope.launch {
            _hadithBusyId.value = bookId
            extClient.downloadHadithBook(bookId)
                .onSuccess {
                    _savedHadithBooks.value = extClient.getSavedHadithBookIds()
                    showToast("হাদিস কিতাব অফলাইনে সেভ হয়েছে")
                }
                .onFailure { showToast("কিতাব ডাউনলোড ব্যর্থ হয়েছে") }
            _hadithBusyId.value = null
        }
    }

    fun removeHadithBook(bookId: String) {
        extClient.deleteHadithBook(bookId)
        _savedHadithBooks.value = extClient.getSavedHadithBookIds()
    }

    fun toggleSalatPrayer(dayKey: String, prayerKey: String) {
        val uid = currentUserId().ifBlank { return }
        val cur = _salatLogs.value.firstOrNull { it.dateKey == dayKey }
        val prayers = cur?.prayers.orEmpty()
        val next = if (prayerKey in prayers) prayers - prayerKey else prayers + prayerKey
        viewModelScope.launch {
            repo.saveSalatAmol(uid, dayKey, next, cur?.amolDone.orEmpty(), cur?.tasbihTotal ?: _tasbihTotal.value)
        }
    }

    fun toggleAmolItem(dayKey: String, item: String) {
        val uid = currentUserId().ifBlank { return }
        val cur = _salatLogs.value.firstOrNull { it.dateKey == dayKey }
        val done = cur?.amolDone.orEmpty()
        val next = if (item in done) done - item else done + item
        viewModelScope.launch {
            repo.saveSalatAmol(uid, dayKey, cur?.prayers.orEmpty(), next, cur?.tasbihTotal ?: _tasbihTotal.value)
        }
    }

    fun addCustomAmolItem(item: String) {
        val clean = item.trim()
        if (clean.isBlank() || clean in _customAmolItems.value) return
        val next = _customAmolItems.value + clean
        _customAmolItems.value = next
        localPrefs.edit().putString("custom_amol", next.joinToString("||")).apply()
    }

    fun tapTasbih(): Boolean {
        val target = _tasbihTarget.value
        val nextCount = _tasbihCount.value + 1
        val nextTotal = _tasbihTotal.value + 1
        _tasbihTotal.value = nextTotal
        var completedRound = false
        if (target > 0 && nextCount >= target) {
            _tasbihCount.value = 0
            _tasbihRounds.value = _tasbihRounds.value + 1
            completedRound = true
        } else {
            _tasbihCount.value = nextCount
        }
        localPrefs.edit()
            .putInt("tasbih_cur", _tasbihCount.value)
            .putInt("tasbih_rounds", _tasbihRounds.value)
            .putInt("tasbih_total", _tasbihTotal.value)
            .apply()

        val uid = currentUserId()
        if (uid.isNotBlank() && (_tasbihTotal.value % 10 == 0 || completedRound)) {
            val todayKey = LocalDate.now().toString()
            val cur = _salatLogs.value.firstOrNull { it.dateKey == todayKey }
            viewModelScope.launch {
                repo.saveSalatAmol(uid, todayKey, cur?.prayers.orEmpty(), cur?.amolDone.orEmpty(), _tasbihTotal.value)
            }
        }
        return completedRound
    }

    fun setTasbihTarget(target: Int) {
        _tasbihTarget.value = target
        localPrefs.edit().putInt("tasbih_target", target).apply()
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
        _tasbihRounds.value = 0
        localPrefs.edit().putInt("tasbih_cur", 0).putInt("tasbih_rounds", 0).apply()
    }
}
