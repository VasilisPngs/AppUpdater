# Android Project Instructions

## Git & repository workflow
- Push changes directly to `main`; do not open pull requests.
- Publish every completed change set as exactly one commit.

## General rules
- Follow only Google's official guidance and rules: Material Design, the Android developer documentation and Google's own libraries and samples. When the user asks for something beyond them, first say plainly that it departs from Google and which guideline it breaks, and go ahead only once they confirm. A confirmed departure changes the code only, never this file, which stays on Google's guidance by default; a departure already in the code is the user's settled decision, so ask before reverting it.
- Do the work end to end.
- Do not undo settled decisions or weaken requirements without a real technical reason.
- Prefer root-cause fixes and the simplest architecture that fully satisfies the requirements.
- Remove dead code, unused dependencies, resources, permissions and configuration.
- Keep code clean, direct, idiomatic and maintainable.
- All code is written in English.
- Do not add explanatory or redundant comments inside code.
- Use the latest appropriate official technologies, APIs, libraries and tools, tracking the newest published build in any release channel, including preview, alpha, beta, release candidate and canary.
- A newer toolchain is only acceptable while the signed release APK still installs on a release device; never compile against a preview Android platform, which stamps the package as preview-built and makes it unparseable on release builds.
- Verify current versions from official sources rather than relying on outdated examples.
- Every change that introduces a library, API, component, platform feature or tool also adds its official documentation to Official references in the same commit: Google's own pages (developer.android.com, source.android.com, m3.material.io, fonts.google.com) or, for a library outside Google, its maintainers' documentation, each checked to load the right page.
- Do not add a numeric product version to the visible product identity, release name, APK filename, release tag, or API User-Agent unless explicitly requested.

## Android
- The product design is Google's Material 3 Expressive, built from the official Compose Material 3 components inside `MaterialExpressiveTheme` with `MotionScheme.expressive()`, taking the Expressive variant where one exists and keeping the components' defaults, Google's rounded shapes included.
- Every color comes from the device's dynamic color scheme, `dynamicLightColorScheme` or `dynamicDarkColorScheme`. The minimum SDK is at least 31 (Android 12), so dynamic color is always available and there is no fallback palette and no hand-picked color in code.
- Each screen has a single-row `TopAppBar` with the plain screen name as its title, and every list entry is its own fully rounded Material 3 Expressive `SegmentedListItem`, spaced apart on the surface container color rather than joined into a group.
- Icons come from Material Symbols Rounded, except a third party's own logo where the interface names that third party.
- Text uses Google Sans from Google Fonts through Compose downloadable fonts.
- Use Google's blur and transparency effects wherever Google's guidance uses them, such as the window blur behind dialogs.
- Add an R8 keep rule only for code that a missing rule demonstrably breaks.
- Prefer current Android APIs and remove obsolete compatibility layers and workarounds.

## Verification
- While the Java target is newer than the ASM bundled with lint, the build adds `androidLintTool(platform(...))` with the newest `org.ow2.asm:asm-bom` from the version catalog; without it lint still passes but skips every class with "Unsupported class file major version".

## Official references

### Design and UI
- Material Design 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose Material 3 API reference: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary
- Get started with Jetpack Compose: https://developer.android.com/develop/ui/compose/documentation
- Material components in Compose: https://developer.android.com/develop/ui/compose/components
- Scaffold: https://developer.android.com/develop/ui/compose/components/scaffold
- Buttons in Compose: https://developer.android.com/develop/ui/compose/components/button
- Icon buttons in Compose: https://developer.android.com/develop/ui/compose/components/icon-button
- Switch in Compose: https://developer.android.com/develop/ui/compose/components/switch
- Radio button in Compose: https://developer.android.com/develop/ui/compose/components/radio-button
- Dialogs in Compose: https://developer.android.com/develop/ui/compose/components/dialog
- Snackbar in Compose: https://developer.android.com/develop/ui/compose/components/snackbar
- Progress indicators in Compose: https://developer.android.com/develop/ui/compose/components/progress
- Pull to refresh in Compose: https://developer.android.com/develop/ui/compose/components/pull-to-refresh
- App bars: https://developer.android.com/develop/ui/compose/components/app-bars
- Navigation bar in Compose: https://developer.android.com/develop/ui/compose/components/navigation-bar
- Navigation rail in Compose: https://developer.android.com/develop/ui/compose/components/navigation-rail
- Badges in Compose: https://developer.android.com/develop/ui/compose/components/badges
- Floating action button in Compose: https://developer.android.com/develop/ui/compose/components/fab
- State and Jetpack Compose: https://developer.android.com/develop/ui/compose/state
- Side effects in Compose: https://developer.android.com/develop/ui/compose/side-effects
- Lazy lists and grids: https://developer.android.com/develop/ui/compose/lists
- Animations in Compose: https://developer.android.com/develop/ui/compose/animation/quick-guide
- Text in Compose: https://developer.android.com/develop/ui/compose/text
- Fonts and downloadable fonts in Compose: https://developer.android.com/develop/ui/compose/text/fonts
- Text fields in Compose: https://developer.android.com/develop/ui/compose/text/user-input
- Resources in Compose: https://developer.android.com/develop/ui/compose/resources
- Icons in Compose: https://developer.android.com/develop/ui/compose/graphics/images/material
- Focus in Compose: https://developer.android.com/develop/ui/compose/touch-input/focus
- Accessibility in Compose: https://developer.android.com/develop/ui/compose/accessibility
- Semantics in Compose: https://developer.android.com/develop/ui/compose/accessibility/semantics
- Copy and paste in Compose: https://developer.android.com/develop/ui/compose/touch-input/copy-and-paste
- Adaptive apps: https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps
- Support different display sizes: https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
- Window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Canonical layouts: https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts
- Adaptive navigation: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Navigation suite API reference: https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/navigationsuite/package-summary

