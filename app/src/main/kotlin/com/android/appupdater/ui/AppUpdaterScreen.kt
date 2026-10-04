package com.android.appupdater.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.android.appupdater.ui.theme.ShapeMark
import com.android.appupdater.ui.theme.ShapePill
import com.android.appupdater.ui.theme.ShapeSheet
import com.android.appupdater.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val BarPadding = 10.dp
private val TabPadding = 6.dp
private val TabGap = 3.dp
private val TabIconSize = 21.dp
private val TabBarGap = 2.dp
private val TabBarBottom = 12.dp
private val TabBarSafeOverlap = 8.dp
private val ContentBottomGap = 28.dp
private val FloatingSheetBreakpoint = 600.dp
private val AppIconSize = 44.dp
private val BrandMarkSize = 26.dp
private val BrandMarkLayer = 39.dp
private val BrandGap = 10.dp
private val DotSize = 7.dp
private val PillGap = 7.dp
private val IconSize = 18.dp
private val SwitchWidth = 58.dp
private val SwitchHeight = 34.dp
private val SwitchKnob = 28.dp
private val SwitchInset = 3.dp
private val SheetMaxWidth = 560.dp
private val SheetFloatingBottom = 24.dp
private val ToastMaxWidth = 544.dp
private val ToastGap = 20.dp
private val ToastEnter = 10.dp
private val ToastExit = 6.dp
private val ViewEnter = 6.dp
private val EmptyPadding = 32.dp
private val GroupInset = 17.dp
private val PrimaryShadowBlur = 26.dp
private val PrimaryShadowOffset = 10.dp
private val MarkShadowBlur = 18.dp
private val MarkShadowOffset = 6.dp
private val KnobShadowBlur = 6.dp
private val KnobShadowOffset = 2.dp
private val KnobShadow = Color(0x4D000000)
private val FocusRingWidth = 2.dp
private val FocusRingOffset = 2.dp
private const val TOAST_MILLIS = 2200L
private const val PULSE_MILLIS = 550
private const val FOCUSED_BORDER_ALPHA = 0.6f
private const val SELECTED_RING_ALPHA = 0.5f
private const val PRIMARY_GRADIENT_ANGLE = 140.0
private const val MAX_VERSION_CODE_DIGITS = 19
private const val TABULAR_FIGURES = "tnum"

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
        ThemeMode.Black -> R.string.theme_black
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
    val density = LocalDensity.current
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val viewEnter = with(density) { ViewEnter.roundToPx() }
    val pillLine = with(density) { Design.type.pill.lineHeight.toDp() }
    val tabLine = with(density) { Design.type.tab.lineHeight.toDp() }
    val installedFormat = stringResource(R.string.update_installed)
    val installedFromPlayFormat = stringResource(R.string.update_installed_play)
    val unavailableFormat = stringResource(R.string.update_unavailable)
    val failedFormat = stringResource(R.string.update_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            toasts.show(
                when (event) {
                    is InstallEvent.Finished -> installedFormat.format(event.appName)
                    is InstallEvent.FinishedFromPlay -> installedFromPlayFormat.format(event.appName, event.versionName)
                    is InstallEvent.Unavailable -> unavailableFormat.format(event.appName)
                    is InstallEvent.Failed -> failedFormat.format(event.message)
                }
            )
        }
    }

    LaunchedEffect(uiState.scanStatus) {
        val status = uiState.scanStatus
        if (status is ScanStatus.Error) toasts.show(status.message)
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
    val scrolled by remember(activeListState) { derivedStateOf { activeListState.canScrollBackward } }
    val scanning = uiState.scanStatus == ScanStatus.Scanning
    val selectTab: (AppTab) -> Unit = { tab ->
        when {
            tab != selectedTab -> {
                selectedTab = tab
                coroutineScope.launch {
                    (if (tab == AppTab.Updates) updatesListState else settingsListState).scrollToItem(0)
                }
            }
            tab == AppTab.Updates && updatesListState.canScrollBackward ->
                coroutineScope.launch { updatesListState.animateScrollToItem(0) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val floatingSheet = maxWidth >= FloatingSheetBreakpoint
        val topInset = insets.calculateTopPadding()
        val bottomInset = insets.calculateBottomPadding()
        val startPadding = max(Space.l, insets.calculateStartPadding(layoutDirection))
        val endPadding = max(Space.l, insets.calculateEndPadding(layoutDirection))
        val topBarHeight = topInset + BarPadding * 2 + max(Space.controlSmall, pillLine)
        val tabBarBottom = max(TabBarBottom, bottomInset - TabBarSafeOverlap)
        val tabBarHeight = (TabPadding + Space.hairline) * 2 + TabPadding * 2 + TabIconSize + TabGap + tabLine
        val contentPadding = PaddingValues(
            start = startPadding,
            end = endPadding,
            top = topBarHeight + Space.l,
            bottom = tabBarBottom + tabBarHeight + ContentBottomGap
        )
        val toastBottom = tabBarBottom + tabBarHeight + ToastGap

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = sheetProgress.value.takeIf { it > 0f }?.let { progress ->
                        backdropEffect(
                            Glass.sheetBackdropBlur.toPx() * progress,
                            1f + (Glass.SHEET_BACKDROP_SATURATION - 1f) * progress
                        )
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().captureBackdrop(backdrop, colors.background)) {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = viewModel::scanForUpdates,
                    state = pullState,
                    enabled = LocalInputModeManager.current.inputMode == InputMode.Touch,
                    indicator = {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = topBarHeight + Space.l)
                                .alpha(pullState.distanceFraction.coerceIn(0f, 1f))
                                .size(DotSize)
                                .clip(CircleShape)
                                .background(colors.accent)
                        )
                    }
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (
                                fadeIn(tween(Motion.NORMAL, easing = Motion.ease)) +
                                    slideInVertically(tween(Motion.NORMAL, easing = Motion.ease)) { viewEnter }
                                ) togetherWith ExitTransition.None
                        },
                        label = "view"
                    ) { tab ->
                        when (tab) {
                            AppTab.Updates -> UpdatesView(
                                listState = updatesListState,
                                focus = updatesFocus,
                                contentPadding = contentPadding,
                                scanning = scanning,
                                updates = updates,
                                installs = uiState.installs,
                                playInstalls = uiState.playInstalls,
                                onApkMirror = { openUrlInBrowser(context, it) },
                                onPlay = viewModel::updateFromPlay,
                                onManual = { manualPackage = it.packageName }
                            )
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
                }
            }

            TopBar(
                backdrop = backdrop,
                observe = { activeListState.firstVisibleItemScrollOffset },
                height = topBarHeight,
                topInset = topInset,
                startPadding = startPadding,
                endPadding = endPadding,
                scrolled = scrolled,
                scanning = scanning,
                failed = uiState.scanStatus is ScanStatus.Error,
                count = updates.size,
                listFocus = activeFocus,
                onRefresh = viewModel::scanForUpdates
            )

            TabBar(
                backdrop = backdrop,
                listFocus = activeFocus,
                observe = { activeListState.firstVisibleItemScrollOffset },
                selectedTab = selectedTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = startPadding, end = endPadding, bottom = tabBarBottom),
                onSelect = selectTab
            )
        }

        if (sheetShown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(colors.scrim.copy(alpha = colors.scrim.alpha * sheetProgress.value)) }
                    .pointerInput(Unit) { detectTapGestures { closeSheets() } }
            )
        }

        ManualSheet(
            target = manualTarget,
            floating = floatingSheet,
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
            floating = floatingSheet,
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = toastBottom)
        )
    }
}

