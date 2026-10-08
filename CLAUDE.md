# Android Project Instructions

## Git & repository workflow
- Push changes directly to `main`; do not open pull requests.
- Publish every completed change set as exactly one commit.

## General rules
- Do the work end to end.
- Do not undo settled decisions or weaken requirements without a real technical reason.
- Prefer root-cause fixes and the simplest architecture that fully satisfies the requirements.
- Remove dead code, unused dependencies, resources, permissions and configuration.
- Avoid unnecessary CPU, RAM, GPU, battery, storage, network use, allocations, I/O, polling and redundant state.
- Keep code clean, direct, idiomatic and maintainable.
- All code is written in English.
- Do not add explanatory or redundant comments inside code.
- Use the latest appropriate official technologies, APIs, libraries and tools, tracking the newest published build in any release channel, including preview, alpha, beta, release candidate and canary.
- A newer toolchain is only acceptable while the signed release APK still installs on a release device; never compile against a preview Android platform, which stamps the package as preview built and makes it unparseable on release builds.
- Verify current versions from official sources rather than relying on outdated examples.
- Do not add a numeric product version to the visible product identity, release name, APK filename, release tag, or API User-Agent unless explicitly requested.

## Android
- Use the latest appropriate official Kotlin, Jetpack Compose, AndroidX, Android SDK, Android Gradle Plugin, Gradle and supported JDK.
- The product design standard is the shared design system used by the owner's other projects (Adblock, GymNotes): the same colour tokens, type scale, corner radii, spacing scale, motion curves, component shapes and interaction states, ported to Compose.
- Every colour is shared with the siblings, the accent included: AppUpdater uses the same Apple system colours with systemBlue as the accent, and its launcher icon and TV banner carry a white glyph on the siblings' black gradient. It has no colour of its own.
- AppUpdater keeps a dedicated refresh button at the end of the top bar on the Updates tab with a static icon; this is a deliberate departure from Adblock, where tapping the status pill refreshes. Do not remove the button for the sake of parity.
- Pull to refresh reproduces the App Store's refresh control as measured from screen recordings, not Material's: the list follows the finger with UIScrollView's rubber band, the iOS style activity indicator stays pinned in the centre of the 60 dp band under the top bar, its spokes form clockwise from twelve o'clock between 40 and 150 dp of pull, and the check starts the moment they complete while the finger is still down. Every later check, whether started by a pull or the refresh button, holds the list 60 dp lower with the indicator spinning; it rolls half a turn while its chase tail fades in over one revolution, then steps clockwise one spoke at a time. When the check ends the indicator disappears at once and the list rises. Only a finger on the screen drives the pull; scrolling with a D-pad or keyboard never does, so on television the refresh button lowers and raises the list exactly as on a phone. Never put the indicator inside the refresh button.
- The first check after launch, while nothing is known yet, shows the App Store's loading state instead: the page stays empty for two seconds, then the 32 dp loading indicator, with a dim base and a four spoke tail that steps every 100 ms, fades in alone at the centre, with no label under it; when the result arrives it fades out and the content fades in. Pull to refresh stays off until that first result.
- The Updates list always shows the most recent updates at the top: it is ordered by publish date, the APKMirror upload time or Google Play's update date read in the session locale, and updates without a known date go to the bottom.
- AppUpdater has no status pill, a deliberate departure from Adblock and GymNotes. The number of available updates follows the Updates title in parentheses, as in Updates (154), both in the large title and in the centred top bar title that fades in on scroll, and is left out when there are none; a running check shows through the activity indicator and a failed one through the notice card. The Settings tab shows only its title in the top bar.
- With a D-pad or keyboard the lists never leave the focused item under the top bar or the tab bar: an item already in view stays put, otherwise it moves to the platform's television pivot, and focusing anything in the first item of a list scrolls it fully to the top so the large title shows.
- With a D-pad or keyboard the version codes are reached only by pressing towards them, left in left to right layouts, from one of an update card's buttons, and up and down from the buttons never land on them. While a version code is focused, up and down move between the version codes of the neighbouring cards and stop at the first and the last, so the refresh button and the tab bar are reached only from the buttons; pressing back returns to the button it came from, or to the card's first button, and selecting a code copies it.
- Navigation is always an Android app layout: the tabs stay in the floating tab bar at the bottom on every screen width, television included, with content across the width. Do not port Adblock's wide screen web layout, its side rail or its centred column.
- Build the interface from Compose foundation primitives so it matches that system exactly, rather than inheriting the visual defaults of Material 3; Material 3 remains the plumbing, not the appearance.
- Keep every value in the shared token set and apply it consistently across all screens and components; do not use dynamic color and do not introduce one-off colours, sizes, radii or durations.
- Text always uses the device's own default font through `FontFamily.Default`, whatever the platform or the manufacturer sets; never bundle, download or name a font family, and let the platform render the type scale weights instead of forcing font variation axes.
- Obtain values from the relevant official platform API whenever the system or the device can provide them at runtime, including device capabilities, application metadata such as icons, labels and package information, locales and configuration, instead of assuming manufacturer-specific behavior or duplicating them in resources and hardcoded mappings.
- Keep expensive work away from the UI thread.
- Prefer current Android APIs and remove obsolete compatibility layers and workarounds.
- Draw edge to edge and let the window insets place the content, rather than reserving space manually.
- Build visual effects with the official platform and Jetpack APIs only; never with a third party effect library.

