package com.manobseba.foundation.data

import com.manobseba.foundation.data.model.BloodDonor
import com.manobseba.foundation.data.model.BloodRequest
import com.manobseba.foundation.data.model.FieldReport
import com.manobseba.foundation.data.model.FoundationEvent
import com.manobseba.foundation.data.model.FoundationSettings
import com.manobseba.foundation.data.model.FoundationTransaction
import com.manobseba.foundation.data.model.Fund
import com.manobseba.foundation.data.model.Member
import com.manobseba.foundation.data.model.Notice
import com.manobseba.foundation.data.model.Poll
import com.manobseba.foundation.data.model.Project
import com.manobseba.foundation.data.model.ReliefLocation
import com.manobseba.foundation.data.model.VoiceTestimonial
import com.manobseba.foundation.data.model.VolunteerDuty
import java.time.LocalDate
import java.time.YearMonth

data class RoleDef(
    val key: String,
    val label: String,
    val rank: Int,
    val perms: List<String>,
    val showOnHome: Boolean,
    val duties: List<String>
)

data class HadithBookMeta(
    val id: String,
    val name: String,
    val sub: String
)

object SeedCatalog {
    const val BOOTSTRAP_ADMIN_EMAIL = "taseenbillah@gmail.com"

    val ROLES = listOf(
        RoleDef("admin", "অ্যাডমিন", 1, listOf("*"), true, listOf("সম্পূর্ণ সিস্টেম ও ভূমিকা ব্যবস্থাপনা", "ব্যবহারকারী ও আবেদন অনুমোদন", "নিরাপত্তা ও অডিট পর্যবেক্ষণ")),
        RoleDef("president", "সভাপতি", 2, listOf("manage_members", "post_notices", "verify_reports"), true, listOf("কার্যনির্বাহী সভায় সভাপতিত্ব", "কৌশলগত প্রকল্প ও নীতি অনুমোদন", "জরুরি ত্রাণ সমন্বয়")),
        RoleDef("general_secretary", "সাধারণ সম্পাদক", 3, listOf("manage_members", "post_notices", "verify_reports"), true, listOf("সদস্য ও স্বেচ্ছাসেবক সমন্বয়", "অফিসিয়াল নোটিশ ও সভার কার্যবিবরণী", "মাঠ পর্যায়ের রিপোর্ট যাচাই")),
        RoleDef("cashier", "কোষাধ্যক্ষ", 4, listOf("edit_finance"), true, listOf("অনুদান, মাসিক চাঁদা ও ব্যয়ের স্বচ্ছ হিসাব", "রসিদ ও ভাউচার যাচাই", "মাসিক আর্থিক প্রতিবেদন প্রস্তুত")),
        RoleDef("health", "স্বাস্থ্য ও রক্তদান সমন্বয়ক", 5, listOf("manage_blood"), true, listOf("২৪ ঘণ্টার জরুরি রক্তের আবেদন পরিচালনা", "স্বেচ্ছাসেবক রক্তদাতা তালিকা হালনাগাদ", "ফ্রি মেডিকেল ক্যাম্প সমন্বয়")),
        RoleDef("sports", "ক্রীড়া ও সাংস্কৃতিক সম্পাদক", 6, emptyList(), true, listOf("যুব স্বেচ্ছাসেবক সমাবেশ ও ইভেন্ট আয়োজন", "মানবিক প্রচারণা ও প্রদর্শনী")),
        RoleDef("publicity", "প্রচার ও প্রকাশনা সম্পাদক", 7, listOf("post_notices"), true, listOf("সংগঠনের বার্তা ও কার্যক্রম প্রচার", "নোটিশ ও প্রামাণ্যচিত্র প্রকাশ")),
        RoleDef("member", "সদস্য", 100, emptyList(), false, listOf("নিয়মিত মাসিক চাঁদা প্রদান", "ত্রাণ ও রক্তদান কার্যক্রমে অংশগ্রহণ"))
    )

    fun roleLabel(key: String): String = ROLES.firstOrNull { it.key == key }?.label ?: key

    fun hasPerm(role: String?, active: Boolean, perm: String): Boolean {
        if (!active || role.isNullOrBlank()) return false
        val def = ROLES.firstOrNull { it.key == role } ?: return false
        return "*" in def.perms || perm in def.perms
    }

