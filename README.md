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

## 💡 2. The Smart Solution (நமது தீர்வு முறை)
நமது **Smart Campus Silencer** வெறும் நேரத்தை (Fixed Timer) மட்டும் நம்பாமல், **3-Layer Hybrid Smart Trigger** முறையைப் பயன்படுத்துகிறது:

```
┌─────────────────────────────────────────────────────────────┐
│                   Layer 1: GPS Geofencing                   │
│   C++ Native Engine calculates distance to college campus   │
│   (Inside College Boundary -> Mute, Inside Home -> Unmute)  │
└──────────────────────────────┬──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Layer 2: Wi-Fi SSID Fallback                │
│    College Wi-Fi Connected -> Instantly Mute (Zero Battery)  │
│    Home Wi-Fi Connected    -> Instantly Restore Ringer      │
└──────────────────────────────┬──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          Layer 3: Academic Calendar & Holiday Sync          │
│   Python Companion API checks Holidays & "Leave Today" flag │
│   If student is at Home on Monday -> Phone NEVER Mutes!     │
└─────────────────────────────────────────────────────────────┘
```

1. **GPS Geofence (College Campus Radius)**: மாணவர் நிஜமாகவே காலேஜ் கேம்பஸுக்குள் நுழையும் போது மட்டுமே போன் Mute ஆகும். வீட்டில் இருக்கும் நாட்களில் திங்கட்கிழமையாக இருந்தாலும் மொபைல் Normal-ஆகவே இருக்கும்!
2. **Auto-Restore at Home**: காலேஜை விட்டு கிளம்பினாலோ அல்லது வீட்டுக்கு வந்தாலோ முந்தைய சத்தத்திலேயே (Original Volume Level) Ringtone தானாக ஆன் ஆகிவிடும்.
3. **One-Tap "Taking Leave Today"**: உடல்நிலை சரியில்லாத நாட்களிலோ அல்லது லீவு போட்டாலோ ஆப்பில் ஒரு முறை டாப் செய்தால் அன்றைய தினம் Silent Mode முழுமையாக Pause செய்யப்படும்.
4. **College Wi-Fi Detection**: காலேஜ் Wi-Fi கனெக்ட் ஆனால் உடனடியாக Silent ஆகும் (Battery மிச்சமாகும்).

---

## 🛠️ 3. Multi-Language Architecture (4 மொழிகளின் பயன்பாடு)

| Language | Role / Layer | Key Files | Purpose |
|---|---|---|---|
| **Kotlin** | Android Modern UI & State | `MainActivity.kt`, `CampusGeofenceService.kt`, `CampusSilencerApp.kt` | Jetpack UI, Foreground Location Tracking, WorkManager Background Tasks |
| **Java** | Android System & Receiver Layer | `AudioModeController.java`, `WifiStateReceiver.java`, `BootCompletedReceiver.java` | System Audio / DND Policy Management, Device Reboot auto-restart, Wi-Fi listener |
| **C++ (NDK)** | High-Performance Native Math | `native-geo.cpp`, `native-geo.h`, `CMakeLists.txt` | Ultra-fast Haversine distance, polygon boundary detection, battery-saving GPS interval logic |
| **Python** | Academic Backend & Simulator | `holiday_sync.py`, `timetable_parser.py`, `api_server.py`, `test_simulator.py` | Academic holiday scraper, REST API for daily timetable sync, CLI end-to-end simulator |

---

## 📂 4. Project Directory Structure

```
smart_college_silencer/
├── README.md                              <- Full Documentation (English & Tanglish)
├── android/                               <- Complete Android Native Project
│   ├── app/
│   │   ├── CMakeLists.txt                 <- C++ NDK CMake build script
│   │   ├── build.gradle.kts               <- App-level gradle build config
│   │   └── src/main/
│   │       ├── AndroidManifest.xml        <- Permissions (Location, DND, Wi-Fi, Boot)
│   │       ├── cpp/                       <- C++ (Native Geofencing Engine)
│   │       │   ├── native-geo.h
│   │       │   └── native-geo.cpp
│   │       ├── java/com/campussilencer/app/
│   │       │   ├── audio/                 <- Java (Audio & Wi-Fi Automation)
│   │       │   │   ├── AudioModeController.java
│   │       │   │   └── WifiStateReceiver.java
│   │       │   └── receiver/              <- Java (Boot Receiver)
│   │       │       └── BootCompletedReceiver.java
│   │       ├── kotlin/com/campussilencer/app/
│   │       │   ├── CampusSilencerApp.kt   <- Kotlin Application & WorkManager
│   │       │   ├── MainActivity.kt        <- Kotlin UI Dashboard
│   │       │   ├── data/CampusConfig.kt   <- Kotlin Data Models & Settings
│   │       │   ├── nativebridge/NativeGeoEngine.kt <- JNI Bridge to C++
│   │       │   └── service/               <- Background Tracking Services
│   │       │       ├── CampusGeofenceService.kt
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

## ⚡ 5. How to Run & Verify

### Step 1: Run the Problem Resolution Simulator (Python)
நீங்கள் எந்த Android போன் அல்லது எமுலேட்டரும் இல்லாமல் உடனே இந்த தீர்வு எப்படி செயல்படுகிறது என்பதை சோதிக்கலாம்:

```bash
cd "C:\Users\acer\.gemini\antigravity-ide\scratch\smart_college_silencer\python_companion"
python test_simulator.py
```

**Simulator output confirms:**
- **Scenario 1**: காலேஜுக்குள் நுழைந்ததும் `Entered College Campus! Phone AUTOMATICALLY MUTED.` வீட்டுக்கு வந்ததும் `Arrived Back Home! Normal Ringer AUTOMATICALLY RESTORED.`
- **Scenario 2 (User's Exact Issue)**: திங்கட்கிழமை லீவு அல்லது காலேஜ் போகாத நாளில் பயனர் வீட்டில் இருக்கும்போது `Ringer State: NORMAL (Ringer ON 80%)` — போன் தேவையில்லாமல் Silent ஆகாது!
- **Scenario 3**: சனிக்கிழமை ஸ்பெஷல் கிளாஸ் போனால் தானாக Mute ஆகும்.

---

### Step 2: Run the Python Sync API Server
```bash
python api_server.py
```
- API starts at `http://localhost:8000`
- Provides `/api/holidays/today` மற்றும் `/api/timetable/today` endpoints.

---

### Step 3: Open and Run in Android Studio
1. Open **Android Studio**.
2. Select **Open an Existing Project** and choose:
   `C:\Users\acer\.gemini\antigravity-ide\scratch\smart_college_silencer\android`
3. Gradle will automatically link:
   - Kotlin source files
   - Java receivers
   - C++ NDK library via `CMakeLists.txt`
4. Run on your Android Device or Emulator.
5. In the app:
   - Click **"Grant DND / Silent Permission"** (Allow Notification Policy Access).
   - Click **"Start Background Monitoring"**.
   - Your college location is protected and battery is conserved!
