@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.android.appupdater.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.isCrossWindowBlurEnabled
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.android.appupdater.R
import com.android.appupdater.data.model.Appearance
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallEvent
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.text.NumberFormat

private val ContentMargin = 16.dp
private val CompactMargin = 16.dp
private val WideMargin = 24.dp
private val SmallSpacing = 8.dp
private val OptionHeight = 56.dp
private val VersionSpacing = 4.dp
private val EmptyPadding = 48.dp
private val EmptyIconSize = 48.dp
private val AppIconSize = 40.dp
private val InstallingIconSize = 28.dp
private val TvOverscanHorizontal = 48.dp
private val TvOverscanVertical = 27.dp
private const val TABULAR_FIGURES = "tnum"
private const val EXPANDED_COLUMNS = 2
private const val FADE_THROUGH_SCALE = 0.92f
private const val PREDICTIVE_BACK_EXIT_SCALE = 0.9f
private const val PREDICTIVE_BACK_ENTER_SCALE = 1.1f
private const val PREDICTIVE_BACK_FADE_THRESHOLD = 0.35f
private val PredictiveBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private val PredictiveBackExitFade = Easing { (PredictiveBackEasing.transform(it) / PREDICTIVE_BACK_FADE_THRESHOLD).coerceAtMost(1f) }
private val PredictiveBackEnterFade = Easing {
    ((PredictiveBackEasing.transform(it) - PREDICTIVE_BACK_FADE_THRESHOLD) / (1f - PREDICTIVE_BACK_FADE_THRESHOLD)).coerceAtLeast(0f)
}
private val DialogBlurRadius = 10.dp
private const val BLURRED_SCRIM_ALPHA = 0.2f
private const val SCRIM_ALPHA = 0.32f
private const val NOTICE_KEY = "notice"
private const val EMPTY_KEY = "empty"
private const val INSTALL_TYPE = "install"
private const val UPDATE_TYPE = "update"

@Serializable
private sealed interface Screen : NavKey {
    @Serializable
    data object Updates : Screen

    @Serializable
    data object Settings : Screen
}

private class TopLevelItem(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int
)

private val TopLevelItems = mapOf(
    Screen.Updates to TopLevelItem(R.string.updates, R.drawable.ic_updates, R.drawable.ic_updates_filled),
    Screen.Settings to TopLevelItem(R.string.settings, R.drawable.ic_settings, R.drawable.ic_settings_filled)
)

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

@Composable
fun AppUpdaterScreen(
    viewModel: AppUpdaterViewModel,
    appearance: Appearance,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bundlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::installBundle)
    }
    val resources = LocalResources.current
    val noBrowser = stringResource(R.string.no_browser)
    val noFilePicker = stringResource(R.string.no_file_picker)
    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }
    val pickBundle = {
        try {
            bundlePicker.launch(arrayOf("*/*"))
        } catch (_: ActivityNotFoundException) {
            showMessage(noFilePicker)
        }
    }

    uiState.messages.firstOrNull()?.let { message ->
        LaunchedEffect(message.id) {
            snackbarHostState.showSnackbar(message.event.text(resources))
            viewModel.messageShown(message.id)
        }
    }

    uiState.confirmation?.let { confirmation ->
        SideEffect(confirmation) {
            viewModel.confirmationShown(confirmation)
            runCatching { context.startActivity(confirmation) }
        }
    }

    AppUpdaterScreen(
        uiState = uiState,
        appearance = appearance,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onPickBundle = pickBundle,
        onApkMirror = { if (!openUrlInBrowser(context, it)) showMessage(noBrowser) },
        onPlay = viewModel::updateFromPlay,
        onIncludeDisabledAppsChange = viewModel::setIncludeDisabledApps,
        onThemeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        modifier = modifier
    )
}