    val DEFAULT_ZIKR = listOf(
        "সুবহানাল্লাহ" to "سُبْحَانَ اللّٰهِ",
        "আলহামদুলিল্লাহ" to "الْحَمْدُ لِلّٰهِ",
        "আল্লাহু আকবার" to "اللّٰهُ أَكْبَرُ",
        "লা ইলাহা ইল্লাল্লাহ" to "لَا إِلٰهَ إِلَّا اللّٰهُ",
        "আস্তাগফিরুল্লাহ" to "أَسْتَغْفِرُ اللّٰهَ",
        "দরুদ শরীফ" to "اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ"
    )

    val DEFAULT_AMOL_ITEMS = listOf(
        "ফজর নামাজ",
        "যোহর নামাজ",
        "আসর নামাজ",
        "মাগরিব নামাজ",
        "এশা নামাজ",
        "কুরআন তিলাওয়াত",
        "জিকির/তসবিহ",
        "দরুদ শরীফ",
        "সদকা/দান",
        "সকাল-সন্ধ্যার দোয়া",
        "মাতা-পিতার খেদমত"
    )

    val HADITH_BOOKS = listOf(
        HadithBookMeta("nawawi", "চল্লিশ হাদিস", "ইমাম নববী (রহ.)"),
        HadithBookMeta("bukhari", "সহীহ বুখারী", "ইমাম বুখারী (রহ.)"),
        HadithBookMeta("muslim", "সহীহ মুসলিম", "ইমাম মুসলিম (রহ.)"),
        HadithBookMeta("abudawud", "সুনানে আবু দাউদ", "ইমাম আবু দাউদ (রহ.)"),
        HadithBookMeta("tirmidhi", "জামে আত-তিরমিযী", "ইমাম তিরমিযী (রহ.)"),
        HadithBookMeta("nasai", "সুনানে নাসায়ী", "ইমাম নাসায়ী (রহ.)"),
        HadithBookMeta("ibnmajah", "সুনানে ইবনে মাজাহ", "ইমাম ইবনে মাজাহ (রহ.)"),
        HadithBookMeta("malik", "মুয়াত্তা মালিক", "ইমাম মালিক (রহ.)")
    )

    fun defaultSettings(uid: String) = FoundationSettings(
        id = "main",
        userId = uid,
        visibility = "foundation",
        orgName = "মানবসেবা ফাউন্ডেশন",
        slogan = "মানুষের পাশে, মানবতার পাশে",
        hotline = "01700-000000",
        bankDetails = "বিকাশ / নগদ: 01700-000000\nব্যাংক অ্যাকাউন্ট: মানবসেবা ফাউন্ডেশন",
        goalTitle = "তহবিল সংগ্রহের লক্ষ্যমাত্রা",
        targetAmount = 200000.0,
        collectedAmount = 120000.0,
        monthlyFee = 200.0,
        joinCode = "MSF2026",
        disabledFeatures = emptyList()
    )

    fun seedFunds(uid: String) = listOf(
        Fund(id = "f1", userId = uid, name = "বন্যা ও জরুরি ত্রাণ", code = "FLOOD", description = "বন্যা ও প্রাকৃতিক দুর্যোগে জরুরি খাদ্য ও আশ্রয় সহায়তা", budget = 150000.0),
        Fund(id = "f2", userId = uid, name = "শীতবস্ত্র বিতরণ", code = "WINTER", description = "উত্তরাঞ্চল ও শীতার্ত মানুষের মাঝে কম্বল ও শীতবস্ত্র বিতরণ", budget = 80000.0),
        Fund(id = "f3", userId = uid, name = "শিক্ষা ও এতিম সহায়তা", code = "EDU", description = "সুবিধাবঞ্চিত শিক্ষার্থী ও এতিমদের বই, খাতা ও বৃত্তি", budget = 90000.0),
        Fund(id = "f4", userId = uid, name = "জরুরি চিকিৎসা", code = "MED", description = "অসহায় রোগীদের ওষুধ, অপারেশন ও রক্তদান সহায়তা", budget = 100000.0)
    )

