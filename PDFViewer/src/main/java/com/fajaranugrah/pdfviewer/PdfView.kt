package com.fajaranugrah.pdfviewer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fajaranugrah.pdfviewer.model.PdfListener
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class PdfView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var currentPdfFile: File? = null
    private var listener: PdfListener? = null

    init {
        setupSettings()
    }

    fun setOnPdfListener(listener: PdfListener) {
        this.listener = listener
    }

    private fun setupSettings() {
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        // JEMBATAN JAVASCRIPT
        // Nama "AndroidInterface" ini akan dipanggil di HTML nanti
        addJavascriptInterface(PdfInterface(), "AndroidInterface")
        webViewClient = PdfWebViewClient()
    }

    // CLASS PENGHUBUNG (BRIDGE)
    private inner class PdfInterface {

        @JavascriptInterface
        fun onSuccess() {
            // JS berjalan di background thread, kita harus lempar ke Main Thread
            post {
                listener?.onLoadSuccess()
            }
        }

        @JavascriptInterface
        fun onError(errorMessage: String) {
            post {
                listener?.onError(Throwable(errorMessage))
            }
        }
    }

    /**
     * FUNGSI UTAMA: Load PDF dengan Cek Permission Otomatis
     */
    fun loadPdf(file: File) {
        this.currentPdfFile = file

        // LOGIKA BARU:
        // Boleh lanjut jika: (File Internal) ATAU (Punya Izin Eksternal)
        val isSafeToRead = isInternalFile(file) || checkStoragePermission()

        if (isSafeToRead) {
            if (file.exists()) {
                val timestamp = System.currentTimeMillis()
                // Gunakan simple_viewer.html (yang terbaru)
                val fakeUrl = "https://aplikasiku.local/web/simple_viewer.html?file=/document.pdf&t=$timestamp"
                this.loadUrl(fakeUrl)
            } else {
                showErrorHtml("File tidak ditemukan di path:<br><b>${file.absolutePath}</b>")
            }
        } else {
            // Jika file eksternal & belum ada izin
            showErrorHtml("""
            <div style="text-align:center; padding-top: 50px;">
                <h2>Izin Akses Ditolak</h2>
                <p>File ini berada di Penyimpanan Eksternal.</p>
                <p style="color: red;">Silakan panggil fungsi <b>requestPermission()</b></p>
            </div>
        """.trimIndent())
        }
    }

    /**
     * Helper untuk menampilkan pesan error HTML di WebView
     */
    private fun showErrorHtml(message: String) {
        val htmlData = """
            <html>
            <body style="font-family: sans-serif; padding: 20px; text-align: center; color: #333;">
                $message
            </body>
            </html>
        """.trimIndent()
        this.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
    }

    /**
     * Logic Pengecekan Permission (Support Android 10 s/d 13+)
     */
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (Manage External Storage)
            Environment.isExternalStorageManager()
        } else {
            // Android 10 kebawah (Read External Storage)
            val result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            result == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Logic Meminta Permission
     * Harus dipanggil dari Activity karena View tidak bisa start ActivityResult
     */
    fun requestStoragePermission(activity: Activity, requestCode: Int = 100) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.packageName}")
                activity.startActivityForResult(intent, requestCode)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, requestCode)
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }

    // --- INTERCEPTOR (Sama seperti sebelumnya) ---
    private inner class PdfWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null

            if (url.startsWith("https://aplikasiku.local/")) {
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
                    e.printStackTrace()
                    null
                }
            }
            return super.shouldInterceptRequest(view, request)
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".html") -> "text/html"
            fileName.endsWith(".css") -> "text/css"
            fileName.endsWith(".js") -> "application/javascript"
            fileName.endsWith(".svg") -> "image/svg+xml"
            fileName.endsWith(".bcmap") -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    /**
     * Cek apakah file berada di penyimpanan internal aplikasi (Cache/Files)
     * File di sini TIDAK BUTUH izin storage.
     */
    private fun isInternalFile(file: File): Boolean {
        val cachePath = context.cacheDir.absolutePath
        val filesPath = context.filesDir.absolutePath
        val filePath = file.absolutePath

        return filePath.startsWith(cachePath) || filePath.startsWith(filesPath)
    }
}