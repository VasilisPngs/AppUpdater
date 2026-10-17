@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.appupdater.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLengthTrim
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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

private val ContentMargin = 16.dp
private val SmallSpacing = 8.dp
private val EmptyPadding = 48.dp
private val EmptyIconSize = 48.dp
private val AppIconSize = 40.dp
private const val MAX_VERSION_CODE_DIGITS = 19
private const val APKMIRROR_SLOT = 0
private const val PLAY_SLOT = 1
private const val MANUAL_SLOT = 2
private const val BUTTON_SLOTS = 3
private const val TABULAR_FIGURES = "tnum"
private const val FOCUS_PIVOT = 0.3f

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
    var manualPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var themeDialog by rememberSaveable { mutableStateOf(false) }
    val manualTarget = uiState.updates.firstOrNull { it.packageName == manualPackage }
    LaunchedEffect(manualTarget == null) {
        if (manualTarget == null) manualPackage = null
    }
    BackHandler(enabled = selectedTab != AppTab.Updates) { selectedTab = AppTab.Updates }
    val installedFormat = stringResource(R.string.update_installed)
    val installedFromPlayFormat = stringResource(R.string.update_installed_play)
    val failedFormat = stringResource(R.string.update_failed)
    val noBrowser = stringResource(R.string.no_browser)
    val noFilePicker = stringResource(R.string.no_file_picker)
    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
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
        modifier = modifier,
        navigationSuiteType = navigationSuiteType
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(activeBarState)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(selectedTab.labelRes)) },
                    actions = {
                        if (selectedTab == AppTab.Updates) {
                            IconButton(
                                onClick = viewModel::refresh,
                                modifier = Modifier.focusProperties { down = updatesFocus }
                            ) {
                                Icon(painter = painterResource(R.drawable.ic_refresh), contentDescription = null)
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { contentPadding ->
            val density = LocalDensity.current
            val focusScroll = remember(density) { FocusScroll(density) }
            SideEffect { focusScroll.padding = contentPadding }

            CompositionLocalProvider(LocalBringIntoViewSpec provides focusScroll) {
                when (selectedTab) {
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
                        onManual = { manualPackage = it.packageName },
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
                        onTheme = { themeDialog = true },
                        onPickBundle = {
                            try {
                                bundlePicker.launch(arrayOf("*/*"))
                            } catch (_: ActivityNotFoundException) {
                                showMessage(noFilePicker)
                            }
                        }
                    )
                }
            }
        }
    }

    manualTarget?.let { update ->
        ManualDialog(
            update = update,
            onDismiss = { manualPackage = null },
            onSubmit = { versionCode ->
                manualPackage = null
                viewModel.installManually(update, versionCode)
            }
        )
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
    onManual: (AppUpdateInfo) -> Unit,
    onRefresh: () -> Unit
) {
    val pullState = rememberPullToRefreshState()
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
        cards.codes = false
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
                .focusRestorer()
                .onFocusChanged { if (!it.hasFocus) cards.codes = false },
            contentPadding = contentPadding
        ) {
            if (notice != null && updates.isNotEmpty()) {
                item(key = "notice") {
                    ListItem(modifier = Modifier.animateItem()) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(installList, key = { it.key }) { (_, state) ->
                ListItem(
                    modifier = Modifier.animateItem(),
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
                            modifier = Modifier.animateItem()
                        )
                    } else {
                        EmptyState(
                            symbol = R.drawable.ic_warning,
                            title = stringResource(R.string.check_failed),
                            message = notice,
                            modifier = Modifier.animateItem()
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
                    keyboard = keyboard,
                    cards = cards,
                    first = index == 0,
                    last = index == updates.lastIndex,
                    onApkMirror = onApkMirror,
                    onPlay = onPlay,
                    onManual = onManual,
                    modifier = Modifier
                        .animateItem()
                        .then(if (index == 0) reveal.modifier else Modifier)
                )
            }
        }

        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
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
    onTheme: () -> Unit,
    onPickBundle: () -> Unit
) {
    val reveal = rememberTopReveal(listState, barState)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScroll)
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = contentPadding
    ) {
        item(key = "appearance") {
            SectionHeader(text = stringResource(R.string.appearance))
        }

        item(key = "theme") {
            ListItem(
                onClick = onTheme,
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
            ListItem(
                checked = includeDisabledApps,
                onCheckedChange = onIncludeDisabledAppsChange,
                supportingContent = { Text(text = stringResource(R.string.disabled_apps_description)) },
                trailingContent = { Switch(checked = includeDisabledApps, onCheckedChange = null) }
            ) {
                Text(text = stringResource(R.string.disabled_apps))
            }
        }

        item(key = "install-bundle") {
            ListItem(onClick = onPickBundle) {
                Text(text = stringResource(R.string.install_bundle))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = ContentMargin, end = ContentMargin, bottom = SmallSpacing)
    )
}

@Composable
private fun UpdateItem(
    app: InstalledApp,
    update: AppUpdateInfo,
    install: PlayInstall?,
    keyboard: Boolean,
    cards: CardFocus,
    first: Boolean,
    last: Boolean,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val packageName = app.packageName
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val toCode = if (ltr) Key.DirectionLeft else Key.DirectionRight
    val toButtons = if (ltr) Key.DirectionRight else Key.DirectionLeft
    val codeFocus = remember { FocusRequester() }
    val buttonFocus = remember { List(BUTTON_SLOTS) { FocusRequester() } }
    var codeFocused by remember(keyboard) { mutableStateOf(false) }
    var lastButton by remember { mutableIntStateOf(-1) }
    val slots = remember(update) {
        listOfNotNull(
            APKMIRROR_SLOT.takeIf { update.apkMirrorUrl != null },
            PLAY_SLOT.takeIf { update.playUpdate != null },
            MANUAL_SLOT.takeIf { update.playAvailable }
        )
    }
    val buttonModifiers = remember(cards) {
        List(BUTTON_SLOTS) { slot ->
            Modifier
                .fillMaxWidth()
                .focusRequester(buttonFocus[slot])
                .focusProperties { if (cards.codes) canFocus = false }
                .onFocusChanged { if (it.isFocused) lastButton = slot }
        }
    }
    val entry = when {
        install == null -> slots.firstOrNull()
        install.manual -> MANUAL_SLOT
        else -> PLAY_SLOT
    }?.let(buttonFocus::get)
    val cardModifier = remember(cards, packageName) {
        Modifier.onFocusChanged {
            if (it.hasFocus) {
                cards.focused = packageName
            } else if (cards.focused == packageName) {
                cards.focused = null
            }
        }
    }
    DisposableEffect(cards) {
        onDispose { if (codeFocused) cards.codes = false }
    }
    DisposableEffect(cards, packageName, entry) {
        if (entry != null) cards.entries[packageName] = entry
        onDispose { cards.entries.remove(packageName) }
    }
    val code = update.newVersionCode.toString()
    val copyCode: () -> Unit = {
        coroutineScope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, code))) }
    }
    val codeModifier = remember(keyboard, cards, first, last, toButtons, slots, copyCode) {
        if (keyboard) {
            Modifier
                .focusRequester(codeFocus)
                .focusProperties {
                    if (!cards.codes) canFocus = false
                    up = if (first) FocusRequester.Cancel else FocusRequester.Default
                    down = if (last) FocusRequester.Cancel else FocusRequester.Default
                    start = FocusRequester.Cancel
                }
                .onKeyEvent { event ->
                    if (event.key != toButtons || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val slot = lastButton.takeIf { it in slots } ?: slots.firstOrNull() ?: return@onKeyEvent true
                    cards.codes = false
                    buttonFocus[slot].requestFocus()
                    true
                }
                .onFocusChanged { codeFocused = it.isFocused }
                .clickable(interactionSource = null, indication = null, onClick = copyCode)
        } else {
            Modifier
        }
    }
    val latestText = stringResource(R.string.version_latest, displayVersion(update.newVersionName), update.newVersionCode)
    val latest = remember(latestText, code, colors, keyboard, codeFocused) {
        val start = latestText.lastIndexOf(code)
        val highlight = SpanStyle(color = colors.onPrimary, background = colors.primary)
        buildAnnotatedString {
            append(latestText)
            if (start >= 0) {
                if (!keyboard) {
                    addLink(
                        LinkAnnotation.Clickable(
                            tag = code,
                            styles = TextLinkStyles(focusedStyle = highlight, pressedStyle = highlight)
                        ) { copyCode() },
                        start,
                        start + code.length
                    )
                } else if (codeFocused) {
                    addStyle(highlight, start, start + code.length)
                }
            }
        }
    }
    val iconSizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
    val iconBitmap by produceState(AppIconCache.peek(app.packageName), app.packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                AppIconCache.load(context, app.packageName, iconSizePx)
            }
        }
    }

    ListItem(
        modifier = modifier.then(cardModifier),
        verticalAlignment = Alignment.CenterVertically,
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
            Column {
                Text(
                    text = if (app.versionName.isEmpty()) {
                        stringResource(R.string.version_current_code, app.versionCode)
                    } else {
                        stringResource(R.string.version_current, displayVersion(app.versionName), app.versionCode)
                    }
                )
                Text(text = latest, modifier = codeModifier)
            }
        },
        trailingContent = {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .onKeyEvent { event ->
                        if (!keyboard || event.key != toCode || event.type != KeyEventType.KeyDown) {
                            return@onKeyEvent false
                        }
                        cards.codes = true
                        codeFocus.requestFocus().also { if (!it) cards.codes = false }
                    },
                verticalArrangement = Arrangement.spacedBy(SmallSpacing)
            ) {
                update.apkMirrorUrl?.let { url ->
                    SourceButton(
                        text = stringResource(R.string.update),
                        icon = R.drawable.ic_source_apkmirror,
                        enabled = install == null,
                        onClick = { onApkMirror(url) },
                        modifier = buttonModifiers[APKMIRROR_SLOT]
                    )
                }
                if (update.playUpdate != null) {
                    SourceButton(
                        text = stringResource(R.string.update),
                        icon = R.drawable.ic_source_play,
                        enabled = install == null,
                        busy = install?.manual == false,
                        progress = install?.progress,
                        onClick = { onPlay(update) },
                        modifier = buttonModifiers[PLAY_SLOT]
                    )
                }
                if (update.playAvailable) {
                    SourceButton(
                        text = stringResource(R.string.manual),
                        icon = R.drawable.ic_source_play,
                        enabled = install == null,
                        busy = install?.manual == true,
                        progress = install?.progress,
                        onClick = { onManual(update) },
                        modifier = buttonModifiers[MANUAL_SLOT]
                    )
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
        modifier = modifier,
        enabled = enabled || busy,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(painter = painterResource(icon), contentDescription = null)
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
private fun ManualDialog(
    update: AppUpdateInfo,
    onDismiss: () -> Unit,
    onSubmit: (Long) -> Unit
) {
    val code = rememberTextFieldState()
    val versionCode = code.text.toString().toLongOrNull()
    val focus = remember { FocusRequester() }
    val submit = { if (versionCode != null) onSubmit(versionCode) }

    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = submit, enabled = versionCode != null) {
                Text(text = stringResource(R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = update.appName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            OutlinedTextField(
                state = code,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                label = { Text(text = stringResource(R.string.version_code)) },
                inputTransformation = DigitsOnly.maxLengthTrim(MAX_VERSION_CODE_DIGITS),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                onKeyboardAction = { submit() },
                lineLimits = TextFieldLineLimits.SingleLine
            )
        }
    )
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
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = { Text(text = stringResource(R.string.theme)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (selected) Modifier.focusRequester(focus) else Modifier)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onSelect(mode) }
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

private val DigitsOnly = InputTransformation {
    val digits = asCharSequence().filter(Char::isDigit)
    if (digits.length != length) replace(0, length, digits)
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
    var codes by mutableStateOf(false)
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