    fun seedMembers(uid: String) = listOf(
        Member(id = "m1", userId = uid, fullName = "আব্দুর রহমান", phone = "01711000101", district = "সাতক্ষীরা", bloodGroup = "O+", role = "president", roleLabel = "সভাপতি", rank = 2, memberCode = "MSF-001", active = true, joinedDate = "2024-01-10"),
        Member(id = "m2", userId = uid, fullName = "মাহমুদুল হাসান", phone = "01811000202", district = "সাতক্ষীরা", bloodGroup = "O+", role = "general_secretary", roleLabel = "সাধারণ সম্পাদক", rank = 3, memberCode = "MSF-002", active = true, joinedDate = "2024-01-12"),
        Member(id = "m3", userId = uid, fullName = "তানভীর আহমেদ", phone = "01712000303", district = "ঢাকা", bloodGroup = "A+", role = "cashier", roleLabel = "কোষাধ্যক্ষ", rank = 4, memberCode = "MSF-003", active = true, joinedDate = "2024-02-01"),
        Member(id = "m4", userId = uid, fullName = "সাদিয়া ইসলাম", phone = "01911000303", district = "সাতক্ষীরা", bloodGroup = "B+", role = "health", roleLabel = "স্বাস্থ্য ও রক্তদান সমন্বয়ক", rank = 5, memberCode = "MSF-004", active = true, joinedDate = "2024-02-14"),
        Member(id = "m5", userId = uid, fullName = "রাশেদ করিম", phone = "01611000404", district = "খুলনা", bloodGroup = "AB+", role = "member", roleLabel = "সদস্য", rank = 100, memberCode = "MSF-005", active = true, joinedDate = "2024-03-05"),
        Member(id = "m6", userId = uid, fullName = "নাঈম হোসেন", phone = "01511000505", district = "সাতক্ষীরা", bloodGroup = "B-", role = "member", roleLabel = "সদস্য", rank = 100, memberCode = "MSF-006", active = false, joinedDate = "2024-04-20")
    )

    fun seedProjects(uid: String) = listOf(
        Project(id = "p1", userId = uid, title = "অসহায় পরিবারের জন্য খাদ্য ও ত্রাণ সহায়তা", category = "flood", area = "সিলেট ও সুনামগঞ্জ", description = "দুর্গত ও নিম্নআয়ের পরিবারের মাঝে চাল, ডাল, তেল ও শুকনো খাবার বিতরণ কার্যক্রম।", goal = 150000.0, fundId = "f1", status = "active"),
        Project(id = "p2", userId = uid, title = "সুবিধাবঞ্চিত শিক্ষার্থীদের শিক্ষা উপকরণ", category = "edu", area = "সারাদেশ", description = "দরিদ্র মেধাবী শিক্ষার্থীদের বই, খাতা, স্কুল ব্যাগ ও মাসিক শিক্ষা বৃত্তি প্রদান।", goal = 80000.0, fundId = "f3", status = "active")
    )

    fun seedTransactions(uid: String): List<FoundationTransaction> {
        val today = LocalDate.now()
        val ym = YearMonth.now()
        val ymStr = ym.toString()
        val m1 = ym.minusMonths(1).toString()
        val m2 = ym.minusMonths(2).toString()
        val m3 = ym.minusMonths(3).toString()
        return listOf(
            FoundationTransaction(id = "d1", userId = uid, kind = "donation", amount = 45000.0, fundId = "f1", donorName = "আব্দুর রহমান", method = "bKash", receiptNo = "MSF-1001", category = "সাধারণ অনুদান", dateStr = "$ymStr-03", year = today.year, month = today.monthValue),
            FoundationTransaction(id = "d2", userId = uid, kind = "donation", amount = 30000.0, fundId = "f3", donorName = "তানভীর আহমেদ", method = "ব্যাংক", receiptNo = "MSF-1002", category = "শিক্ষা অনুদান", dateStr = "$ymStr-06", year = today.year, month = today.monthValue),
            FoundationTransaction(id = "d3", userId = uid, kind = "donation", amount = 25000.0, fundId = "f2", donorName = "শুভাকাঙ্ক্ষী দাতা", method = "নগদ", receiptNo = "MSF-1003", category = "শীতবস্ত্র তহবিল", dateStr = "$m1-14", year = ym.minusMonths(1).year, month = ym.minusMonths(1).monthValue),
            FoundationTransaction(id = "d4", userId = uid, kind = "donation", amount = 20000.0, fundId = "f4", donorName = "ডা. ফয়সাল", method = "ব্যাংক", receiptNo = "MSF-1004", category = "চিকিৎসা তহবিল", dateStr = "$m2-19", year = ym.minusMonths(2).year, month = ym.minusMonths(2).monthValue),
            FoundationTransaction(id = "d5", userId = uid, kind = "donation", amount = 35000.0, fundId = "f1", donorName = "প্রবাসী কল্যাণ গ্রুপ", method = "ব্যাংক", receiptNo = "MSF-1005", category = "জরুরি ত্রাণ", dateStr = "$m3-10", year = ym.minusMonths(3).year, month = ym.minusMonths(3).monthValue),
            FoundationTransaction(id = "e1", userId = uid, kind = "expense", amount = 28000.0, fundId = "f1", voucherNo = "V-2001", category = "ত্রাণ", description = "১০০ পরিবারের মাঝে খাদ্যসামগ্রী বিতরণ", dateStr = "$ymStr-05", year = today.year, month = today.monthValue),
            FoundationTransaction(id = "e2", userId = uid, kind = "expense", amount = 14000.0, fundId = "f3", voucherNo = "V-2002", category = "শিক্ষা", description = "শিক্ষার্থীদের খাতা, বই ও কলম ক্রয়", dateStr = "$ymStr-08", year = today.year, month = today.monthValue),
            FoundationTransaction(id = "fee1", userId = uid, kind = "fee", amount = 200.0, fundId = "f1", memberId = "m1", donorName = "আব্দুর রহমান", method = "bKash", dateStr = "$ymStr-02", year = today.year, month = today.monthValue, status = "paid"),
            FoundationTransaction(id = "fee2", userId = uid, kind = "fee", amount = 200.0, fundId = "f1", memberId = "m2", donorName = "মাহমুদুল হাসান", method = "নগদ", dateStr = "$ymStr-04", year = today.year, month = today.monthValue, status = "paid"),
            FoundationTransaction(id = "fee3", userId = uid, kind = "fee", amount = 200.0, fundId = "f1", memberId = "m3", donorName = "তানভীর আহমেদ", method = "ব্যাংক", dateStr = "$ymStr-05", year = today.year, month = today.monthValue, status = "paid")
        )
    }

