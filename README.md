# Dima AI

Android chat app for the `Dima-dian-0.0.3` model.

Tap **Обучить** to load the built-in basic answers (usually under one second). Afterward, the header button toggles automatic learning from matched chat answers on or off. The app saves examples, conversation, and notes you ask it to remember on the device. This is a lightweight offline retrieval prototype, not a neural network; no model weights or inference API are connected yet.

## Build

Open the project in Android Studio, or build the debug APK with Gradle after installing Android SDK platform 35:

```sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Dictionary and search

The app bundles the full available JSON export of N. Abramov's Russian synonym dictionary: 19,430 entries and 77,204 synonyms, with definitions omitted. Entries contain their source-provided variants (up to 92); spelling suggestions use edit distance. The source data is distributed under MIT; its license is included at `app/src/main/assets/DICTIONARY_LICENSE.txt`. The export was published by Egor Rudinsky; the source notes conversion by Aleksandr Ilin.

The globe button enables or disables HTTPS search in Russian Wikipedia. Word and synonym lookup stays local; broader questions can use Wikipedia when enabled. This app does not include a generative language model and does not expose hidden chain-of-thought.

## Dictionary data

The app includes the Russian synonym dictionary by N. Abramov, converted to JSON by Aleksandr Ilin and published by Egor Rudinsky. The bundled file contains 19,430 entries and 77,204 synonyms, without definitions. The dataset is distributed under MIT; its license is included at `app/src/main/assets/DICTIONARY_LICENSE.txt`.

Unknown lookups can use Russian Wikipedia over HTTPS when the Internet toggle is enabled. Queries and answers are sent to Wikipedia; the app does not use a generative language model.

GitHub Actions builds the APK on pushes and pull requests to `main` or `master`. You can also start a build from the Actions tab with **Build Android APK**. Download the `Dima-dian-0.0.3-debug-apk` artifact from the completed run.
