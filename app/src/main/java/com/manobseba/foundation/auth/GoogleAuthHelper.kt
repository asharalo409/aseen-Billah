package com.manobseba.foundation.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.manobseba.foundation.R
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

object GoogleAuthHelper {
    suspend fun signInWithGoogle(activityContext: Context): GoogleSignInResult {
        return try {
            val serverClientId = activityContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)
            val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            handleCredential(result.credential)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            // Fallback to GetGoogleIdOption if standard bottom sheet cannot launch
            trySilentOrBottomSheetFallback(activityContext, e)
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.message ?: "Google সাইন-ইন ব্যর্থ হয়েছে।")
        }
    }

    private suspend fun trySilentOrBottomSheetFallback(
        activityContext: Context,
        originalError: Exception
    ): GoogleSignInResult {
        return try {
            val serverClientId = activityContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activityContext, request)
            handleCredential(result.credential)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: Exception) {
            GoogleSignInResult.Error(originalError.localizedMessage ?: "Google অ্যাকাউন্ট পাওয়া যায়নি।")
        }
    }

    private suspend fun handleCredential(credential: androidx.credentials.Credential): GoogleSignInResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            return if (user != null) {
                GoogleSignInResult.Success(user)
            } else {
                GoogleSignInResult.Error("ব্যবহারকারীর তথ্য লোড করা যায়নি।")
            }
        }
        return GoogleSignInResult.Error("অসমর্থিত ক্রেডেনশিয়াল ধরন।")
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            FirebaseAuth.getInstance().signOut()
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
        }
    }
}
