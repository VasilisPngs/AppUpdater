package com.android.appupdater.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLengthTrim
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.appupdater.R
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstallState
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayInstall
import com.android.appupdater.data.model.ThemeMode
import com.android.appupdater.data.repository.ScanStatus
import com.android.appupdater.ui.theme.Design
import com.android.appupdater.ui.theme.DisabledOpacity
import com.android.appupdater.ui.theme.Glass
import com.android.appupdater.ui.theme.Motion
import com.android.appupdater.ui.theme.PressedOpacity
import com.android.appupdater.ui.theme.PressedScale
import com.android.appupdater.ui.theme.Radius
import com.android.appupdater.ui.theme.ShapeCard
import com.android.appupdater.ui.theme.ShapeIcon
import com.android.appupdater.ui.theme.ShapePill
import com.android.appupdater.ui.theme.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.min

private val TabBarInset = 21.dp
private val TabBarPaddingVertical = 3.dp
private val TabBarPaddingHorizontal = 1.dp
private val TabTop = 8.dp
private val TabBottom = 9.dp
private val TabGap = 1.dp
private val TabIconWidth = 32.dp
private val TabBarBottom = 12.dp
private val TabBarSafeOverlap = 13.dp
private val BarTopMin = 8.dp
private val BarBottom = 6.dp
private val BarIconSize = 22.dp
private val ContentBottomGap = 28.dp
private val WideSheetBreakpoint = 600.dp
private val AppIconSize = 44.dp
private val DotSize = 7.dp
private val IconSize = 18.dp
private val ChevronInset = 2.dp
private val SwitchWidth = 51.dp
private val SwitchHeight = 31.dp
private val SwitchKnob = 27.dp
private val SwitchInset = 2.dp
private val SheetMaxWidth = 560.dp
private val SheetWideBottom = 24.dp
private val SheetDragClose = 200.dp
private val GrabberWidth = 36.dp
private val GrabberHeight = 5.dp
private val GrabberTop = 6.dp
private val ToastMaxWidth = 544.dp
private val ToastGap = 20.dp
private val ToastEnter = 10.dp
private val ToastExit = 6.dp
private val EmptyPadding = 32.dp
private val FieldPadding = 10.dp
private val GroupInset = 17.dp
private val KnobShadowBlur = 6.dp
private val KnobShadowOffset = 2.dp
private val KnobShadow = Color(0x4D000000)
private val FocusRingWidth = 2.dp
private val FocusRingOffset = 2.dp
private const val TOAST_MILLIS = 2200L
private const val LOADING_DELAY_MILLIS = 2000L
private const val PULSE_MILLIS = 550
private const val PULSE_SHRINK = 0.14f
private const val PULSE_FADE = 0.55f
private const val MAX_VERSION_CODE_DIGITS = 19
private const val APKMIRROR_SLOT = 0
private const val PLAY_SLOT = 1
private const val MANUAL_SLOT = 2
private const val BUTTON_SLOTS = 3
private const val TABULAR_FIGURES = "tnum"
private const val TOP_BAR_EMS = 3.125f
private const val TAB_ICON_ASPECT = 1.2f
private const val TITLE_KEY = "title"
private const val APPEARANCE_KEY = "appearance"
private const val SHEET_FLICK_DP_PER_SECOND = 500
private const val SHEET_DRAG_FADE = 0.6f
private const val GRABBER_ALPHA = 0.28f
private const val BAR_ITEM_HIDDEN_SCALE = 0.9f
private const val FOCUS_PIVOT = 0.3f

private enum class AppTab(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    Updates(R.string.updates, R.drawable.ic_updates),
    Settings(R.string.settings, R.drawable.ic_settings)
}

private enum class CardStyle(val horizontal: Dp, val vertical: Dp, val gap: Dp) {
    Regular(Space.l, Space.l, Space.m),
    Tight(Space.l, Space.m, Space.s),
    Flush(0.dp, 0.dp, 0.dp)
}

private enum class ButtonStyle { Regular, Primary, Ghost, Selected }

private class Toast(val id: Long, val message: String)

