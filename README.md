# Average Price — Android app (Kotlin + Jetpack Compose)

A small Android app to track your stock buy entries and calculate your
weighted average price, with a field to enter the current LTP (Last
Traded Price) and see live P&L.

## What's included

- 5 buy-entry rows to start (Price + Qty each), with "+ Add another buy"
  for more, and a remove (×) button per row
- Live weighted average price, total quantity, and total invested
- LTP field with live P&L in ₹ and %, colored green/red
- Entries are saved on-device (SharedPreferences) so they persist
  between app launches
- "Clear all entries" with a confirmation dialog

## How to build it into a real .apk

You have two options — pick whichever is easier for you.

### Option A: Build in the cloud with GitHub Actions (no local install needed)

1. Create a free account at github.com if you don't have one.
2. Create a new repository (Settings can be public or private).
3. Upload every file/folder from this project into that repo — easiest
   way: on the repo page, click **Add file → Upload files**, then drag
   the entire unzipped `AvgPriceApp` folder contents in (make sure the
   hidden `.github` folder comes along — if your OS hides it, use
   "Show hidden files" first, or upload via `git` from a terminal
   instead: `git init`, `git add .`, `git commit -m init`,
   `git remote add origin <your-repo-url>`, `git push -u origin main`).
4. Go to the **Actions** tab of your repo. A workflow called
   "Build APK" should run automatically (or click **Run workflow** if
   it doesn't start on its own).
5. Wait for it to finish (a few minutes), then open the completed run
   and download the **AvgPrice-debug-apk** artifact from the bottom of
   the page — it's a zip containing `app-debug.apk`.
6. Transfer that `.apk` to your Android phone (email, Drive, USB, etc.)
   and tap it to install — you may need to allow "install from unknown
   sources" once.

This builds the app entirely on GitHub's servers, which already have
the Android SDK installed — nothing to install on your own computer.

### Option B: Build locally with Android Studio

You'll need **Android Studio** (free, from developer.android.com) —
that's the only thing required; it bundles everything else (Gradle,
Kotlin, the Android SDK).

1. Unzip this project.
2. Open Android Studio → **File → Open** → select the unzipped
   `AvgPriceApp` folder.
3. Android Studio will prompt to sync Gradle — let it. On first open it
   may also offer to regenerate the Gradle wrapper jar (this project
   ships the wrapper *properties* but not the binary jar, to keep the
   download small) — accept that prompt, or just let Android Studio use
   its own bundled Gradle, either works.
4. Once sync finishes, click the green **Run ▶** button with an emulator
   or your phone (via USB debugging) selected — this installs and
   launches the app directly.
5. To get an installable `.apk` file: **Build → Build App Bundle(s) /
   APK(s) → Build APK(s)**. Android Studio will show a notification with
   a link to the generated `.apk` (under `app/build/outputs/apk/debug/`).
   You can send that file to your phone and install it directly (you may
   need to allow "install from unknown sources" once).

## Project structure

```
AvgPriceApp/
├── app/
│   ├── build.gradle.kts          — app module config & dependencies
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/avgprice/
│       │   └── MainActivity.kt   — all app UI & logic (single file)
│       └── res/
│           ├── values/           — strings, theme
│           └── mipmap-*/         — app icon at each density
├── build.gradle.kts              — root project config
├── settings.gradle.kts
└── gradle.properties
```

## Customizing

- **App name / package**: change `applicationId`/`namespace` in
  `app/build.gradle.kts` and the package declaration at the top of
  `MainActivity.kt` if you want your own package id before publishing.
- **Colors**: all defined as named `Color(...)` constants near the top
  of `MainActivity.kt`.
- **Default row count**: change the `5` in `loadEntries()` and in the
  "Clear all entries" handler in `MainActivity.kt`.