@Composable
internal fun AppUpdaterScreen(
    uiState: AppUpdaterUiState,
    appearance: Appearance,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onPickBundle: () -> Unit,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = rememberSerializable(serializer = serializer()) { NavBackStack<Screen>(Screen.Updates) }
    val current = backStack.last()
    val updatesListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val updatesBarState = rememberTopAppBarState()
    val settingsBarState = rememberTopAppBarState()
    val updatesFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val navigationFocus = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    val coroutineScope = rememberCoroutineScope()
    var themeDialog by rememberSaveable { mutableStateOf(false) }
    var backNavigation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (inputModeManager.inputMode == InputMode.Keyboard) navigationFocus.requestFocus()
    }

    val updates = remember(uiState.installedApps, uiState.updates) {
        val installedByPackage = uiState.installedApps.associateBy(InstalledApp::packageName)
        uiState.updates.mapNotNull { update ->
            installedByPackage[update.packageName]?.let { it to update }
        }
    }

    val activeListState = if (current == Screen.Updates) updatesListState else settingsListState
    val activeBarState = if (current == Screen.Updates) updatesBarState else settingsBarState
    val activeFocus = if (current == Screen.Updates) updatesFocus else settingsFocus
    val navigate: (Screen) -> Unit = { screen ->
        if (screen != current) {
            backNavigation = false
            backStack.subList(1, backStack.size).clear()
            if (screen != Screen.Updates) backStack.add(screen)
        } else {
            coroutineScope.reveal(activeListState, activeBarState)
        }
    }
    val windowInfo = currentWindowAdaptiveInfoV2()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowInfo)
    val margin = if (windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        WideMargin
    } else {
        CompactMargin
    }
    val barMargin = margin - CompactMargin
    val columns = if (windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        EXPANDED_COLUMNS
    } else {
        1
    }
    val navigationBar = navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium
    val window = LocalActivity.current?.window
    val navigationBarScrim = !navigationBar && WindowInsets.navigationBars.getBottom(LocalDensity.current) > 0
    SideEffect(window, navigationBarScrim) { window?.isNavigationBarContrastEnforced = navigationBarScrim }
    val updatesAvailable = pluralStringResource(R.plurals.updates_available, updates.size, updates.size)

    val context = LocalContext.current
    val television = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val overscan = if (television) {
        WindowInsets(TvOverscanHorizontal, TvOverscanVertical, TvOverscanHorizontal, TvOverscanVertical)
    } else {
        WindowInsets(0)
    }

    NavigationSuiteScaffold(
        navigationItems = {
            TopLevelItems.forEach { (screen, item) ->
                val selected = screen == current
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { navigate(screen) },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) item.selectedIconRes else item.iconRes),
                            contentDescription = null
                        )
                    },
                    label = { Text(text = stringResource(item.labelRes)) },
                    navigationSuiteType = navigationSuiteType,
                    badge = if (screen == Screen.Updates && updates.isNotEmpty()) {
                        {
                            Badge {
                                Text(
                                    text = "${updates.size}",
                                    modifier = Modifier.semantics { contentDescription = updatesAvailable }
                                )
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .then(if (selected) Modifier.focusRequester(navigationFocus) else Modifier)
                        .focusProperties {
                            val first = screen == TopLevelItems.keys.first()
                            val last = screen == TopLevelItems.keys.last()
                            if (navigationBar) {
                                up = activeFocus
                                if (first) start = FocusRequester.Cancel
                                if (last) end = FocusRequester.Cancel
                            } else {
                                end = activeFocus
                                if (first) up = FocusRequester.Cancel
                                if (last) down = FocusRequester.Cancel
                            }
                        }
                )
            }
        },
        modifier = modifier.windowInsetsPadding(overscan.only(WindowInsetsSides.Start)),
        navigationSuiteType = navigationSuiteType
    ) {
        val motion = MaterialTheme.motionScheme
        val content: @Composable (Screen) -> Unit = { screen ->
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(if (screen == Screen.Updates) updatesBarState else settingsBarState)
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(TopLevelItems.getValue(screen).labelRes)) },
                        actions = {
                            if (screen == Screen.Updates) {
                                val toUpdates = Modifier.focusProperties { down = updatesFocus }
                                BarAction(
                                    icon = R.drawable.ic_apk_install,
                                    label = stringResource(R.string.install_bundle),
                                    onClick = onPickBundle,
                                    modifier = toUpdates
                                )
                                BarAction(
                                    icon = R.drawable.ic_refresh,
                                    label = stringResource(R.string.refresh),
                                    onClick = onRefresh,
                                    modifier = toUpdates
                                )
                            }
                        },
                        windowInsets = TopAppBarDefaults.windowInsets.add(overscan.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                        contentPadding = PaddingValues(horizontal = barMargin),
                        scrollBehavior = scrollBehavior
                    )
                },
                snackbarHost = { if (screen == current) SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets.add(overscan.only(WindowInsetsSides.Bottom + WindowInsetsSides.End))
            ) { padding ->
                val layoutDirection = LocalLayoutDirection.current
                val density = LocalDensity.current
                val contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + margin,
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection) + margin,
                    bottom = padding.calculateBottomPadding() + margin
                )
                val platformScroll = LocalBringIntoViewSpec.current
                val focusScroll = remember(platformScroll, density) { FocusScroll(platformScroll, density) }
                SideEffect { focusScroll.padding = contentPadding }

                CompositionLocalProvider(LocalBringIntoViewSpec provides focusScroll) {
                    when (screen) {
                        Screen.Updates -> UpdatesView(
                            listState = updatesListState,
                            barState = updatesBarState,
                            nestedScroll = scrollBehavior.nestedScrollConnection,
                            focus = updatesFocus,
                            exitFocus = navigationFocus,
                            contentPadding = contentPadding,
                            loaded = uiState.loaded,
                            loading = uiState.scanning && !uiState.loaded,
                            refreshing = uiState.scanning && uiState.loaded,
                            notice = uiState.notice,
                            updates = updates,
                            installs = uiState.installs,
                            playInstalls = uiState.playInstalls,
                            onApkMirror = onApkMirror,
                            onPlay = onPlay,
                            onRefresh = onRefresh,
                            columns = columns
                        )
                        Screen.Settings -> SettingsView(
                            listState = settingsListState,
                            barState = settingsBarState,
                            nestedScroll = scrollBehavior.nestedScrollConnection,
                            focus = settingsFocus,
                            contentPadding = contentPadding,
                            appearance = appearance,
                            includeDisabledApps = uiState.includeDisabledApps,
                            onIncludeDisabledAppsChange = onIncludeDisabledAppsChange,
                            onTheme = { themeDialog = true },
                            onDynamicColorChange = onDynamicColorChange
                        )
                    }
                }
            }
        }
        NavDisplay(
            backStack = backStack,
            onBack = {
                backNavigation = true
                backStack.removeLastOrNull()
            },
            transitionSpec = { fadeThrough(motion) },
            popTransitionSpec = { if (backNavigation) backTransition(motion) else fadeThrough(motion) },
            predictivePopTransitionSpec = { predictiveBackTransition() }
        ) { screen -> NavEntry(screen, content = content) }
    }

    if (themeDialog) {
        ThemeDialog(
            current = appearance.themeMode,
            onDismiss = { themeDialog = false },
            onSelect = { mode ->
                themeDialog = false
                onThemeChange(mode)
            }
        )
    }
}

