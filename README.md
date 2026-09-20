# Velocity

NOTICE:- this project is made with the help of ai, if you do not like the use of ai, etc. please click off

Velocity is a fast, deliberately plain home screen for watching things on an
Android Auto / Android Automotive head unit: pick YouTube, Netflix, Stremio,
or any other streaming web app from one grid, and get straight into
playback. No feeds, no recommendations, no animated chrome - just an app
picker and a player.

It's built in the spirit of community head-unit apps like **CarStream** and
uses the same playback engine as **NewPipe** for YouTube, rather than
embedding the full YouTube website or app.

> **Driving safety.** Velocity plays video. Watching video while a vehicle is
> in motion is illegal in most places and dangerous everywhere. Use it only
> while parked, or as a passenger. Nothing here overrides your head unit's or
> vehicle's own driving-state locks, and it shouldn't.

> **Unofficial software.** Velocity, AAEnabler, and KingsInstaller are
> independent, community-maintained tools, not affiliated with Google,
> YouTube, Netflix, Stremio, or your head unit's manufacturer. Installing
> them may violate your unit's warranty or terms of use. Use at your own
> risk.

## What's on the home screen

- **YouTube** - search and play, using [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)
  to resolve real video/audio stream URLs and [Media3 ExoPlayer](https://developer.android.com/media/media3)
  to play them directly. No WebView, no ads, no heavyweight YouTube UI - just
  a search box, a results list, and a player.
- **Netflix** and **Stremio** (via [web.stremio.com](https://web.stremio.com/))
  - these don't offer a public playback SDK, so Velocity opens their own web
  apps in a stripped-down, full-screen WebView shell (no tabs, no address
  bar, no browser chrome).
- **Add app** - pin any other web app (Disney+, Max, a work dashboard,
  whatever) to the home grid with a name and a URL. It opens through the same
  WebView shell. Long-press a custom tile to remove it.

## Why it's built this way

Android Auto's official app categories don't allow general video playback
while driving, so there's no "supported" way to do this through Google's own
car-app APIs. Velocity is instead a normal Android app meant to be installed
directly onto the (usually Android-based) head unit itself - the same model
used by CarStream and similar community apps - and it leans on the same
lightweight extraction approach NewPipe uses for YouTube instead of shipping
a full browser-based YouTube client, which is what actually keeps it fast on
head-unit hardware that is often years behind a modern phone.

## Project layout

```
app/src/main/java/com/velocity/auto/
  VelocityApp.kt              application entrypoint, initializes NewPipeExtractor
  home/                        app picker: model, repository, adapter, activity
  web/WebAppActivity.kt        full-screen WebView shell for Netflix/Stremio/custom apps
  youtube/                     search UI, results adapter, player activity
  youtube/extractor/           NewPipeExtractor downloader, search, and stream resolver
  util/SystemUiHelper.kt       edge-to-edge + keep-screen-on helpers
```

## Building

You'll need Android Studio (Koala or newer) or a JDK 17 + Gradle 8.7 command
line setup.

1. Open the project root in Android Studio and let it sync (it will offer to
   generate the Gradle wrapper if it's missing) - or run `gradle wrapper
   --gradle-version 8.7` once yourself first.
2. Build a debug APK: `./gradlew assembleDebug`, or a release APK:
   `./gradlew assembleRelease` (unsigned unless you've configured a signing
   config).
3. The APK lands in `app/build/outputs/apk/debug/` or `.../release/`.

## Installing on a head unit

Most aftermarket and OEM "Android Auto" head units actually run a locked-down
Android build under the hood, but block installing your own APKs the normal
way - no Play Store account, no visible "unknown sources" toggle, sometimes
no accessible file manager either. Two community tools exist specifically to
get around that:

- **AAEnabler** unlocks the head unit itself: it exposes (or patches in) the
  developer/unknown-sources settings the manufacturer hid, so the unit will
  run apps that didn't come from its own preloaded app store.
- **KingsInstaller** is a sideloading and app-management tool built for these
  units. Once AAEnabler has unlocked installs, KingsInstaller is what you
  actually use to install Velocity's APK, grant it the permissions it needs,
  and (usually) set it to survive the unit's aggressive background-app
  killing.

Rough steps:

1. **Get AAEnabler and KingsInstaller** for your specific head unit/chipset
   from their usual community distribution channels (XDA, head-unit-specific
   forums, or Telegram groups dedicated to your unit's chipset). Builds are
   often chipset-specific, so make sure you grab the one matching your unit.
2. **Run AAEnabler first** and follow its unlock flow for your unit. This is
   what makes the next step possible at all.
3. **Get Velocity's APK onto the unit** - a USB drive works on most units, or
   KingsInstaller's own APK browser/fetcher if it has one.
4. **Install Velocity through KingsInstaller**, not through whatever stock
   file manager the unit has - KingsInstaller handles the certificate and
   permission quirks these units tend to have with self-signed APKs.
5. In KingsInstaller (or the unit's own app settings once Velocity is
   installed), grant Velocity network access, disable battery/background
   restrictions for it, and enable autostart if you want it to survive a
   reboot.
6. Launch **Velocity** from the unit's app drawer.

If a step above doesn't match your specific unit's version of AAEnabler or
KingsInstaller, follow that tool's own instructions for "install a
third-party APK" - the general shape (unlock with AAEnabler, install with
KingsInstaller) holds across most units even when the exact menus don't.

### Signing the APK

A debug build (`assembleDebug`) is signed automatically with Android's
default debug key and is enough for most testing. For a release build you
sideload long-term, sign it yourself - either through Android Studio's
*Build > Generate Signed Bundle / APK*, or with the included helper script,
which wraps [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer)
to stamp the APK with the full v1/v2/v3 signature set in one pass. That
broad compatibility matters here specifically because some installer tools
(KingsInstaller included, on certain builds) are pickier about signature
schemes than Android itself is:

```bash
./gradlew assembleRelease
# download a release jar from https://github.com/patrickfav/uber-apk-signer/releases
# to tools/uber-apk-signer.jar, then:
./scripts/sign-release.sh app/build/outputs/apk/release/app-release-unsigned.apk
```

See `scripts/sign-release.sh` for details, including how to pass your own
keystore instead of the throwaway one it generates by default.

## Status

This is a young project - straightforward YouTube search/playback, a
Netflix/Stremio WebView shell, and a custom-app picker all work end to end,
but there's plenty of room for more (a proper settings screen, steering-wheel
media key handling, a queue, offline caching). Contributions welcome.