### Material 3 Expressive
- Building with M3 Expressive: https://m3.material.io/blog/building-with-m3-expressive
- M3 Expressive motion: https://m3.material.io/blog/m3-expressive-motion-theming
- Material components: https://m3.material.io/components
- Top app bar: https://m3.material.io/components/app-bars/overview
- Navigation bar: https://m3.material.io/components/navigation-bar/overview
- Navigation rail: https://m3.material.io/components/navigation-rail/overview
- Badges: https://m3.material.io/components/badges/overview
- Buttons: https://m3.material.io/components/buttons/overview
- Icon buttons: https://m3.material.io/components/icon-buttons/overview
- Lists: https://m3.material.io/components/lists/overview
- Loading indicator: https://m3.material.io/components/loading-indicator/overview
- Progress indicators: https://m3.material.io/components/progress-indicators/overview
- Dialogs: https://m3.material.io/components/dialogs/overview
- Text fields: https://m3.material.io/components/text-fields/overview
- Switch: https://m3.material.io/components/switch/overview
- Radio button: https://m3.material.io/components/radio-button/overview
- Snackbar: https://m3.material.io/components/snackbar/overview
- Toolbars: https://m3.material.io/components/toolbars/overview
- Toolbars guidelines: https://m3.material.io/components/toolbars/guidelines
- Toolbars specs: https://m3.material.io/components/toolbars/specs
- Floating action button: https://m3.material.io/components/floating-action-button/overview
- Color roles: https://m3.material.io/styles/color/roles
- Dynamic color: https://m3.material.io/styles/color/dynamic/choosing-a-source
- Typography: https://m3.material.io/styles/typography/overview
- Typography fonts: https://m3.material.io/styles/typography/fonts
- Google Sans: https://fonts.google.com/specimen/Google+Sans
- Roboto: https://fonts.google.com/specimen/Roboto
- Shape: https://m3.material.io/styles/shape/overview-principles
- Shape morph: https://m3.material.io/styles/shape/shape-morph
- Motion: https://m3.material.io/styles/motion/overview/how-it-works
- Spacing: https://m3.material.io/styles/spacing/overview
- Icons: https://m3.material.io/styles/icons/overview
- Accessibility: https://m3.material.io/foundations/overview
- Material Symbols guide: https://developers.google.com/fonts/docs/material_symbols
- Material Symbols catalogue: https://fonts.google.com/icons
- Material Symbols Android drawables: https://github.com/google/material-design-icons/tree/master/symbols/android

### Graphics and system surfaces
- Graphics in Compose: https://developer.android.com/develop/ui/compose/graphics/draw/overview
- Graphics modifiers: https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
- Window blurs: https://source.android.com/docs/core/display/window-blurs
- Blur behind a window: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#setBlurBehindRadius(int)
- Window background blur: https://developer.android.com/reference/android/view/Window#setBackgroundBlurRadius(int)
- RenderEffect: https://developer.android.com/reference/android/graphics/RenderEffect
- Blur in Compose: https://developer.android.com/develop/ui/compose/graphics/images/customize#blur-image
- Dialog window in Compose: https://developer.android.com/reference/kotlin/androidx/compose/ui/window/DialogWindowProvider
- Shapes in Compose: https://developer.android.com/develop/ui/compose/graphics/draw/shapes
- Window insets: https://developer.android.com/develop/ui/compose/system/insets
- System bars: https://developer.android.com/develop/ui/compose/system/system-bars
- Edge to edge in Compose: https://developer.android.com/develop/ui/compose/system/setup-e2e
- Predictive back in Compose: https://developer.android.com/develop/ui/compose/system/predictive-back
- Adaptive icons: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
- System color resources: https://developer.android.com/reference/android/R.color
- Splash screens: https://developer.android.com/develop/ui/views/launch/splash-screen
- Notifications: https://developer.android.com/develop/ui/compose/notifications
- Notification runtime permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Widgets with Jetpack Glance: https://developer.android.com/develop/ui/compose/glance

