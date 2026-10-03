package com.android.appupdater.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.android.appupdater.data.repository.ScanStatus
import com.android.appupdater.ui.theme.Design
import com.android.appupdater.ui.theme.DisabledOpacity
import com.android.appupdater.ui.theme.Motion
import com.android.appupdater.ui.theme.PressedOpacity
import com.android.appupdater.ui.theme.PressedScale
import com.android.appupdater.ui.theme.Radius
import com.android.appupdater.ui.theme.ShapeL
import com.android.appupdater.ui.theme.ShapeM
import com.android.appupdater.ui.theme.ShapePill
import com.android.appupdater.ui.theme.ShapeS
import com.android.appupdater.ui.theme.ShapeSheet
import com.android.appupdater.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val BarPadding = 12.dp
private val BarContentHeight = 36.dp
private val TabRowHeight = 50.dp
private val TabBarPadding = 6.dp
private val AppIconSize = 44.dp
private val DotSize = 7.dp
private val SwitchWidth = 58.dp
private val SwitchHeight = 34.dp
private val SwitchKnob = 28.dp
private val SwitchInset = 3.dp
private val TabIconSize = 21.dp
private val SheetMaxWidth = 560.dp
private val SheetPadding = 18.dp
private val SheetBlur = 14.dp
private val PrimaryShadowBlur = 26.dp
private val PrimaryShadowOffset = 10.dp
private const val SheetSaturation = 1.4f
private const val SheetSurfaceAlpha = 0.9f
private const val FocusedBorderAlpha = 0.6f
private const val PrimaryGradientAngle = 140.0
private const val MAX_VERSION_CODE_DIGITS = 19
private const val TABULAR_FIGURES = "tnum"
private val FocusRingWidth = 2.dp
private val FocusRingOffset = 2.dp

