# eleeth.com TV

Native Android IPTV player built from scratch for eleeth.com.

**APK size:** ~9.7 MB · **Package:** `com.eleeth.tv2` · **Min Android:** 7.0 (API 24)

## Features

- **13,274 channels** loaded from `iptv.m3u` (bundled in `assets/`, also reads from `/mnt/ftpstorage/iptv.m3u` on device)
- **HLS playback** via ExoPlayer 2.19.1
- **Search bar** with live filter and clear-X button
- **Category chips** (All / Movies / News / Sports / Music / Kids / Entertainment)
- **Channel grid** — 2 cols portrait, 3 cols landscape, 4 cols tablets
- **Persistent video box** at the top of the screen (always visible)
- **Fullscreen button** (icon-only) — rotates app to landscape, fills screen
- **Stop button** (icon-only) — releases the player and clears the now-playing strip
- **Ko-fi tip button** — opens https://ko-fi.com/eleeth in browser
- **Real-time filter** across channel name AND category group
- **Eleeth dark theme** — gold accent `#C8A97E` on `#0D0D12`

## Architecture

Single-activity Kotlin app:
- `MainActivity` — all UI, player, search, filtering
- `Channel` — data class (Parcelable, but only used internally)
- `ChannelAdapter` — plain `RecyclerView.Adapter` (no DiffUtil) so search reflows reliably
- `SplashActivity` — not used; MainActivity is the launcher directly

The player is a **single `PlayerView`** that's moved between containers (`videoContainer` ↔ `fullscreenOverlay`) so ExoPlayer's surface stays attached across fullscreen toggles.

## Build

Requires Android SDK 34, JDK 17, Gradle 8.7.

```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Sign

Generate a keystore once:
```bash
keytool -genkey -v -keystore your.keystore -alias YOUR_ALIAS   -keyalg RSA -keysize 2048 -validity 10000   -storepass YOUR_PASS -keypass YOUR_PASS   -dname "CN=your-name,O=your-org,C=US"
```

Zipalign + sign:
```bash
zipalign -p 4 app/build/outputs/apk/debug/app-debug.apk aligned.apk
apksigner sign --ks your.keystore   --ks-pass pass:YOUR_PASS --key-pass pass:YOUR_PASS   --out eleeth.com-TV.apk aligned.apk
```

## Asset sources

- **iptv.m3u** — bundled in `app/src/main/assets/iptv.m3u` (also read from `/mnt/ftpstorage/iptv.m3u` if present on device)
- **icon.png** — bundled at all mipmap densities (mdpi through xxxhdpi), plus adaptive icon XML (`mipmap-anydpi-v26/ic_launcher.xml`)

## Ko-fi tip

The Tip button opens `https://ko-fi.com/eleeth` — a hard-coded link, no API key needed.

## License

Personal project for eleeth.com. All rights reserved.

---


## ❤️ Support the Project

If eleeth TV saves you time or brings value, consider supporting development:

[![GitHub Sponsors](https://img.shields.io/badge/GitHub_Sponsors-Sponsor-ea4aaa?style=for-the-badge&logo=github)](https://github.com/sponsors/eleeths)
[![Sponsor via PayPal](https://img.shields.io/badge/KoFi-Sponsor-yellow?style=for-the-badge&logo=paypal)](https://ko-fi.com/eleeth)
