package com.fajaranugrah.pdfviewer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.webkit.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fajaranugrah.pdfviewer.model.PdfListener
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by
 * Fajar Anugrah Ramadhan License
 * ===============================================
 *
 * Copyright (C).
 * All right reserved
 *
 * Name      : Fajar Anugrah Ramadhan
 * E-mail    : fajarconan@gmail.com
 * Github    : https://github.com/fajaranugrah
 * LinkedIn  : linkedin.com/in/fajar-anugrah
 *
 */

class PdfView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    // --- VARIABLES ---
    private var currentPdfFile: File? = null
    private var listener: PdfListener? = null
    private var pendingLocalFile: File? = null
    private var isInterceptingBlob = false

    init {
        setupSettings()
        setupLifecycle()
    }

    // ========================================================================
    // 1. SETUP
    // ========================================================================

    private fun setupSettings() {
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        addJavascriptInterface(PdfInterface(), "AndroidInterface")
        webViewClient = PdfWebViewClient()

        setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("blob:")) injectBlobGrabber(url)
        }
    }

    private fun setupLifecycle() {
        val lifecycleOwner = findLifecycleOwner(context)
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    private fun findLifecycleOwner(context: Context): LifecycleOwner? {
        if (context is LifecycleOwner) return context
        if (context is ContextWrapper) return findLifecycleOwner(context.baseContext)
        return null
    }

    override fun onResume(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onResume(owner)
        // Auto-Retry jika user baru saja memberikan izin untuk file lokal
        if (pendingLocalFile != null && checkStoragePermission()) {
            loadPdf(pendingLocalFile!!)
        }
    }

    fun setOnPdfListener(listener: PdfListener) {
        this.listener = listener
    }

    // ========================================================================
    // 2. PUBLIC API
    // ========================================================================

    /**
     * LOAD FILE LOKAL (Internal / External)
     */
    fun loadPdf(file: File) {
        this.isInterceptingBlob = false
        this.pendingLocalFile = null

        // 1. Jika Internal Cache -> Langsung Buka
        if (isInternalFile(file)) {
            this.currentPdfFile = file
            loadViewerHtml()
            return
        }

        // 2. Jika External -> Cek Izin -> Copy ke Cache -> Buka
        if (checkStoragePermission()) {
            copyToCacheAndLoad(file)
        } else {
            this.pendingLocalFile = file
            showPermissionUi("Izin Penyimpanan diperlukan.")
        }
    }

    /**
     * LOAD URL (Download Dulu -> Simpan Cache -> Buka)
     * Ini mengatasi masalah "Simbol Aneh" dan "Layar Putih".
     */
    fun loadPdfStream(url: String, headers: Map<String, String> = emptyMap()) {
        this.isInterceptingBlob = false
        this.currentPdfFile = null

        // Jalankan Download di Background
        downloadToCache(url, headers)
    }

    // ========================================================================
    // 3. LOGIC DOWNLOADER (KUNCI PERBAIKAN)
    // ========================================================================

    private fun downloadToCache(urlStr: String, headers: Map<String, String>) {
        Thread {
            try {
                //Log.d("PdfDownloader", "Connecting...")
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val userAgent = WebSettings.getDefaultUserAgent(context)
                connection.setRequestProperty("User-Agent", userAgent)
                headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

                // --- DETEKSI TIPE KONTEN ---
                val contentType = connection.contentType ?: ""
                //Log.d("PdfDownloader", "Server Content-Type: $contentType")

                // Cek apakah ini HTML Generator?
                // TAPI: Jika URL berakhiran .pdf, kita ABAIKAN content-type HTML (Server Bohong)
                val isPdfExtension = urlStr.endsWith(".pdf", true)

                if (contentType.contains("text/html", true) && !isPdfExtension) {
                    Log.w("PdfDownloader", "HTML Generator Detected. Switching Mode.")
                    isInterceptingBlob = true
                    post { loadUrl(urlStr, headers) } // Load URL asli
                    return@Thread
                }

                // --- DOWNLOAD PROCESS (Binary Safe) ---
                //Log.d("PdfDownloader", "Downloading Binary...")

                val input = BufferedInputStream(connection.inputStream)
                // Simpan dengan nama unik di Cache
                val tempFile = File(context.cacheDir, "downloaded_doc_${System.currentTimeMillis()}.pdf")
                if (tempFile.exists()) tempFile.delete()

                val output = FileOutputStream(tempFile)
                val data = ByteArray(8192) // Buffer 8KB
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }

                output.flush(); output.close(); input.close()

                //Log.d("PdfDownloader", "Download Success. File size: ${tempFile.length()}")

                // Kembali ke UI Thread -> Load File Cache
                post {
                    loadPdf(tempFile)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                post { listener?.onError(e) }
            }
        }.start()
    }

    // ========================================================================
    // 4. INTERCEPTOR & VIEWER
    // ========================================================================

    private fun loadViewerHtml() {
        val timestamp = System.currentTimeMillis()
        // Panggil viewer dengan parameter file
        val fakeUrl = "https://aplikasiku.local/web/simple_viewer.html?file=/document.pdf&t=$timestamp"
        this.loadUrl(fakeUrl)
    }

    private inner class PdfWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null

            if (url.contains("aplikasiku.local")) {
                // A. REQUEST DATA PDF
                if (url.contains("/document.pdf")) {
                    return serveLocalFile(url)
                }

                // B. REQUEST ASET (JS/CSS)
                val cleanPath = url.substringAfter("aplikasiku.local/").substringBefore("?")
                if (cleanPath.endsWith(".pdf")) return null

                return try {
                    val stream = context.assets.open("pdfjs/$cleanPath")
                    WebResourceResponse(getMimeType(cleanPath), "UTF-8", stream)
                } catch (e: Exception) {
                    WebResourceResponse("text/plain", "UTF-8", null)
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            post { listener?.onLoad() }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            post { listener?.onLoadSuccess() }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            post { listener?.onError(Throwable("${error?.errorCode} + ${error?.description}")) }
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            post { listener?.onError(Throwable("${errorResponse?.statusCode} + ${errorResponse?.data.toString()}")) }
        }
    }

    private fun serveLocalFile(url: String): WebResourceResponse? {
        try {
            val rawPath = url.replace("https://aplikasiku.local/", "")
            val cleanPath = rawPath.substringBefore("?")

            if (cleanPath.contains("favicon.ico")) {
                return WebResourceResponse("image/x-icon", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }

            return try {
                if (cleanPath.endsWith(".pdf")) {
                    // Cek apakah file valid
                    if (currentPdfFile != null && currentPdfFile!!.exists()) {

                        // LOGIKA BARU: Cek keamanan akses
                        val isInternal = isInternalFile(currentPdfFile!!)
                        val hasPermission = checkStoragePermission()

                        // Izinkan jika Internal ATAU punya Izin
                        if (isInternal || hasPermission) {
                            val fileStream = FileInputStream(currentPdfFile)
                            return WebResourceResponse("application/pdf", "UTF-8", fileStream)
                        }
                    }
                    // Jika tidak lolos, return null (404)
                    return null
                } else {
                    val assetPath = "pdfjs/$cleanPath"
                    val mimeType = getMimeType(assetPath)
                    WebResourceResponse(mimeType, "UTF-8", context.assets.open(assetPath))
                }
            } catch (e: Exception) {
                if (e.message?.contains("EACCES") == true) {
                    post { showPermissionUi("System menolak akses file. Cek izin All Files.") }
                    WebResourceResponse("text/html", "UTF-8", "".byteInputStream())
                } else {
                    throw e
                    post { listener?.onError(e) }
                }
                null
            }
        } catch (e: Exception) {
            post { listener?.onError(e) }
        }

        return WebResourceResponse("text/plain", "UTF-8", null)
    }

    // ========================================================================
    // 5. HELPER: COPY & PERMISSION
    // ========================================================================

    private fun copyToCacheAndLoad(sourceFile: File) {
        Thread {
            try {
                if (!sourceFile.exists()) throw Exception("File not found")

                val destFile = File(context.cacheDir, "local_copy_${System.currentTimeMillis()}.pdf")
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }

                post { loadPdf(destFile) } // Load file hasil copy (Internal)
            } catch (e: Exception) {
                if (e.message?.contains("EACCES") == true) {
                    post { showPermissionUi("Akses sistem ditolak. Cek izin.") }
                } else {
                    post { listener?.onError(e) }
                }
            }
        }.start()
    }

    private fun isInternalFile(file: File): Boolean {
        return file.absolutePath.startsWith(context.cacheDir.absolutePath) ||
                file.absolutePath.startsWith(context.filesDir.absolutePath)
    }

    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionUi(msg: String) {
        val html = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: sans-serif; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; text-align: center; background: #f5f5f5; color: #333; margin: 0; }
                    .card { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); width: 80%; max-width: 300px; }
                    h2 { margin-top: 0; color: #d32f2f; }
                    p { font-size: 14px; color: #666; }
                    button { background: #007BFF; color: white; border: none; padding: 12px 24px; border-radius: 8px; font-size: 16px; margin-top: 15px; cursor: pointer; width: 100%; }
                    button:active { background: #0056b3; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h2>Akses Ditolak</h2>
                    <p>$msg</p>
                    <button onclick="AndroidInterface.requestPermission()">Izinkan Akses</button>
                </div>
            </body>
            </html>
        """.trimIndent()
        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun requestStoragePermission(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                activity.startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            activity.startActivity(intent)
        }
    }

    // ========================================================================
    // 6. HELPER: BLOB & MIME
    // ========================================================================

    private fun injectBlobGrabber(blobUrl: String) {
        val js = """
            javascript:(function() {
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '$blobUrl', true);
                xhr.responseType = 'blob';
                xhr.onload = function(e) {
                    if (this.status == 200) {
                        var reader = new FileReader();
                        reader.readAsDataURL(this.response);
                        reader.onloadend = function() { window.AndroidInterface.onBlobFetched(reader.result); }
                    }
                };
                xhr.send();
            })();
        """.trimIndent()
        post { evaluateJavascript(js, null) }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".css") -> "text/css"
            fileName.endsWith(".js") -> "application/javascript"
            fileName.endsWith(".html") -> "text/html"
            else -> "application/octet-stream"
        }
    }

    private inner class PdfInterface {
        @JavascriptInterface fun onSuccess() { post { listener?.onLoadSuccess() } }
        @JavascriptInterface fun onError(msg: String) { post { listener?.onError(Throwable(msg)) } }
        @JavascriptInterface fun requestPermission() { if (context is Activity) post { requestStoragePermission(context as Activity) } }
        @JavascriptInterface fun onBlobFetched(base64Data: String) {
            Thread {
                try {
                    val clean = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
                    val bytes = Base64.decode(clean, Base64.DEFAULT)
                    val file = File(context.cacheDir, "blob_captured.pdf")
                    FileOutputStream(file).use { it.write(bytes) }
                    post { loadPdf(file) }
                } catch (e: Exception) { post { listener?.onError(e) } }
            }.start()
        }
    }
}