# Dima AI

Android chat app for the `Dima-dian-0.0.3` model.

Tap **Обучить** in the chat to spend one minute processing a larger set of built-in examples. The app saves the examples, conversation, and notes you ask it to remember on the device. It also remembers question-and-answer pairs it can match. This is a lightweight offline retrieval prototype, not a neural network; no model weights or inference API are connected yet.

## Build

Open the project in Android Studio, or build the debug APK with Gradle after installing Android SDK platform 35:

```sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the APK on pushes and pull requests to `main` or `master`. You can also start a build from the Actions tab with **Build Android APK**. Download the `Dima-dian-0.0.3-debug-apk` artifact from the completed run.
