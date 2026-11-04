package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldLight
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondarySlate

private class WebAppBridge(
    private val onExportCsv: (String) -> Unit
) {
    @JavascriptInterface
    fun exportCsvFromWeb(csvContent: String) {
        onExportCsv(csvContent)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebVersionScreen(
    paidDates: Set<String>,
    transactionNotes: Map<String, String>,
    onBackToAndroidUi: () -> Unit,
    onExportCsvFromWeb: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToAndroidUi()
    }

    val context = LocalContext.current
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val htmlSourceCode = remember {
        try {
            context.assets.open("index.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    val downloadHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null && htmlSourceCode.isNotEmpty()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(htmlSourceCode.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                statusBanner = "✓ index.html ওয়েব অ্যাপ ফাইল আপনার ডিভাইসে সংরক্ষিত হয়েছে!"
            } catch (e: Exception) {
                statusBanner = "HTML সংরক্ষণে ত্রুটি: ${e.localizedMessage ?: "Error"}"
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("web_version_screen"),
        containerColor = NavyBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBackground)
                .padding(innerPadding)
        ) {
            // Top Web App Control Bar (Back to Native UI + Download / Copy / Share index.html)
            Surface(
                color = NavySurface,
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBackToAndroidUi,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("back_from_web_version_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Android App",
                                    tint = CyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Web Version",
                                tint = PaidEmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ওয়েব ভার্সন (index.html Web App)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = TextPrimaryWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "HTML + CSS + Vanilla JS + Firebase Cloud Sync",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondarySlate
                                    )
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onBackToAndroidUi,
                            border = BorderStroke(1.dp, CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "অ্যান্ড্রয়েড ভিউ",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Action Buttons to Download / Copy / Share the standalone index.html Web App
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { downloadHtmlLauncher.launch("index.html") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaidEmerald,
                                contentColor = Color(0xFF041E16)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                                .testTag("download_index_html_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download index.html",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "index.html ডাউনলোড",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("index.html", htmlSourceCode)
                                )
                                statusBanner = "✓ সম্পূর্ণ index.html ওয়েব অ্যাপ কোড ক্লিপবোর্ডে কপি হয়েছে!"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBluePrimary,
                                contentColor = TextPrimaryWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("copy_index_html_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy HTML Code",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "কোড কপি",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "My Saving Tracker Web App (index.html)")
                                    putExtra(Intent.EXTRA_TEXT, htmlSourceCode)
                                }
                                val chooser = Intent.createChooser(sendIntent, "index.html শেয়ার করুন")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            },
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("share_index_html_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share HTML",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (statusBanner != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurfaceElevated,
                            border = BorderStroke(1.dp, PaidEmeraldLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusBanner ?: "",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = PaidEmeraldLight,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Interactive HTML/CSS/Vanilla JS + Firebase Web App Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webChromeClient = WebChromeClient()
                            addJavascriptInterface(
                                WebAppBridge(onExportCsv = onExportCsvFromWeb),
                                "AndroidWebBridge"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Seed localStorage with any current paidDates/notes so Web & Native stay in sync
                                    val paidJsonArray = paidDates.joinToString(
                                        prefix = "[",
                                        postfix = "]"
                                    ) { "\"$it\"" }
                                    val script = """
                                        try {
                                          const existing = localStorage.getItem('my_saving_tracker_v2_paid_local_user');
                                          const arr = existing ? JSON.parse(existing) : [];
                                          const merged = Array.from(new Set([...arr, ...$paidJsonArray]));
                                          localStorage.setItem('my_saving_tracker_v2_paid_local_user', JSON.stringify(merged));
                                          if (window.__savingTrackerApp) {
                                            window.__savingTrackerApp.mergeCloudRecords([], merged);
                                          }
                                        } catch (e) {}
                                    """.trimIndent()
                                    view?.evaluateJavascript(script, null)
                                }
                            }
                            loadUrl("file:///android_asset/index.html")
                        }
                    }
                )
            }
        }
    }
}
