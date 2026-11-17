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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.manobseba.foundation.R
import com.manobseba.foundation.auth.AppUser
import com.manobseba.foundation.auth.SupabaseAuthHelper
import com.manobseba.foundation.auth.SupabaseAuthResult
import com.manobseba.foundation.domain.PrayerCalculator
import com.manobseba.foundation.security.AppLockManager
import com.manobseba.foundation.ui.components.FoundationCard
import com.manobseba.foundation.ui.theme.paletteForKey
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onSignedInSuccess: (AppUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val palette = paletteForKey("green")
    val isConfigured = remember { SupabaseAuthHelper.isConfigured() }

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
                Spacer(Modifier.height(14.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isRegisterMode,
                        onClick = { isRegisterMode = false; errorMsg = null; infoMsg = null },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("auth_tab_login")
                    ) { Text("লগইন") }
                    SegmentedButton(
                        selected = isRegisterMode,
                        onClick = { isRegisterMode = true; errorMsg = null; infoMsg = null },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("auth_tab_register")
                    ) { Text("নতুন অ্যাকাউন্ট") }
                }

                Spacer(Modifier.height(14.dp))

                if (!isConfigured) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Supabase সিক্রেট কী (VITE_SUPABASE_URL ও VITE_SUPABASE_ANON_KEY) কনফিগার করা নেই। Secrets প্যানেলে যোগ করে পুনরায় বিল্ড করুন।",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("পূর্ণ নাম *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("register_name_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("register_phone_input")
                    )
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("জিমেইল / ইমেইল *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("পাসওয়ার্ড *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                )

                if (isRegisterMode) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it },
                        label = { Text("রেজিস্ট্রেশন কোড (প্রযোজ্য হলে)") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("register_code_input")
                    )
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

                if (!infoMsg.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = infoMsg!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (busy) return@Button
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "অনুগ্রহ করে ইমেইল ও পাসওয়ার্ড লিখুন।"
                            return@Button
                        }
                        if (isRegisterMode && fullName.isBlank()) {
                            errorMsg = "অনুগ্রহ করে আপনার পূর্ণ নাম লিখুন।"
                            return@Button
                        }
                        busy = true
                        errorMsg = null
                        infoMsg = null
                        scope.launch {
                            val res = if (isRegisterMode) {
                                SupabaseAuthHelper.signUpWithEmail(context, email, password, fullName, phone, joinCode)
                            } else {
                                SupabaseAuthHelper.signInWithEmail(context, email, password)
                            }
                            when (res) {
                                is SupabaseAuthResult.Success -> onSignedInSuccess(res.user)
                                is SupabaseAuthResult.VerificationRequired -> {
                                    infoMsg = res.message
                                    isRegisterMode = false
                                }
                                is SupabaseAuthResult.Error -> errorMsg = res.message
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(if (isRegisterMode) "নিবন্ধন হচ্ছে..." else "লগইন হচ্ছে...")
                    } else {
                        Text(if (isRegisterMode) "নতুন অ্যাকাউন্ট তৈরি করুন" else "ইমেইল দিয়ে লগইন করুন")
                    }
                }

                if (!isRegisterMode) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.End).testTag("forgot_password_button")
                    ) {
                        Text("পাসওয়ার্ড ভুলে গেছেন?")
                    }
                }
            }
        }
    }

    if (showForgotDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var sending by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!sending) showForgotDialog = false },
            title = { Text("পাসওয়ার্ড রিসেট করুন") },
            text = {
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("আপনার অ্যাকাউন্ট ইমেইল") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = !sending && resetEmail.isNotBlank(),
                    onClick = {
                        sending = true
                        scope.launch {
                            SupabaseAuthHelper.sendPasswordReset(resetEmail)
                                .onSuccess {
                                    infoMsg = "পাসওয়ার্ড রিসেট লিংক আপনার ইমেইলে পাঠানো হয়েছে।"
                                    showForgotDialog = false
                                }
                                .onFailure {
                                    errorMsg = it.localizedMessage ?: "রিসেট লিংক পাঠানো যায়নি।"
                                    showForgotDialog = false
                                }
                            sending = false
                        }
                    }
                ) { Text("লিংক পাঠান") }
            },
            dismissButton = {
                TextButton(enabled = !sending, onClick = { showForgotDialog = false }) { Text("বাতিল") }
            }
        )
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
