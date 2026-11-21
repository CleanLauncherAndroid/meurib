package com.example.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.Locale

data class ApkShareSession(
    val shareUrl: String,
    val localPreviewUrl: String,
    val downloadUrl: String,
    val apkSizeMb: String,
    val isLanReachable: Boolean
)

enum class SocialTarget(val labelAr: String, val packages: List<String>) {
    WHATSAPP("واتساب", listOf("com.whatsapp", "com.whatsapp.w4b")),
    MESSENGER("ماسنجر", listOf("com.facebook.orca")),
    FACEBOOK("فيسبوك", listOf("com.facebook.katana")),
    INSTAGRAM("إنستغرام", listOf("com.instagram.android")),
    SYSTEM("تطبيقات أخرى", emptyList())
}

/**
 * Embedded local HTTP server that generates a live shareable link on the user's device,
 * serves a polished RTL Arabic landing page with a large Download APK button, and streams
 * the app's own installed APK directly to visitors on the same Wi-Fi / Hotspot.
 */
class ApkShareServer(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    @Volatile
    private var activePort: Int = -1

    @Volatile
    private var currentToken: String = randomToken()

    private val appIconDataUri: String by lazy { buildAppIconDataUri(appContext) }

    suspend fun startOrRefreshSession(regenerateToken: Boolean = false): ApkShareSession =
        withContext(Dispatchers.IO) {
            if (regenerateToken || activePort <= 0 || serverSocket == null || serverSocket?.isClosed == true) {
                currentToken = randomToken()
            }
            ensureServerRunning()

            val lanIp = findBestLanIpv4Address()
            val port = activePort
            val host = lanIp ?: "127.0.0.1"
            val shareUrl = "http://$host:$port/share/$currentToken"
            val previewUrl = "http://127.0.0.1:$port/share/$currentToken"
            val downloadUrl = "http://$host:$port/download/$currentToken/muerib.apk"

            val apkFile = File(appContext.applicationInfo.sourceDir)
            val mb = if (apkFile.exists()) {
                String.format(Locale.US, "%.1f", apkFile.length() / (1024.0 * 1024.0))
            } else {
                "—"
            }

            ApkShareSession(
                shareUrl = shareUrl,
                localPreviewUrl = previewUrl,
                downloadUrl = downloadUrl,
                apkSizeMb = mb,
                isLanReachable = lanIp != null
            )
        }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        activePort = -1
    }

    @Synchronized
    private fun ensureServerRunning() {
        val existing = serverSocket
        if (existing != null && !existing.isClosed && activePort > 0) return

        val socket = runCatching { ServerSocket(8765) }.getOrElse { ServerSocket(0) }
        serverSocket = socket
        activePort = socket.localPort

        acceptJob?.cancel()
        acceptJob = scope.launch {
            while (isActive && !socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                launch {
                    handleClient(client)
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            runCatching {
                client.soTimeout = 10_000
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val requestLine = reader.readLine() ?: return
                // Drain HTTP headers until blank line
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }

                val path = requestLine.split(" ").getOrNull(1)?.substringBefore("?") ?: "/"
                val out = client.getOutputStream()

                if (path.startsWith("/download/")) {
                    serveApkFile(out)
                } else {
                    serveLandingHtml(out)
                }
            }
        }
    }

    private fun serveLandingHtml(out: OutputStream) {
        val apkFile = File(appContext.applicationInfo.sourceDir)
        val sizeMb = if (apkFile.exists()) {
            String.format(Locale.US, "%.1f", apkFile.length() / (1024.0 * 1024.0))
        } else {
            "—"
        }
        val downloadHref = "/download/$currentToken/muerib.apk"
        val iconHtml = if (appIconDataUri.isNotEmpty()) {
            """<img src="$appIconDataUri" alt="شعار تطبيق معرب" class="logo-img"/>"""
        } else {
            """<div class="logo-fallback">مُعْرِبٌ</div>"""
        }

        val html = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>تحميل تطبيق معرب (APK)</title>
              <style>
                :root {
                  --emerald-dark: #063229;
                  --emerald: #0F5D4E;
                  --gold: #E9BF6D;
                  --cream: #FAF6EE;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: system-ui, -apple-system, "Segoe UI", Tahoma, Arial, sans-serif;
                  background: radial-gradient(circle at top, #0F5D4E 0%, #063229 70%, #04211B 100%);
                  color: #FFFFFF;
                  min-height: 100vh;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  padding: 20px;
                }
                .card {
                  width: 100%;
                  max-width: 460px;
                  background: rgba(255, 255, 255, 0.08);
                  backdrop-filter: blur(14px);
                  border: 1.5px solid rgba(233, 191, 109, 0.45);
                  border-radius: 28px;
                  padding: 32px 24px;
                  text-align: center;
                  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
                }
                .logo-img {
                  width: 104px;
                  height: 104px;
                  border-radius: 24px;
                  border: 2px solid var(--gold);
                  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35);
                  margin-bottom: 18px;
                  object-fit: cover;
                }
                .logo-fallback {
                  width: 104px;
                  height: 104px;
                  border-radius: 24px;
                  margin: 0 auto 18px;
                  background: #0B4A3E;
                  border: 2px solid var(--gold);
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  font-size: 28px;
                  font-weight: bold;
                  color: var(--gold);
                }
                h1 {
                  font-size: 32px;
                  margin-bottom: 6px;
                  color: #FFFFFF;
                }
                .subtitle {
                  font-size: 16px;
                  color: #D9EFE7;
                  margin-bottom: 18px;
                }
                .meta {
                  display: inline-flex;
                  gap: 10px;
                  flex-wrap: wrap;
                  justify-content: center;
                  margin-bottom: 24px;
                }
                .pill {
                  background: rgba(0, 0, 0, 0.28);
                  border: 1px solid rgba(255, 255, 255, 0.16);
                  padding: 6px 14px;
                  border-radius: 999px;
                  font-size: 13px;
                  color: #F4E8CD;
                }
                .download-btn {
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  gap: 12px;
                  width: 100%;
                  padding: 20px 24px;
                  font-size: 21px;
                  font-weight: 800;
                  color: #062820;
                  background: linear-gradient(135deg, #F6DC9D 0%, #E9BF6D 55%, #D9A243 100%);
                  border-radius: 18px;
                  text-decoration: none;
                  box-shadow: 0 12px 30px rgba(233, 191, 109, 0.38);
                  transition: transform 0.15s ease;
                }
                .download-btn:active {
                  transform: scale(0.97);
                }
                .steps {
                  margin-top: 22px;
                  padding: 14px 16px;
                  border-radius: 16px;
                  background: rgba(0, 0, 0, 0.24);
                  text-align: right;
                  font-size: 14px;
                  line-height: 1.75;
                  color: #DCEEE8;
                }
              </style>
            </head>
            <body>
              <main class="card">
                $iconHtml
                <h1>تطبيق معرب</h1>
                <p class="subtitle">محلل النحو والإعراب العربي الصرفي (أوفلاين بالكامل)</p>
                <div class="meta">
                  <span class="pill">الإصدار ${BuildConfig.VERSION_NAME}</span>
                  <span class="pill">الحجم: $sizeMb ميجابايت</span>
                  <span class="pill">صيغة APK</span>
                </div>
                <a class="download-btn" href="$downloadHref" download="muerib.apk">
                  <span>⬇</span>
                  <span>تحميل ملف التطبيق (APK)</span>
                </a>
                <div class="steps">
                  <strong>طريقة التثبيت:</strong><br/>
                  ١. اضغط على زر التحميل الكبير بالأعلى.<br/>
                  ٢. بعد اكتمال التنزيل افتح ملف <strong>muerib.apk</strong> واختر «تثبيت».
                </div>
              </main>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.US_ASCII)

        out.write(headers)
        out.write(bytes)
        out.flush()
    }

    private fun serveApkFile(out: OutputStream) {
        val apkFile = File(appContext.applicationInfo.sourceDir)
        if (!apkFile.exists() || !apkFile.canRead()) {
            val notFound = "APK file unavailable".toByteArray(Charsets.UTF_8)
            val headers = "HTTP/1.1 404 Not Found\r\nContent-Length: ${notFound.size}\r\nConnection: close\r\n\r\n"
                .toByteArray(Charsets.US_ASCII)
            out.write(headers)
            out.write(notFound)
            out.flush()
            return
        }

        val length = apkFile.length()
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: application/vnd.android.package-archive\r\n")
            append("Content-Disposition: attachment; filename=\"muerib.apk\"\r\n")
            append("Content-Length: $length\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.US_ASCII)

        out.write(headers)
        BufferedInputStream(FileInputStream(apkFile), 65_536).use { input ->
            val buffer = ByteArray(65_536)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
            }
        }
        out.flush()
    }

    private fun findBestLanIpv4Address(): String? {
        val candidates = mutableListOf<String>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("127.")) {
                            candidates.add(host)
                        }
                    }
                }
            }
        }
        return candidates.firstOrNull { it.startsWith("192.168.") }
            ?: candidates.firstOrNull { it.startsWith("10.") }
            ?: candidates.firstOrNull { it.startsWith("172.") }
            ?: candidates.firstOrNull()
    }

    private fun randomToken(): String {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val rng = SecureRandom()
        return buildString(6) {
            repeat(6) { append(chars[rng.nextInt(chars.length)]) }
        }
    }

    private fun buildAppIconDataUri(context: Context): String {
        return runCatching {
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_muerib_clean)
                ?: return ""
            val scaled = Bitmap.createScaledBitmap(bmp, 160, 160, true)
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 90, stream)
            if (scaled !== bmp) scaled.recycle()
            val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            "data:image/png;base64,$encoded"
        }.getOrDefault("")
    }

    companion object {
        fun buildShareMessage(url: String): String {
            return "حمّل تطبيق «معرب» (محلل النحو والإعراب العربي الصرفي الأوفلاين) مباشرة من الرابط:\n$url"
        }

        fun shareLinkToTarget(
            context: Context,
            target: SocialTarget,
            url: String,
            onFeedbackMessage: (String) -> Unit
        ) {
            val text = buildShareMessage(url)
            if (target == SocialTarget.SYSTEM) {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    "مشاركة رابط تطبيق معرب"
                )
                context.startActivity(chooser)
                return
            }

            // Try targeted app packages first
            for (pkg in target.packages) {
                val targeted = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    setPackage(pkg)
                }
                val launched = runCatching {
                    context.startActivity(targeted)
                }.isSuccess
                if (launched) return
            }

            // Fallback (e.g. Instagram text share restriction or app not installed):
            // copy link cleanly and open system share sheet so user can paste/share anywhere.
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Muerib Link", url))
            onFeedbackMessage("تم نسخ الرابط، اختر ${target.labelAr} أو الصق الرابط مباشرة")
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "مشاركة عبر ${target.labelAr}"
            )
            runCatching { context.startActivity(chooser) }
        }

        suspend fun shareApkFileDirectly(
            context: Context,
            onError: (String) -> Unit
        ) {
            val uri: Uri? = withContext(Dispatchers.IO) {
                runCatching {
                    val source = File(context.applicationInfo.sourceDir)
                    if (!source.exists()) return@runCatching null
                    val dir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
                    val outFile = File(dir, "muerib-v${BuildConfig.VERSION_NAME}.apk")
                    source.copyTo(outFile, overwrite = true)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outFile
                    )
                }.getOrNull()
            }

            if (uri == null) {
                onError("تعذر تجهيز ملف APK للمشاركة من الجهاز.")
                return
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تطبيق معرب (APK)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "إرسال ملف تطبيق معرب (APK)")
            runCatching { context.startActivity(chooser) }
                .onFailure { onError("لا يوجد تطبيق متاح لاستقبال ملف الـ APK.") }
        }
    }
}
