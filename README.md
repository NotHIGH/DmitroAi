# Dima AI

Android chat app with a pretrained Russian-capable Qwen3 model running locally through llama.cpp. No cloud AI key or account is required.

## First start

Tap **Qwen 1,28 ГБ** to download the quantized model once (about 1.28 GB). After it loads, replies are generated on-device and work offline. The download needs an Internet connection and at least 1.4 GB of free storage. Model loading needs about 2.5 GB of available RAM; speed depends on the phone's CPU and memory.

Use the globe button to enable or disable Internet access. With Internet enabled, send a public HTTPS page link in the chat to read and summarize its HTML/text content. PDFs and private/local addresses are not supported. Other unknown factual questions can fall back to Russian Wikipedia.

The app also saves chat history and explicit notes such as `Запомни, что я люблю космос` on the device. The local dictionary helps with synonyms and spelling suggestions; it is not used as a replacement for the model.

## Model and dictionary

The model is `Qwen3-1.7B-Q4_K_M` in GGUF format, converted by ggml-org from Qwen/Qwen3-1.7B. The model weights are downloaded separately and are not embedded in the APK. Qwen is licensed under Apache-2.0; see `app/src/main/assets/QWEN_MODEL_LICENSE.txt`. llama.cpp is licensed under MIT; see `app/src/main/assets/LLAMA_CPP_LICENSE.txt`.

The app includes N. Abramov's Russian synonym dictionary: 19,430 entries and 77,204 synonyms, without definitions. It is distributed under MIT; see `app/src/main/assets/DICTIONARY_LICENSE.txt`.

## Build

GitHub Actions installs Android SDK, NDK, and CMake, then builds the debug APK on pushes and pull requests to `main` or `master`. Start it manually from Actions with **Build Android APK**. Download `Dima-dian-0.0.3-debug-apk` from the completed run.

With the same Android SDK, NDK 29, and CMake 3.31.6 installed locally:

```sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`; it contains the app and native runtime, not the 1.28 GB model weights.
