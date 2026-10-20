# 🎓 Smart Campus Silencer (ஆட்டோ காலேஜ் சைலன்சர்)

Smart Automated Ringer Profile Manager for College Students & Staff built using **Kotlin**, **Java**, **C++ (NDK)**, and **Python**.

---

## 📌 1. Client Problem (பிரச்சனை என்ன?)
1. **காலேஜ் போகும்போது மறதி**: காலேஜுக்கு போகும்போது மொபைலை Mute/Vibrate-ல் போட மறந்துடுறாங்க. கிளாஸ்ல போன் ரிங் அடிச்சு திட்டு வாங்குறாங்க.
2. **வீட்டுக்கு வந்ததும் மறதி**: அப்படியே கஷ்டப்பட்டு Mute போட்டாலும், சாயங்காலம் வீட்டுக்கு வந்ததும் Normal Mode-க்கு மாத்த மறந்துடுறாங்க. முக்கியமான கால், மெசேஜ் மிஸ் ஆகிடுது.
3. **Existing Third-party Apps-ல் உள்ள பெரிய குறைபாடு**:
   - Play Store-ல் இருக்கும் பெரும்பாலான timer apps **Fixed Days (Monday to Friday, 9 AM - 4 PM)** அடிப்படையில் மட்டுமே இயங்கும்.
   - **User காலேஜ் போகாத நாட்கள்** (Sick Leave, Semester Holidays, Rain Holiday, Strike) அன்றும் மொபைல் தானாக Silent-ல் போய்விடுகிறது! இதனால் வீட்டில் இருக்கும்போதும் முக்கியமான போன் கால்கள் கேட்காமல் போகிறது.
   - சனி/ஞாயிறு Special Class அல்லது Symposium போனால், அந்த ஆப் வேலை செய்யாமல் போன் சத்தமாக அடித்துவிடுகிறது.

---

## 💡 2. The Smart Solution (3 Automation Modes)

நமது **Smart Campus Silencer** 3 அதிநவீன முறைகளில் செயல்படுகிறது:

```
┌─────────────────────────────────────────────────────────────┐
│          Option 1: Mobile Network Cell Tower Shield         │
│   Reads current Cell Tower ID (CID) with ZERO battery drain │
│   and NO GPS! When connected to college tower -> Auto Mute  │
└──────────────────────────────┬──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          Option 2: Morning Interactive Prompt Shield        │
│   8:00 AM Notification: "Going to College?"                 │
│   [Yes, Going to College] -> Arms auto-mute                 │
│   [🏖️ On Leave]           -> Keeps normal ringer all day    │
└──────────────────────────────┬──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│      Option 3: Schedule + Auto Holidays + Weekdays          │
│   - Custom Monday to Sunday weekday chips                   │
│   - Tamil Nadu Government & University Holiday Database     │
│   - One-tap "Taking Leave Today" (resets at midnight)       │
│   - Optional Location Guard: checks campus GPS before mute  │
│   - ALWAYS restores normal ringer at college end time!      │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛡️ 3. Google Play Protect & Security Best Practices

To ensure maximum security and resolve Play Protect "Unrecognized app / Blocked" warnings when installing the direct APK:

### 🔒 Permission Minimization (Zero Spyware Heuristics)
Play Protect aggressively flags apps requesting risky combinations like `READ_PHONE_STATE` (device ID/IMEI) + `ACCESS_BACKGROUND_LOCATION` + `INTERNET`.

Smart Campus Silencer strictly adheres to permission minimization:
- **`READ_PHONE_STATE`**: ❌ **REMOVED**. Telephony cell info is read using coarse/fine location, eliminating device ID snooping flags.
- **`ACCESS_BACKGROUND_LOCATION`**: ❌ **REMOVED**. No continuous background GPS drain.
- **`ACCESS_WIFI_STATE` & `ACCESS_NETWORK_STATE`**: ❌ **REMOVED**.
- **`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`**: ❌ **REMOVED**.
- **`INTERNET`**: ❌ **REMOVED**. The app is **100% offline**, guaranteeing that no user data ever leaves the device.
- **Retained Permissions**: Only legitimate Android audio automation essentials:
  - `MODIFY_AUDIO_SETTINGS` & `ACCESS_NOTIFICATION_POLICY` (for DND & volume management)
  - `SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM` (for precision start/end time triggers)
  - `RECEIVE_BOOT_COMPLETED` (for restoring alarms on reboot)
  - `POST_NOTIFICATIONS` (for morning departure prompts)
  - `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` (strictly for the optional "Mute only at college" location guard)

### 🔑 Permanent Release Keystore Management
Android requires all updates to be signed with the **exact same cryptographic key**. Changing the keystore causes `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and flags the APK as unrecognized.

#### How the Keystore Was Generated:
```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -alias campussilencer \
  -storepass "<SECRET_KEYSTORE_PASSWORD>" \
  -keypass "<SECRET_KEY_PASSWORD>" \
  -dname "CN=Smart Campus Silencer, OU=Mobile Security, O=Campus Guardian, L=Chennai, ST=Tamil Nadu, C=IN"
```

> [!IMPORTANT]
> - **The keystore is valid for 10,000 days (until the year 2054).**
> - **Never commit `release.jks` or passwords into Git.**
> - In GitHub Actions, the keystore is safely encoded as a base64 secret (`KEYSTORE_BASE64`) and decoded strictly during release compilation.
> - **Always increment `versionCode` (e.g., 1 -> 2 -> 3) and `versionName` (e.g., 1.0.0 -> 1.1.0) in `android/app/build.gradle.kts` for each release.**