private enum class AppTab(val labelRes: Int, val iconRes: Int) {
    Updates(R.string.updates, R.drawable.ic_updates),
    Settings(R.string.settings, R.drawable.ic_settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdaterScreen(
    viewModel: AppUpdaterViewModel,
    modifier: Modifier = Modifier
) {
    val colors = Design.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Updates) }
    val updatesListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val updatesFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val insets = WindowInsets.systemBars.asPaddingValues()
    val backdrop = rememberGraphicsLayer()
    val bundlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::installBundle)
    }
    var manualPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val manualTarget = uiState.updates.firstOrNull { it.packageName == manualPackage }
    BackHandler(enabled = manualTarget == null && selectedTab != AppTab.Updates) { selectedTab = AppTab.Updates }
    BackHandler(enabled = manualTarget != null) { manualPackage = null }
    val sheetProgress = animateFloatAsState(
        targetValue = if (manualTarget != null) 1f else 0f,
        animationSpec = if (manualTarget != null) {
            tween(Motion.SHEET, easing = Motion.easeSheet)
        } else {
            tween(Motion.NORMAL, easing = Motion.ease)
        },
        label = "sheet"
    )
    val sheetShown by remember { derivedStateOf { sheetProgress.value > 0f } }
    val density = LocalDensity.current
    val context = LocalContext.current

    val topBarHeight = insets.calculateTopPadding() + BarPadding * 2 + BarContentHeight
    val tabBarHeight = TabBarPadding + TabRowHeight +
        max(insets.calculateBottomPadding(), TabBarPadding)

    val installedFormat = stringResource(R.string.update_installed)
    val failedFormat = stringResource(R.string.update_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                message = when (event) {
                    is InstallEvent.Finished -> installedFormat.format(event.appName)
                    is InstallEvent.Failed -> failedFormat.format(event.message)
                },
                duration = when (event) {
                    is InstallEvent.Finished -> SnackbarDuration.Short
                    is InstallEvent.Failed -> SnackbarDuration.Long
                }
            )
        }
    }

    LaunchedEffect(uiState.scanStatus) {
        val status = uiState.scanStatus
        if (status is ScanStatus.Error) snackbarHostState.showSnackbar(status.message)
    }

    val updates = remember(uiState.installedApps, uiState.updates) {
        val installedByPackage = uiState.installedApps.associateBy(InstalledApp::packageName)
        uiState.updates.mapNotNull { update ->
            installedByPackage[update.packageName]?.let { it to update }
        }
    }

    val activeListState = if (selectedTab == AppTab.Updates) updatesListState else settingsListState
    val scanning = uiState.scanStatus == ScanStatus.Scanning

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = sheetProgress.value.takeIf { it > 0f }?.let { progress ->
                        backdropEffect(
                            with(density) { SheetBlur.toPx() } * progress,
                            1f + (SheetSaturation - 1f) * progress
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
                                    slideInVertically(tween(Motion.NORMAL, easing = Motion.ease)) { it / 24 }
                                ) togetherWith fadeOut(tween(Motion.FAST, easing = Motion.ease))
                        },
                        label = "view"
                    ) { tab ->
                        when (tab) {
                            AppTab.Updates -> UpdatesView(
                                listState = updatesListState,
                                focus = updatesFocus,
                                topInset = topBarHeight,
                                bottomInset = tabBarHeight,
                                scanning = scanning,
                                updates = updates,
                                installs = uiState.installs,
                                playInstalls = uiState.playInstalls,
                                onUpdate = { update ->
                                    val url = update.apkMirrorUrl
                                    if (url == null) {
                                        viewModel.installFromPlay(update, update.newVersionCode, manual = false)
                                    } else {
                                        openUrlInBrowser(context, url)
                                    }
                                },
                                onManual = { manualPackage = it.packageName }
                            )
                            AppTab.Settings -> SettingsView(
                                listState = settingsListState,
                                focus = settingsFocus,
                                topInset = topBarHeight,
                                bottomInset = tabBarHeight,
                                includeDisabledApps = uiState.includeDisabledApps,
                                onIncludeDisabledAppsChange = viewModel::setIncludeDisabledApps,
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
                topInset = insets.calculateTopPadding(),
                scanning = scanning,
                failed = uiState.scanStatus is ScanStatus.Error,
                count = updates.size
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = tabBarHeight + Space.xl)
            ) { data ->
                BackdropSurface(
                    backdrop = backdrop,
                    tint = colors.surface3.copy(alpha = 0.78f),
                    shape = ShapePill,
                    edgeHighlight = colors.glassEdge,
                    modifier = Modifier
                        .padding(horizontal = Space.l)
                        .border(Space.hairline, colors.border, ShapePill)
                ) {
                    Text(
                        text = data.visuals.message,
                        style = Design.type.muted,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Space.l, vertical = 9.dp)
                    )
                }
            }

            TabBar(
                backdrop = backdrop,
                listFocus = if (selectedTab == AppTab.Updates) updatesFocus else settingsFocus,
                observe = { activeListState.firstVisibleItemScrollOffset },
                height = tabBarHeight,
                bottomInset = max(insets.calculateBottomPadding(), TabBarPadding),
                selectedTab = selectedTab,
                modifier = Modifier.align(Alignment.BottomCenter),
                onSelect = { tab ->
                    when {
                        tab != selectedTab -> selectedTab = tab
                        tab != AppTab.Updates -> Unit
                        updatesListState.canScrollBackward ->
                            coroutineScope.launch { updatesListState.animateScrollToItem(0) }
                        else -> viewModel.scanForUpdates()
                    }
                }
            )
        }

        if (sheetShown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(colors.scrim.copy(alpha = colors.scrim.alpha * sheetProgress.value)) }
                    .pointerInput(Unit) { detectTapGestures { manualPackage = null } }
            )
        }

        ManualSheet(
            target = manualTarget,
            modifier = Modifier.align(Alignment.BottomCenter),
            onSubmit = { update, versionCode ->
                manualPackage = null
                viewModel.installFromPlay(update, versionCode, manual = true)
            }
        )
    }
}