@Composable
private fun UpdatesView(
    listState: LazyListState,
    barState: TopAppBarState,
    nestedScroll: NestedScrollConnection,
    focus: FocusRequester,
    exitFocus: FocusRequester,
    contentPadding: PaddingValues,
    loaded: Boolean,
    loading: Boolean,
    refreshing: Boolean,
    notice: String?,
    updates: List<Pair<InstalledApp, AppUpdateInfo>>,
    installs: Map<String, InstallState>,
    playInstalls: Map<String, PlayInstall>,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onRefresh: () -> Unit,
    columns: Int
) {
    val pullState = rememberPullToRefreshState()
    val motion = MaterialTheme.motionScheme
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
    val fade = motion.defaultEffectsSpec<Float>()
    val placement = motion.defaultSpatialSpec<IntOffset>()
    val reveal = rememberTopReveal(listState, barState)
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val cards = remember(keyboard) { CardFocus() }
    val installList = remember(installs) { installs.entries.toList() }
    val rows = remember(updates, columns) { updates.chunked(columns) }
    val installStart = if (notice != null && updates.isNotEmpty()) 1 else 0
    val updateStart = installStart + installList.size

    val previous = cards.keys
    val lost = cards.focused?.takeIf { key -> updates.none { it.first.packageName == key } }

    LaunchedEffect(cards, updates) {
        if (!keyboard) return@LaunchedEffect
        val keys = updates.map { it.first.packageName }
        cards.keys = keys
        if (lost == null) return@LaunchedEffect
        val present = keys.toHashSet()
        val index = previous.indexOf(lost)
        val target = previous.subList(index + 1, previous.size).firstOrNull { it in present }
            ?: previous.subList(0, index.coerceAtLeast(0)).lastOrNull { it in present }
        val entry = target?.let(cards.entries::get) ?: target?.let { withFrameNanos { }; cards.entries[it] }
        if (entry?.requestFocus() != true) exitFocus.requestFocus()
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        enabled = loaded,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullState,
                isRefreshing = refreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding())
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScroll)
                .focusRequester(focus)
                .focusRestorer(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            if (notice != null && updates.isNotEmpty()) {
                item(key = NOTICE_KEY, contentType = NOTICE_KEY) {
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(0, updateStart + rows.size),
                        colors = listItemColors(),
                        modifier = Modifier.animateItem(fade, placement, fade)
                    ) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            itemsIndexed(installList, key = { _, install -> install.key }, contentType = { _, _ -> INSTALL_TYPE }) { index, (_, state) ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(installStart + index, updateStart + rows.size),
                    colors = listItemColors(),
                    modifier = Modifier.animateItem(fade, placement, fade),
                    supportingContent = { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                ) {
                    Text(text = state.appName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (updates.isEmpty() && loaded) {
                item(key = EMPTY_KEY, contentType = EMPTY_KEY) {
                    if (notice == null) {
                        EmptyState(
                            symbol = R.drawable.ic_up_to_date,
                            title = stringResource(R.string.all_up_to_date),
                            message = stringResource(R.string.all_up_to_date_note),
                            modifier = Modifier.animateItem(fade, placement, fade)
                        )
                    } else {
                        EmptyState(
                            symbol = R.drawable.ic_warning,
                            title = stringResource(R.string.check_failed),
                            message = notice,
                            modifier = Modifier.animateItem(fade, placement, fade)
                        ) {
                            FilledTonalButton(
                                onClick = onRefresh,
                                shapes = ButtonDefaults.shapes(),
                                modifier = reveal.modifier
                            ) {
                                Text(text = stringResource(R.string.try_again))
                            }
                        }
                    }
                }
            }

            itemsIndexed(
                items = rows,
                key = { _, row -> row.first().first.packageName },
                contentType = { _, _ -> UPDATE_TYPE }
            ) { index, row ->
                Row(
                    modifier = Modifier
                        .animateItem(fade, placement, fade)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    row.forEachIndexed { column, (app, update) ->
                        UpdateItem(
                            app = app,
                            update = update,
                            shapes = ListItemDefaults.segmentedShapes(
                                updateStart + index,
                                updateStart + (updates.size - column + columns - 1) / columns
                            ),
                            install = playInstalls[app.packageName],
                            cards = cards,
                            context = context,
                            iconSizePx = iconSizePx,
                            motion = motion,
                            onApkMirror = onApkMirror,
                            onPlay = onPlay,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(if (index == 0) reveal.modifier else Modifier)
                        )
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(motion.defaultEffectsSpec()),
            exit = fadeOut(motion.defaultEffectsSpec()),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(contentPadding)
        ) {
            LoadingIndicator()
        }
    }
}

@Composable
private fun SettingsView(
    listState: LazyListState,
    barState: TopAppBarState,
    nestedScroll: NestedScrollConnection,
    focus: FocusRequester,
    contentPadding: PaddingValues,
    appearance: Appearance,
    includeDisabledApps: Boolean,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    onTheme: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val reveal = rememberTopReveal(listState, barState)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScroll)
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        item(key = "appearance") {
            SectionHeader(text = stringResource(R.string.appearance))
        }

        item(key = "theme") {
            SegmentedListItem(
                onClick = onTheme,
                shapes = ListItemDefaults.segmentedShapes(0, 2),
                colors = listItemColors(),
                modifier = reveal.modifier,
                supportingContent = { Text(text = stringResource(appearance.themeMode.labelRes)) }
            ) {
                Text(text = stringResource(R.string.theme))
            }
        }

        item(key = "dynamic-color") {
            SegmentedListItem(
                checked = appearance.dynamicColor,
                onCheckedChange = onDynamicColorChange,
                modifier = Modifier.semantics { role = Role.Switch },
                shapes = ListItemDefaults.segmentedShapes(1, 2),
                colors = listItemColors(),
                supportingContent = { Text(text = stringResource(R.string.dynamic_color_description)) },
                trailingContent = { Switch(checked = appearance.dynamicColor, onCheckedChange = null) }
            ) {
                Text(text = stringResource(R.string.dynamic_color))
            }
        }

        item(key = "updates") {
            SectionHeader(text = stringResource(R.string.updates), modifier = Modifier.padding(top = ContentMargin))
        }

        item(key = "disabled-apps") {
            SegmentedListItem(
                checked = includeDisabledApps,
                onCheckedChange = onIncludeDisabledAppsChange,
                modifier = Modifier.semantics { role = Role.Switch },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = listItemColors(),
                supportingContent = { Text(text = stringResource(R.string.disabled_apps_description)) },
                trailingContent = { Switch(checked = includeDisabledApps, onCheckedChange = null) }
            ) {
                Text(text = stringResource(R.string.disabled_apps))
            }
        }
    }
}

@Composable
private fun BarAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text = label) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes(), modifier = modifier) {
            Icon(painter = painterResource(icon), contentDescription = label)
        }
    }
}