    fun seedReliefLocations(uid: String) = listOf(
        ReliefLocation(id = "r1", userId = uid, title = "সুনামগঞ্জ বন্যার্ত ত্রাণ ক্যাম্প", division = "সিলেট বিভাগ", district = "সুনামগঞ্জ", area = "সুনামগঞ্জ সদর ও তাহিরপুর", disasterType = "বন্যা", familiesHelped = 120, amountDistributed = 85000.0, distributedDate = LocalDate.now().minusDays(4).toString(), lat = 25.07, lng = 91.40),
        ReliefLocation(id = "r2", userId = uid, title = "কুড়িগ্রাম শীতবস্ত্র ও খাদ্য সহায়তা", division = "রংপুর বিভাগ", district = "কুড়িগ্রাম", area = "চিলমারী ও উলিপুর", disasterType = "শীত", familiesHelped = 85, amountDistributed = 52000.0, distributedDate = LocalDate.now().minusDays(12).toString(), lat = 25.81, lng = 89.65),
        ReliefLocation(id = "r3", userId = uid, title = "সাতক্ষীরা উপকূলীয় ত্রাণ ও চিকিৎসা ক্যাম্প", division = "খুলনা বিভাগ", district = "সাতক্ষীরা", area = "আশাশুনি ও শ্যামনগর", disasterType = "ঘূর্ণিঝড়", familiesHelped = 150, amountDistributed = 110000.0, distributedDate = LocalDate.now().minusDays(2).toString(), lat = 22.55, lng = 89.17)
    )

    fun seedFieldReports(uid: String) = listOf(
        FieldReport(id = "fr1", userId = uid, title = "১০০ অসহায় পরিবারের মাঝে খাদ্য প্যাকেট বিতরণ", category = "ত্রাণ", location = "সুনামগঞ্জ সদর", body = "সুনামগঞ্জ সদরের নদীভাঙন কবলিত ১০০ পরিবারের মাঝে চাল, ডাল, তেল ও জরুরি ওষুধ পৌঁছে দেওয়া হয়েছে।", spent = 28000.0, beneficiaries = 100, status = "verified", authorName = "আব্দুর রহমান", reactionCount = 14)
    )

    fun seedNotices(uid: String) = listOf(
        Notice(id = "n1", userId = uid, title = "মানবসেবা ফাউন্ডেশনের স্বেচ্ছাসেবী কার্যক্রম চলমান রয়েছে", body = "যেকোনো জরুরি প্রয়োজনে আমাদের হটলাইনে যোগাযোগ করুন অথবা দ্রুত আবেদন ফর্ম পূরণ করুন।", urgent = false, likeCount = 12, supportCount = 8)
    )

    fun seedBloodRequests(uid: String) = listOf(
        BloodRequest(id = "b1", userId = uid, bloodGroup = "O+", hospital = "ঢাকা মেডিকেল কলেজ হাসপাতাল", district = "ঢাকা", upazila = "শাহবাগ", contact = "01700000111", patientProblem = "জরুরি অপারেশনের জন্য ২ ব্যাগ রক্ত প্রয়োজন", neededByMillis = System.currentTimeMillis() + 6 * 3600_000L, urgent = true, applicantName = "কামরুল ইসলাম", managed = false)
    )

