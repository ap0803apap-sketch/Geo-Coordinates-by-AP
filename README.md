📍 GeoCoordinates – GPS Location Tracker

GeoCoordinates is a powerful, privacy-focused Android application designed for high-precision location monitoring and remote device tracking.
Built with modern Android development technologies, the app provides reliable background location tracking, secure SMS-based triggers, and seamless cloud synchronization.

The application is optimized for accuracy, security, and battery efficiency, making it suitable for both personal tracking and advanced location logging use cases.

🚀 Features
📡 Real-Time Location Tracking

High-accuracy location monitoring using Foreground Services

Ensures continuous tracking even when the app is closed

Optimized for battery efficiency and stability

📩 SMS Remote Trigger

Remotely capture device location using a secure SMS trigger key

Allows location retrieval without opening the app

Useful for emergency tracking or remote monitoring

🛰 Multiple Location Providers

Choose the best method based on accuracy or battery consumption:

Fused Location Provider (Google Play Services) – Best accuracy and efficiency

Network Location Provider – Faster with moderate accuracy

GPS-only Mode – Maximum accuracy for outdoor tracking

📶 Offline Support

Automatically logs location data locally when internet is unavailable

Stores data in:

Room Database

Fallback text files

Syncs data automatically once connectivity is restored

☁ Cloud Synchronization

Google Drive integration for automatic backups

Securely syncs location history across devices

🔐 Advanced Security
Biometric Authentication

Protects location history with:

Fingerprint

Face Unlock

Device Administrator Mode

Optional protection that prevents unauthorized app uninstallation

🎨 Modern UI / UX

Built completely with Jetpack Compose

Uses Material Design 3

Supports Dynamic Colors (Android 12+)

Includes AMOLED Dark Mode

📊 Data Export

Export location history as CSV files

Useful for:

Data analysis

Mapping tools

External processing

🛠 Tech Stack
Category	Technology
Language	Kotlin
UI Framework	Jetpack Compose (Material 3)
Architecture	MVVM + Clean Architecture
Database	Room
Preferences	DataStore
Background Tasks	WorkManager
Networking	Retrofit + OkHttp
Google Services	Google Maps, GMS Location, Google Drive API
Dependency Management	Version Catalog (libs.versions.toml) + KSP
Logging	Timber
📱 Required Permissions

To enable full functionality, the app requires the following permissions:

Permission	Purpose
ACCESS_FINE_LOCATION	High-accuracy GPS tracking
ACCESS_BACKGROUND_LOCATION	Continuous background tracking
RECEIVE_SMS	Detect remote trigger messages
READ_SMS	Process SMS commands securely
FOREGROUND_SERVICE_LOCATION	Maintain active background tracking
USE_BIOMETRIC	Secure authentication
🛠 Installation
1️⃣ Clone the Repository
git clone https://github.com/ap0803apap-sketch/GeoCoordinates.git
2️⃣ Open in Android Studio

Use Android Studio Ladybug (or newer) for best compatibility.

3️⃣ Add Google Services Configuration

Place your google-services.json file in:

app/google-services.json

This is required for:

Google Maps

Google Drive Sync

4️⃣ Build and Run

Run the :app module on:

Android Emulator

Physical Android Device

📷 Screenshots

<img width="1080" height="2253" alt="Screenshot_20260311_205409_Geo coordinates by AP" src="https://github.com/user-attachments/assets/5b136e90-5a08-439b-b085-d2d415faa4a0" />

<img width="1080" height="3878" alt="Screenshot_20260311_205417_Geo coordinates by AP" src="https://github.com/user-attachments/assets/8d966210-4d87-48cf-a904-66ac7f673aec" />

🤝 Contributing

Contributions are welcome!
If you'd like to improve the project, feel free to:

Fork the repository

Create a feature branch

Submit a Pull Request

👨‍💻 Developer

AP

GitHub:
https://github.com/ap0803apap-sketch
