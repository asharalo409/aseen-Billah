package com.manobseba.foundation.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.manobseba.foundation.R
import com.manobseba.foundation.auth.GoogleAuthHelper
import com.manobseba.foundation.auth.GoogleSignInResult
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.security.AppLockManager
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.theme.paletteForKey
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onSignedInSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val palette = paletteForKey("green")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.primary,
                        palette.deepGradientEnd,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(96.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_art),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.sign_in_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_slogan),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFD1FAE5),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            FoundationCard {
                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                val features = listOf(
                    "১০০% স্বচ্ছ আয়-ব্যয়, মাসিক চাঁদা ও তহবিল খাত",
                    "মানবসেবা AI — বাংলা ভিশন খাতা রিডার ও সহকারী",
                    "২৪ ঘণ্টার জরুরি রক্তের আবেদন, ত্রাণ মানচিত্র ও ইসলামিক কর্নার"
                )
                features.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (!errorMsg.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        errorMsg = null
                        scope.launch {
                            when (val res = GoogleAuthHelper.signInWithGoogle(context)) {
                                is GoogleSignInResult.Success -> onSignedInSuccess()
                                is GoogleSignInResult.Cancelled -> errorMsg = "সাইন-ইন বাতিল করা হয়েছে।"
                                is GoogleSignInResult.Error -> errorMsg = res.message
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("সাইন-ইন হচ্ছে...")
                    } else {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.sign_in_google_button))
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockOverlay(
    lockManager: AppLockManager,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var wrongAttempts by remember { mutableStateOf(0) }
    var err by remember { mutableStateOf<String?>(null) }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    err = errString.toString()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("মানবসেবা ফাউন্ডেশন আনলক")
            .setSubtitle("বায়োমেট্রিক ব্যবহার করে আনলক করুন")
            .setNegativeButtonText("পিন ব্যবহার করুন")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (lockManager.isBiometricEnabled()) {
            triggerBiometric()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "অ্যাপ লক",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("অ্যাপ লক সক্রিয়", style = MaterialTheme.typography.headlineSmall)
                Text("আপনার গোপন পিন দিন", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < pin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                if (!err.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(24.dp))
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "bio" -> {
                                    if (lockManager.isBiometricEnabled()) {
                                        FilledTonalButton(
                                            onClick = { triggerBiometric() },
                                            modifier = Modifier.size(64.dp),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Fingerprint, contentDescription = "বায়োমেট্রিক")
                                        }
                                    } else {
                                        Spacer(Modifier.size(64.dp))
                                    }
                                }
                                "del" -> {
                                    IconButton(
                                        onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(Icons.Default.Backspace, contentDescription = "মুছুন")
                                    }
                                }
                                else -> {
                                    FilledTonalButton(
                                        onClick = {
                                            if (pin.length < 4) {
                                                val next = pin + key
                                                pin = next
                                                if (next.length == 4) {
                                                    if (lockManager.verifyPin(next)) {
                                                        onUnlocked()
                                                    } else {
                                                        wrongAttempts++
                                                        pin = ""
                                                        err = "পিন সঠিক নয় (${PrayerCalculator.bnDigits(wrongAttempts)} বার ভুল)"
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(64.dp).testTag("pin_key_$key"),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = PrayerCalculator.bnDigits(key),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockSettingsContent(
    lockManager: AppLockManager,
    onToast: (String) -> Unit
) {
    var enabled by remember { mutableStateOf(lockManager.isLockEnabled()) }
    var bioEnabled by remember { mutableStateOf(lockManager.isBiometricEnabled()) }
    var newPin by remember { mutableStateOf("") }

    FoundationCard {
        Text("অ্যাপ লক ও নিরাপত্তা", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            if (enabled) "বর্তমানে ৪-সংখ্যার পিন লক চালু আছে" else "বর্তমানে কোনো অ্যাপ লক সেট করা নেই",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = newPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
            label = { Text("নতুন ৪-সংখ্যার পিন সেট করুন") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("app_lock_pin_input")
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (newPin.length != 4) {
                        onToast("ঠিক ৪ সংখ্যার পিন দিন")
                    } else {
                        lockManager.setPin(newPin)
                        enabled = true
                        newPin = ""
                        onToast("৪-সংখ্যার পিন লক চালু হয়েছে")
                    }
                },
                modifier = Modifier.weight(1f).testTag("app_lock_save_btn")
            ) {
                Text(if (enabled) "পিন পরিবর্তন" else "পিন চালু করুন")
            }
            if (enabled) {
                FilledTonalButton(
                    onClick = {
                        lockManager.disableLock()
                        enabled = false
                        bioEnabled = false
                        onToast("অ্যাপ লক বন্ধ করা হয়েছে")
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f).testTag("app_lock_disable_btn")
                ) {
                    Text("লক বন্ধ")
                }
            }
        }
        if (enabled) {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = {
                    val next = !bioEnabled
                    lockManager.setBiometricEnabled(next)
                    bioEnabled = next
                    onToast(if (next) "বায়োমেট্রিক আনলক চালু" else "বায়োমেট্রিক আনলক বন্ধ")
                },
                modifier = Modifier.fillMaxWidth().testTag("app_lock_bio_toggle")
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (bioEnabled) "বায়োমেট্রিক চালু আছে (বন্ধ করতে ট্যাপ)" else "আঙুলের ছাপ / ফেস আনলক চালু করুন")
            }
        }
    }
}
