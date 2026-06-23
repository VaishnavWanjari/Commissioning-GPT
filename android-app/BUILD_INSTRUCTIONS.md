# MOM Recorder - Build Instructions

## Quick Build (Recommended: GitHub Actions)

The repository includes a GitHub Actions workflow that automatically builds the APK.

**Steps:**
1. Push this code to GitHub (already done on `claude/call-transcript-mom-app-aymd9n` branch)
2. Go to **Actions** tab in your repository
3. Run the **"Build MOM Recorder APK"** workflow
4. Download `MOMRecorder-debug-apk` from the workflow artifacts

---

## Local Build (Android Studio)

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API Level 34

### Steps
```bash
# 1. Open Android Studio
# 2. File > Open > select android-app/ folder
# 3. Wait for sync to complete
# 4. Build > Build Bundle(s) / APK(s) > Build APK(s)
# 5. APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Command Line Build
```bash
cd android-app

# On macOS/Linux:
./gradlew assembleDebug

# On Windows:
gradlew.bat assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## App Setup After Installation

### 1. Get a Claude API Key
- Visit [console.anthropic.com](https://console.anthropic.com)
- Create an account and generate an API key
- The key starts with `sk-ant-api03-...`

### 2. Configure the App
- Open the app → tap Settings (gear icon)
- Enter your Claude API key
- Select your default meeting platform
- Save settings

### 3. Start Recording
- Open a WhatsApp or Instagram video call on your phone
- Switch to MOM Recorder
- Tap the microphone button to start recording
- Place the phone so it can hear both sides of the call (speaker mode recommended)
- The transcript appears in real-time

### 4. Generate MOM
- After the call, tap "Generate MOM Report with AI"
- The AI analyzes the transcript and creates a professional MOM
- View, share as text, or export as PDF

---

## Technical Notes

### How Transcription Works
The app uses Android's built-in `SpeechRecognizer` (powered by Google) to transcribe speech in real-time from the device microphone. This captures both your voice and the speaker's voice (via the phone's speaker during calls).

**For best results:**
- Use speaker mode on your call app
- Keep the phone in a quiet environment  
- Speak clearly

### AI-Powered MOM Generation
The transcript is sent to the Claude AI API, which generates a structured MOM including:
- Executive summary
- Key decisions made
- Action items (with owner, due date, priority)
- Discussion points
- Next steps
- Next meeting date

### Privacy
- Audio is processed locally via Google's on-device Speech Recognition
- Only the text transcript (not audio) is sent to Claude API for MOM generation
- All data is stored only on your device

---

## Minimum Requirements
- Android 8.0 (API 26) or higher
- Microphone permission
- Internet connection (for AI-powered MOM generation)
- Google app installed (for speech recognition)