@Composable
private fun UpdatesView(
    listState: LazyListState,
    focus: FocusRequester,
    topInset: Dp,
    bottomInset: Dp,
    scanning: Boolean,
    updates: List<Pair<InstalledApp, AppUpdateInfo>>,
    installs: Map<String, InstallState>,
    playInstalls: Map<String, PlayInstall>,
    onUpdate: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = PaddingValues(
            start = Space.l,
            end = Space.l,
            top = topInset + Space.l,
            bottom = bottomInset + Space.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        items(installs.entries.toList(), key = { it.key }) { (_, state) ->
            Card(tight = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s)
                ) {
                    StatusDot(color = Design.colors.accent, pulsing = true)
                    Text(
                        text = state.appName,
                        style = Design.type.h3,
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
                onUpdate = onUpdate,
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
    topInset: Dp,
    bottomInset: Dp,
    includeDisabledApps: Boolean,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    onPickBundle: () -> Unit
) {
    val colors = Design.colors
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusRestorer(),
        contentPadding = PaddingValues(
            start = Space.l,
            end = Space.l,
            top = topInset + Space.l,
            bottom = bottomInset + Space.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Space.m)
    ) {
        item(key = "disabled-apps") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeM)
                        .background(colors.surface2)
                        .border(Space.hairline, colors.border, ShapeM)
                        .padding(horizontal = Space.m, vertical = Space.s),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.disabled_apps),
                        style = Design.type.switchLabel,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    SwitchTrack(
                        checked = includeDisabledApps,
                        onCheckedChange = onIncludeDisabledAppsChange
                    )
                }
                Text(
                    text = stringResource(R.string.disabled_apps_description),
                    style = Design.type.tiny,
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = 13.dp)
                )
            }
        }

        item(key = "install-bundle") {
            Button(
                text = stringResource(R.string.install_bundle),
                onClick = onPickBundle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    tight: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = Design.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeL)
            .background(colors.surface)
            .border(Space.hairline, colors.border, ShapeL)
            .padding(
                horizontal = if (tight) 14.dp else Space.l,
                vertical = if (tight) Space.m else Space.l
            ),
        verticalArrangement = Arrangement.spacedBy(if (tight) Space.s else Space.m)
    ) {
        content()
    }
}

