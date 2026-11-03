<div align="center">

<img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Scann logo" width="110" />

# Scann

A fast, lightweight QR and barcode scanner and QR generator for Android.

<img src="https://img.shields.io/github/v/release/thekami-dev/Scanner?style=flat-square&label=release" alt="Latest release" />
<img src="https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
<img src="https://img.shields.io/badge/built%20with-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
<img src="https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square" alt="GPL-3.0 license" />

</div>

---

## About

Most scanner apps are slow to open, full of permissions you never asked for, or both. Scann does one job: point the camera at a code, see what is inside, and act on it.

You can also create your own QR codes, scan from a gallery image, and start scanning straight from a Quick Settings tile without opening the app first.

## App info

| | |
| --- | --- |
| Platform | Android 7.0+ (API 24) |
| Price | Free |
| License | GPL-3.0, open source |
| Package | `com.scann.thekami` |
| Account | Not needed |
| Ads | None |
| Your scans | Processed on your device |

## How it works

1. Open the app and point the camera at a QR code or barcode.
2. Scann reads it on-device and detects the content type: link, text, Wi-Fi, contact and so on.
3. Pick an action from the result screen, such as open, copy or share.
4. Need a code of your own? Use the Generator tab to create one and share it.

## Features

- Live camera scanning for QR codes and common barcode formats
- Scan from an image in your gallery
- QR code generator
- Smart result screen with actions based on the content type
- Quick Settings tiles for Scan and Create
- App shortcuts from the launcher icon
- Works offline
- Material 3 interface built with Jetpack Compose

## Technical info

| | |
| --- | --- |
| App | Kotlin, Jetpack Compose, Material 3 |
| Camera | [CameraX](https://developer.android.com/training/camerax) |
| Scanning | [ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) |
| Generating | [ZXing](https://github.com/zxing/zxing) core |
| Images | [Coil](https://coil-kt.github.io/coil/) |
| Min / target SDK | 24 / 36 |
| CI/CD | GitHub Actions: signed APK builds, GitHub Releases, Play Store publishing |

### Branches

| Branch | What it does |
| --- | --- |
| `test` | Lint, unit tests and a debug APK, available from the Actions run. No release. |
| `main` | Builds a signed release APK and publishes it as a GitHub Release (`v1.0.<n>`). |
| `store` | Publishes a GitHub Release (`store-v1.0.<n>`) and sends the build to Google Play Production. |

The version code is the git commit count, so history is never rewritten on release branches.

## Build from source

```bash
git clone https://github.com/thekami-dev/Scanner.git
cd Scanner
./gradlew assembleDebug
```

The debug APK is created in `app/build/outputs/apk/debug/`. Release builds need a signing setup (`key.properties` and a keystore), which is not included in the repository.

## Download

| Where | Link |
| --- | --- |
| Google Play | [Scann on Google Play](https://play.google.com/store/apps/details?id=com.scann.thekami) |
| APK | [Latest GitHub release](https://github.com/thekami-dev/Scanner/releases/latest) |

## Contributors

<p align="center">
  <a href="https://github.com/asikrshoudo"><img src="https://wsrv.nl/?url=github.com/asikrshoudo.png&w=96&h=96&fit=cover&mask=circle" width="48" height="48" alt="asikrshoudo" /></a>
  &nbsp;
  <a href="https://github.com/Ruhannn"><img src="https://wsrv.nl/?url=github.com/Ruhannn.png&w=96&h=96&fit=cover&mask=circle" width="48" height="48" alt="Ruhannn" /></a>
  &nbsp;
  <a href="https://github.com/tkami-dev"><img src="https://wsrv.nl/?url=github.com/tkami-dev.png&w=96&h=96&fit=cover&mask=circle" width="48" height="48" alt="tkami-dev" /></a>
</p>

Issues and pull requests are welcome.

## Privacy

Scanning and generating happen on your device, and scanned content is not uploaded by the app. The app shows no ads.

## Community

| Platform | Link |
| --- | --- |
| Discord | [Join the Thekami community](https://www.thekami.tech/discord/) |
| Instagram | [@thekami_official](https://www.instagram.com/thekami_official) |
| LinkedIn | [Thekami](https://www.linkedin.com/company/thekamiofficial) |
| Facebook | [thekamidev](https://www.facebook.com/thekamidev/) |
| GitHub | [thekami-dev](https://github.com/thekami-dev) |
| Support the project | [supportkori.com/thekami](https://www.supportkori.com/thekami) |

## License

GPL-3.0 © [Thekami](https://www.thekami.tech)

---

<p align="center">
  Made by <a href="https://www.thekami.tech"><img src="https://www.thekami.tech/logo.png" alt="Thekami" height="20" /></a>
</p>
