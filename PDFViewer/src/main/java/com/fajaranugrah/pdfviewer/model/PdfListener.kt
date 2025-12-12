package com.fajaranugrah.pdfviewer.model

interface PdfListener {
    fun onLoadSuccess()

    fun onError(t: Throwable)
}