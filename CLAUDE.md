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
- The product design is Google's Material 3 Expressive, built from the official Compose Material 3 components inside `MaterialExpressiveTheme` with `MotionScheme.expressive()`.
- Every colour comes from the device's dynamic colour scheme, `dynamicLightColorScheme` or `dynamicDarkColorScheme`, which the minimum SDK always provides, so there is no fallback palette and no hand picked colour in code. Pages use `surfaceContainer`, the top app bar turns `surfaceContainerHigh` when content scrolls under it, and list items keep the segmented items' `surface`. The window background follows the same system colour resources, and the launcher icon and TV banner carry a white glyph on `system_accent1_600`, with a monochrome layer for themed icons.
- AppUpdater keeps a refresh icon button in the top app bar actions on the Updates tab, because pull to refresh needs a finger and a television has none. Do not remove it.
- Pull to refresh is Material's `PullToRefreshBox` with the Expressive `PullToRefreshDefaults.LoadingIndicator` under the top app bar. Every later check, whether started by a pull, the refresh button or Try again, shows the same indicator until it ends. The top app bar's nested scroll connection sits on the list itself, nearer than the pull, so pulling down first expands the large title and only then refreshes.
- The first check after launch, while nothing is known yet, shows only the Expressive `LoadingIndicator` at the centre, fading in and out with the motion scheme's effects spec. Pull to refresh stays off until that first result.
- The Updates list always shows the most recent updates at the top: it is ordered by publish date, the APKMirror upload time or Google Play's update date read in the session locale, and updates without a known date go to the bottom.
- An APKMirror variant is never hidden because of version code digits. The selector only filters for what can actually install and is the same build: a newer version code, a supported SDK, ABI and form factor, a matching signature, the same flavour, which is read after the version number on both sides, and the same family. When the release's builds that pass the other filters, whatever their ABI, span several minimum SDKs, only the installed package's minimum SDK qualifies, or the lowest one when the app raised its minimum, and otherwise the release is skipped until its family is uploaded, so Google Play services never swaps its Android 15+ family for the Android 9+ one; a release with a single minimum SDK always qualifies, because Google Play can rewrite the minimum SDK of a bundle it installs. It then ranks the remaining variants by the device's ABI order, a shared version code suffix of at least two digits, density, and finally the newest code. Version numbers keep alphanumeric parts and drop a leading v. The selector stays pure and covered by JVM unit tests.
- APKMirror stays offered for every app, apps that take the device's language from a language split included; never hide it for the sake of the language. Its API carries no language data and does not say whether a variant is an APK or a bundle, so the selector cannot choose by language; Google Play and the bundle installer install the language splits of the device's languages.
- "All apps are up to date" shows only when the check found nothing and reported no failure. A failed check with nothing to show takes the failure state instead: the warning symbol, "Couldn't check for updates", the failure text and a Try again button; with updates to show, the failure sits in a segmented item above them as plain supporting text. Both keep the last result while a later check runs. A failure names the apps that Google Play could not check. A delivery check counts as a failure unless Google Play actually refused that version.
- The number of available updates follows the Updates title in parentheses, as in Updates (154), in the `LargeFlexibleTopAppBar` title both expanded and collapsed, and is left out when there are none; a running check shows through the pull to refresh indicator. The Settings tab shows only its title.
- The top app bar is `LargeFlexibleTopAppBar` with `exitUntilCollapsedScrollBehavior`; each tab keeps its own `TopAppBarState`, so switching tabs takes the new tab's state at once.
- With a D-pad or keyboard the lists never leave the focused item under the top app bar or the navigation: an item already in view stays put, otherwise it moves to the platform's television pivot, and focusing anything in the first item of a list scrolls it fully to the top and expands the top app bar so the large title shows.
- A button that starts a Google Play update or a manual install keeps the focus while it downloads and installs, showing its progress and ignoring presses, and the focus returns to the Manual button when its dialog closes; the item's other buttons are disabled and skipped until the install ends. When the focused item leaves the list, because its app was updated or a check dropped it, the focus moves to the first button of the next item, or of the previous one when it was the last, and to the navigation when the list is empty.
- With a D-pad or keyboard the version codes are reached only by pressing towards them, left in left to right layouts, from one of an update item's buttons, and up and down from the buttons never land on them. While a version code is focused, up and down move between the version codes of the neighbouring items and stop at the first and the last, so the refresh button and the navigation are reached only from the buttons; pressing back returns to the button it came from, or to the item's first button, and selecting a code copies it.
- Navigation follows Google's adaptive guidance through `NavigationSuiteScaffold` with `NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())`: a short navigation bar at the bottom on compact widths and compact heights, and a wide navigation rail on larger windows, televisions included. Selected destinations show the filled Material Symbol. With a D-pad or keyboard the navigation stops at its ends: pressing outwards from the first or the last destination keeps the focus there, and pressing towards the content moves into the list.
- Dialogs are Material `AlertDialog`s: the manual install asks for the version code in an `OutlinedTextField` with Cancel and Update, and the Theme item opens a dialog with radio buttons for System, Light and Dark that applies the choice at once. Changing the theme recolours the running screen in place: the activity handles uiMode itself and updates the system bars and window background, instead of being recreated.
- Each tab keeps its own scroll position, and tapping the active tab scrolls it to the top and expands its top app bar. Switching tabs never animates the content.
- Build the interface from the official Material 3 components and their defaults, taking the Expressive variant where one exists (segmented list items, shape morphing buttons, loading and wavy progress indicators), rather than drawing components by hand. Icons are Material Symbols Rounded from Google, except the Google Play and APKMirror marks that name a source.
- Take colours, type, shapes and motion only from `MaterialTheme`; do not introduce one-off colours, type styles, radii or animation specs, and keep spacing on Material's 4 dp grid.
- Text always uses the device's own default font through `FontFamily.Default`, whatever the platform or the manufacturer sets; never bundle, download or name a font family, and let the platform render the type scale weights instead of forcing font variation axes.
- Obtain values from the relevant official platform API whenever the system or the device can provide them at runtime, including device capabilities, application metadata such as icons, labels and package information, locales and configuration, instead of assuming manufacturer-specific behavior or duplicating them in resources and hardcoded mappings.
- An intent that no installed app can handle, such as an APKMirror link on a television without a browser or the APK bundle picker, shows a snackbar instead of failing silently or crashing; every other message is a Material snackbar too.
- A check runs only when the app starts and when the user asks for one with the refresh button, pull to refresh or Try again. Returning to the app never starts a check, because every check sends Google Play requests for each update, details for each Play candidate and a purchase for each APKMirror version also on Play, over the shared anonymous account; no timer or minimum interval replaces it. A check the user asks for never cancels and restarts a running one, and only changing the disabled apps setting does, because it changes the set of apps.
- A package broadcast updates only the package it names: removals and additions that are part of a replacement are ignored, and a replacement, a new installation or a removal rereads or drops just that package.
- AppUpdater adds no accessibility code of its own: no content descriptions, roles, or selected, toggleable or disabled semantics; its own interactive elements use plain clickable, and Material components keep only what they build in.
- Release builds run R8 through the `optimization` DSL, with the default Android rules and the libraries' consumer rules, except gplayapi's, which keeps the whole library and is ignored; the app's own rules keep only the protobuf message fields that protobuf-javalite reads by name. Add a keep rule only for code that a missing rule demonstrably breaks.
- Keep expensive work away from the UI thread.
- Prefer current Android APIs and remove obsolete compatibility layers and workarounds.
- Draw edge to edge and let the window insets place the content, rather than reserving space manually.
- Build visual effects with the official platform and Jetpack APIs only; never with a third party effect library.

## Verification
- Before considering a change complete, verify build/lint results, dependencies, resources, release configuration and relevant runtime behavior.
- Lint keeps `androidLintTool(platform(libs.asm.bom))` while the Java target is newer than the ASM bundled with lint: without it lint still passes but skips every class with "Unsupported class file major version 71".

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