---

## 🔍 4. Verifying APK Signatures (apksigner)

You can verify that the release APK contains full v1 (JAR signing), v2 (APK Signature Scheme v2), and v3 (APK Signature Scheme v3) signatures:

```bash
# Verify signature and print certificate info:
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose --print-certs SmartCampusSilencer.apk
```

**Expected Verification Output:**
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Verified using v4 scheme: false
Number of signers: 1
Signer #1 certificate DN: CN=Smart Campus Silencer, OU=Mobile Security, O=Campus Guardian, L=Chennai, ST=Tamil Nadu, C=IN
Signer #1 key algorithm: RSA (4096 bits)
```

---

## 🚀 5. How to Permanently Remove Play Protect Warnings

There are two legitimate paths to clear Google Play Protect warnings on client devices:

### Method (a): Google Play Console Track (Recommended for Full Trust)
1. Register a Google Play Developer account.
2. Upload the signed release APK or Android App Bundle (AAB) to **Internal Testing** or **Closed Testing**.
3. Create an internal tester email list or share the closed testing join link with students/staff.
4. **Result:** Because Google's servers inspect and sign off on the app during console upload, **Play Protect warnings are completely eliminated** across all devices worldwide.

### Method (b): Direct APK Sideloading (One-Time Trust Onboarding)
For direct distribution without publishing to the Play Store:
1. On first installation, Google Play Protect may show: *"Blocked by Play Protect / Unrecognized app"* (standard for newly minted developer certificates).
2. Tap **"More details"** ➔ **"Install anyway"**.
3. Allow the app to be scanned by Play Protect.
4. Because the app has **zero internet permission, zero spyware permissions, and clean R8 minified code**, Play Protect's cloud reputation system establishes that the signature is completely benign. Subsequent updates signed with the same permanent keystore install cleanly.

---

## 🛠️ 6. Multi-Language Architecture (4 Languages)

| Language | Role / Layer | Key Files | Purpose |
|---|---|---|---|
| **Kotlin** | Android Modern UI & State | `MainActivity.kt`, `CampusSilencerApp.kt`, `PowerSaveThemeHelper.kt` | Modern AMOLED / Luxury Light UI, Exact Alarms, WorkManager midnight sync |
| **Java** | Android System & Receiver Layer | `AudioModeController.java`, `BootCompletedReceiver.java` | System Audio / DND Policy Management, Device Reboot auto-restart |
| **C++ (NDK)** | High-Performance Native Math | `native-geo.cpp`, `native-geo.h`, `CMakeLists.txt` | Ultra-fast Haversine distance, polygon boundary detection |
| **Python** | Academic Backend & Simulator | `holiday_sync.py`, `timetable_parser.py`, `api_server.py`, `test_simulator.py` | Academic holiday scraper, REST API for daily timetable sync, CLI simulator |

---

## 📂 7. Project Directory Structure

```
smart_college_silencer/
├── README.md                              <- Full Documentation (English & Tamil)
├── .github/workflows/
│   └── build_apk.yml                      <- Signed Release APK GitHub Actions Workflow
├── android/                               <- Complete Android Native Project
│   ├── app/
│   │   ├── CMakeLists.txt                 <- C++ NDK CMake build script
│   │   ├── build.gradle.kts               <- App-level gradle build config (v1+v2+v3 signing, R8)
│   │   ├── proguard-rules.pro             <- ProGuard / R8 keep rules
│   │   └── src/main/
│   │       ├── AndroidManifest.xml        <- Minimized Permissions (DND, Alarms, Boot)
│   │       ├── cpp/                       <- C++ (Native Geofencing Engine)
│   │       │   ├── native-geo.h
│   │       │   └── native-geo.cpp
│   │       ├── java/com/campussilencer/app/
│   │       │   ├── audio/                 <- Java (Audio Automation)
│   │       │   │   └── AudioModeController.java
│   │       │   └── receiver/              <- Java (Boot Receiver)
│   │       │       └── BootCompletedReceiver.java
│   │       ├── kotlin/com/campussilencer/app/
│   │       │   ├── CampusSilencerApp.kt   <- Kotlin Application & WorkManager
│   │       │   ├── MainActivity.kt        <- Kotlin Modern UI Dashboard & Transparency Dialogs
│   │       │   ├── data/CampusConfig.kt   <- Kotlin Data Models & Settings
│   │       │   ├── nativebridge/NativeGeoEngine.kt <- JNI Bridge to C++
│   │       │   └── service/               <- Background Alarm Scheduling
│   │       │       ├── CampusAlarmScheduler.kt
│   │       │       ├── CampusAlarmReceiver.kt
│   │       │       ├── CellTowerHelper.kt
│   │       │       └── SmartSilencerWorker.kt
│   │       └── res/
│   │           ├── layout/activity_main.xml
│   │           └── values/colors.xml, strings.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── python_companion/                      <- Python Companion & Test Simulator
    ├── holiday_sync.py                    <- Tamil Nadu & Indian College Holiday Tracker
    ├── timetable_parser.py                <- Class Schedule Hours Validator
    ├── api_server.py                      <- Local REST API Server (port 8000)
    ├── test_simulator.py                  <- End-to-End Problem Demonstration Simulator
    └── requirements.txt
```

---

## ⚡ 8. How to Build Signed Release APK Locally

To build a signed release APK on your machine:

```bash
cd android
./gradlew assembleRelease --no-daemon
```

The resulting signed release APK will be located at:
`android/app/build/outputs/apk/release/SmartCampusSilencer.apk`