private fun SnapshotStateList<Toast>.show(message: String) {
    add(Toast(System.nanoTime(), message))
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
    val colors = Design.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Updates) }
    val updatesListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val updatesFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val tabFocus = remember { FocusRequester() }
    val pull = rememberPullRefreshState(
        atTop = { !updatesListState.canScrollBackward },
        onRefresh = { viewModel.scanForUpdates() }
    )
    val inputModeManager = LocalInputModeManager.current
    val coroutineScope = rememberCoroutineScope()
    val toasts = remember { mutableStateListOf<Toast>() }
    val backdrop = rememberGraphicsLayer()
    val bundlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::installBundle)
    }
    var manualPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var themeSheet by rememberSaveable { mutableStateOf(false) }
    var pendingTheme by remember { mutableStateOf<ThemeMode?>(null) }
    val manualTarget = uiState.updates.firstOrNull { it.packageName == manualPackage }
    LaunchedEffect(manualTarget == null) {
        if (manualTarget == null) manualPackage = null
    }
    val sheetOpen = manualTarget != null || themeSheet
    val closeSheets = {
        manualPackage = null
        themeSheet = false
    }
    BackHandler(enabled = !sheetOpen && selectedTab != AppTab.Updates) { selectedTab = AppTab.Updates }
    BackHandler(enabled = sheetOpen, onBack = closeSheets)
    val sheetProgress = animateFloatAsState(
        targetValue = if (sheetOpen) 1f else 0f,
        animationSpec = tween(Motion.SHEET, easing = Motion.easeSheet),
        label = "sheet"
    )
    val sheetShown by remember { derivedStateOf { sheetProgress.value > 0f } }
    val sheetDrag = remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val barTop = max(insets.calculateTopPadding(), BarTopMin)
    val barOffset = with(density) { (Design.type.body.fontSize * TOP_BAR_EMS).toDp() }
    val barEdge = with(density) { (barTop + Space.control + BarBottom).roundToPx() }
    val tabLine = with(density) { Design.type.tab.lineHeight.toDp() }
    val installedFormat = stringResource(R.string.update_installed)
    val installedFromPlayFormat = stringResource(R.string.update_installed_play)
    val failedFormat = stringResource(R.string.update_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            toasts.show(
                when (event) {
                    is InstallEvent.Finished -> installedFormat.format(event.appName)
                    is InstallEvent.FinishedFromPlay -> installedFromPlayFormat.format(event.appName, displayVersion(event.versionName))
                    is InstallEvent.Failed -> failedFormat.format(event.message)
                }
            )
        }
    }

    LaunchedEffect(pendingTheme, sheetShown) {
        val mode = pendingTheme ?: return@LaunchedEffect
        if (!sheetShown) {
            pendingTheme = null
            viewModel.setThemeMode(mode)
        }
    }

    val updates = remember(uiState.installedApps, uiState.updates) {
        val installedByPackage = uiState.installedApps.associateBy(InstalledApp::packageName)
        uiState.updates.mapNotNull { update ->
            installedByPackage[update.packageName]?.let { it to update }
        }
    }

    val activeListState = if (selectedTab == AppTab.Updates) updatesListState else settingsListState
    val activeFocus = if (selectedTab == AppTab.Updates) updatesFocus else settingsFocus
    val titled by remember(activeListState, barEdge) {
        derivedStateOf {
            val info = activeListState.layoutInfo
            val heading = info.visibleItemsInfo.firstOrNull { it.key == TITLE_KEY }
            if (heading == null) {
                activeListState.firstVisibleItemIndex > 0
            } else {
                activeListState.canScrollBackward && heading.offset + heading.size - info.viewportStartOffset <= barEdge
            }
        }
    }
    val scanning = uiState.scanStatus == ScanStatus.Scanning
    val loading = scanning && !uiState.loaded
    val refreshing = scanning && uiState.loaded && selectedTab == AppTab.Updates
    var loadingShown by remember { mutableStateOf(false) }

    LaunchedEffect(loading) {
        loadingShown = false
        if (loading) {
            delay(LOADING_DELAY_MILLIS)
            loadingShown = true
        }
    }
    val selectTab: (AppTab) -> Unit = { tab ->
        val state = if (tab == AppTab.Updates) updatesListState else settingsListState
        if (tab != selectedTab) {
            selectedTab = tab
            coroutineScope.launch { state.scrollToItem(0) }
        } else if (state.canScrollBackward) {
            coroutineScope.launch { state.animateScrollToItem(0) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        LaunchedEffect(Unit) {
            if (inputModeManager.inputMode == InputMode.Keyboard) tabFocus.requestFocus()
        }
        SideEffect {
            pull.enabled = selectedTab == AppTab.Updates && uiState.loaded && inputModeManager.inputMode == InputMode.Touch
            pull.extent = constraints.maxHeight.toFloat()
            pull.update(refreshing)
        }
        val wideSheet = maxWidth >= WideSheetBreakpoint
        val topInset = insets.calculateTopPadding()
        val bottomInset = insets.calculateBottomPadding()
        val startInset = insets.calculateStartPadding(layoutDirection)
        val endInset = insets.calculateEndPadding(layoutDirection)
        val startPadding = max(Space.l, startInset)
        val endPadding = max(Space.l, endInset)
        val contentTop = barTop + barOffset + Space.l
        val tabBarBottom = max(TabBarBottom, bottomInset - TabBarSafeOverlap)
        val tabBarHeight = (TabBarPaddingVertical + Space.hairline) * 2 + TabTop + TabBottom +
            TabIconWidth / TAB_ICON_ASPECT + TabGap + tabLine
        val contentPadding = PaddingValues(
            start = startPadding,
            end = endPadding,
            top = contentTop,
            bottom = tabBarBottom + tabBarHeight + ContentBottomGap
        )
        val toastBottom = tabBarBottom + tabBarHeight + ToastGap
        val focusScroll = remember(contentPadding, density) {
            with(density) {
                FocusScroll(contentPadding.calculateTopPadding().toPx(), contentPadding.calculateBottomPadding().toPx())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = (sheetProgress.value * (1f - SHEET_DRAG_FADE * sheetDrag.floatValue)).takeIf { it > 0f }?.let { progress ->
                        backdropEffect(
                            Glass.sheetBackdropBlur.toPx() * progress,
                            1f + (Glass.SHEET_BACKDROP_SATURATION - 1f) * progress
                        )
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().captureBackdrop(backdrop, colors.background)) {
                CompositionLocalProvider(LocalBringIntoViewSpec provides focusScroll) {
                    when (selectedTab) {
                        AppTab.Updates -> {
                            DisposableEffect(pull) { onDispose(pull::release) }
                            UpdatesView(
                                listState = updatesListState,
                                focus = updatesFocus,
                                contentPadding = contentPadding,
                                loaded = uiState.loaded,
                                notice = (uiState.scanStatus as? ScanStatus.Error)?.message,
                                updates = updates,
                                installs = uiState.installs,
                                playInstalls = uiState.playInstalls,
                                onApkMirror = { openUrlInBrowser(context, it) },
                                onPlay = viewModel::updateFromPlay,
                                onManual = { manualPackage = it.packageName },
                                modifier = Modifier
                                    .pullRefresh(pull)
                                    .graphicsLayer { translationY = pull.offset }
                            )
                        }
                        AppTab.Settings -> SettingsView(
                            listState = settingsListState,
                            focus = settingsFocus,
                            contentPadding = contentPadding,
                            themeMode = themeMode,
                            includeDisabledApps = uiState.includeDisabledApps,
                            onIncludeDisabledAppsChange = viewModel::setIncludeDisabledApps,
                            onTheme = { themeSheet = true },
                            onPickBundle = { bundlePicker.launch(arrayOf("*/*")) }
                        )
                    }
                }
                AnimatedVisibility(
                    visible = loadingShown && selectedTab == AppTab.Updates,
                    enter = fadeIn(tween(Motion.FAST, easing = LinearEasing)),
                    exit = fadeOut(tween(Motion.FAST, easing = LinearEasing)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    LoadingIndicator(color = colors.muted, modifier = Modifier.size(LoadingSpinnerSize))
                }
                RefreshIndicator(
                    color = colors.muted,
                    spinning = refreshing,
                    progress = { pull.progress },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = barTop + Space.control + BarBottom + (PullBand - PullSpinnerSize) / 2)
                        .size(PullSpinnerSize)
                        .graphicsLayer {
                            translationY = pull.lift
                            alpha = if (pull.refreshing && pull.offset <= 0f) 0f else 1f
                        }
                )
            }

            TopBar(
                backdrop = backdrop,
                observe = {
                    activeListState.firstVisibleItemScrollOffset
                    pull.offset
                },
                top = barTop,
                startPadding = startPadding,
                endPadding = endPadding,
                title = if (selectedTab == AppTab.Updates && updates.isNotEmpty()) {
                    stringResource(R.string.updates_count, updates.size)
                } else {
                    stringResource(selectedTab.labelRes)
                },
                titled = titled,
                actions = selectedTab == AppTab.Updates,
                listFocus = activeFocus,
                onRefresh = viewModel::scanForUpdates
            )

            TabBar(
                backdrop = backdrop,
                focus = tabFocus,
                listFocus = activeFocus,
                observe = { activeListState.firstVisibleItemScrollOffset },
                selectedTab = selectedTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = max(TabBarInset, startInset), end = max(TabBarInset, endInset), bottom = tabBarBottom),
                onSelect = selectTab
            )
        }

        if (sheetShown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val progress = sheetProgress.value * (1f - SHEET_DRAG_FADE * sheetDrag.floatValue)
                        drawRect(colors.scrim.copy(alpha = colors.scrim.alpha * progress))
                    }
                    .pointerInput(Unit) { detectTapGestures { closeSheets() } }
            )
        }

        ManualSheet(
            target = manualTarget,
            wide = wideSheet,
            drag = sheetDrag,
            returnFocus = updatesFocus,
            modifier = Modifier.align(Alignment.BottomCenter),
            onCancel = { manualPackage = null },
            onSubmit = { update, versionCode ->
                manualPackage = null
                viewModel.installManually(update, versionCode)
            }
        )

        ThemeSheet(
            visible = themeSheet,
            current = themeMode,
            wide = wideSheet,
            drag = sheetDrag,
            returnFocus = settingsFocus,
            modifier = Modifier.align(Alignment.BottomCenter),
            onCancel = { themeSheet = false },
            onSelect = { mode ->
                themeSheet = false
                pendingTheme = mode
            }
        )

        ToastHost(
            toasts = toasts,
            backdrop = backdrop,
            modifier = if (sheetOpen) {
                Modifier.align(Alignment.TopCenter).padding(top = topInset + Space.m)
            } else {
                Modifier.align(Alignment.BottomCenter).padding(bottom = toastBottom)
            }
        )
    }
}

