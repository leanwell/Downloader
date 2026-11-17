package com.leanwell.downloader.ui.dialogs

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.leanwell.downloader.settings.AppSettings
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary

private const val CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
@Composable
fun YouTubeLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun checkAndSaveCookies(): Boolean {
        val cookieManager = CookieManager.getInstance()
        val ytCookies = cookieManager.getCookie("https://www.youtube.com") ?: ""
        val googleCookies = cookieManager.getCookie("https://accounts.google.com") ?: ""

        val hasAuth = ytCookies.contains("LOGIN_INFO") ||
                (ytCookies.contains("SID=") && ytCookies.contains("HSID=")) ||
                (googleCookies.contains("SID=") && googleCookies.contains("HSID="))

        if (hasAuth) {
            val saved = AppSettings.saveCookiesFromWebview(context, ytCookies, googleCookies)
            if (saved) {
                Toast.makeText(context, "YouTube session saved successfully!", Toast.LENGTH_SHORT).show()
                onSuccess()
                return true
            }
        }
        return false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF9F9FB)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Sign in with YouTube",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Secure Google Account Sign-In",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Save session button
                    Surface(
                        onClick = {
                            val saved = checkAndSaveCookies()
                            if (!saved) {
                                Toast.makeText(
                                    context,
                                    "Please finish signing in on YouTube first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianPrimary,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Loading progress indicator
                if (isPageLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = DigitalIrisSecondary,
                        trackColor = Color.Transparent
                    )
                }

                // WebView container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    userAgentString = CHROME_USER_AGENT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    safeBrowsingEnabled = true
                                    allowFileAccessFromFileURLs = false
                                    allowUniversalAccessFromFileURLs = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        webProgress = newProgress / 100f
                                        isPageLoading = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val host = request?.url?.host?.lowercase() ?: return true

                                        // Allow Google and YouTube auth / service hosts only
                                        val isAllowedHost = host == "accounts.google.com" ||
                                                host == "myaccount.google.com" ||
                                                host == "youtube.com" ||
                                                host == "m.youtube.com" ||
                                                host == "www.youtube.com" ||
                                                host.endsWith(".google.com") ||
                                                host.endsWith(".youtube.com") ||
                                                host.endsWith(".gstatic.com") ||
                                                host.endsWith(".googleusercontent.com")

                                        if (isAllowedHost) {
                                            return false // Continue loading inside sign-in flow
                                        }

                                        // For external links (terms, help), launch in system browser
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, request?.url)
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                        return true // Disallow loading third-party domains in this WebView
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isPageLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isPageLoading = false

                                        // Auto-detect login completion
                                        val current = url ?: ""
                                        if (current.contains("youtube.com") && !current.contains("ServiceLogin")) {
                                            checkAndSaveCookies()
                                        }
                                    }
                                }

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                // Load Google/YouTube login URL
                                loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fm.youtube.com%2F")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Privacy Note Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign-in happens directly with Google. Sessions are encrypted in hardware Keystore and automatically deleted after 30 days.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }
    }
}
