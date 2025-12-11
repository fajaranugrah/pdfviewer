# Simple Android PDF Viewer 📄

[![](https://jitpack.io/v/User/Repo.svg)](https://jitpack.io/#User/Repo) A lightweight and highly customizable Android PDF Viewer library powered by **Mozilla's PDF.js** and **WebView**. 

This library is designed to overcome common issues when using PDF.js on Android, providing a native-like experience with smooth scrolling, high-fidelity rendering, and low memory usage.

## 🌟 Key Features

* **Native Gestures:** Supports smooth **Pinch-to-Zoom** and **Double Tap** to zoom.
* **Text Selection:** Select and copy text directly from the PDF (using a precise transparent text layer).
* **High Performance:** Uses **Canvas Tiling** to render high-resolution PDFs without hitting Android's GPU texture limits (no more blank/cut-off pages).
* **Lazy Loading:** Implements `IntersectionObserver` to render pages only when they enter the screen, keeping memory usage low.
* **Storage Support:** Handles files from Internal Storage and Downloads folder (Android 11+ Scoped Storage compatible).
* **No Native SDKs:** Pure Kotlin and WebView implementation.

## 📦 Installation

Add the JitPack repository to your project level `settings.gradle`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url '[https://jitpack.io](https://jitpack.io)' }
    }
}