@Composable
private fun UpdatesView(
    listState: LazyListState,
    focus: FocusRequester,
    contentPadding: PaddingValues,
    loaded: Boolean,
    notice: String?,
    updates: List<Pair<InstalledApp, AppUpdateInfo>>,
    installs: Map<String, InstallState>,
    playInstalls: Map<String, PlayInstall>,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstKey = updates.firstOrNull()?.first?.packageName
    val reveal = rememberTitleReveal(listState, firstKey)
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val codes = remember(keyboard) { CodeFocus() }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusRestorer()
            .onFocusChanged { if (!it.hasFocus) codes.active = false },
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        item(key = TITLE_KEY) {
            Text(
                text = if (updates.isEmpty()) stringResource(R.string.updates) else stringResource(R.string.updates_count, updates.size),
                style = Design.type.h1,
                color = Design.colors.text
            )
        }

        if (notice != null) {
            item(key = "notice") {
                Card(style = CardStyle.Tight, modifier = Modifier.animateItem()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.s)
                    ) {
                        StatusDot(color = Design.colors.danger, pulsing = false)
                        Text(text = notice, style = Design.type.muted, color = Design.colors.muted)
                    }
                }
            }
        }

        items(installs.entries.toList(), key = { it.key }) { (_, state) ->
            Card(style = CardStyle.Tight, modifier = Modifier.animateItem()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s)
                ) {
                    StatusDot(color = Design.colors.accent, pulsing = true)
                    Text(
                        text = state.appName,
                        style = Design.type.body,
                        color = Design.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (updates.isEmpty() && loaded) {
            item(key = "empty") { EmptyState(stringResource(R.string.all_up_to_date), Modifier.animateItem()) }
        }

        itemsIndexed(items = updates, key = { _, pair -> pair.first.packageName }) { index, pair ->
            UpdateCard(
                app = pair.first,
                update = pair.second,
                install = playInstalls[pair.first.packageName],
                keyboard = keyboard,
                codes = codes,
                first = index == 0,
                last = index == updates.lastIndex,
                onApkMirror = onApkMirror,
                onPlay = onPlay,
                onManual = onManual,
                modifier = Modifier
                    .animateItem()
                    .then(if (pair.first.packageName == firstKey) Modifier.revealTitle(reveal) else Modifier)
            )
        }
    }
}

