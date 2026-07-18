# 🏟️ FIFA AI Stadium Companion

FIFA AI Stadium Companion is an Android application designed to improve the football match-day experience using Artificial Intelligence. The app combines AI assistance with stadium-related services to help users access match information, navigate the stadium, explore services, and interact with event features through a modern mobile interface.

---

## Features

- 🤖 AI-powered Stadium Assistant using Google Gemini
- ⚽ Match information and football updates
- 🏟️ Stadium navigation interface
- 🚆 Travel planning section
- 🍔 Food & beverage browsing
- 🚨 Emergency assistance section
- 🎒 Lost & Found module
- 🤝 Volunteer services
- 📊 Stadium management dashboard
- 📱 Modern Jetpack Compose UI
- 🌙 Material Design 3 interface
- 📍 Responsive Android experience

---

## Tech Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- Google Gemini API
- Android Studio
- Gradle

---

# Requirements

- Android Studio
- Android SDK
- JDK 17
- Gemini API Key

---

# Running the Project

## 1. Clone the repository

```bash
git clone https://github.com/ToxApks/Fifa-AI-Stadium-Campanion-App.git
cd Fifa-AI-Stadium-Campanion-App
```

## 2. Open in Android Studio

- Open Android Studio.
- Select **Open**.
- Choose the project folder.
- Wait for Gradle Sync to complete.

## 3. Configure Environment Variables

Create a `.env` file in the project root.

Example:

```env
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

Refer to `.env.example` if available.

## 4. Update Gradle Configuration

If required, remove the following line from `app/build.gradle.kts`:

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

## 5. Run the App

- Connect an Android device or start an emulator.
- Click **Run ▶** in Android Studio.

---

## Run Locally

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device


---

## Developer

Developed by **Sanskar Patil**
