package com.example.pdfviewer

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.fajaranugrah.pdfviewer.PdfView
import com.fajaranugrah.pdfviewer.model.PdfListener
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val pdfView = findViewById<PdfView>(R.id.myPdfView)
        val fileName = "sample.pdf"

        pdfView.setOnPdfListener(object : PdfListener {
            override fun onLoad() {
                Toast.makeText(this@MainActivity, "Start Load Data", Toast.LENGTH_SHORT).show()
            }

            override fun onLoadSuccess() {
                // Sembunyikan Loading Bar / ProgressBar jika ada
                //progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "PDF Siap Dibaca!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(t: Throwable) {
                // Sembunyikan Loading Bar
                //progressBar.visibility = View.GONE

                // Tampilkan Error ke User
                //Log.e("PDF_ERROR", "Gagal load: ${t.message}")

                // Contoh penanganan error spesifik
                if (t.message?.contains("Password") == true) {
                    Toast.makeText(this@MainActivity, "PDF ini butuh Password!", Toast.LENGTH_LONG).show()
                } else if (t.message?.contains("Invalid PDF structure") == true) {
                    Toast.makeText(this@MainActivity, "File PDF Rusak/Corrupt", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Gagal memuat: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })

        val targetFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        pdfView.loadPdf(targetFile)

        // only url
        //pdfView.loadPdfStream("https://example-files.online-convert.com/document/pdf/example.pdf")

        // url with Bearer Token
        /*val headers = mapOf(
            "Authorization" to "Bearer eyJhbGciOiJIUz...",
            "User-Agent" to "MyAndroidApp/1.0",
            "Cookie" to "session_id=12345"
        )*/

        //pdfView.loadPdfStream("[https://secure-api.com/v1/statement](https://secure-api.com/v1/statement)", headers)

        // url with Basic Auth (Username & Password)
        //val credentials = "admin:rahasia123"
        /*val base64Credentials = android.util.Base64.encodeToString(
            credentials.toByteArray(),
            android.util.Base64.NO_WRAP
        )*/

        /*val headers = mapOf(
            "Authorization" to "Basic $base64Credentials"
        )*/

        //pdfView.loadPdfFromUrl("https://myserver.com/protected/report.pdf", headers)
    }
}