package com.manobseba.foundation.data.remote

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.min

private const val TAG = "AiRepository"

@Serializable
data class AiActionProposal(
    val actionType: String = "", // record_donation | record_expense | record_fee | draft_notice | draft_project
    val title: String = "",
    val summary: String = "",
    val amount: Double = 0.0,
    val fundName: String = "",
    val donorOrMemberName: String = "",
    val category: String = "",
    val method: String = "হাতে হাতে",
    val description: String = "",
    val urgent: Boolean = false
)

@Serializable
data class AiChatStructuredReply(
    val replyText: String = "",
    val proposal: AiActionProposal? = null
)

@Serializable
data class ParsedLedgerEntry(
    val entryKind: String = "fee", // fee | donation | expense
    val personName: String = "",
    val memberCode: String = "",
    val amount: Double = 0.0,
    val fundName: String = "",
    val dateStr: String = "",
    val note: String = "",
    val confidence: String = "high"
)

@Serializable
data class ParsedLedgerResult(
    val summary: String = "",
    val items: List<ParsedLedgerEntry> = emptyList()
)

@Serializable
data class ParsedLegacyMember(
    val fullName: String = "",
    val phone: String = "",
    val district: String = "",
    val bloodGroup: String = "",
    val memberCode: String = "",
    val roleLabel: String = "সদস্য",
    val openingBalance: Double = 0.0
)

@Serializable
data class ParsedLegacyResult(
    val summary: String = "",
    val members: List<ParsedLegacyMember> = emptyList()
)

class AiRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val streamChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "আপনি 'মানবসেবা AI' — মানবসেবা ফাউন্ডেশনের দাপ্তরিক, আর্থিক ও মানবিক সহকারী। " +
                    "সবসময় স্পষ্ট, বিনয়ী এবং নির্ভুল বাংলায় উত্তর দিন। প্রদত্ত ফাউন্ডেশন পরিসংখ্যান ছাড়া কোনো সংখ্যা অনুমান করবেন না।"
                )
            }
        )
    }

    private val chatActionModel by lazy {
        val actionSchema = Schema.obj(
            properties = mapOf(
                "actionType" to Schema.enumeration(
                    listOf("record_donation", "record_expense", "record_fee", "draft_notice", "draft_project")
                ),
                "title" to Schema.string("সংক্ষিপ্ত শিরোনাম"),
                "summary" to Schema.string("এক লাইনের সারাংশ"),
                "amount" to Schema.double("টাকার পরিমাণ (যদি প্রযোজ্য হয়)"),
                "fundName" to Schema.string("তহবিলের নাম"),
                "donorOrMemberName" to Schema.string("দাতা বা সদস্যের নাম"),
                "category" to Schema.string("ক্যাটাগরি"),
                "method" to Schema.string("পেমেন্ট মাধ্যম যেমন bKash বা হাতে হাতে"),
                "description" to Schema.string("বিস্তারিত বিবরণ"),
                "urgent" to Schema.boolean("জরুরি কিনা")
            ),
            optionalProperties = listOf("amount", "fundName", "donorOrMemberName", "category", "method", "description", "urgent")
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "replyText" to Schema.string("ব্যবহারকারীর প্রশ্নের বিস্তারিত ও বিনয়ী বাংলা উত্তর"),
                "proposal" to actionSchema
            ),
            optionalProperties = listOf("proposal")
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            },
            systemInstruction = content {
                text(
                    "আপনি 'মানবসেবা AI' — মানবসেবা ফাউন্ডেশনের বুদ্ধিমান সহকারী। " +
                    "ব্যবহারকারী যদি কোনো অনুদান, মাসিক চাঁদা, খরচ, নতুন নোটিশ বা প্রকল্প যুক্ত করতে বলে বা ছবি দেয়, তাহলে 'proposal' অবজেক্ট পূরণ করুন। " +
                    "অন্যথায় 'proposal' বাদ দিন এবং শুধু 'replyText'-এ সঠিক বাংলা উত্তর দিন।"
                )
            }
        )
    }

    private val visionLedgerModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string("হাতে লেখা খাতার পৃষ্ঠার সংক্ষিপ্ত বাংলা বিবরণ"),
                "items" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "entryKind" to Schema.enumeration(listOf("fee", "donation", "expense")),
                            "personName" to Schema.string("ব্যক্তি বা দাতার নাম"),
                            "memberCode" to Schema.string("সদস্য কোড যদি থাকে"),
                            "amount" to Schema.double("টাকার পরিমাণ"),
                            "fundName" to Schema.string("তহবিল বা খাতের নাম"),
                            "dateStr" to Schema.string("তারিখ YYYY-MM-DD ফরম্যাটে"),
                            "note" to Schema.string("অতিরিক্ত মন্তব্য বা রসিদ নম্বর"),
                            "confidence" to Schema.enumeration(listOf("high", "medium", "low"))
                        ),
                        optionalProperties = listOf("memberCode", "fundName", "dateStr", "note")
                    )
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val legacyImportModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string("শনাক্তকৃত সদস্য তালিকার সংক্ষিপ্ত বিবরণ"),
                "members" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "fullName" to Schema.string("পূর্ণ নাম"),
                            "phone" to Schema.string("ফোন নম্বর"),
                            "district" to Schema.string("জেলা"),
                            "bloodGroup" to Schema.string("রক্তের গ্রুপ যেমন O+"),
                            "memberCode" to Schema.string("সদস্য কোড"),
                            "roleLabel" to Schema.string("পদবী"),
                            "openingBalance" to Schema.double("পূর্বের মোট জমা টাকা")
                        ),
                        optionalProperties = listOf("phone", "district", "bloodGroup", "memberCode", "roleLabel", "openingBalance")
                    )
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun streamFoundationSummary(contextSummary: String): Flow<String> {
        val prompt = """
            নিচের মানবসেবা ফাউন্ডেশনের বর্তমান আর্থিক ও মানবিক তথ্যের ভিত্তিতে ২টি সংক্ষিপ্ত, অনুপ্রেরণাদায়ক ও তথ্যবহুল বাংলা বাক্যে সারসংক্ষেপ লিখুন:
            $contextSummary
        """.trimIndent()
        return streamChatModel.generateContentStream(prompt).map { it.text.orEmpty() }
    }

    suspend fun sendStructuredChat(
        userPrompt: String,
        foundationContext: String,
        attachedImage: Bitmap? = null
    ): AiChatStructuredReply {
        return try {
            val scaled = attachedImage?.let { shrinkForUpload(it) }
            val input = content {
                text("ফাউন্ডেশন প্রেক্ষাপট:\n$foundationContext\n\nব্যবহারকারীর বার্তা:\n$userPrompt")
                if (scaled != null) image(scaled)
            }
            val response = withTimeout(60_000L) {
                chatActionModel.generateContent(input)
            }
            val raw = response.text ?: error("AI কোনো উত্তর দেয়নি।")
            json.decodeFromString<AiChatStructuredReply>(raw)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat timed out", e)
            throw IllegalStateException("AI অনুরোধে সময় বেশি লেগেছে (৬০ সেকেন্ড)। আবার চেষ্টা করুন।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat failed", e)
            throw e
        }
    }

    suspend fun analyzeHandwrittenLedger(
        bitmap: Bitmap,
        knownMembersText: String,
        knownFundsText: String
    ): ParsedLedgerResult {
        return try {
            val scaled = shrinkForUpload(bitmap)
            val prompt = """
                এই হাতে লেখা বা ছাপা খাতার ছবিটি মনোযোগ দিয়ে পড়ুন। বাংলা ও ইংরেজি অঙ্ক (০-৯ / 0-9) সঠিকভাবে রূপান্তর করে প্রতিটি আয়, মাসিক চাঁদা বা খরচের এন্ট্রি আলাদা করুন।
                পরিচিত সদস্য: $knownMembersText
                পরিচিত তহবিল খাত: $knownFundsText
                তারিখ না থাকলে আজকের তারিখ দিন।
            """.trimIndent()
            val response = withTimeout(60_000L) {
                visionLedgerModel.generateContent(
                    content {
                        text(prompt)
                        image(scaled)
                    }
                )
            }
            val raw = response.text ?: error("খাতার ছবি বিশ্লেষণ ব্যর্থ হয়েছে।")
            json.decodeFromString<ParsedLedgerResult>(raw)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vision ledger timed out", e)
            throw IllegalStateException("খাতা বিশ্লেষণে সময় শেষ হয়েছে। ছোট বা স্পষ্ট ছবি দিয়ে আবার চেষ্টা করুন।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vision ledger failed", e)
            throw e
        }
    }

    suspend fun parseLegacyMembers(
        rawText: String,
        bitmap: Bitmap? = null
    ): ParsedLegacyResult {
        return try {
            val scaled = bitmap?.let { shrinkForUpload(it) }
            val input = content {
                text(
                    "নিচের পুরোনো সদস্য তালিকা বা ব্যালেন্স শীট থেকে প্রতিটি সদস্যের নাম, ফোন, জেলা, রক্তের গ্রুপ, কোড, পদবী ও পূর্বের জমা টাকা আলাদা করুন:\n$rawText"
                )
                if (scaled != null) image(scaled)
            }
            val response = withTimeout(60_000L) {
                legacyImportModel.generateContent(input)
            }
            val raw = response.text ?: error("তথ্য পার্স করা যায়নি।")
            json.decodeFromString<ParsedLegacyResult>(raw)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Legacy parse timed out", e)
            throw IllegalStateException("তথ্য বিশ্লেষণে সময় শেষ হয়েছে।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Legacy parse failed", e)
            throw e
        }
    }

    private fun shrinkForUpload(bitmap: Bitmap, maxDim: Int = 1600): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = min(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