    fun seedBloodDonors(uid: String) = listOf(
        BloodDonor(id = "bd1", userId = uid, fullName = "আব্দুর রহমান", bloodGroup = "O+", district = "সাতক্ষীরা", upazila = "আশাশুনি", phone = "01711000101", whatsapp = "01711000101", available = true, totalDonations = 5, lastDonated = LocalDate.now().minusDays(120).toString()),
        BloodDonor(id = "bd2", userId = uid, fullName = "মাহমুদুল হাসান", bloodGroup = "O+", district = "ঢাকা", upazila = "মিরপুর", phone = "01811000202", available = true, totalDonations = 3, lastDonated = LocalDate.now().minusDays(150).toString()),
        BloodDonor(id = "bd3", userId = uid, fullName = "সাদিয়া ইসলাম", bloodGroup = "B+", district = "সাতক্ষীরা", upazila = "বাঁকড়া", phone = "01911000303", available = true, totalDonations = 2, lastDonated = LocalDate.now().minusDays(100).toString())
    )

    fun seedEvents(uid: String) = listOf(
        FoundationEvent(id = "ev1", userId = uid, title = "মাসিক কার্যনির্বাহী সভা ও ত্রাণ পরিকল্পনা", type = "meeting", eventDateMillis = System.currentTimeMillis() + 3 * 86400_000L, location = "কেন্দ্রীয় কার্যালয় ও অনলাইন", description = "আসন্ন মাসের ত্রাণ ও শিক্ষা সহায়তা বাজেট পর্যালোচনা।", attendeeUids = listOf(uid)),
        FoundationEvent(id = "ev2", userId = uid, title = "জরুরি রক্তদান ও স্বাস্থ্য ক্যাম্পেইন", type = "urgent", eventDateMillis = System.currentTimeMillis() + 7 * 86400_000L, location = "সাতক্ষীরা সদর হাসপাতাল প্রাঙ্গণ", description = "বিনামূল্যে রক্তের গ্রুপ নির্ণয় ও রক্তদাতা নিবন্ধন কর্মসূচি।", attendeeUids = emptyList())
    )

    fun seedPolls(uid: String) = listOf(
        Poll(id = "poll1", userId = uid, question = "আসন্ন ত্রাণ কার্যক্রমে কোন জেলাকে অগ্রাধিকার দেওয়া উচিত?", description = "সদস্যদের গোপন ভোটের মাধ্যমে অগ্রাধিকার এলাকা নির্ধারিত হবে।", options = listOf("সুনামগঞ্জ হাওর অঞ্চল", "কুড়িগ্রাম চরাঞ্চল", "সাতক্ষীরা উপকূল"), voteCounts = listOf(4, 3, 5), closed = false)
    )

    fun seedVoices(uid: String) = listOf(
        VoiceTestimonial(id = "v1", userId = uid, name = "ডা. আনোয়ার হোসেন", label = "দাতা ও শুভাকাঙ্ক্ষী", rating = 5, body = "মানবসেবা ফাউন্ডেশনের প্রতিটি টাকার স্বচ্ছ হিসাব দেখে আমরা অনুপ্রাণিত।", status = "approved"),
        VoiceTestimonial(id = "v2", userId = uid, name = "রোকসানা বেগম", label = "উপকারভোগী পরিবার", rating = 5, body = "বন্যার সময় আমাদের পরিবারে সবার আগে খাদ্যসামগ্রী পৌঁছে দিয়েছিলেন আপনারা।", status = "approved")
    )

    fun seedDuties(uid: String) = listOf(
        VolunteerDuty(id = "du1", userId = uid, title = "সুনামগঞ্জ ত্রাণ প্যাকেট তালিকা যাচাই", description = "১০০ পরিবারের এনআইডি ও খাদ্য প্যাকেট ডেলিভারি নিশ্চিত করা।", assignedToName = "মাহমুদুল হাসান", dueOn = LocalDate.now().plusDays(2).toString(), status = "doing"),
        VolunteerDuty(id = "du2", userId = uid, title = "রক্তদাতা ডিরেক্টরি ফোন যাচাই", description = "নতুন নিবন্ধিত ১০ জন রক্তদাতার প্রাপ্যতা যাচাই।", assignedToName = "সাদিয়া ইসলাম", dueOn = LocalDate.now().plusDays(4).toString(), status = "todo")
    )
}