@Composable
private fun listItemColors() = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = ContentMargin, end = ContentMargin, bottom = SmallSpacing)
            .semantics { heading() }
    )
}

@Composable
private fun UpdateItem(
    app: InstalledApp,
    update: AppUpdateInfo,
    shapes: ListItemShapes,
    install: PlayInstall?,
    cards: CardFocus,
    context: Context,
    iconSizePx: Int,
    motion: MotionScheme,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    modifier: Modifier
) {
    val packageName = app.packageName
    val playFocus = remember { FocusRequester() }
    val apkMirrorFocus = remember { FocusRequester() }
    val entry = when {
        update.playUpdate != null -> playFocus
        update.apkMirrorUrl != null -> apkMirrorFocus
        else -> null
    }
    val cardModifier = remember(cards, packageName) {
        Modifier.onFocusChanged {
            if (it.hasFocus) {
                cards.focused = packageName
            } else if (cards.focused == packageName) {
                cards.focused = null
            }
        }
    }
    DisposableEffect(cards, packageName, entry) {
        if (entry != null) cards.entries[packageName] = entry
        onDispose { cards.entries.remove(packageName) }
    }
    val iconBitmap by produceState(AppIconCache.peek(app.packageName), app.packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                AppIconCache.load(context, app.packageName, iconSizePx)
            }
        }
    }

    SegmentedListItem(
        shapes = shapes,
        colors = listItemColors(),
        modifier = modifier.then(cardModifier),
        leadingContent = {
            val iconScale by animateFloatAsState(
                targetValue = if (install == null) 1f else InstallingIconSize / AppIconSize,
                animationSpec = motion.defaultSpatialSpec(),
                label = "icon"
            )
            Box(modifier = Modifier.size(AppIconSize), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visible = install != null,
                    modifier = Modifier.requiredSize(WavyProgressIndicatorDefaults.CircularContainerSize),
                    enter = fadeIn(motion.defaultEffectsSpec()),
                    exit = fadeOut(motion.defaultEffectsSpec())
                ) {
                    val progress = install?.progress
                    if (progress == null) {
                        CircularWavyProgressIndicator()
                    } else {
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                            label = "progress"
                        )
                        CircularWavyProgressIndicator(progress = { animatedProgress })
                    }
                }
                iconBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(AppIconSize)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        supportingContent = {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(SmallSpacing)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VersionSpacing)) {
                    val versionFormat = stringResource(R.string.version_number)
                    Text(
                        text = if (app.versionName.isEmpty()) {
                            stringResource(R.string.version_current_code, app.versionCode)
                        } else {
                            stringResource(
                                R.string.version_current,
                                displayVersion(app.versionName, versionFormat),
                                app.versionCode
                            )
                        }
                    )
                    Text(
                        text = stringResource(
                            R.string.version_latest,
                            displayVersion(update.newVersionName, versionFormat),
                            update.newVersionCode
                        )
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SmallSpacing, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(SmallSpacing)
                ) {
                    if (update.playUpdate != null) {
                        SourceButton(
                            text = stringResource(R.string.update),
                            icon = R.drawable.ic_source_play,
                            source = stringResource(R.string.source_play),
                            motion = motion,
                            busy = install != null,
                            progress = install?.progress,
                            onClick = { onPlay(update) },
                            modifier = Modifier.focusRequester(playFocus)
                        )
                    }
                    update.apkMirrorUrl?.let { url ->
                        SourceButton(
                            text = stringResource(R.string.update),
                            icon = R.drawable.ic_source_apkmirror,
                            source = stringResource(R.string.source_apkmirror),
                            motion = motion,
                            enabled = install == null,
                            onClick = { onApkMirror(url) },
                            modifier = Modifier.focusRequester(apkMirrorFocus)
                        )
                    }
                }
            }
        }
    ) {
        Text(text = update.appName, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SourceButton(
    text: String,
    @DrawableRes icon: Int,
    source: String,
    motion: MotionScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    progress: Float? = null
) {
    val percent = remember { NumberFormat.getPercentInstance() }
    val active = enabled && !busy
    val progressShown by animateFloatAsState(
        targetValue = if (busy && progress != null) 1f else 0f,
        animationSpec = motion.fastEffectsSpec(),
        label = "progress"
    )

    FilledTonalButton(
        onClick = { if (active) onClick() },
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.semantics { if (busy) disabled() },
        enabled = enabled || busy,
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight, hasStartIcon = true)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = source,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight))
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = 1f - progressShown }
            )
            if (busy && progress != null) {
                Text(
                    text = percent.format(progress),
                    style = LocalTextStyle.current.copy(fontFeatureSettings = TABULAR_FIGURES),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = progressShown }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    @DrawableRes symbol: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ContentMargin, vertical = EmptyPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SmallSpacing)
    ) {
        Icon(
            painter = painterResource(symbol),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(EmptyIconSize)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) Box(modifier = Modifier.padding(top = SmallSpacing)) { action() }
    }
}

