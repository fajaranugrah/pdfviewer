package com.fajaranugrah.pdfviewer.model

interface PdfListener {
    fun onLoad()
    fun onLoadSuccess()

    fun onError(t: Throwable)
}