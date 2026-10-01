# Dima AI

Android chat app for the `Dima-dian-0.0.3` model.

Tap **Обучить** to load the built-in basic answers (usually under one second). The header also has toggles for automatic learning and Internet access. For generated, context-aware replies, add your own API key using the key icon. The app saves conversation and notes on the device.

## Build

Open the project in Android Studio, or build the debug APK with Gradle after installing Android SDK platform 35:

```sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Dictionary and search

The app bundles the full available JSON export of N. Abramov's Russian synonym dictionary: 19,430 entries and 77,204 synonyms, with definitions omitted. Entries contain their source-provided variants (up to 92); spelling suggestions use edit distance. The source data is distributed under MIT; its license is included at `app/src/main/assets/DICTIONARY_LICENSE.txt`. The export was published by Egor Rudinsky; the source notes conversion by Aleksandr Ilin.

The globe button enables or disables HTTPS access. Word and synonym lookup stays local; broader questions use Russian Wikipedia when no AI key is configured. The app does not expose hidden chain-of-thought.

## Generative AI

For natural, context-aware replies, open the key icon and add a personal Pollinations API key from `https://enter.pollinations.ai/keys`. The key is encrypted with Android Keystore. When Internet is enabled and a key is configured, the app sends the latest 12 chat messages and matching dictionary hints to the hosted `openai/gpt-5.4-nano` model. The service may apply account credit limits or charges. Without a key, local dictionary matching and Wikipedia lookup remain available.

GitHub Actions builds the APK on pushes and pull requests to `main` or `master`. You can also start a build from the Actions tab with **Build Android APK**. Download the `Dima-dian-0.0.3-debug-apk` artifact from the completed run.