@Composable
private fun ThemeDialog(
    current: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    val inputModeManager = LocalInputModeManager.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (inputModeManager.inputMode == InputMode.Keyboard) focus.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = stringResource(R.string.theme)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (selected) Modifier.focusRequester(focus) else Modifier)
                            .height(OptionHeight)
                            .selectable(selected = selected, onClick = { onSelect(mode) }, role = Role.RadioButton),
                        horizontalArrangement = Arrangement.spacedBy(ContentMargin),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(text = stringResource(mode.labelRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        properties = dialogProperties()
    )
}

@Composable
private fun dialogProperties() = DialogProperties(
    blurBehindRadius = DialogBlurRadius,
    scrimAlpha = if (LocalWindowInfo.current.isCrossWindowBlurEnabled) BLURRED_SCRIM_ALPHA else SCRIM_ALPHA
)

private fun fadeThrough(motion: MotionScheme) =
    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), FADE_THROUGH_SCALE))
        .togetherWith(fadeOut(motion.fastEffectsSpec()))

private fun backTransition(motion: MotionScheme) =
    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), PREDICTIVE_BACK_ENTER_SCALE))
        .togetherWith(fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), PREDICTIVE_BACK_EXIT_SCALE))

private fun predictiveBackTransition() =
    (fadeIn(tween(easing = PredictiveBackEnterFade)) + scaleIn(tween(easing = PredictiveBackEasing), PREDICTIVE_BACK_ENTER_SCALE))
        .togetherWith(fadeOut(tween(easing = PredictiveBackExitFade)) + scaleOut(tween(easing = PredictiveBackEasing), PREDICTIVE_BACK_EXIT_SCALE))

