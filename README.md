<div align="center">

<img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/icon.png" alt="TeleWalls app icon" width="200" />

# TeleWalls

### Personal Wallpaper Library

<br/>

[![Latest release](https://img.shields.io/github/v/release/jaival-11/TeleWalls?style=for-the-badge&labelColor=0d1117)](https://github.com/jaival-11/TeleWalls/releases/latest) [![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge&labelColor=0d1117&color=EA7233)](https://github.com/jaival-11/TeleWalls/blob/main/LICENSE)

<br/>

[![Follow on X](https://img.shields.io/badge/techironic11-1da1f2?style=for-the-badge&labelColor=0d1117&logo=x)](https://x.com/techironic11)[![Telegram Channel](https://img.shields.io/badge/Telegram-Join-0088cc?style=for-the-badge&labelColor=0d1117&logo=telegram)](https://t.me/techironic)[![Email Support](https://img.shields.io/badge/Email-Contact_Me-db4437?style=for-the-badge&labelColor=0d1117&logo=gmail)](mailto:jaival7909@gmail.com)

<br/>

[**Download**](#where-to-get-it) · [**Features**](#features) · [**Support**](#bug-reports--feature-suggestions)

---

## Table of Contents

| Section | Description |
| :--- | :--- |
| [Features](#features) | What the app can do |
| [Where to Get It (Download)](#where-to-get-it) | Download links for the latest stable APK |
| [Build from Source](#build-from-source) | Prerequisites and compilation instructions |
| [Contributions](#contributions) | Guidelines for contributing to the repository |
| [Bug Reports & Feature Suggestions](#bug-reports--feature-suggestions) | How to report issues or request new features |
| [Under the Hood](#under-the-hood-for-developers) | Tech stack and  core architecture |
| [Attributions & Credits](#attributions--credits) | Acknowledgments of third-party tools and creators |
| [Privacy Policy](#privacy-policy) | Link to the complete Privacy Policy |
| [Disclaimer](#disclaimer) | Terms of service, liability, and disclosures |
| [License](#license) | GNU General Public License v3.0 details |

</div>

---

<h2><a id="screenshots"></a>Screenshots</h2>

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot1.jpg" alt="Home tab" /></td>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot2.jpg" alt="Categories Tab" /></td>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot3.jpg" alt="Saved Wallpapers" /></td>
  </tr>
  <tr>
    <td align="center">Home tab</td>
    <td align="center">Categories Tab</td>
    <td align="center">Saved Wallpapers</td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot5.jpg" alt="Single Upload screen" /></td>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot6.jpg" alt="Multi upload" /></td>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot7.jpg" alt="Wallpaper Details" /></td>
  </tr>
  <tr>
    <td align="center">Single Upload screen</td>
    <td align="center">Multi upload</td>
    <td align="center">Wallpaper Details</td>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot8.jpg" alt="Manage Categories" /></td>
    <td align="center"><img src="https://raw.githubusercontent.com/jaival-11/TeleWalls/refs/heads/main/assets/screenshots/Screenshot4.jpg" alt="Settings" /></td>
    <td align="center"></td>
  </tr>
  <tr>
    <td align="center">Manage Categories</td>
    <td align="center">Settings</td>
    <td align="center"></td>
  </tr>
</table>



---

## Features

* **Personal Telegram Wallpaper Cloud:** Use your own Telegram channel as unlimited cloud storage for your custom wallpaper library.
* **Home Feed:** Explore your wallpaper library with high-quality preview grids and quick action controls.
* **Categories & Collections:** Organize wallpapers into customizable categories for effortless browsing.
* **Saved & Favorites:** Keep track of your favorite wallpapers in a dedicated tab for quick access and offline availability.
* **Single & Multi-Upload:** Upload single images or bulk upload multiple wallpapers at once directly to your Telegram channel.
* **One-Tap Wallpaper Set:** Apply wallpapers directly to your Home Screen, Lock Screen, or both.
* **Modern & Lightweight UI:** Built with Jetpack Compose and Material Design 3 featuring Monet dynamic color integration, fluid animations, and smooth screen transitions.
* **Privacy Respecting:** Fully self-hosted on your personal Telegram account with zero data collection or third-party tracking. For more info refer to [Privacy Policy](#privacy-policy).

---

## Where to Get It

You can download the latest compiled APK directly from the following sources:

<table>
  <tr>
    <th align="center">GitHub</th>
    <th align="center">Obtainium </th>  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/jaival-11/TeleWalls/releases/latest">
        <img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" alt="Download from GitHub Releases" height="70">
      </a>
    </td>    
    <td align="center">
      <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/jaival-11/TeleWalls">
        <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/refs/heads/main/assets/graphics/badge_obtainium.png" alt="Download from Obtainium" height="70">
      </a>
    </td>
  </tr>
 </table>

Builds are split per CPU architecture, so pick the one that matches your device:

| APK | Use it when |
| --- | --- |
| `TeleWalls-<version>-arm64-v8a-release.apk` | Almost every phone from the last several years |
| `TeleWalls-<version>-armeabi-v7a-release.apk` | Older 32-bit devices |
| `TeleWalls-<version>-x86_64-release.apk` | 64-bit emulators and x86 Chromebooks |
| `TeleWalls-<version>-x86-release.apk` | 32-bit x86, rare outside older emulators |
| `TeleWalls-<version>-universal-release.apk` | You are unsure, or sideloading somewhere unusual |

---

## Build from Source

If you prefer to compile the app yourself, you can easily build it from the source code using the included Gradle wrapper. 

**Prerequisites:**
* **Git**
* **JDK 17** (or higher)
* **Android SDK** (Command Line Tools, API level 35)
* The `ANDROID_HOME` environment variable must be set, pointing to your SDK path.
* You must have accepted the SDK licenses (e.g., running `yes | sdkmanager --licenses`).

**Compilation Commands:**

```bash
# Clone the repository
git clone https://github.com/jaival-11/TeleWalls.git
cd TeleWalls

# Build debug APK
./gradlew assembleDebug

# Build release APKs (both ABI splits and universal APK)
./gradlew assembleRelease
```

---

## Contributions

Contributions from the open-source community are always welcome!

1. Fork the repository.
2. Create a new branch for your feature (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'feat: add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request!

---

## Bug Reports & Feature Suggestions

Encountered a bug or have a brilliant idea to make TeleWalls even better?

* **Found a bug?**   

Open a [Bug Report](https://github.com/jaival-11/TeleWalls/issues/new?template=bug_report.yml) using the provided template. Please include necessary and correct details so it can be squashed quickly!  

* **Have an idea?**   

Open a [Feature Request](https://github.com/jaival-11/TeleWalls/issues/new?template=feature_request.yml) using the provided template and describe how it would improve the app. 

### Got any questions?

Feel free to open an [Issue](https://github.com/jaival-11/TeleWalls/issues/new/choose)

*You can also contact me directly via details given at the top, though [GitHub Issues](https://github.com/jaival-11/TeleWalls/issues) is preferred.*

---

## Under the hood (for Developers)

### Tech Stack

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material Design 3](https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-00599C?style=for-the-badge&logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room_DB-4285F4?style=for-the-badge&logo=android&logoColor=white)
![TDLib](https://img.shields.io/badge/TDLib-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)

### Core Architecture & APIs

TeleWalls is built using **Kotlin** and **Jetpack Compose** following modern Android development practices:

* **Telegram Integration (TDLib):** The app communicates directly with Telegram servers using **TDLib** (Telegram Database Library). Wallpapers are stored as documents in your private Telegram channel, serving as unlimited cloud storage.
* **Jetpack Compose & Material 3:** Declarative UI built with Jetpack Compose featuring Material Design 3, Monet dynamic color scheme integration, and smooth micro-animations.
* **Hilt Dependency Injection:** Hilt manages dependencies across ViewModels, Repositories, and UI components to maintain a clean MVVM architecture.
* **Local Persistence & Caching:** **Room DB** and **DataStore Preferences** handle local metadata indexing, wallpaper categorization, and offline preference caching.
* **Image Management & Palette Extraction:** **Coil** handles async image loading and caching, while **AndroidX Palette** extracts adaptive accent colors from wallpaper thumbnails.
* **Background Operations:** **WorkManager** schedules and executes background upload and sync tasks reliably.

---

## Attributions & Credits

This project stands on the shoulders of giants. A massive thank you to the developers, projects, and communities that make TeleWalls possible:

### 1. Telegram & TDLib
* **[TDLib (Telegram Database Library)](https://core.telegram.org/tdlib):** The official, cross-platform Telegram library by Telegram. It powers the secure communication with Telegram servers for cloud wallpaper storage.

### 2. UI & Framework
* **[Jetpack Compose](https://developer.android.com/compose) & [Kotlin](https://kotlinlang.org/):** By JetBrains and Google. Provides the modern declarative UI framework, coroutines, and reactive state management.
* **[Creamie](https://github.com/rajatt04/Creamie):** Used for UI design, layout structure, and theming inspiration.
* **[TeleDrive](https://github.com/Mahmud0808/TeleDrive):** Used as reference for TDLib integration and backend storage architecture.
* **[Material Design Icons](https://fonts.google.com/icons):** By Google. Provides the iconography across the app UI.

### 3. Third-Party Libraries
* **[Coil](https://github.com/coil-kt/coil):** Image loading library for Android powered by Kotlin Coroutines.
* **[Lottie Compose](https://github.com/Airbnb/lottie-android):** By Airbnb. Powers vector-based UI animations.
* **[AboutLibraries](https://github.com/mikepenz/AboutLibraries):** By Mike Penz. Powers the in-app open-source credits and license viewer.

---

## Privacy Policy
Read complete Privacy Policy at [Privacy Policy](PRIVACY.md).  
  
*The [Privacy Policy](PRIVACY.md) was last updated on 9th Sept, 2026.*

---

## Disclaimer

### Third-Party Affiliation
TeleWalls is an independent, open-source project. It is not affiliated with, endorsed by, or in any way officially connected to Telegram Messenger Inc. All product and company names are trademarks™ or registered® trademarks of their respective holders. Use of them does not imply any affiliation with or endorsement by them.

### Liability and Warranty
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. 

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.

### User Responsibility & Platform ToS
This application acts as a neutral, local wallpaper management tool. By utilizing TeleWalls, you acknowledge that you are solely responsible for ensuring your use of the software complies with the Terms of Service of any third-party platforms accessed (including Telegram). The developer assumes no liability for any account restrictions or ToS violations incurred by the user.

###  AI Disclaimer
This project— including the codebase, documentation was developed with the assistance of AI.

---

## License
TeleWalls is licensed under the [GNU General Public License v3.0](LICENSE).  
Copyright © 2026 Jaival

---

<div align="center">

**Made with ❤️ by [Jaival](https://github.com/jaival-11)**

</div>
