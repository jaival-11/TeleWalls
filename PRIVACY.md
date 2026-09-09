# Privacy Policy for TeleWalls

**Effective Date:** September 9, 2026  
**Developer:** Jaival  
**Repository:** [https://github.com/jaival-11/TeleWalls](https://github.com/jaival-11/TeleWalls)

This Privacy Policy explains how TeleWalls ("the App", "we", "our") handles information when you install and use the application. By installing and using TeleWalls, you acknowledge and agree to the data processing practices described in this document.

---

## 1. Zero Developer Data Collection Philosophy

TeleWalls is engineered with privacy by design. **The Developer does not collect, store, transmit, or harvest any of your personal data, credentials, usage metrics, or telemetry.**

* **No Telemetry or Tracking:** TeleWalls contains no third-party analytics SDKs, no crash reporting services, and no advertising or tracking scripts.
* **No Custom Crash Logs:** TeleWalls does not generate, log, or save custom crash log files to local device storage or remote servers. Standard Android OS system debug traces (Logcat) generated during unexpected application crashes remain strictly in volatile device memory managed by your operating system and never leave your device unless you explicitly export and share them manually.
* **No Developer Servers:** The Developer does not operate, host, or maintain any intermediary servers, proxy infrastructure, or user databases. All network requests are executed directly from your Android device to the relevant third-party endpoints (Telegram and GitHub).

---

## 2. How the App Processes Data & Third-Party Services

To provide seamless wallpaper management, cloud backup, dynamic color extraction, and application update capabilities, TeleWalls interacts with specific services as detailed below:

### A. Telegram Integration (TDLib & Storage Channels)
TeleWalls uses the official Telegram Database Library (**TDLib**) to interact directly with Telegram servers.

* **Account Authentication & Session Data:** When logging into TeleWalls, you may provide your Telegram API credentials (`api_id` and `api_hash`), phone number, and verification code (or 2FA password). These credentials and authentication session tokens are processed locally on your device via TDLib and stored in protected local app storage. They are **never** transmitted to or accessible by the Developer.
* **TeleWalls Cloud Channel Storage:** TeleWalls uses a Telegram channel (e.g., `TeleWalls Vault`) within your own Telegram account to store wallpaper images, metadata (title, category, tags, resolution, aspect ratio, color palette, author, description), category configurations (`#Categories`), and favorites (`#Favorites`).
* **Strict Scope of Account Access:** While TeleWalls requires your Telegram login to function, the application is strictly programmed to interact only with your designated channel(s). The App does not read, scan, download, export, or interact with your personal private chats, group messages, contacts, or any other Telegram data outside of its core wallpaper management functions.
* **Direct Network Transmission:** All authentication requests, wallpaper uploads, thumbnail downloads, and channel synchronizations occur directly between your device and Telegram’s official servers over MTProto-encrypted connections.
* **Privacy Impact & Third-Party Terms:** Because your device communicates directly with Telegram, your IP address and standard network metadata are processed by Telegram Messenger Inc., governed by the [Telegram Privacy Policy](https://telegram.org/privacy) and [Telegram API Terms of Service](https://core.telegram.org/api/terms).

### B. Application Update Checks (GitHub API)
To ensure you are running the latest version of TeleWalls, the App checks for software updates directly from GitHub.

* **The Process:** TeleWalls periodically queries the public GitHub REST API endpoint (`https://api.github.com/repos/jaival-11/TeleWalls/releases/latest`) to inspect release tags. If an update is available and requested, the application bundle (APK) is downloaded directly from GitHub's assets server.
* **Privacy Impact:** No user account information, personal data, or telemetry is sent to GitHub. However, standard network information (including your IP address and a standard `User-Agent` string) is transmitted directly to GitHub, Inc., governed by the [GitHub Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement).

***Note**: While the app uses secure protocols to protect your data during transmission, no method of communication over the internet is completely secure, and the developer cannot guarantee absolute security against unauthorised interception or server-side exploits.*

---

## 3. Android Permissions & Local Processing

TeleWalls requests only the minimum set of Android system permissions strictly necessary to perform its functions.

* `INTERNET` & `ACCESS_NETWORK_STATE`: Required to establish encrypted connections directly to Telegram servers (for fetching and backing up wallpapers) and GitHub (for app update checks).
* `SET_WALLPAPER` & `SET_WALLPAPER_HINTS`: Required to apply chosen wallpapers to your device's home screen, lock screen, or both, and to optimize target crop dimensions.
* `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`: Required only when you select local images from your device gallery to upload into your TeleWalls vault.
* `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`: Required on Android 13+ to maintain foreground execution and display real-time progress notifications during batch/mass wallpaper uploads.
* `REQUEST_INSTALL_PACKAGES`: Required to prompt the installation of downloaded application APK updates when triggered by the user.

**Local Media & Palette Processing:** Color palette extraction, thumbnail compression, metadata parsing, and search indexing are performed **100% locally** on your device processor. No image content or metadata leaves your device except when explicitly uploaded to your Telegram account.

---

## 4. Local Data Storage & Deletion

TeleWalls stores non-sensitive user preferences and cache files on your device using standard Android mechanisms:

* **SharedPreferences & DataStore (`telewalls_prefs`, `telewalls_settings`):** Stores user settings (e.g.,  hidden categories, animation preferences, active channel ID).
* **Room Database (`telewalls_db`):** Caches wallpaper metadata, categories, and favorite flags locally for fast offline searching and browsing.
* **TDLib Cache Directory (`context.filesDir/tdlib`):** Stores local TDLib session state and downloaded media cache.
* **App Cache (`context.cacheDir`):** Stores temporary wallpaper thumbnails and update installer files.

### Data Management & Erasure
The Developer claims no ownership or control over your data. You retain direct control over your information, which is managed entirely on your device or within your personal Telegram account:
* **Clear Cache:** You can clear cached thumbnails and images at any time via **Settings > Clear Image Cache** inside the App.
* **Reset App / Account Logout:** Logging out of Telegram inside the App invalidates the TDLib session. Performing **Settings > Reset / Clear All Data** or clearing app data through Android Settings permanently wipes all local databases, preferences, credentials, and cached files from your device.
* **Cloud Data Erasure:** Wallpapers and metadata backed up to your `TeleWalls Vault` channel can be deleted directly within TeleWalls or via any official Telegram client.

---

## 5. Third-Party Services & Legal Disclaimers

TeleWalls acts as an independent client application that connects to external platforms. The Developer has no control over, and assumes no responsibility for, the privacy practices or terms of service of third parties:

* **Telegram Messenger Inc.:** [Telegram Privacy Policy](https://telegram.org/privacy)
* **GitHub, Inc.:** [GitHub Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement)

*Disclaimer: TeleWalls is an independent open-source project and is not affiliated with, endorsed by, sponsored by, or approved by Telegram Messenger Inc. or GitHub, Inc.*

### Service Availability & Third-Party API Outage Disclaimer
TeleWalls relies entirely on third-party APIs and infrastructure provided by Telegram Messenger Inc. (TDLib/MTProto protocol) and GitHub, Inc. (REST API). The Developer is not responsible or liable for any app feature degradation, rate-limiting, protocol modifications, download failures, or service interruptions caused by third-party API changes, server downtime, or external outages beyond our control.

---

## 6. Regulatory Disclosures (GDPR / CCPA / CPRA / Indian DPDPA)

* **Data Controller & Data Processor:** Because the Developer does not receive, store, or process your personal data on any server infrastructure, TeleWalls operates under a zero-data-retention architecture. Your data remains strictly on your device or in your personal Telegram account.
* **General Data Protection Regulation (GDPR) & CCPA/CPRA:** Rights of access, correction, data portability, and erasure under the European GDPR and California CCPA/CPRA are directly satisfied by local app controls and your Telegram account management.
* **Indian Digital Personal Data Protection Act (DPDP Act, 2023):** As TeleWalls operates locally on your device without collecting, processing, or transmitting personal data to developer-controlled infrastructure, no personal data is processed by the Developer as a Data Fiduciary under the DPDPA. All user privacy rights, data autonomy, and consent principles outlined in India's DPDP Act, 2023 are natively respected.
* **No Sale or Sharing of Personal Data:** TeleWalls does not sell, rent, lease, or share personal information with third parties for monetary or other valuable consideration, nor for cross-context behavioral advertising.

---

## 7. Children's Privacy

TeleWalls is not intended for use by children under the age of 13 (or under the applicable legal age in your jurisdiction). The Developer does not knowingly collect personal data from children. Because all processing occurs locally or directly via your Telegram account, we do not have the capability to identify user age.

---

## 8. Changes to this Privacy Policy

This Privacy Policy may be updated periodically to reflect architectural changes or legal developments. Because TeleWalls does not collect user contact information, we cannot notify you directly. Material changes to this policy will be published in the application source repository and become effective upon publication. We encourage you to review this document periodically. Continued use of TeleWalls after any policy modification constitutes acceptance of the updated terms.

---

## 9. Contact Information

If you have questions, feedback, or concerns regarding this Privacy Policy or TeleWalls' technical practices, please contact us via:

* **Email:** `jaival7909@gmail.com`
* **GitHub Issue Tracker:** [https://github.com/jaival-11/TeleWalls/issues](https://github.com/jaival-11/TeleWalls/issues)

---

*This Privacy Policy was last updated on September 9, 2026.*