@Composable
private fun SettingsView(
    listState: LazyListState,
    focus: FocusRequester,
    contentPadding: PaddingValues,
    themeMode: ThemeMode,
    includeDisabledApps: Boolean,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    onTheme: () -> Unit,
    onPickBundle: () -> Unit
) {
    val colors = Design.colors
    val reveal = rememberTitleReveal(listState, APPEARANCE_KEY)
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        item(key = TITLE_KEY) {
            Text(text = stringResource(R.string.settings), style = Design.type.h1, color = colors.text)
        }

        item(key = APPEARANCE_KEY) {
            Group(heading = stringResource(R.string.appearance), modifier = Modifier.revealTitle(reveal)) {
                Card(style = CardStyle.Flush) {
                    ListRow(onClick = onTheme) {
                        Text(
                            text = stringResource(R.string.theme),
                            style = Design.type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(themeMode.labelRes), style = Design.type.body, color = colors.muted)
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_down),
                                contentDescription = null,
                                tint = colors.chevron,
                                modifier = Modifier.padding(end = ChevronInset).size(IconSize)
                            )
                        }
                    }
                }
            }
        }

        item(key = "disabled-apps") {
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Group(note = stringResource(R.string.disabled_apps_description)) {
                Card(style = CardStyle.Tight) {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = Space.row - Space.m * 2)
                            .toggleable(
                                value = includeDisabledApps,
                                interactionSource = interaction,
                                indication = null,
                                onValueChange = onIncludeDisabledAppsChange
                            ),
                        horizontalArrangement = Arrangement.spacedBy(Space.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.disabled_apps),
                            style = Design.type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        SwitchTrack(checked = includeDisabledApps, focused = focused)
                    }
                }
            }
        }

        item(key = "install-bundle") {
            Card(style = CardStyle.Flush) {
                ListRow(onClick = onPickBundle) {
                    Text(text = stringResource(R.string.install_bundle), style = Design.type.button, color = colors.accent)
                }
            }
        }
    }
}

@Composable
private fun Group(
    modifier: Modifier = Modifier,
    heading: String? = null,
    note: String? = null,
    content: @Composable () -> Unit
) {
    val colors = Design.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Space.s)) {
        if (heading != null) {
            Text(
                text = heading,
                style = Design.type.h2,
                color = colors.text,
                modifier = Modifier.padding(horizontal = GroupInset)
            )
        }
        content()
        if (note != null) {
            Text(
                text = note,
                style = Design.type.muted,
                color = colors.muted,
                modifier = Modifier.padding(horizontal = GroupInset)
            )
        }
    }
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.Regular,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = Design.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeCard)
            .background(colors.surface)
            .padding(horizontal = style.horizontal, vertical = style.vertical),
        verticalArrangement = Arrangement.spacedBy(style.gap),
        content = content
    )
}

@Composable
private fun ListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = Design.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val fill by animateColorAsState(
        targetValue = if (pressed || focused) colors.surface2 else Color.Transparent,
        animationSpec = tween(Motion.FAST, easing = Motion.ease),
        label = "row"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Space.row)
            .background(fill)
            .focusRing(focused, colors.accent, Radius.card, inset = true)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = Space.l, vertical = Space.m),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun UpdateCard(
    app: InstalledApp,
    update: AppUpdateInfo,
    install: PlayInstall?,
    keyboard: Boolean,
    codes: CodeFocus,
    first: Boolean,
    last: Boolean,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit,
    modifier: Modifier
) {
    val colors = Design.colors
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
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
    val button: (Int) -> Modifier = { slot ->
        Modifier
            .fillMaxWidth()
            .focusRequester(buttonFocus[slot])
            .focusProperties { if (codes.active) canFocus = false }
            .onFocusChanged { if (it.isFocused) lastButton = slot }
    }
    DisposableEffect(codes) {
        onDispose { if (codeFocused) codes.active = false }
    }
    val code = update.newVersionCode.toString()
    val copyCode: () -> Unit = {
        coroutineScope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, code))) }
    }
    val latestText = stringResource(R.string.version_latest, displayVersion(update.newVersionName), update.newVersionCode)
    val latest = remember(latestText, code, colors, keyboard, codeFocused) {
        val start = latestText.lastIndexOf(code)
        val highlight = SpanStyle(color = Color.White, background = colors.accent)
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

    Card(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m)
        ) {
            Box(modifier = Modifier.size(AppIconSize).clip(ShapeIcon)) {
                iconBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = update.appName,
                    style = Design.type.body,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (app.versionName.isEmpty()) {
                        stringResource(R.string.version_current_code, app.versionCode)
                    } else {
                        stringResource(R.string.version_current, displayVersion(app.versionName), app.versionCode)
                    },
                    style = Design.type.muted,
                    color = colors.muted
                )
                Text(
                    text = latest,
                    style = Design.type.muted,
                    color = colors.muted,
                    modifier = if (keyboard) {
                        Modifier
                            .focusRequester(codeFocus)
                            .focusProperties {
                                if (!codes.active) canFocus = false
                                up = if (first) FocusRequester.Cancel else FocusRequester.Default
                                down = if (last) FocusRequester.Cancel else FocusRequester.Default
                                start = FocusRequester.Cancel
                            }
                            .onKeyEvent { event ->
                                if (event.key != toButtons || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                val slot = lastButton.takeIf { it in slots } ?: slots.firstOrNull() ?: return@onKeyEvent true
                                codes.active = false
                                buttonFocus[slot].requestFocus()
                                true
                            }
                            .onFocusChanged { codeFocused = it.isFocused }
                            .clickable(interactionSource = null, indication = null, onClick = copyCode)
                    } else {
                        Modifier
                    }
                )
            }
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .onKeyEvent { event ->
                        if (!keyboard || event.key != toCode || event.type != KeyEventType.KeyDown) {
                            return@onKeyEvent false
                        }
                        codes.active = true
                        codeFocus.requestFocus().also { if (!it) codes.active = false }
                    },
                verticalArrangement = Arrangement.spacedBy(Space.s)
            ) {
                update.apkMirrorUrl?.let { url ->
                    Button(
                        text = stringResource(R.string.update),
                        small = true,
                        enabled = install == null,
                        icon = R.drawable.ic_source_apkmirror,
                        onClick = { onApkMirror(url) },
                        modifier = button(APKMIRROR_SLOT)
                    )
                }
                if (update.playUpdate != null) {
                    Button(
                        text = stringResource(R.string.update),
                        small = true,
                        enabled = install == null,
                        busy = install?.manual == false,
                        progress = install?.progress,
                        icon = R.drawable.ic_source_play,
                        onClick = { onPlay(update) },
                        modifier = button(PLAY_SLOT)
                    )
                }
                if (update.playAvailable) {
                    Button(
                        text = stringResource(R.string.manual),
                        small = true,
                        enabled = install == null,
                        busy = install?.manual == true,
                        progress = install?.progress,
                        icon = R.drawable.ic_source_play,
                        onClick = { onManual(update) },
                        modifier = button(MANUAL_SLOT)
                    )
                }
            }
        }
    }
}

