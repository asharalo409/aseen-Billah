package com.manobseba.foundation.auth

import android.content.Context
import android.util.Log
import com.manobseba.foundation.BuildConfig
import com.manobseba.foundation.data.SeedCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "SupabaseAuth"
private const val PREF_AUTH = "manobseba_supabase_auth"

data class AppUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val role: String = "member",
    val accessToken: String = ""
)

sealed interface SupabaseAuthResult {
    data class Success(val user: AppUser) : SupabaseAuthResult
    data class VerificationRequired(val message: String) : SupabaseAuthResult
    data class Error(val message: String) : SupabaseAuthResult
}

object SupabaseAuthHelper {
    fun baseUrl(): String = BuildConfig.VITE_SUPABASE_URL.trim().trimEnd('/')

    fun anonKey(): String = BuildConfig.VITE_SUPABASE_ANON_KEY.trim()

    fun isConfigured(): Boolean =
        baseUrl().startsWith("https://") && anonKey().isNotBlank()

    suspend fun restoreSession(context: Context): AppUser? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        val prefs = context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val access = prefs.getString("access_token", null).orEmpty()
        val refresh = prefs.getString("refresh_token", null).orEmpty()
        val uid = prefs.getString("uid", null).orEmpty()
        val email = prefs.getString("email", null)
        val displayName = prefs.getString("display_name", null)
        val expiresAtSec = prefs.getLong("expires_at", 0L)

        if (uid.isBlank() || access.isBlank()) return@withContext null

        val nowSec = System.currentTimeMillis() / 1000L
        if (expiresAtSec > nowSec + 120L) {
            return@withContext AppUser(
                uid = uid,
                email = email,
                displayName = displayName,
                role = defaultRoleForEmail(email),
                accessToken = access
            )
        }

        if (refresh.isNotBlank()) {
            val refreshed = refreshAccessToken(context, refresh)
            if (refreshed != null) return@withContext refreshed
        }