@Composable
private fun UpdateCard(
    app: InstalledApp,
    update: AppUpdateInfo,
    install: PlayInstall?,
    onUpdate: (AppUpdateInfo) -> Unit,
    onManual: (AppUpdateInfo) -> Unit,
    modifier: Modifier
) {
    val colors = Design.colors
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val latestInteraction = remember { MutableInteractionSource() }
    val latestFocused by latestInteraction.collectIsFocusedAsState()
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
            horizontalArrangement = Arrangement.spacedBy(Space.s + 2.dp)
        ) {
            Box(modifier = Modifier.size(AppIconSize).clip(ShapeS)) {
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
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = update.appName,
                    style = Design.type.h3,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.version_current,
                        app.versionName,
                        app.versionCode
                    ),
                    style = Design.type.tiny,
                    color = colors.text
                )
                Text(
                    text = stringResource(
                        R.string.version_latest,
                        update.newVersionName,
                        update.newVersionCode
                    ),
                    style = Design.type.tiny,
                    color = colors.text,
                    modifier = Modifier
                        .focusRing(latestFocused, colors.accent, 0.dp)
                        .combinedClickable(
                            interactionSource = latestInteraction,
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                coroutineScope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText(null, update.newVersionCode.toString()))
                                    )
                                }
                            }
                        )
                )
            }
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(Space.s)
            ) {
                Button(
                    text = stringResource(R.string.update),
                    small = true,
                    enabled = install == null,
                    busy = install?.manual == false,
                    progress = install?.progress,
                    icon = if (update.apkMirrorUrl == null) R.drawable.ic_source_play else R.drawable.ic_source_apkmirror,
                    onClick = { onUpdate(update) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (update.playAvailable) {
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
    primary: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
    progress: Float? = null,
    @DrawableRes icon: Int? = null
) {
    val colors = Design.colors
    val shape = if (small) ShapeS else ShapeM
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = tween(Motion.FAST, easing = Motion.ease),
        label = "press"
    )
    val percent = remember { NumberFormat.getPercentInstance() }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Space.touch)
            .scale(scale)
            .alpha(if (enabled || busy) 1f else DisabledOpacity)
            .focusRing(focused, colors.accent, if (small) Radius.s else Radius.m)
            .then(
                if (primary) {
                    Modifier.dropShadow(shape) {
                        radius = PrimaryShadowBlur.toPx()
                        offset = Offset(0f, PrimaryShadowOffset.toPx())
                        color = colors.accentSoft
                    }
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(
                if (primary) {
                    Modifier.drawBehind { drawRect(primaryGradient(size, colors.accent, colors.accentEnd)) }
                } else {
                    Modifier.background(colors.surface2).border(Space.hairline, colors.border, shape)
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !busy,
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = if (small) 11.dp else 14.dp,
                vertical = if (small) 7.dp else 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val style = if (small) Design.type.buttonSmall else Design.type.button
            val color = if (primary) Color.White else colors.text
            if (icon != null) Icon(painter = painterResource(icon), contentDescription = null, tint = color)
            Text(text = text, style = style, color = color, maxLines = 1)
            if (busy && progress != null) {
                val digits = style.copy(fontFeatureSettings = TABULAR_FIGURES)
                Box(contentAlignment = Alignment.CenterEnd) {
                    Text(text = percent.format(1), style = digits, color = Color.Transparent, maxLines = 1)
                    Text(text = percent.format(progress), style = digits, color = color, maxLines = 1)
                }
            }
        }
    }
}

private fun primaryGradient(size: Size, start: Color, end: Color): Brush {
    val angle = Math.toRadians(PrimaryGradientAngle)
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
private fun ManualSheet(
    target: AppUpdateInfo?,
    modifier: Modifier,
    onSubmit: (AppUpdateInfo, Long) -> Unit
) {
    AnimatedVisibility(
        visible = target != null,
        modifier = modifier,
        enter = slideInVertically(tween(Motion.SHEET, easing = Motion.easeSheet)) { it },
        exit = fadeOut(tween(Motion.NORMAL, easing = Motion.ease))
    ) {
        val update = remember { checkNotNull(target) }
        val colors = Design.colors
        val focusManager = LocalFocusManager.current
        val code = rememberTextFieldState()
        val versionCode = code.text.toString().toLongOrNull()
        val submit = {
            if (versionCode != null) {
                focusManager.clearFocus()
                onSubmit(update, versionCode)
            }
        }
        if (target == null) LaunchedEffect(Unit) { focusManager.clearFocus() }

        Column(
            modifier = Modifier
                .widthIn(max = SheetMaxWidth)
                .fillMaxWidth()
                .clip(ShapeSheet)
                .background(colors.surface.copy(alpha = SheetSurfaceAlpha))
                .border(Space.hairline, colors.border, ShapeSheet)
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
                .padding(horizontal = Space.l, vertical = SheetPadding),
            verticalArrangement = Arrangement.spacedBy(Space.m)
        ) {
            Text(
                text = update.appName,
                style = Design.type.h2,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            VersionCodeField(state = code, onDone = submit)
            Button(
                text = stringResource(R.string.update),
                onClick = submit,
                primary = true,
                enabled = versionCode != null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VersionCodeField(state: TextFieldState, onDone: () -> Unit) {
    val colors = Design.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = if (focused) colors.surface else colors.surface2,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fieldBackground"
    )
    val border by animateColorAsState(
        targetValue = if (focused) colors.accent.copy(alpha = FocusedBorderAlpha) else colors.border,
        animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
        label = "fieldBorder"
    )
    val placeholder = stringResource(R.string.version_code)

    BasicTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
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
                    .defaultMinSize(minHeight = Space.touch)
                    .focusRing(focused, colors.accent, Radius.m)
                    .clip(ShapeM)
                    .background(background)
                    .border(Space.hairline, border, ShapeM)
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
    val colors = Design.colors
    val density = LocalDensity.current
    val stroke = with(density) { Space.hairline.toPx() }
    val dash = remember(density) {
        with(density) { PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())) }
    }
    val radius = with(density) { Radius.l.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.border,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = stroke, pathEffect = dash)
                )
            }
            .padding(horizontal = Space.l, vertical = 34.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Design.type.muted,
            color = colors.text,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TopBar(
    backdrop: GraphicsLayer,
    observe: () -> Unit,
    height: Dp,
    topInset: Dp,
    scanning: Boolean,
    failed: Boolean,
    count: Int
) {
    val colors = Design.colors
    BackdropSurface(
        backdrop = backdrop,
        tint = colors.background.copy(alpha = 0.82f),
        shape = RectangleShape,
        edgeHighlight = colors.glassEdge,
        observe = observe,
        modifier = Modifier.fillMaxWidth().height(height)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset + BarPadding, bottom = BarPadding)
                .padding(horizontal = Space.l)
                .height(BarContentHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = Design.type.brand,
                color = colors.text
            )
            StatusPill(scanning = scanning, failed = failed, count = count)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(Space.hairline)
                .background(colors.border)
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
            .clip(ShapePill)
            .background(colors.surface)
            .border(Space.hairline, colors.border, ShapePill)
            .padding(horizontal = Space.m, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
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
            animation = tween(1100, easing = Motion.ease),
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
    height: Dp,
    bottomInset: Dp,
    selectedTab: AppTab,
    modifier: Modifier,
    onSelect: (AppTab) -> Unit
) {
    val colors = Design.colors
    BackdropSurface(
        backdrop = backdrop,
        tint = colors.background.copy(alpha = 0.88f),
        shape = RectangleShape,
        edgeHighlight = colors.glassEdge,
        observe = observe,
        modifier = modifier.fillMaxWidth().height(height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Space.hairline)
                .background(colors.border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TabBarPadding, bottom = bottomInset)
                .padding(horizontal = Space.s),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val focused by interaction.collectIsFocusedAsState()
                val fade by animateFloatAsState(
                    targetValue = if (pressed) PressedOpacity else 1f,
                    animationSpec = tween(Motion.FAST, easing = Motion.ease),
                    label = "tab"
                )
                val fill by animateColorAsState(
                    targetValue = if (selected) colors.surface2 else Color.Transparent,
                    animationSpec = tween(Motion.NORMAL, easing = Motion.ease),
                    label = "fill"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(TabRowHeight)
                        .alpha(fade)
                        .focusRing(focused, colors.accent, Radius.m)
                        .clip(ShapeM)
                        .background(fill)
                        .focusProperties { up = listFocus }
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab) }
                        )
                        .padding(vertical = TabBarPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(TabIconSize)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(tab.labelRes),
                        style = Design.type.tab,
                        color = colors.text
                    )
                }
            }
        }
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
            .focusRing(focused, colors.accent, SwitchHeight / 2)
            .clip(ShapePill)
            .background(track)
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
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private fun Modifier.focusRing(focused: Boolean, color: Color, cornerRadius: Dp): Modifier =
    drawWithContent {
        drawContent()
        if (focused) {
            val width = FocusRingWidth.toPx()
            val inset = FocusRingOffset.toPx() + width / 2
            val radius = cornerRadius.toPx() + inset
            drawRoundRect(
                color = color,
                topLeft = Offset(-inset, -inset),
                size = Size(size.width + inset * 2, size.height + inset * 2),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width)
            )
        }
    }

private fun openUrlInBrowser(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
