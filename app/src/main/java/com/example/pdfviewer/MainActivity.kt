package com.example.pdfviewer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.fajaranugrah.pdfviewer.PdfView
import com.fajaranugrah.pdfviewer.model.PdfListener
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var pdfView: PdfView
    private lateinit var targetFile: File
    private val PDF_PERMISSION_CODE = 999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val pdfView = findViewById<PdfView>(R.id.myPdfView)
        val fileName = "sample.pdf"

        pdfView.setOnPdfListener(object : PdfListener {
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

        val targetFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        pdfView.loadPdf(targetFile)

        if (!pdfView.checkStoragePermission()) {
            pdfView.requestStoragePermission(this, PDF_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PDF_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pdfView.loadPdf(targetFile)
            } else {
                Toast.makeText(this, "Izin Ditolak!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_PERMISSION_CODE) {
            // Cek lagi apakah izin sudah diberikan
            if (pdfView.checkStoragePermission()) {
                pdfView.loadPdf(targetFile)
            }
        }
    }
}