## Verification
- Before considering a change complete, verify build/lint results, dependencies, resources, release configuration and relevant runtime behavior.

## Official references

### Design and UI
- Material Design 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Get started with Jetpack Compose: https://developer.android.com/develop/ui/compose/documentation
- App bars: https://developer.android.com/develop/ui/compose/components/app-bars
- State and Jetpack Compose: https://developer.android.com/develop/ui/compose/state
- Side effects in Compose: https://developer.android.com/develop/ui/compose/side-effects
- Lazy lists and grids: https://developer.android.com/develop/ui/compose/lists
- Animations in Compose: https://developer.android.com/develop/ui/compose/animation/quick-guide
- Adaptive apps: https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps
- Support different display sizes: https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
- Dynamic color: https://developer.android.com/develop/ui/views/theming/dynamic-colors
- Dark theme: https://developer.android.com/develop/ui/views/theming/darktheme

### Graphics and system surfaces
- Graphics in Compose: https://developer.android.com/develop/ui/compose/graphics/draw/overview
- Graphics modifiers: https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- Window insets: https://developer.android.com/develop/ui/compose/system/insets
- System bars: https://developer.android.com/develop/ui/compose/system/system-bars
- Edge to edge: https://developer.android.com/develop/ui/views/layout/edge-to-edge

### Platform behavior
- Permissions: https://developer.android.com/guide/topics/permissions/overview
- Package visibility: https://developer.android.com/training/package-visibility
- Background tasks: https://developer.android.com/develop/background-work/background-tasks
- Per-app language preferences: https://developer.android.com/guide/topics/resources/app-languages
- Predictive back gesture: https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- Kotlin coroutines on Android: https://developer.android.com/kotlin/coroutines

### Architecture, quality and testing
- Guide to app architecture: https://developer.android.com/topic/architecture
- Core app quality guidelines: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Test apps on Android: https://developer.android.com/training/testing

### Build and release
- Configure your build: https://developer.android.com/build
- Version catalogs: https://developer.android.com/build/migrate-to-catalogs
- App optimization with R8: https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
- Sign your app: https://developer.android.com/studio/publish/app-signing
- Platform and API versions: https://developer.android.com/tools/releases/platforms
- Latest Android developer updates: https://developer.android.com/latest-updates
- AndroidX releases: https://developer.android.com/jetpack/androidx/versions
- Compose releases: https://developer.android.com/jetpack/androidx/releases/compose
- Compose UI release notes: https://developer.android.com/jetpack/androidx/releases/compose-ui
- Android Gradle Plugin release notes: https://developer.android.com/build/releases/gradle-plugin
