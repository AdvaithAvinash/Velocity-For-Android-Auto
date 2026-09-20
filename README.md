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
car-app APIs - which is exactly the gap AAEnabler and KingsInstaller exist to
work around. Velocity ships **two** faces that both need to work for the
whole chain to function:

1. **A normal phone app** (`home/`, `web/`, `youtube/`) - what you get
   tapping Velocity's icon on your phone. This part needs nothing special.
2. **A car-screen app** (`car/`), built on Google's own
   [androidx.car.app](https://developer.android.com/training/cars/navigation)
   framework, that's what actually renders on the head unit's display when
   Android Auto is projecting. This is the part AAEnabler and KingsInstaller
   are really unlocking - normally only Google-approved apps in a handful of
   categories (navigation, parking, EV charging, etc.) are allowed to bind a
   car-screen service at all, and video playback specifically isn't one of
   them. AAEnabler patches the phone's Android Auto app to stop checking that
   allowlist; KingsInstaller is what then lets you toggle Velocity into the
   car's app grid.

For YouTube specifically, the car screen uses the one loophole this
framework has: a `NavigationTemplate` is the only template the car host
hands your app a raw video `Surface` for (it's meant for drawing a map).
Velocity hands that `Surface` straight to ExoPlayer instead - the same trick
apps like **CarStream** use to get real video onto the car display at all.
Netflix and Stremio don't offer a video-surface-only path like that (they're
full interactive web apps), so the car screen instead mirrors an off-screen
WebView onto that same surface a few times a second and forwards taps/scrolls
back into it - the "screen mirroring" approach apps like **Screen2Auto** use.
It leans on the same lightweight extraction approach NewPipe uses for YouTube
instead of shipping a full browser-based YouTube client, which is what keeps
it fast on head units that are often years behind a modern phone.

## Project layout

```
app/src/main/java/com/velocity/auto/
  VelocityApp.kt               application entrypoint, initializes NewPipeExtractor
  home/                        phone-side app picker: model, repository, adapter, activity
  web/WebAppActivity.kt        phone-side full-screen WebView shell for Netflix/Stremio/custom apps
  youtube/                     phone-side search UI, results adapter, player activity
  youtube/extractor/           NewPipeExtractor downloader, search, and stream resolver (shared)
  car/                         the actual in-car screens (see "Why it's built this way")
    VelocityCarAppService.kt     bind point the Android Auto host looks for
    VelocitySession.kt           hands out the first screen
    HomeCarScreen.kt              car-side app grid (GridTemplate)
    YouTubeSearchCarScreen.kt     car-side YouTube search (SearchTemplate)
    YouTubePlayerCarScreen.kt     car-side YouTube playback (NavigationTemplate + raw Surface)
    WebMirrorCarScreen.kt         car-side Netflix/Stremio (NavigationTemplate + WebView mirroring)
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

## Installing (phone projecting to Android Auto)

The most common setup - a phone running the real Android Auto app, projecting
to a head unit's screen (aftermarket or OEM) - normally only lets
Google-approved apps show up on the car display at all. Two community tools
exist specifically to get around that, and Velocity is built to work with
both:

- **AAEnabler** patches your phone's Android Auto app to stop checking
  whether an app is on Google's approved list before letting it bind a
  car-screen service. Without this, Android Auto won't even consider showing
  Velocity, regardless of anything in Velocity itself.
- **KingsInstaller** is what you use to actually install Velocity's APK and,
  crucially, to open its **"customize launcher"** screen and toggle Velocity
  on so it appears in the car's app grid. It also handles the certificate/
  permission quirks these sideloaded, self-signed APKs tend to hit.

Steps:

1. **Install AAEnabler and KingsInstaller** on your phone from their usual
   community distribution channels (XDA, Telegram groups for your specific
   AAEnabler build). Different AAEnabler forks patch slightly different
   Android Auto versions, so match the build to your phone's Android Auto
   version if the tool asks.
2. **Run AAEnabler's unlock/patch step first.** This is what makes Velocity
   eligible to appear at all - do this before anything else.
3. **Install Velocity's APK through KingsInstaller** (not your phone's
   regular package installer) - open the APK from KingsInstaller so it can
   apply whatever signature/permission handling it does for sideloaded apps.
4. **Open KingsInstaller's "customize launcher" screen** and enable Velocity.
   This is the step that actually adds it to Android Auto's app grid; simply
   having it installed is not enough.
5. Plug into (or connect wirelessly to) your head unit and start Android
   Auto. Velocity's icon should now be on the app grid.

### If AAEnabler says Velocity has no Android Auto metadata

Earlier builds of Velocity genuinely didn't declare itself as a car app at
all - that's now fixed (see `car/VelocityCarAppService.kt` and
`res/xml/automotive_app_desc.xml`, and the `com.google.android.gms.car.application`
meta-data + `androidx.car.app.CarAppService` `<service>` in
`AndroidManifest.xml`). If you still see this error on a current build:

- Make sure you actually reinstalled the new APK - KingsInstaller sometimes
  needs an explicit uninstall-then-reinstall to pick up manifest changes
  rather than treating it as an update.
- Double-check your AAEnabler build's own requirements - some forks look for
  additional markers beyond the standard ones, or scan a specific manifest
  attribute format. That's undocumented and varies by fork; if this build
  still doesn't satisfy yours, please open an issue with the exact message.

### If Velocity doesn't show up in "customize launcher"

This is almost always the same root cause as above (no/incomplete car-app
metadata) rather than a separate problem - fix that first. If Velocity has
metadata and still doesn't list:

- Restart KingsInstaller (and if that doesn't help, the phone's Android Auto
  app / the phone itself) - these tools often cache the installed-app list.
- Confirm AAEnabler's patch is actually active for this Android Auto session
  - some patches don't survive an Android Auto app update and need
  re-running.

If a step above doesn't match your specific AAEnabler/KingsInstaller build,
follow that tool's own instructions for "add a third-party app to the
launcher" - the general shape (unlock with AAEnabler, install + enable with
KingsInstaller) holds even when the exact menus don't. These are unofficial,
undocumented, community-maintained tools with no public spec Velocity can
build against with certainty - if your specific build still refuses it after
the above, that's most likely a difference in what that fork checks for, and
worth filing as an issue with the exact error text.

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

`.github/workflows/release-apk.yml` builds and signs the APK on every push to
`main` and attaches it as a downloadable build artifact on that Actions run -
it no longer publishes a GitHub release automatically; that's a manual step
now.

## Status

This is a young project. The phone-side app (YouTube search/playback, the
Netflix/Stremio WebView shell, the custom-app picker) works end to end. The
car-screen side (`car/`) is newer and rougher: YouTube-in-car uses a solid,
well-supported mechanism (ExoPlayer rendering straight to the Surface the
car-app framework hands it), but the Netflix/Stremio WebView mirroring is
inherently more fragile - an unattached WebView's rendering isn't a fully
supported Android configuration, so quality/reliability will vary by device.
Whether *any* of the car-screen side actually appears on your head unit
still ultimately depends on your specific AAEnabler/KingsInstaller build,
which Velocity has no way to verify against ahead of time. There's plenty of
room for more (a proper settings screen, steering-wheel media key handling, a
queue, offline caching). Contributions welcome.

[![GitHub release](https://shields.io)](https://github.com)

