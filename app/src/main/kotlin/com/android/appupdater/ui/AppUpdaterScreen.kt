@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.android.appupdater.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.appupdater.R
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.function.Consumer

private val ContentMargin = 16.dp
private val SmallSpacing = 8.dp
private val VersionSpacing = 4.dp
private val EmptyPadding = 48.dp
private val EmptyIconSize = 48.dp
private val AppIconSize = 40.dp
private val TvOverscanHorizontal = 48.dp
private val TvOverscanVertical = 27.dp
private const val PLAY_SLOT = 0
private const val APKMIRROR_SLOT = 1
private const val BUTTON_SLOTS = 2
private const val TABULAR_FIGURES = "tnum"
private const val FOCUS_PIVOT = 0.3f
private const val FADE_THROUGH_SCALE = 0.92f
private const val BLUR_BEHIND_RADIUS = 20
private const val DIM_WITH_BLUR = 0.1f
private const val DIM_WITHOUT_BLUR = 0.4f

private enum class AppTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int
) {
    Updates(R.string.updates, R.drawable.ic_updates, R.drawable.ic_updates_filled),
    Settings(R.string.settings, R.drawable.ic_settings, R.drawable.ic_settings_filled)
}

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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Updates) }
    val updatesListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val updatesBarState = rememberTopAppBarState()
    val settingsBarState = rememberTopAppBarState()
    val updatesFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val navigationFocus = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val inputModeManager = LocalInputModeManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bundlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::installBundle)
    }
    var themeDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = selectedTab != AppTab.Updates) { selectedTab = AppTab.Updates }
    val installedFormat = stringResource(R.string.update_installed)
    val installedFromPlayFormat = stringResource(R.string.update_installed_play)
    val failedFormat = stringResource(R.string.update_failed)
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            showMessage(
                when (event) {
                    is InstallEvent.Finished -> installedFormat.format(event.appName)
                    is InstallEvent.FinishedFromPlay -> installedFromPlayFormat.format(event.appName, displayVersion(event.versionName))
                    is InstallEvent.Failed -> failedFormat.format(event.message)
                }
            )
        }
    }
    LaunchedEffect(Unit) {
        if (inputModeManager.inputMode == InputMode.Keyboard) navigationFocus.requestFocus()
    }

    val updates = remember(uiState.installedApps, uiState.updates) {
        val installedByPackage = uiState.installedApps.associateBy(InstalledApp::packageName)
        uiState.updates.mapNotNull { update ->
            installedByPackage[update.packageName]?.let { it to update }
        }
    }

    val activeListState = if (selectedTab == AppTab.Updates) updatesListState else settingsListState
    val activeBarState = if (selectedTab == AppTab.Updates) updatesBarState else settingsBarState
    val activeFocus = if (selectedTab == AppTab.Updates) updatesFocus else settingsFocus
    val selectTab: (AppTab) -> Unit = { tab ->
        if (tab != selectedTab) {
            selectedTab = tab
        } else {
            coroutineScope.reveal(activeListState, activeBarState)
        }
    }
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    val navigationBar = navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        navigationSuiteType == NavigationSuiteType.ShortNavigationBarMedium
    val updatesAvailable = pluralStringResource(R.plurals.updates_available, updates.size, updates.size)

    val television = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val overscan = if (television) {
        WindowInsets(TvOverscanHorizontal, TvOverscanVertical, TvOverscanHorizontal, TvOverscanVertical)
    } else {
        WindowInsets(0)
    }

    NavigationSuiteScaffold(
        navigationItems = {
            AppTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { selectTab(tab) },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) tab.selectedIconRes else tab.iconRes),
                            contentDescription = null
                        )
                    },
                    label = { Text(text = stringResource(tab.labelRes)) },
                    navigationSuiteType = navigationSuiteType,
                    badge = if (tab == AppTab.Updates && updates.isNotEmpty()) {
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
                            val first = tab == AppTab.entries.first()
                            val last = tab == AppTab.entries.last()
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
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(activeBarState)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(selectedTab.labelRes)) },
                    actions = {
                        if (selectedTab == AppTab.Updates) {
                            val toUpdates = Modifier.focusProperties { down = updatesFocus }
                            BarAction(
                                icon = R.drawable.ic_apk_install,
                                label = stringResource(R.string.install_bundle),
                                onClick = pickBundle,
                                modifier = toUpdates
                            )
                            BarAction(
                                icon = R.drawable.ic_refresh,
                                label = stringResource(R.string.refresh),
                                onClick = viewModel::refresh,
                                modifier = toUpdates
                            )
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets.add(overscan.only(WindowInsetsSides.Top + WindowInsetsSides.End)),
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.add(overscan.only(WindowInsetsSides.Bottom + WindowInsetsSides.End))
        ) { padding ->
            val layoutDirection = LocalLayoutDirection.current
            val density = LocalDensity.current
            val contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + ContentMargin,
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection) + ContentMargin,
                bottom = padding.calculateBottomPadding() + ContentMargin
            )
            val focusScroll = remember(density) { FocusScroll(density) }
            val motion = MaterialTheme.motionScheme
            SideEffect { focusScroll.padding = contentPadding }

            CompositionLocalProvider(LocalBringIntoViewSpec provides focusScroll) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), FADE_THROUGH_SCALE))
                            .togetherWith(fadeOut(motion.fastEffectsSpec()))
                    }
                ) { tab ->
                    when (tab) {
                        AppTab.Updates -> UpdatesView(
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
                            onApkMirror = { if (!openUrlInBrowser(context, it)) showMessage(noBrowser) },
                            onPlay = viewModel::updateFromPlay,
                            onRefresh = viewModel::refresh
                        )
                        AppTab.Settings -> SettingsView(
                            listState = settingsListState,
                            barState = settingsBarState,
                            nestedScroll = scrollBehavior.nestedScrollConnection,
                            focus = settingsFocus,
                            contentPadding = contentPadding,
                            themeMode = themeMode,
                            includeDisabledApps = uiState.includeDisabledApps,
                            onIncludeDisabledAppsChange = viewModel::setIncludeDisabledApps,
                            onTheme = { themeDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (themeDialog) {
        ThemeDialog(
            current = themeMode,
            onDismiss = { themeDialog = false },
            onSelect = { mode ->
                themeDialog = false
                viewModel.setThemeMode(mode)
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
    onRefresh: () -> Unit
) {
    val pullState = rememberPullToRefreshState()
    val motion = MaterialTheme.motionScheme
    val fade = motion.defaultEffectsSpec<Float>()
    val placement = motion.defaultSpatialSpec<IntOffset>()
    val reveal = rememberTopReveal(listState, barState)
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val cards = remember(keyboard) { CardFocus() }
    val installList = installs.entries.toList()

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
            verticalArrangement = Arrangement.spacedBy(SmallSpacing)
        ) {
            if (notice != null && updates.isNotEmpty()) {
                item(key = "notice") {
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(0, 1),
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

            items(installList, key = { it.key }) { (_, state) ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    colors = listItemColors(),
                    modifier = Modifier.animateItem(fade, placement, fade),
                    supportingContent = { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                ) {
                    Text(text = state.appName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (updates.isEmpty() && loaded) {
                item(key = "empty") {
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

            itemsIndexed(items = updates, key = { _, pair -> pair.first.packageName }) { index, pair ->
                UpdateItem(
                    app = pair.first,
                    update = pair.second,
                    install = playInstalls[pair.first.packageName],
                    cards = cards,
                    onApkMirror = onApkMirror,
                    onPlay = onPlay,
                    modifier = Modifier
                        .animateItem(fade, placement, fade)
                        .then(if (index == 0) reveal.modifier else Modifier)
                )
            }
        }

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(motion.defaultEffectsSpec()),
            exit = fadeOut(motion.defaultEffectsSpec()),
            modifier = Modifier.align(Alignment.Center)
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
    themeMode: ThemeMode,
    includeDisabledApps: Boolean,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    onTheme: () -> Unit
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
        verticalArrangement = Arrangement.spacedBy(SmallSpacing)
    ) {
        item(key = "appearance") {
            SectionHeader(text = stringResource(R.string.appearance))
        }

        item(key = "theme") {
            SegmentedListItem(
                onClick = onTheme,
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = listItemColors(),
                modifier = reveal.modifier,
                supportingContent = { Text(text = stringResource(themeMode.labelRes)) }
            ) {
                Text(text = stringResource(R.string.theme))
            }
        }

        item(key = "updates") {
            SectionHeader(text = stringResource(R.string.updates), modifier = Modifier.padding(top = ContentMargin))
        }

        item(key = "disabled-apps") {
            SegmentedListItem(
                checked = includeDisabledApps,
                onCheckedChange = onIncludeDisabledAppsChange,
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
    install: PlayInstall?,
    cards: CardFocus,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val packageName = app.packageName
    val buttonFocus = remember { List(BUTTON_SLOTS) { FocusRequester() } }
    val slots = remember(update) {
        listOfNotNull(
            PLAY_SLOT.takeIf { update.playUpdate != null },
            APKMIRROR_SLOT.takeIf { update.apkMirrorUrl != null }
        )
    }
    val buttonModifiers = remember {
        List(BUTTON_SLOTS) { slot -> Modifier.focusRequester(buttonFocus[slot]) }
    }
    val entry = (if (install == null) slots.firstOrNull() else PLAY_SLOT)?.let(buttonFocus::get)
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
    val iconSizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
    val iconBitmap by produceState(AppIconCache.peek(app.packageName), app.packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                AppIconCache.load(context, app.packageName, iconSizePx)
            }
        }
    }

    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = listItemColors(),
        modifier = modifier.then(cardModifier),
        leadingContent = {
            Box(modifier = Modifier.size(AppIconSize)) {
                iconBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(SmallSpacing)) {
                Column(verticalArrangement = Arrangement.spacedBy(VersionSpacing)) {
                    Text(
                        text = if (app.versionName.isEmpty()) {
                            stringResource(R.string.version_current_code, app.versionCode)
                        } else {
                            stringResource(R.string.version_current, displayVersion(app.versionName), app.versionCode)
                        }
                    )
                    Text(
                        text = stringResource(
                            R.string.version_latest,
                            displayVersion(update.newVersionName),
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
                            enabled = install == null,
                            busy = install != null,
                            progress = install?.progress,
                            onClick = { onPlay(update) },
                            modifier = buttonModifiers[PLAY_SLOT]
                        )
                    }
                    update.apkMirrorUrl?.let { url ->
                        SourceButton(
                            text = stringResource(R.string.update),
                            icon = R.drawable.ic_source_apkmirror,
                            source = stringResource(R.string.source_apkmirror),
                            enabled = install == null,
                            onClick = { onApkMirror(url) },
                            modifier = buttonModifiers[APKMIRROR_SLOT]
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
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "progress"
    )

    FilledTonalButton(
        onClick = { if (active) onClick() },
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.semantics { if (busy) disabled() },
        enabled = enabled || busy,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = source,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
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
            DialogBlurBehind()
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (selected) Modifier.focusRequester(focus) else Modifier)
                            .selectable(selected = selected, onClick = { onSelect(mode) }, role = Role.RadioButton)
                            .minimumInteractiveComponentSize(),
                        horizontalArrangement = Arrangement.spacedBy(ContentMargin),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(text = stringResource(mode.labelRes), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    )
}

@Composable
private fun DialogBlurBehind() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    DisposableEffect(window) {
        val windowManager = window.windowManager
        val listener = Consumer<Boolean> { enabled ->
            window.setDimAmount(if (enabled) DIM_WITH_BLUR else DIM_WITHOUT_BLUR)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = BLUR_BEHIND_RADIUS }
        windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }
}

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

private class FocusScroll(private val density: Density) : BringIntoViewSpec {
    var padding = PaddingValues()

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val top = with(density) { padding.calculateTopPadding().toPx() }
        val end = containerSize - with(density) { padding.calculateBottomPadding().toPx() }
        if (offset >= top && offset + size <= end) return 0f
        return offset - (containerSize * FOCUS_PIVOT).coerceIn(top, maxOf(top, end - size))
    }
}

private fun displayVersion(name: String): String =
    if (name.length > 1 && name[0].lowercaseChar() == 'v' && name[1].isDigit()) name.substring(1) else name

private fun openUrlInBrowser(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}