@Composable
private fun UpdatesView(
    listState: LazyListState,
    focus: FocusRequester,
    contentPadding: PaddingValues,
    scanning: Boolean,
    updates: List<Pair<InstalledApp, AppUpdateInfo>>,
    installs: Map<String, InstallState>,
    playInstalls: Map<String, PlayInstall>,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        items(installs.entries.toList(), key = { it.key }) { (_, state) ->
            Card(style = CardStyle.Tight) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s)
                ) {
                    StatusDot(color = Design.colors.accent, pulsing = true)
                    Text(
                        text = state.appName,
                        style = Design.type.name,
                        color = Design.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (updates.isEmpty() && !scanning) {
            item(key = "empty") { EmptyState(stringResource(R.string.all_up_to_date)) }
        }

        items(items = updates, key = { it.first.packageName }) { pair ->
            UpdateCard(
                app = pair.first,
                update = pair.second,
                install = playInstalls[pair.first.packageName],
                onApkMirror = onApkMirror,
                onPlay = onPlay,
                onManual = onManual,
                modifier = Modifier.animateItem()
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
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.l)
    ) {
        item(key = "title") {
            Text(text = stringResource(R.string.settings), style = Design.type.h1, color = colors.text)
        }

        item(key = "appearance") {
            Group(heading = stringResource(R.string.appearance)) {
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
                                tint = colors.muted,
                                modifier = Modifier.size(IconSize)
                            )
                        }
                    }
                }
            }
        }

        item(key = "disabled-apps") {
            Group(note = stringResource(R.string.disabled_apps_description)) {
                Card(style = CardStyle.Tight) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.disabled_apps),
                            style = Design.type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        SwitchTrack(checked = includeDisabledApps, onCheckedChange = onIncludeDisabledAppsChange)
                    }
                }
            }
        }

        item(key = "install-bundle") {
            Card(style = CardStyle.Flush) {
                ListRow(onClick = onPickBundle) {
                    Text(text = stringResource(R.string.install_bundle), style = Design.type.name, color = colors.accentText)
                }
            }
        }
    }
}

