# Vyaay (व्यय) - UPI Payment Tracker 🚀💸

Vyaay is a powerful, privacy-focused Android application designed to track your UPI expenses automatically by analyzing incoming SMS alerts. It helps you manage your finances with modern tools like OCR scanning, shared wallets, and insightful visualizations.

## ✨ Features

- **📱 Automatic SMS Tracking**: Real-time detection and categorization of UPI payment alerts.
- **🛡️ Biometric Security**: Fingerprint and FaceID protection to keep your financial data private.
- **📸 OCR Receipt Scanner**: Scan physical bills using Google ML Kit to automatically extract amounts and merchants.
- **🤝 Shared Wallet & Split Bill**: Easily split expenses with partners or flatmates and sync via secure codes.
- **📊 Insights & Analytics**:
  - **Dynamic Dashboard**: Total spending overview with category-wise breakdown.
  - **Heatmap Calendar**: Visual representation of your daily spending intensity.
  - **Recurring Subscriptions**: Identify and track annual costs for services like Netflix and Spotify.
- **🎨 Custom Themes**: Toggle between Light, Dark, **Retro 90s (Win 95 style)**, and **Neon Night** modes.
- **📅 Monthly Navigation**: View and filter your history month by month.
- **📄 Multi-Format Export**: Export your spending reports to PDF, Excel (CSV), Word, or PPT.
- **📍 Map Visualization**: See where you spend your money with location-based markers.
- **🚫 Smart Spam Filter**: Automatically ignores marketing and loan-related SMS spam.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern Declarative UI)
- **Database**: Room Persistence Library (Local SQLite)
- **OCR**: Google ML Kit (Text Recognition)
- **Maps**: Google Maps SDK for Android
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **DI**: ViewModel Factory & Repository Pattern.

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android Device / Emulator (SDK 24+)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Vyaay.git
   ```
2. Open the project in Android Studio.
3. Build and Run on your device.

*Note: For Map functionality, add your Google Maps API Key in `AndroidManifest.xml`.*

## 🔒 Privacy
Vyaay values your privacy. All data is stored locally on your device in an encrypted-at-rest manner using Android's security best practices. Biometric authentication ensures only you can access the app.

<img width="400" height="600" alt="image" src="https://github.com/user-attachments/assets/23e92514-1897-411b-9188-59f4be92bfba" />


---
Made with ❤️ by Ayush