private class TopReveal(scope: CoroutineScope, listState: LazyListState, barState: TopAppBarState) {
    val modifier = Modifier.onFocusEvent { state ->
        if (state.hasFocus) scope.reveal(listState, barState)
    }
}

@Composable
private fun rememberTopReveal(listState: LazyListState, barState: TopAppBarState): TopReveal {
    val scope = rememberCoroutineScope()
    return remember(listState, barState, scope) { TopReveal(scope, listState, barState) }
}

private fun CoroutineScope.reveal(listState: LazyListState, barState: TopAppBarState) {
    launch {
        listState.animateScrollToItem(0)
        barState.contentOffset = 0f
    }
}

private class CardFocus {
    var focused: String? = null
    var keys: List<String> = emptyList()
    val entries = HashMap<String, FocusRequester>()
}

private class FocusScroll(private val platform: BringIntoViewSpec, private val density: Density) : BringIntoViewSpec {
    var padding = PaddingValues()

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val top = with(density) { padding.calculateTopPadding().toPx() }
        val bottom = with(density) { padding.calculateBottomPadding().toPx() }
        return platform.calculateScrollDistance(offset - top, size, containerSize - top - bottom)
    }
}

internal fun InstallEvent.text(resources: Resources): String = when (this) {
    is InstallEvent.Installed -> resources.getString(R.string.app_installed, appName)
    is InstallEvent.InstallFailed -> resources.getString(R.string.install_failed, message)
    is InstallEvent.Finished -> resources.getString(R.string.update_installed, appName)
    is InstallEvent.FinishedFromPlay -> resources.getString(
        R.string.update_installed_play,
        appName,
        displayVersion(versionName, resources.getString(R.string.version_number))
    )
    is InstallEvent.Failed -> resources.getString(R.string.update_failed, message)
}

private fun displayVersion(name: String, numberFormat: String): String {
    val version = if (name.length > 1 && name[0].lowercaseChar() == 'v' && name[1].isDigit()) name.substring(1) else name
    return if (version.firstOrNull()?.isDigit() == true) numberFormat.format(version) else version
}

private fun openUrlInBrowser(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}
