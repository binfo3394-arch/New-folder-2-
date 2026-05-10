# Parental Monitoring App - Setup Guide

## 📁 Project Structure

```
monitoring-app/
├── child-app/          # Child device app (Android - Java)
├── parent-app/         # Parent device app (Android - Java)
└── firebase-rules.json # Firebase security rules
```

## 🔥 Firebase Setup

1. **Go to** [Firebase Console](https://console.firebase.google.com/)
2. **Create a new project** (or use existing)
3. **Enable Authentication:**
   - Go to Authentication > Sign-in method
   - Enable **Email/Password** sign-in
4. **Enable Realtime Database:**
   - Create in test mode first
   - Then apply rules from `firebase-rules.json`
5. **Enable Storage:**
   - Create with default rules
   - Apply storage rules from `firebase-rules.json`
6. **Register Android apps:**
   - Register **child-app** with package name: `com.monitor.child`
   - Register **parent-app** with package name: `com.monitor.parent`
   - Download `google-services.json` for each and place in:
     - `child-app/app/google-services.json`
     - `parent-app/app/google-services.json`

## 📱 How the pairing works

1. Both parent and child sign in with the **same email address**
2. The email is used as the key to link devices in Firebase
3. Parent sends commands through Firebase Realtime Database
4. Child listens for commands and responds (switch camera, start/stop)
5. Child uploads camera frames, audio chunks, and location to Firebase Storage
6. Parent polls for new data and displays it

## 🔧 Build & Install

### Child App:
```bash
cd child-app
./gradlew assembleDebug
# Install on child device: adb install app/build/outputs/apk/debug/app-debug.apk
```

### Parent App:
```bash
cd parent-app
./gradlew assembleDebug
# Install on parent device: adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Permissions (Child App)

The child app requires these runtime permissions:
- `CAMERA` - For front/back camera capture
- `RECORD_AUDIO` - For audio monitoring
- `ACCESS_FINE_LOCATION` - For GPS location
- `ACCESS_BACKGROUND_LOCATION` - For background location
- `POST_NOTIFICATIONS` - Android 13+ notification permission
- `FOREGROUND_SERVICE` - For background services

## 🎯 Features

| Feature | Child App | Parent App |
|---------|-----------|------------|
| Camera (Front/Back) | Captures & uploads frames | Views live feed |
| Audio | Records & uploads chunks | Plays audio |
| Location | Sends GPS coordinates | Shows on map |
| Remote Commands | Listens for commands | Sends commands |

## 📡 How the monitoring works

1. **Child app** runs foreground services that:
   - Capture camera frames every 2 seconds (front or back camera)
   - Record audio in 5-second chunks
   - Send GPS location every 10 seconds
   - All data is uploaded to Firebase Storage under the shared email key

2. **Parent app** polls for:
   - Latest camera frame every 3 seconds (displayed via Glide)
   - Latest audio chunk (auto-plays when new)
   - Latest location coordinates (tap to open in Google Maps)

3. **Parent can send commands:**
   - Switch camera (front/back)
   - Start/stop monitoring
   - Commands are sent via Firebase Realtime Database
