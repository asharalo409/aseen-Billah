package com.manobseba.foundation.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

@Serializable
data class FirestoreErrorInfo(
    val error: String,
    val operationType: String,
    val path: String?,
    val authInfo: AuthInfo
) {
    @Serializable
    data class AuthInfo(
        val userId: String?,
        val email: String?,
        val emailVerified: Boolean?,
        val isAnonymous: Boolean?,
        val tenantId: String?,
        val providerInfo: List<ProviderInfo>
    )

    @Serializable
    data class ProviderInfo(
        val providerId: String?,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?
    )
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?
): Nothing {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val errInfo = FirestoreErrorInfo(
        error = error.message ?: error.toString(),
        operationType = operationType.value,
        path = path,
        authInfo = FirestoreErrorInfo.AuthInfo(
            userId = currentUser?.uid,
            email = currentUser?.email,
            emailVerified = currentUser?.isEmailVerified,
            isAnonymous = currentUser?.isAnonymous,
            tenantId = currentUser?.tenantId,
            providerInfo = currentUser?.providerData?.map { p ->
                FirestoreErrorInfo.ProviderInfo(
                    providerId = p.providerId,
                    displayName = p.displayName,
                    email = p.email,
                    photoUrl = p.photoUrl?.toString()
                )
            } ?: emptyList()
        )
    )
    val json = Json.encodeToString(errInfo)
    Log.e("FirestoreError", "Firestore Error: $json")
    throw RuntimeException(json, error)
}