@Composable
private fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    style: ButtonStyle = ButtonStyle.Regular,
    enabled: Boolean = true,
    busy: Boolean = false,
    progress: Float? = null,
    @DrawableRes icon: Int? = null
) {
    val colors = Design.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = tween(Motion.FAST, easing = Motion.ease),
        label = "press"
    )
    val fill by animateColorAsState(
        targetValue = when (style) {
            ButtonStyle.Regular -> colors.fill
            ButtonStyle.Selected -> colors.accentSoft
            ButtonStyle.Primary -> colors.accent
            ButtonStyle.Ghost -> Color.Transparent
        },
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fill"
    )
    val content by animateColorAsState(
        targetValue = when (style) {
            ButtonStyle.Primary -> Color.White
            ButtonStyle.Selected -> colors.accent
            ButtonStyle.Regular, ButtonStyle.Ghost -> colors.text
        },
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "content"
    )
    val percent = remember { NumberFormat.getPercentInstance() }
    val progressShown by animateFloatAsState(
        targetValue = if (busy && progress != null) 1f else 0f,
        animationSpec = tween(Motion.FAST, easing = Motion.ease),
        label = "progress"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = if (small) Space.controlSmall else Space.control)
            .scale(scale)
            .alpha(if (enabled || busy) 1f else DisabledOpacity)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .clip(ShapePill)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !busy,
                onClick = onClick
            )
            .padding(horizontal = if (small) Space.m else Space.l),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textStyle = if (small) Design.type.buttonSmall else Design.type.button
            if (icon != null) Icon(painter = painterResource(icon), contentDescription = null, tint = content)
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = textStyle,
                    color = content,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = 1f - progressShown }
                )
                if (busy && progress != null) {
                    Text(
                        text = percent.format(progress),
                        style = textStyle.copy(fontFeatureSettings = TABULAR_FIGURES),
                        color = content,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.matchParentSize().graphicsLayer { alpha = progressShown }
                    )
                }
            }
        }
    }
}

@Composable
private fun BarButton(
    @DrawableRes icon: Int,
    backdrop: GraphicsLayer,
    observe: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Design.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val fade by animateFloatAsState(
        targetValue = if (pressed) PressedOpacity else 1f,
        animationSpec = tween(Motion.FAST, easing = Motion.ease),
        label = "press"
    )
    BackdropSurface(
        backdrop = backdrop,
        tint = colors.surface2.copy(alpha = Glass.MATERIAL).compositeOver(colors.background.copy(alpha = Glass.BAR)),
        shape = CircleShape,
        blurRadius = Glass.blur,
        highlight = colors.glassEdge,
        observe = observe,
        modifier = modifier
            .size(Space.control)
            .alpha(fade)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .border(Space.hairline, colors.glassRim, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.text,
            modifier = Modifier
                .align(Alignment.Center)
                .size(BarIconSize)
        )
    }
}

