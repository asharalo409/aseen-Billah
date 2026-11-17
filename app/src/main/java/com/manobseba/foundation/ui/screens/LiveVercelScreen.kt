package com.manobseba.foundation.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.manobseba.foundation.R
import com.manobseba.foundation.config.ServerConfig
import com.manobseba.foundation.ui.components.FoundationCard

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveVercelScreen(
    onSwitchToNative: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMainError by remember { mutableStateOf(false) }
    var errorDetail by remember { mutableStateOf<String?>(null) }

    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var isMultiFileMode by remember { mutableStateOf(false) }

    val multiFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }

    val singleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val res = if (uri != null) arrayOf(uri) else emptyArray()
        pendingFileCallback?.onReceiveValue(res)
        pendingFileCallback = null
    }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canGoBack) {
                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                modifier = Modifier.testTag("vercel_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "পিছনে যান"
                                )
                            }
                        } else {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_launcher_art),
                                    contentDescription = stringResource(R.string.app_name),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vercel লাইভ সার্ভার",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = {
                                hasMainError = false
                                errorDetail = null
                                val wv = webViewRef
                                if (wv != null) {
                                    if (wv.url.isNullOrBlank()) wv.loadUrl(ServerConfig.VERCEL_SERVER_URL)
                                    else wv.reload()
                                }
                            },
                            modifier = Modifier.testTag("vercel_reload_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "পুনরায় লোড করুন"
                            )
                        }

                        FilledTonalButton(
                            onClick = onSwitchToNative,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("switch_native_mode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("অ্যাপ মোড", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (isLoading && loadProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { loadProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            trackColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag("vercel_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        val wv = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(wv, true)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
                            allowContentAccess = true
                            loadsImagesAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasMainError = false
                                errorDetail = null
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                runCatching { CookieManager.getInstance().flush() }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    hasMainError = true
                                    errorDetail = error?.description?.toString()
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUri = request?.url ?: return false
                                val urlStr = targetUri.toString()
                                val scheme = targetUri.scheme?.lowercase().orEmpty()
                                val host = targetUri.host?.lowercase().orEmpty()

                                if (scheme in listOf("tel", "mailto", "sms", "whatsapp", "tg", "intent")) {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                    }.onFailure {
                                        onToast("লিংকটি খোলার জন্য উপযুক্ত অ্যাপ পাওয়া যায়নি")
                                    }
                                    return true
                                }

                                val keepInside = host.endsWith("vercel.app") ||
                                    host.endsWith("vercel.com") ||
                                    host.endsWith("supabase.co")

                                if (keepInside) return false

                                if (host.contains("wa.me") ||
                                    host.contains("whatsapp.com") ||
                                    host.contains("facebook.com") ||
                                    host.contains("youtube.com") ||
                                    host.contains("youtu.be") ||
                                    host.contains("maps.google") ||
                                    urlStr.endsWith(".pdf", ignoreCase = true)
                                ) {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                    }
                                    return true
                                }

                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                pendingFileCallback?.onReceiveValue(null)
                                pendingFileCallback = filePathCallback
                                val accept = fileChooserParams?.acceptTypes
                                    ?.filter { it.isNotBlank() }
                                    ?.ifEmpty { listOf("*/*") }
                                    ?.toTypedArray()
                                    ?: arrayOf("*/*")
                                isMultiFileMode =
                                    fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                                return try {
                                    if (isMultiFileMode) {
                                        multiFileLauncher.launch(accept)
                                    } else {
                                        singleFileLauncher.launch(accept)
                                    }
                                    true
                                } catch (_: Exception) {
                                    pendingFileCallback?.onReceiveValue(null)
                                    pendingFileCallback = null
                                    false
                                }
                            }
                        }

                        setDownloadListener { url, _, _, _, _ ->
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    onToast("ফাইল ডাউনলোড শুরু করা যায়নি")
                                }
                            } else {
                                onToast("ব্রাউজার বা শেয়ার থেকে ফাইল সংরক্ষণ করুন")
                            }
                        }

                        webViewRef = this
                        loadUrl(ServerConfig.VERCEL_SERVER_URL)
                    }
                }
            )

            if (hasMainError) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FoundationCard(modifier = Modifier.widthIn(max = 460.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Vercel সার্ভারে সংযোগ করা যাচ্ছে না",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = errorDetail ?: "আপনার ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন, অথবা অফলাইন/নেটিভ অ্যাপ মোড ব্যবহার করুন।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        hasMainError = false
                                        errorDetail = null
                                        webViewRef?.loadUrl(ServerConfig.VERCEL_SERVER_URL)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("vercel_retry_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("পুনরায় চেষ্টা করুন")
                                }
                                OutlinedButton(
                                    onClick = onSwitchToNative,
                                    modifier = Modifier.fillMaxWidth().testTag("vercel_error_native_btn")
                                ) {
                                    Icon(Icons.Default.Apps, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("নেটিভ / অফলাইন মোড খুলুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