### Platform behavior
- Permissions: https://developer.android.com/guide/topics/permissions/overview
- Package visibility: https://developer.android.com/training/package-visibility
- Intents and intent filters: https://developer.android.com/guide/components/intents-filters
- Get a result from an activity: https://developer.android.com/training/basics/intents/result
- Broadcasts: https://developer.android.com/develop/background-work/background-tasks/broadcasts
- Background tasks: https://developer.android.com/develop/background-work/background-tasks
- Task scheduling with WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Foreground services: https://developer.android.com/develop/background-work/services/fgs
- Configuration changes: https://developer.android.com/guide/topics/resources/runtime-changes
- UiModeManager: https://developer.android.com/reference/android/app/UiModeManager
- PackageInstaller: https://developer.android.com/reference/android/content/pm/PackageInstaller
- Per-app language preferences: https://developer.android.com/guide/topics/resources/app-languages
- String resources: https://developer.android.com/guide/topics/resources/string-resource
- Kotlin coroutines on Android: https://developer.android.com/kotlin/coroutines
- Kotlin flows on Android: https://developer.android.com/kotlin/flow
- Kotlin serialization: https://kotlinlang.org/docs/serialization.html
- Data and file storage: https://developer.android.com/training/data-storage
- SharedPreferences: https://developer.android.com/training/data-storage/shared-preferences
- Documents and files from shared storage: https://developer.android.com/training/data-storage/shared/documents-files
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Room: https://developer.android.com/training/data-storage/room
- Photo picker: https://developer.android.com/training/data-storage/shared/photo-picker
- Connect to the network: https://developer.android.com/develop/connectivity/network-ops/connecting
- Security checklist: https://developer.android.com/privacy-and-security/security-tips
- Android 17 behavior changes for all apps: https://developer.android.com/about/versions/17/behavior-changes-all
- Android 17 behavior changes for apps targeting it: https://developer.android.com/about/versions/17/behavior-changes-17

### Architecture, quality and testing
- Guide to app architecture: https://developer.android.com/topic/architecture
- Architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- ViewModel: https://developer.android.com/topic/libraries/architecture/viewmodel
- Lifecycle in Compose: https://developer.android.com/topic/libraries/architecture/lifecycle
- Navigation 3: https://developer.android.com/guide/navigation/navigation-3
- Core app quality guidelines: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Build accessible apps: https://developer.android.com/guide/topics/ui/accessibility
- Adaptive app quality guidelines: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- App performance guide: https://developer.android.com/topic/performance/overview
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Test apps on Android: https://developer.android.com/training/testing
- Local unit tests: https://developer.android.com/training/testing/local-tests
- Test Compose layouts: https://developer.android.com/develop/ui/compose/testing
- Lint: https://developer.android.com/studio/write/lint

### Television
- Android TV: https://developer.android.com/training/tv
- Compose on Android TV: https://developer.android.com/training/tv/playback/compose
- Adaptive apps for TV: https://developer.android.com/develop/adaptive-apps/guides/tv/build-adaptive-apps-for-tv
- TV app icons and banners: https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines
- TV app quality: https://developer.android.com/docs/quality-guidelines/tv-app-quality

### Build and release
- Configure your build: https://developer.android.com/build
- Version catalogs: https://developer.android.com/build/migrate-to-catalogs
- Built-in Kotlin: https://developer.android.com/build/migrate-to-built-in-kotlin
- Java versions in Android builds: https://developer.android.com/build/jdks
- App optimization with R8: https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
- Keep rules: https://developer.android.com/topic/performance/app-optimization/keep-rules-overview
- Sign your app: https://developer.android.com/studio/publish/app-signing
- Platform and API versions: https://developer.android.com/tools/releases/platforms
- Latest Android developer updates: https://developer.android.com/latest-updates
- AndroidX releases: https://developer.android.com/jetpack/androidx/versions
- Compose releases: https://developer.android.com/jetpack/androidx/releases/compose
- Compose UI release notes: https://developer.android.com/jetpack/androidx/releases/compose-ui
- Compose Material 3 release notes: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Compose Material 3 Adaptive release notes: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- Navigation 3 release notes: https://developer.android.com/jetpack/androidx/releases/navigation3
- Android Gradle Plugin release notes: https://developer.android.com/build/releases/gradle-plugin
- Kotlin releases: https://kotlinlang.org/docs/releases.html
- Kotlin Early Access Preview: https://kotlinlang.org/docs/eap.html
- Gradle distributions: https://services.gradle.org/distributions/
- OpenJDK builds: https://jdk.java.net/

### AI agents
- Android tools for agents: https://developer.android.com/tools/agents
- Android CLI: https://developer.android.com/tools/agents/android-cli
- Android skills: https://developer.android.com/tools/agents/android-skills
- Android skills repository: https://github.com/android/skills
