
[![](https://jitpack.io/v/User/Repo.svg)](https://jitpack.io/#User/Repo) [![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A high-performance, **Hybrid Android PDF Viewer** library built on top of WebView and PDF.js.

It solves the complex problems of rendering PDFs on Android, including **Scoped Storage restrictions (Android 11+)**, **Authenticated URLs**, and **Client-Side PDF Generators** (HTML to PDF).

## ✨ Key Features

* **⚡ Hybrid Rendering Engine:** Automatically detects if a URL is a raw PDF or an HTML-based PDF Generator (e.g., `jspdf`, `html2canvas`). It intercepts the generated Blob and renders it natively.
* **🛡️ Android 11+ Ready:** Solves `EACCES Permission Denied` errors on Scoped Storage devices by using a smart "Copy-to-Cache" strategy for external files.
* **🌐 Universal Streaming:** Streams PDFs from any URL (HTTP/HTTPS) with support for **Cookies**, **User-Agents**, and **Authorization Headers**.
* **🔒 Built-in Permission Handling:** Automatically checks for Storage Permissions. If denied, displays a user-friendly UI inside the viewer to request access without crashing.
* **🔍 Native Experience:** Supports Pinch-to-Zoom, Text Selection, and High-Res Tiling (prevents blurry text on zoom).
---

## 🛠 Requirements

* **Min SDK:** 21 (Android 5.0 Lollipop)
* **Target SDK:** 34+ (Android 14)
* **Language:** Kotlin

---

## 📦 Installation

This library is available via **JitPack**.

### Step 1. Add the JitPack repository

**For Kotlin DSL (`settings.gradle.kts`):**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("(https://jitpack.io)") }
    }
}
````

**For Groovy (`settings.gradle`):**

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url '(https://jitpack.io)' }
    }
}
```

### Step 2. Add the dependency

**For Kotlin DSL (`build.gradle.kts`):**

```kotlin
dependencies {
    // Replace 'Tag' with the latest version (e.g., 1.0.0)
    implementation("com.github.fajaranugrah:pdfviewer:Tag")
}
```

**For Groovy (`build.gradle`):**

```gradle
dependencies {
    implementation 'com.github.fajaranugrah:pdfviewer:Tag'
}
```

-----

⚙️ Configuration (AndroidManifest.xml)
To support Android 11+ and HTTP URLs, add the following permissions and attributes to your AndroidManifest.xml:

```xml
<manifest xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    package="com.your.package">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />

    <application
    android:allowBackup="true"
    android:requestLegacyExternalStorage="true"
    android:usesCleartextTraffic="true">
    
    </application>
</manifest>
```

-----

## 🚀 Usage

### 1\. Add to Layout XML

Add the `PdfView` component to your XML layout.

```xml
<com.fajaranugrah.pdfviewer.PdfView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 2\. Setup Listener (Recommended)

Handle success and error states (loading bars, toasts, etc.).

```kotlin
val pdfView = findViewById<PdfView>(R.id.pdfView)

pdfView.setOnPdfListener(object : PdfListener {
    override fun onLoadSuccess() {
        progressBar.visibility = View.GONE
        Log.d("PDF", "Document Loaded Successfully")
    }

    override fun onError(t: Throwable) {
        progressBar.visibility = View.GONE
        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_LONG).show()
        Log.e("PDF_ERROR", t.message.toString())
    }
})
```

### 3\. Loading Methods

#### A\. Load from Local Storage (Robust)
Handles Internal Storage, SD Cards, and Downloads folder. It automatically handles permissions and Android 11+ Scoped Storage restrictions.
You can load a PDF file directly from a `File` object. The library handles the internal rendering logic.

```kotlin
// Example: Loading from Downloads folder
val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "report.pdf")

// Just call loadPdf. The library handles permissions and file access automatically.
pdfView.loadPdf(file)
```

#### B\. Load from Remote URL (Stream)
Supports direct links (.pdf) and auto-detects HTML-based PDF generators.

```kotlin
// 1. Standard PDF URL
pdfView.loadPdfStream("[https://example.com/document.pdf](https://example.com/document.pdf)")

// 2. HTML PDF Generator (e.g., Reports, Invoices)
// The library will automatically intercept the blob and render it!
pdfView.loadPdfStream("[https://api.myserver.com/generate/invoice/123](https://api.myserver.com/generate/invoice/123)")
```

#### C\. Remote URL with Authentication (Headers)
Pass a Map of headers for Bearer Tokens, Cookies, or Basic Auth.

```kotlin
val headers = mapOf(
    "Authorization" to "Bearer eyJhbGciOiJIUz...",
    "User-Agent" to "MyAndroidApp/1.0",
    "Cookie" to "session_id=12345"
)

pdfView.loadPdfStream("[https://secure-api.com/v1/statement](https://secure-api.com/v1/statement)", headers)
```

-----

## 🧠 How it Works (Under the Hood)

### 1\. The EACCES Fix (Android 11+)
   WebView is sandboxed and often cannot read files directly from /storage/emulated/0/Download/ even with permissions.

Solution: This library automatically copies the target file to the app's Internal Cache (/data/user/0/...) in a background thread and then renders the cached copy. This ensures 100% compatibility.

### 2\. The Hybrid Interceptor
   Some URLs return an HTML page that generates a PDF using JavaScript (e.g., jspdf) instead of a PDF binary.

Solution: The library detects text/html Content-Type. It switches to "Intercept Mode", lets the JavaScript run, captures the generated blob: URL, converts it to a file, and reloads the viewer.

-----
-----

## 🤝 Contributing

Contributions are welcome\! If you find a bug or want to improve the tiling algorithm, please fork the repository and submit a Pull Request.

-----

## 📄 License
This project is licensed under the MIT License.