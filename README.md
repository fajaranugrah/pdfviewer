
# 📄 Android Advanced PDF Viewer

[![](https://jitpack.io/v/User/Repo.svg)](https://jitpack.io/#User/Repo) [![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A high-performance, lightweight Android PDF Viewer library built on top of **WebView** and a customized **PDF.js** engine. 

Unlike standard WebView implementations, this library solves common issues such as blurry text, memory crashes on large files, and lack of native gestures. It uses **Canvas Tiling** for high-resolution rendering and **Intersection Observer** for efficient memory management.

## ✨ Key Features

* **🔍 Native Pinch-to-Zoom & Double Tap:** Smooth zooming experience using Android's native touch gestures (no laggy JavaScript zooming).
* **📝 Text Selection & Copy:** Select and copy text directly from the PDF using a precise transparent text layer.
* **🧩 Smart Canvas Tiling:** Renders pages in chunks (tiles) to bypass Android GPU texture limits (4096px), ensuring high-resolution images never get cut off.
* **⚡ Lazy Loading:** Implements `IntersectionObserver` to render pages only when they enter the screen, keeping RAM usage extremely low even for large documents.
* **📱 High DPI Support:** Automatically detects screen density to render crisp text and images.
* **📂 Storage Support:** Supports reading files from Internal Storage and External Storage (Downloads), compatible with Android 11+ Scoped Storage.

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

## 🚀 Usage

### 1\. Add to Layout XML

Add the `PdfView` component to your XML layout.

```xml
<com.fajaranugrah.pdfviewer.PdfView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 2\. Load PDF in Activity/Fragment

You can load a PDF file directly from a `File` object. The library handles the internal rendering logic.

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pdfView = findViewById<com.mycustom.pdfviewer.PdfView>(R.id.pdfView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // 1. Set the Listener BEFORE loading the file
        pdfView.setOnPdfListener(object : com.mycustom.pdfviewer.PdfListener {
            override fun onLoadSuccess() {
                // Hide loading indicator
                progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "PDF Loaded Successfully!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(t: Throwable) {
                // Hide loading indicator
                progressBar.visibility = View.GONE

                // Handle specific errors
                Log.e("PDF_VIEWER", "Error: ${t.message}")

                if (t.message?.contains("Password") == true) {
                    Toast.makeText(this@MainActivity, "Password Required!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to load PDF: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        })

        // 2. Prepare the File
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "report.pdf")

        // 3. Load the PDF
        // The library handles permission checks automatically.
        // If permission is missing, it will show an HTML error message in the WebView
        // or you can implement 'requestStoragePermission' manually.
        if (file.exists()) {
            progressBar.visibility = View.VISIBLE
            pdfView.loadPdf(file)
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }
}
```

-----

## 🔐 Permissions

The library includes a helper method to check for storage permissions, but you must ensure your app has the necessary rights to access the file path.

**For Android 11+ (API 30+):**
If you are accessing files outside app-specific storage (like the Downloads folder), you may need `MANAGE_EXTERNAL_STORAGE`.

**For Android 10 and below:**
Ensure you have `READ_EXTERNAL_STORAGE` permission.

**Helper Function included in Library:**

```kotlin
// You can check permission status using the library's built-in helper
if (pdfView.checkStoragePermission()) {
    pdfView.loadPdf(file)
} else {
    // This will open the Settings page for All Files Access (Android 11+)
    // or request standard permission (Android <11)
    pdfView.requestStoragePermission(this)
}
```

-----

## ⚠️ Important Configuration

To ensure smooth scrolling and prevent OutOfMemory errors on large PDFs, please enable `largeHeap` and `hardwareAccelerated` in your **app's** `AndroidManifest.xml`:

```xml
<application
    android:largeHeap="true"
    android:hardwareAccelerated="true"
    ... >
```

-----

## 🤝 Contributing

Contributions are welcome\! If you find a bug or want to improve the tiling algorithm, please fork the repository and submit a Pull Request.