@Composable
private fun Group(
    heading: String? = null,
    note: String? = null,
    content: @Composable () -> Unit
) {
    val colors = Design.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
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
                style = Design.type.tiny,
                color = colors.text,
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
            .border(Space.hairline, colors.border, ShapeCard)
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
                role = Role.Button,
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
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit,
    modifier: Modifier
) {
    val colors = Design.colors
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val latestText = stringResource(R.string.version_latest, update.newVersionName, update.newVersionCode)
    val latest = remember(latestText, update.newVersionCode, colors) {
        val code = update.newVersionCode.toString()
        val start = latestText.lastIndexOf(code)
        buildAnnotatedString {
            append(latestText)
            if (start >= 0) {
                addLink(
                    LinkAnnotation.Clickable(
                        tag = code,
                        styles = TextLinkStyles(
                            focusedStyle = SpanStyle(color = Color.White, background = colors.accent),
                            pressedStyle = SpanStyle(color = Color.White, background = colors.accent)
                        )
                    ) {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, code)))
                        }
                    },
                    start,
                    start + code.length
                )
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
            Box(modifier = Modifier.size(AppIconSize).clip(ShapeMark)) {
                iconBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    text = update.appName,
                    style = Design.type.name,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.version_current, app.versionName, app.versionCode),
                    style = Design.type.tiny,
                    color = colors.text
                )
                Text(text = latest, style = Design.type.tiny, color = colors.text)
            }
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(Space.s)
            ) {
                update.apkMirrorUrl?.let { url ->
                    Button(
                        text = stringResource(R.string.update),
                        small = true,
                        enabled = install == null,
                        icon = R.drawable.ic_source_apkmirror,
                        onClick = { onApkMirror(url) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (update.playAvailable) {
                    Button(
                        text = stringResource(R.string.update),
                        small = true,
                        enabled = install == null,
                        busy = install?.manual == false,
                        progress = install?.progress,
                        icon = R.drawable.ic_source_play,
                        onClick = { onPlay(update) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        text = stringResource(R.string.manual),
                        small = true,
                        enabled = install == null,
                        busy = install?.manual == true,
                        progress = install?.progress,
                        icon = R.drawable.ic_source_play,
                        onClick = { onManual(update) },
                        modifier = Modifier.fillMaxWidth()
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
    val primary = style == ButtonStyle.Primary
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
            ButtonStyle.Regular -> colors.surface2
            ButtonStyle.Selected -> colors.accentSoft
            ButtonStyle.Primary, ButtonStyle.Ghost -> Color.Transparent
        },
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fill"
    )
    val edge by animateColorAsState(
        targetValue = when (style) {
            ButtonStyle.Primary -> Color.Transparent
            ButtonStyle.Selected -> colors.accent.copy(alpha = SELECTED_RING_ALPHA)
            ButtonStyle.Regular, ButtonStyle.Ghost -> colors.border
        },
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "edge"
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
            .then(
                if (primary) {
                    Modifier.dropShadow(ShapePill) {
                        radius = PrimaryShadowBlur.toPx()
                        offset = Offset(0f, PrimaryShadowOffset.toPx())
                        color = colors.accentSoft
                    }
                } else {
                    Modifier
                }
            )
            .clip(ShapePill)
            .then(
                if (primary) {
                    Modifier.drawBehind { drawRect(primaryGradient(size, colors.accent, colors.accentEnd)) }
                } else {
                    Modifier.background(fill)
                }
            )
            .border(Space.hairline, edge, ShapePill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !busy,
                role = Role.Button,
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
            val color = if (primary) Color.White else colors.text
            if (icon != null) Icon(painter = painterResource(icon), contentDescription = null, tint = color)
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = textStyle,
                    color = color,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = 1f - progressShown }
                )
                if (busy && progress != null) {
                    Text(
                        text = percent.format(progress),
                        style = textStyle.copy(fontFeatureSettings = TABULAR_FIGURES),
                        color = color,
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
private fun IconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier
            .scale(scale)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .clip(CircleShape)
            .background(colors.surface2)
            .border(Space.hairline, colors.border, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label }
            .padding(Space.s)
            .paint(
                painter = painterResource(icon),
                sizeToIntrinsics = false,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(colors.text)
            )
    )
}

private fun primaryGradient(size: Size, start: Color, end: Color): Brush {
    val angle = Math.toRadians(PRIMARY_GRADIENT_ANGLE)
    val direction = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
    val length = abs(size.width * direction.x) + abs(size.height * direction.y)
    val center = size.center
    return Brush.linearGradient(
        colors = listOf(start, end),
        start = center - direction * (length / 2),
        end = center + direction * (length / 2)
    )
}

@Composable
private fun Sheet(
    visible: Boolean,
    floating: Boolean,
    returnFocus: FocusRequester,
    modifier: Modifier,
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
        val focusManager = LocalFocusManager.current
        val inputModeManager = LocalInputModeManager.current
        val initialFocus = remember { FocusRequester() }
        val open by rememberUpdatedState(visible)
        val shape = if (floating) ShapeCard else ShapeSheet
        val margin = WindowInsets(bottom = SheetFloatingBottom)
        val floatingInsets = WindowInsets.ime.add(margin)
            .union(WindowInsets.navigationBars)
            .union(margin)
            .only(WindowInsetsSides.Bottom)
        val dockedInsets = WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom)

        LaunchedEffect(Unit) {
            if (inputModeManager.inputMode == InputMode.Keyboard) initialFocus.requestFocus()
        }
        if (!visible) LaunchedEffect(Unit) {
            if (inputModeManager.inputMode == InputMode.Keyboard) returnFocus.requestFocus() else focusManager.clearFocus()
        }

        Column(
            modifier = Modifier
                .then(if (floating) Modifier.windowInsetsPadding(floatingInsets).padding(horizontal = Space.l) else Modifier)
                .widthIn(max = SheetMaxWidth)
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface.copy(alpha = Glass.SHEET))
                .innerTopHighlight(shape, colors.glassEdge)
                .border(Space.hairline, colors.glassRim, shape)
                .pointerInput(Unit) { detectTapGestures { } }
                .focusProperties { onExit = { if (open) cancelFocusChange() } }
                .focusGroup()
                .then(if (floating) Modifier else Modifier.windowInsetsPadding(dockedInsets))
                .padding(horizontal = Space.l, vertical = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m)
        ) {
            content(initialFocus)
        }
    }
}

@Composable
private fun ManualSheet(
    target: AppUpdateInfo?,
    floating: Boolean,
    returnFocus: FocusRequester,
    modifier: Modifier,
    onCancel: () -> Unit,
    onSubmit: (AppUpdateInfo, Long) -> Unit
) {
    Sheet(visible = target != null, floating = floating, returnFocus = returnFocus, modifier = modifier) { initialFocus ->
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
    floating: Boolean,
    returnFocus: FocusRequester,
    modifier: Modifier,
    onCancel: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    Sheet(visible = visible, floating = floating, returnFocus = returnFocus, modifier = modifier) { initialFocus ->
        Text(text = stringResource(R.string.theme), style = Design.type.h2, color = Design.colors.text)
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = if (focused) colors.surface else colors.surface2,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fieldBackground"
    )
    val border by animateColorAsState(
        targetValue = if (focused) colors.accent.copy(alpha = FOCUSED_BORDER_ALPHA) else colors.border,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fieldBorder"
    )
    val placeholder = stringResource(R.string.version_code)

    BasicTextField(
        state = state,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        inputTransformation = DigitsOnly.maxLengthTrim(MAX_VERSION_CODE_DIGITS),
        textStyle = Design.type.body.copy(color = colors.text),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        onKeyboardAction = { onDone() },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interaction,
        cursorBrush = SolidColor(colors.text),
        decorator = { field ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Space.control)
                    .clip(ShapePill)
                    .background(background)
                    .border(Space.hairline, border, ShapePill)
                    .padding(horizontal = Space.m, vertical = 10.dp),
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
private fun EmptyState(text: String) {
    Card(style = CardStyle.Flush) {
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
    height: Dp,
    topInset: Dp,
    startPadding: Dp,
    endPadding: Dp,
    scrolled: Boolean,
    scanning: Boolean,
    failed: Boolean,
    count: Int,
    listFocus: FocusRequester,
    onRefresh: () -> Unit
) {
    val colors = Design.colors
    val divider by animateColorAsState(
        targetValue = if (scrolled) colors.border else Color.Transparent,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "divider"
    )
    BackdropSurface(
        backdrop = backdrop,
        tint = colors.background.copy(alpha = Glass.BAR),
        shape = RectangleShape,
        blurRadius = Glass.blur,
        observe = observe,
        modifier = Modifier.fillMaxWidth().height(height)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset + BarPadding, bottom = BarPadding, start = startPadding, end = endPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BrandGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark()
                Text(text = stringResource(R.string.app_name), style = Design.type.brand, color = colors.text)
            }
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(scanning = scanning, failed = failed, count = count)
                IconButton(
                    icon = R.drawable.ic_refresh,
                    label = stringResource(R.string.refresh),
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .focusProperties { down = listFocus }
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(Space.hairline)
                .background(divider)
        )
    }
}

@Composable
private fun BrandMark() {
    val colors = Design.colors
    Box(
        modifier = Modifier
            .size(BrandMarkSize)
            .dropShadow(ShapeMark) {
                radius = MarkShadowBlur.toPx()
                offset = Offset(0f, MarkShadowOffset.toPx())
                color = colors.accentSoft
            }
            .clip(ShapeMark),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.requiredSize(BrandMarkLayer)
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(BrandMarkLayer)
        )
    }
}

@Composable
private fun StatusPill(scanning: Boolean, failed: Boolean, count: Int) {
    val colors = Design.colors
    val dotColor = when {
        failed -> colors.danger
        scanning -> colors.accent
        else -> colors.success
    }
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = Space.controlSmall)
            .clip(ShapePill)
            .background(colors.surface)
            .border(Space.hairline, colors.border, ShapePill)
            .padding(horizontal = Space.m),
        horizontalArrangement = Arrangement.spacedBy(PillGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(color = dotColor, pulsing = scanning)
        Text(
            text = when {
                scanning -> stringResource(R.string.status_checking)
                count == 0 -> stringResource(R.string.status_up_to_date)
                else -> pluralStringResource(R.plurals.status_updates, count, count)
            },
            style = Design.type.pill,
            color = colors.text,
            maxLines = 1
        )
    }
}

@Composable
private fun pulseFactor(): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS, easing = Motion.ease),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    return progress
}

@Composable
private fun StatusDot(color: Color, pulsing: Boolean) {
    val factor = if (pulsing) pulseFactor() else 0f

    Box(
        modifier = Modifier
            .size(DotSize)
            .scale(1f - 0.14f * factor)
            .alpha(1f - 0.55f * factor)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun TabBar(
    backdrop: GraphicsLayer,
    listFocus: FocusRequester,
    observe: () -> Unit,
    selectedTab: AppTab,
    modifier: Modifier,
    onSelect: (AppTab) -> Unit
) {
    val colors = Design.colors
    BackdropSurface(
        backdrop = backdrop,
        tint = colors.surface2.copy(alpha = Glass.TAB_BAR),
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
                .padding(TabPadding + Space.hairline),
            horizontalArrangement = Arrangement.spacedBy(TabBarGap)
        ) {
            AppTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { up = listFocus },
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
    val fill by animateColorAsState(
        targetValue = if (selected) colors.accentSoft else Color.Transparent,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fill"
    )
    val ring by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = SELECTED_RING_ALPHA) else Color.Transparent,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "ring"
    )
    Column(
        modifier = modifier
            .alpha(fade)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .clip(ShapePill)
            .background(fill)
            .border(Space.hairline, ring, ShapePill)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = TabPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TabGap)
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null,
            tint = colors.text,
            modifier = Modifier.size(TabIconSize)
        )
        Text(text = stringResource(tab.labelRes), style = Design.type.tab, color = colors.text)
    }
}

@Composable
private fun SwitchTrack(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = Design.colors
    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.surface3,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "track"
    )
    val travel by animateDpAsState(
        targetValue = if (checked) SwitchWidth - SwitchKnob - SwitchInset * 2 else 0.dp,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "knob"
    )
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(width = SwitchWidth, height = SwitchHeight)
            .focusRing(focused, colors.accent, Dp.Infinity)
            .background(track, ShapePill)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
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
        enter = fadeIn(tween(Motion.NORMAL, easing = Motion.ease)) +
            slideInVertically(tween(Motion.NORMAL, easing = Motion.ease)) { with(density) { ToastEnter.roundToPx() } },
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

private fun openUrlInBrowser(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