@Composable
private fun Sheet(
    visible: Boolean,
    wide: Boolean,
    drag: MutableFloatState,
    returnFocus: FocusRequester,
    modifier: Modifier,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(FocusRequester) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(tween(Motion.SHEET, easing = Motion.easeSheet)) { it } +
            fadeIn(tween(Motion.SHEET, easing = Motion.easeSheet)),
        exit = slideOutVertically(tween(Motion.SHEET, easing = Motion.easeSheet)) { it } +
            fadeOut(tween(Motion.SHEET, easing = Motion.easeSheet))
    ) {
        val colors = Design.colors
        val density = LocalDensity.current
        val focusManager = LocalFocusManager.current
        val inputModeManager = LocalInputModeManager.current
        val touch = inputModeManager.inputMode == InputMode.Touch
        val initialFocus = remember { FocusRequester() }
        val open by rememberUpdatedState(visible)
        var height by remember { mutableIntStateOf(0) }
        var offset by remember { mutableFloatStateOf(0f) }
        val follow = { value: Float ->
            offset = value
            drag.floatValue = if (height > 0) (value / height).coerceAtMost(1f) else 0f
        }
        val dragState = rememberDraggableState { delta -> follow((offset + delta).coerceAtLeast(0f)) }
        val closeDistance = with(density) { SheetDragClose.toPx() }
        val flick = with(density) { SHEET_FLICK_DP_PER_SECOND.dp.toPx() }
        val insets = WindowInsets.ime.union(WindowInsets.navigationBars)
            .add(WindowInsets(bottom = if (wide) SheetWideBottom else Space.s))
            .only(WindowInsetsSides.Bottom)

        LaunchedEffect(Unit) {
            if (inputModeManager.inputMode == InputMode.Keyboard) initialFocus.requestFocus()
        }
        if (!visible) LaunchedEffect(Unit) {
            if (inputModeManager.inputMode == InputMode.Keyboard) returnFocus.requestFocus() else focusManager.clearFocus()
        }
        DisposableEffect(Unit) {
            onDispose { drag.floatValue = 0f }
        }

        Box(
            modifier = Modifier
                .windowInsetsPadding(insets)
                .padding(horizontal = if (wide) Space.l else Space.s)
                .widthIn(max = SheetMaxWidth)
                .fillMaxWidth()
                .onSizeChanged { height = it.height }
                .graphicsLayer { translationY = offset }
                .clip(ShapeCard)
                .background(colors.surface.copy(alpha = Glass.SHEET))
                .innerTopHighlight(ShapeCard, colors.glassEdge)
                .border(Space.hairline, colors.glassRim, ShapeCard)
                .pointerInput(Unit) { detectTapGestures { } }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = touch && visible,
                    onDragStopped = { velocity ->
                        if (offset > min(height / 2f, closeDistance) || velocity > flick) {
                            onDismiss()
                        } else {
                            animate(offset, 0f, animationSpec = tween(Motion.SHEET, easing = Motion.easeSheet)) { value, _ ->
                                follow(value)
                            }
                        }
                    }
                )
                .focusProperties { onExit = { if (open) cancelFocusChange() } }
                .focusGroup()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Space.l, vertical = Space.xl),
                verticalArrangement = Arrangement.spacedBy(Space.m)
            ) {
                content(initialFocus)
            }
            if (touch) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = GrabberTop)
                        .size(width = GrabberWidth, height = GrabberHeight)
                        .background(colors.text.copy(alpha = GRABBER_ALPHA), ShapePill)
                )
            }
        }
    }
}