        clearSession(context)
        null
    }

    suspend fun getValidAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val access = prefs.getString("access_token", null).orEmpty()
        val refresh = prefs.getString("refresh_token", null).orEmpty()
        val expiresAtSec = prefs.getLong("expires_at", 0L)
        val nowSec = System.currentTimeMillis() / 1000L

        if (access.isNotBlank() && expiresAtSec > nowSec + 60L) {
            return@withContext access
        }
        if (refresh.isNotBlank()) {
            return@withContext refreshAccessToken(context, refresh)?.accessToken
        }
        null
    }

    suspend fun signInWithEmail(
        context: Context,
        email: String,
        password: String
    ): SupabaseAuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext SupabaseAuthResult.Error(
                "Supabase কনফিগারেশন (VITE_SUPABASE_URL ও VITE_SUPABASE_ANON_KEY) পাওয়া যায়নি।"
            )
        }
        try {
            val body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .toString()
            val (code, raw) = postAuth("/auth/v1/token?grant_type=password", body)
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (code in 200..299 && json != null) {
                val user = saveSessionFromJson(context, json)
                if (user != null) {
                    SupabaseAuthResult.Success(user)
                } else {
                    SupabaseAuthResult.Error("সেশন তথ্য পড়া যায়নি।")
                }
            } else {
                SupabaseAuthResult.Error(translateAuthError(json, raw))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in error", e)
            SupabaseAuthResult.Error("সাইন-ইন ব্যর্থ হয়েছে: ${e.localizedMessage ?: "নেটওয়ার্ক সমস্যা"}")
        }
    }

    suspend fun signUpWithEmail(
        context: Context,
        email: String,
        password: String,
        fullName: String,
        phone: String,
        joinCode: String
    ): SupabaseAuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext SupabaseAuthResult.Error(
                "Supabase কনফিগারেশন (VITE_SUPABASE_URL ও VITE_SUPABASE_ANON_KEY) পাওয়া যায়নি।"
            )
        }
        try {
            val cleanCode = joinCode.trim()
            if (cleanCode.isNotEmpty()) {
                val validCode = verifyJoinCodeIfConfigured(cleanCode)
                if (!validCode) {
                    return@withContext SupabaseAuthResult.Error("প্রদত্ত রেজিস্ট্রেশন কোডটি সঠিক নয়।")
                }
            }

            val meta = JSONObject()
                .put("full_name", fullName.trim().ifBlank { "সদস্য" })
                .put("phone", phone.trim())
                .put("registration_code", cleanCode)

            val body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", meta)
                .toString()

            val (code, raw) = postAuth("/auth/v1/signup", body)
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (code in 200..299 && json != null) {
                val access = json.optString("access_token")
                if (access.isNotBlank()) {
                    val user = saveSessionFromJson(context, json)
                    if (user != null) return@withContext SupabaseAuthResult.Success(user)
                }
                SupabaseAuthResult.VerificationRequired(
                    "নিবন্ধন সম্পন্ন হয়েছে! আপনার ইমেইলে পাঠানো ভেরিফিকেশন লিংক নিশ্চিত করে লগইন করুন।"
                )
            } else {
                SupabaseAuthResult.Error(translateAuthError(json, raw))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up error", e)
            SupabaseAuthResult.Error("নিবন্ধন ব্যর্থ হয়েছে: ${e.localizedMessage ?: "নেটওয়ার্ক সমস্যা"}")
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase কনফিগারেশন পাওয়া যায়নি।"))
        }
        runCatching {
            val body = JSONObject().put("email", email.trim()).toString()
            val (code, raw) = postAuth("/auth/v1/recover", body)
            if (code !in 200..299) {
                val json = runCatching { JSONObject(raw) }.getOrNull()
                error(translateAuthError(json, raw))
            }
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable + Dispatchers.IO) {
            val prefs = context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
            val access = prefs.getString("access_token", null).orEmpty()
            if (access.isNotBlank() && isConfigured()) {
                runCatching {
                    val conn = (URL("${baseUrl()}/auth/v1/logout").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        setRequestProperty("apikey", anonKey())
                        setRequestProperty("Authorization", "Bearer $access")
                        doOutput = true
                    }
                    try {
                        conn.outputStream.use { it.write(ByteArray(0)) }
                        conn.responseCode
                    } finally {
                        conn.disconnect()
                    }
                }
            }
            clearSession(context)
        }
    }

    private fun refreshAccessToken(context: Context, refreshToken: String): AppUser? {
        return try {
            val body = JSONObject().put("refresh_token", refreshToken).toString()
            val (code, raw) = postAuth("/auth/v1/token?grant_type=refresh_token", body)
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (code in 200..299 && json != null) {
                saveSessionFromJson(context, json)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Token refresh failed", e)
            null
        }
    }

    private fun saveSessionFromJson(context: Context, json: JSONObject): AppUser? {
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        val expiresIn = json.optLong("expires_in", 3600L)
        val userObj = json.optJSONObject("user") ?: return null
        val uid = userObj.optString("id")
        if (uid.isBlank() || access.isBlank()) return null

        val email = userObj.optString("email").takeIf { it.isNotBlank() }
        val meta = userObj.optJSONObject("user_metadata")
        val displayName = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
            ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")
            ?: "সদস্য"
        val role = meta?.optString("role")?.takeIf { it.isNotBlank() }
            ?: defaultRoleForEmail(email)

        val expiresAtSec = (System.currentTimeMillis() / 1000L) + expiresIn
        context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .putString("uid", uid)
            .putString("email", email)
            .putString("display_name", displayName)
            .putLong("expires_at", expiresAtSec)
            .commit()

        return AppUser(uid = uid, email = email, displayName = displayName, role = role, accessToken = access)
    }

    private fun clearSession(context: Context) {
        context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun verifyJoinCodeIfConfigured(enteredCode: String): Boolean {
        return try {
            val conn = (URL("${baseUrl()}/rest/v1/settings?select=*&limit=1").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("apikey", anonKey())
                setRequestProperty("Authorization", "Bearer ${anonKey()}")
            }
            try {
                if (conn.responseCode !in 200..299) return true
                val txt = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = org.json.JSONArray(txt)
                if (arr.length() == 0) return true
                val row = arr.getJSONObject(0)
                val serverCode = row.optString("registration_code").ifBlank {
                    row.optString("join_code").ifBlank { row.optString("joinCode") }
                }
                serverCode.isBlank() || serverCode.equals(enteredCode, ignoreCase = true)
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun postAuth(path: String, jsonBody: String): Pair<Int, String> {
        val conn = (URL("${baseUrl()}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", anonKey())
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
        }
        return try {
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            conn.disconnect()
        }
    }

    private fun defaultRoleForEmail(email: String?): String =
        if (email != null && email.equals(SeedCatalog.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) "admin" else "member"

    private fun translateAuthError(json: JSONObject?, raw: String): String {
        val msg = (json?.optString("msg")?.ifBlank { null }
            ?: json?.optString("error_description")?.ifBlank { null }
            ?: json?.optString("message")?.ifBlank { null }
            ?: raw).lowercase()
        return when {
            "invalid login credentials" in msg -> "ইমেইল অথবা পাসওয়ার্ড সঠিক নয়।"
            "email not confirmed" in msg -> "আপনার ইমেইল ভেরিফিকেশন এখনো সম্পন্ন হয়নি। ইনবক্স পরীক্ষা করুন।"
            "user already registered" in msg || "already exists" in msg -> "এই ইমেইল দিয়ে আগেই অ্যাকাউন্ট তৈরি করা আছে।"
            "password should be at least" in msg -> "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
            "rate limit" in msg -> "অনেকবার চেষ্টা করা হয়েছে, কিছুক্ষণ পর আবার চেষ্টা করুন।"
            "unable to validate email" in msg || "invalid email" in msg -> "সঠিক ইমেইল ঠিকানা লিখুন।"
            else -> json?.optString("msg")?.ifBlank { null }
                ?: json?.optString("error_description")?.ifBlank { null }
                ?: "প্রমাণীকরণ ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
        }
    }
}