@Composable
private fun ManualSheet(
    target: AppUpdateInfo?,
    wide: Boolean,
    drag: MutableFloatState,
    returnFocus: FocusRequester,
    modifier: Modifier,
    onCancel: () -> Unit,
    onSubmit: (AppUpdateInfo, Long) -> Unit
) {
    Sheet(
        visible = target != null,
        wide = wide,
        drag = drag,
        returnFocus = returnFocus,
        modifier = modifier,
        onDismiss = onCancel
    ) { initialFocus ->
        val update = remember { checkNotNull(target) }
        val code = rememberTextFieldState()
        val versionCode = code.text.toString().toLongOrNull()
        val submit = {
            if (versionCode != null) onSubmit(update, versionCode)
        }

        Text(
            text = update.appName,
            style = Design.type.h2,
            color = Design.colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        VersionCodeField(state = code, focus = initialFocus, onDone = submit)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Button(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            Button(
                text = stringResource(R.string.update),
                onClick = submit,
                style = ButtonStyle.Primary,
                enabled = versionCode != null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeSheet(
    visible: Boolean,
    current: ThemeMode,
    wide: Boolean,
    drag: MutableFloatState,
    returnFocus: FocusRequester,
    modifier: Modifier,
    onCancel: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    Sheet(
        visible = visible,
        wide = wide,
        drag = drag,
        returnFocus = returnFocus,
        modifier = modifier,
        onDismiss = onCancel
    ) { initialFocus ->
        Text(text = stringResource(R.string.theme), style = Design.type.h2, color = Design.colors.text)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ThemeMode.entries.forEach { mode ->
                val selected = mode == current
                Button(
                    text = stringResource(mode.labelRes),
                    onClick = { onSelect(mode) },
                    style = if (selected) ButtonStyle.Selected else ButtonStyle.Regular,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (selected) Modifier.focusRequester(initialFocus) else Modifier)
                )
            }
            Button(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                style = ButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VersionCodeField(state: TextFieldState, focus: FocusRequester, onDone: () -> Unit) {
    val colors = Design.colors
    val placeholder = stringResource(R.string.version_code)

    BasicTextField(
        state = state,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        inputTransformation = DigitsOnly.maxLengthTrim(MAX_VERSION_CODE_DIGITS),
        textStyle = Design.type.body.copy(color = colors.text),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        onKeyboardAction = { onDone() },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(colors.text),
        decorator = { field ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Space.control)
                    .clip(ShapePill)
                    .background(colors.fill)
                    .padding(horizontal = Space.m, vertical = FieldPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.text.isEmpty()) {
                    Text(text = placeholder, style = Design.type.body, color = colors.muted)
                }
                field()
            }
        }
    )
}

private val DigitsOnly = InputTransformation {
    val digits = asCharSequence().filter(Char::isDigit)
    if (digits.length != length) replace(0, length, digits)
}

@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Card(style = CardStyle.Flush, modifier = modifier) {
        Text(
            text = text,
            style = Design.type.muted,
            color = Design.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.l, vertical = EmptyPadding)
        )
    }
}

@Composable
private fun TopBar(
    backdrop: GraphicsLayer,
    observe: () -> Unit,
    top: Dp,
    startPadding: Dp,
    endPadding: Dp,
    title: String,
    titled: Boolean,
    actions: Boolean,
    listFocus: FocusRequester,
    onRefresh: () -> Unit
) {
    val colors = Design.colors
    val itemEnter = fadeIn(tween(Motion.NORMAL, easing = Motion.ease)) +
        scaleIn(tween(Motion.SPRING, easing = Motion.easeSpring), initialScale = BAR_ITEM_HIDDEN_SCALE)
    val itemExit = fadeOut(tween(Motion.FAST, easing = Motion.ease)) +
        scaleOut(tween(Motion.FAST, easing = Motion.ease), targetScale = BAR_ITEM_HIDDEN_SCALE)
    val titleAlpha by animateFloatAsState(
        targetValue = if (titled) 1f else 0f,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "title"
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        BackdropSurface(
            backdrop = backdrop,
            tint = colors.background.copy(alpha = Glass.BAR),
            shape = RectangleShape,
            blurRadius = Glass.blur,
            observe = observe,
            modifier = Modifier
                .fillMaxWidth()
                .height(top + Space.control + BarBottom + Glass.edgeFade)
                .fadeBottom(Glass.edgeFade)
        )
        Layout(
            content = {
                Text(
                    text = title,
                    style = Design.type.barTitle,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                )
                Box {
                    AnimatedVisibility(visible = actions, enter = itemEnter, exit = itemExit) {
                        BarButton(
                            icon = R.drawable.ic_refresh,
                            backdrop = backdrop,
                            observe = observe,
                            onClick = onRefresh,
                            modifier = Modifier.focusProperties { down = listFocus }
                        )
                    }
                }
            },
            modifier = Modifier
                .padding(top = top, start = startPadding, end = endPadding, bottom = BarBottom)
                .fillMaxWidth()
                .height(Space.control)
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val end = measurables[1].measure(loose)
            val side = end.width + Space.m.roundToPx()
            val heading = measurables[0].measure(loose.copy(maxWidth = (constraints.maxWidth - side * 2).coerceAtLeast(0)))
            layout(constraints.maxWidth, constraints.maxHeight) {
                heading.placeRelative((constraints.maxWidth - heading.width) / 2, (constraints.maxHeight - heading.height) / 2)
                end.placeRelative(constraints.maxWidth - end.width, (constraints.maxHeight - end.height) / 2)
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color, pulsing: Boolean) {
    val pulse = if (pulsing) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(PULSE_MILLIS, easing = Motion.ease),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        null
    }

    Box(
        modifier = Modifier
            .size(DotSize)
            .graphicsLayer {
                val factor = pulse?.value ?: 0f
                scaleX = 1f - PULSE_SHRINK * factor
                scaleY = scaleX
                alpha = 1f - PULSE_FADE * factor
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun TabBar(
    backdrop: GraphicsLayer,
    focus: FocusRequester,
    listFocus: FocusRequester,
    observe: () -> Unit,
    selectedTab: AppTab,
    modifier: Modifier,
    onSelect: (AppTab) -> Unit
) {
    val colors = Design.colors
    val lens = animateFloatAsState(
        targetValue = selectedTab.ordinal.toFloat(),
        animationSpec = tween(Motion.SPRING, easing = Motion.easeSpring),
        label = "lens"
    )
    BackdropSurface(
        backdrop = backdrop,
        tint = colors.surface2.copy(alpha = Glass.MATERIAL),
        shape = ShapePill,
        blurRadius = Glass.blur,
        highlight = colors.glassEdge,
        observe = observe,
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(ShapePill) {
                radius = Glass.shadowBlur.toPx()
                offset = Offset(0f, Glass.shadowOffset.toPx())
                color = colors.shadow
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(Space.hairline, colors.glassRim, ShapePill)
                .padding(
                    horizontal = Space.hairline + TabBarPaddingHorizontal,
                    vertical = Space.hairline + TabBarPaddingVertical
                )
                .drawBehind {
                    val width = size.width / AppTab.entries.size
                    val index = if (layoutDirection == LayoutDirection.Rtl) AppTab.entries.lastIndex - lens.value else lens.value
                    drawRoundRect(
                        color = colors.tabLens,
                        topLeft = Offset(width * index, 0f),
                        size = Size(width, size.height),
                        cornerRadius = CornerRadius(size.height / 2)
                    )
                }
        ) {
            AppTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (tab == selectedTab) Modifier.focusRequester(focus) else Modifier)
                        .focusProperties {
                            up = listFocus
                            if (tab == AppTab.entries.first()) start = FocusRequester.Cancel
                            if (tab == AppTab.entries.last()) end = FocusRequester.Cancel
                        },
                    onClick = { onSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: AppTab,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = Design.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val fade by animateFloatAsState(
        targetValue = if (pressed) PressedOpacity else 1f,
        animationSpec = tween(Motion.FAST, easing = Motion.ease),
        label = "tab"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.text,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "tint"
    )
    Column(
        modifier = modifier
            .alpha(fade)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(top = TabTop, bottom = TabBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TabGap)
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.width(TabIconWidth).aspectRatio(TAB_ICON_ASPECT)
        )
        Text(text = stringResource(tab.labelRes), style = Design.type.tab, color = tint)
    }
}

@Composable
private fun SwitchTrack(checked: Boolean, focused: Boolean) {
    val colors = Design.colors
    val track by animateColorAsState(
        targetValue = if (checked) colors.success else colors.surface3,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "track"
    )
    val travel by animateDpAsState(
        targetValue = if (checked) SwitchWidth - SwitchKnob - SwitchInset * 2 else 0.dp,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "knob"
    )
    Box(
        modifier = Modifier
            .size(width = SwitchWidth, height = SwitchHeight)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .background(track, ShapePill)
            .padding(SwitchInset),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(travel.roundToPx(), 0) }
                .size(SwitchKnob)
                .dropShadow(CircleShape) {
                    radius = KnobShadowBlur.toPx()
                    offset = Offset(0f, KnobShadowOffset.toPx())
                    color = KnobShadow
                }
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun ToastHost(
    toasts: SnapshotStateList<Toast>,
    backdrop: GraphicsLayer,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = Space.l)
            .widthIn(max = ToastMaxWidth),
        verticalArrangement = Arrangement.spacedBy(Space.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        toasts.forEach { toast ->
            key(toast.id) {
                ToastItem(message = toast.message, backdrop = backdrop, onGone = { toasts.remove(toast) })
            }
        }
    }
}

@Composable
private fun ToastItem(message: String, backdrop: GraphicsLayer, onGone: () -> Unit) {
    val colors = Design.colors
    val density = LocalDensity.current
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }

    LaunchedEffect(Unit) {
        delay(TOAST_MILLIS)
        visibility.targetState = false
    }
    if (visibility.isIdle && !visibility.currentState && !visibility.targetState) {
        LaunchedEffect(Unit) { onGone() }
    }

    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(Motion.SPRING, easing = Motion.easeSpring)) +
            slideInVertically(tween(Motion.SPRING, easing = Motion.easeSpring)) { with(density) { ToastEnter.roundToPx() } },
        exit = fadeOut(tween(Motion.NORMAL, easing = Motion.ease)) +
            slideOutVertically(tween(Motion.NORMAL, easing = Motion.ease)) { with(density) { ToastExit.roundToPx() } }
    ) {
        BackdropSurface(
            backdrop = backdrop,
            tint = colors.surface3.copy(alpha = Glass.TOAST),
            shape = ShapePill,
            blurRadius = Glass.toastBlur,
            highlight = colors.glassEdge,
            modifier = Modifier.dropShadow(ShapePill) {
                radius = Glass.shadowBlur.toPx()
                offset = Offset(0f, Glass.shadowOffset.toPx())
                color = colors.shadow
            }
        ) {
            Text(
                text = message,
                style = Design.type.muted,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .border(Space.hairline, colors.glassRim, ShapePill)
                    .padding(horizontal = Space.l, vertical = Space.s)
            )
        }
    }
}

private fun Modifier.fadeBottom(length: Dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val mask = Brush.verticalGradient(
                0f to Color.Black,
                (1f - length.toPx() / size.height).coerceIn(0f, 1f) to Color.Black,
                1f to Color.Transparent
            )
            onDrawWithContent {
                drawContent()
                drawRect(mask, blendMode = BlendMode.DstIn)
            }
        }

private fun Modifier.focusRing(
    focused: Boolean,
    color: Color,
    cornerRadius: Dp,
    inset: Boolean = false
): Modifier =
    drawWithContent {
        drawContent()
        if (focused) {
            val width = FocusRingWidth.toPx()
            val offset = (FocusRingOffset.toPx() + width / 2) * if (inset) -1f else 1f
            val radius = (min(cornerRadius.toPx(), size.minDimension / 2) + offset).coerceAtLeast(0f)
            drawRoundRect(
                color = color,
                topLeft = Offset(-offset, -offset),
                size = Size(size.width + offset * 2, size.height + offset * 2),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width)
            )
        }
    }

private class TitleReveal(val requester: BringIntoViewRequester, val scope: CoroutineScope) {
    var reach = 0
    var height = 0
}

@Composable
private fun rememberTitleReveal(listState: LazyListState, key: Any?): TitleReveal {
    val scope = rememberCoroutineScope()
    val reveal = remember(scope) { TitleReveal(BringIntoViewRequester(), scope) }
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard

    LaunchedEffect(listState, key, keyboard) {
        if (!keyboard) return@LaunchedEffect
        snapshotFlow {
            val items = listState.layoutInfo.visibleItemsInfo
            val title = items.firstOrNull { it.key == TITLE_KEY }
            val item = items.firstOrNull { it.key == key }
            if (title != null && item != null) item.offset - title.offset else null
        }.collect { distance -> if (distance != null) reveal.reach = distance }
    }
    return reveal
}

private fun Modifier.revealTitle(reveal: TitleReveal): Modifier =
    bringIntoViewRequester(reveal.requester)
        .onSizeChanged { reveal.height = it.height }
        .onFocusEvent { state ->
            if (state.hasFocus) {
                reveal.scope.launch {
                    reveal.requester.bringIntoView(Rect(0f, -reveal.reach.toFloat(), 1f, reveal.height.toFloat()))
                }
            }
        }

private class CodeFocus {
    var active by mutableStateOf(false)
}

private class FocusScroll(private val top: Float, private val bottom: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = containerSize - bottom
        if (offset >= top && offset + size <= end) return 0f
        return offset - (containerSize * FOCUS_PIVOT).coerceIn(top, maxOf(top, end - size))
    }
}

private fun displayVersion(name: String): String =
    if (name.length > 1 && name[0].lowercaseChar() == 'v' && name[1].isDigit()) name.substring(1) else name

private fun openUrlInBrowser(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
