package com.vasilispngs.appupdater.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.vasilispngs.appupdater.R
import com.vasilispngs.appupdater.data.model.AppUpdateInfo
import com.vasilispngs.appupdater.data.model.InstallState
import com.vasilispngs.appupdater.data.model.InstalledApp
import com.vasilispngs.appupdater.data.model.PlayInstall
import com.vasilispngs.appupdater.data.model.SelfUpdate
import com.vasilispngs.appupdater.data.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.text.NumberFormat
import kotlin.math.roundToInt

private val TvOverscanHorizontal = 48.dp
private val TvOverscanVertical = 27.dp
private val TvGutter = 20.dp
private val TvLineSpacing = 4.dp
private val ListItemInset = 16.dp
private val TvListPadding = TvOverscanHorizontal - ListItemInset
private val TvContentPadding = PaddingValues(top = TvGutter, bottom = TvOverscanVertical)
private const val SIDEBAR_FRACTION = 0.4f

@Serializable
private sealed interface TvDestination : NavKey {
    @Serializable
    data object Updates : TvDestination

    @Serializable
    data object Settings : TvDestination
}

private enum class SettingsSection { AppUpdate, Theme, DisabledApps }

private enum class Source { Play, ApkMirror }

@Composable
internal fun TvAppUpdaterScreen(
    uiState: AppUpdaterUiState,
    themeMode: ThemeMode,
    onRefresh: () -> Unit,
    onPickBundle: () -> Unit,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    selfUpdate: SelfUpdate,
    onUpdateApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = rememberSerializable(serializer = serializer()) { NavBackStack<TvDestination>(TvDestination.Updates) }

    Surface(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<TvDestination.Updates> {
                    TvUpdates(
                        uiState = uiState,
                        onRefresh = onRefresh,
                        onPickBundle = onPickBundle,
                        onSettings = { if (backStack.last() != TvDestination.Settings) backStack.add(TvDestination.Settings) },
                        onApkMirror = onApkMirror,
                        onPlay = onPlay
                    )
                }
                entry<TvDestination.Settings> {
                    TvSettings(
                        themeMode = themeMode,
                        onThemeChange = onThemeChange,
                        includeDisabledApps = uiState.includeDisabledApps,
                        onIncludeDisabledAppsChange = onIncludeDisabledAppsChange,
                        selfUpdate = selfUpdate,
                        onUpdateApp = onUpdateApp
                    )
                }
            }
        )
    }
}

@Composable
private fun TvUpdates(
    uiState: AppUpdaterUiState,
    onRefresh: () -> Unit,
    onPickBundle: () -> Unit,
    onSettings: () -> Unit,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit
) {
    val resources = LocalResources.current
    val updates = remember(uiState.installedApps, uiState.updates) {
        val installedByPackage = uiState.installedApps.associateBy(InstalledApp::packageName)
        uiState.updates.mapNotNull { update ->
            installedByPackage[update.packageName]?.let { app ->
                listOfNotNull(
                    if (update.playUpdate != null) Triple(app, update, Source.Play) else null,
                    if (update.apkMirrorUrl != null) Triple(app, update, Source.ApkMirror) else null
                )
            }
        }.flatten()
    }
    val notice = uiState.problems.takeIf { it.isNotEmpty() }?.joinToString("\n") { it.text(resources) }
    val installs = remember(uiState.installs) { uiState.installs.entries.toList() }
    val focus = rememberInitialFocus(uiState.loaded)

    TvScreen(
        title = stringResource(R.string.updates),
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(TvLineSpacing)) {
                TvAction(icon = R.drawable.ic_refresh, label = R.string.refresh, onClick = onRefresh)
                TvAction(icon = R.drawable.ic_apk_install, label = R.string.install_bundle, onClick = onPickBundle)
                TvAction(icon = R.drawable.ic_settings, label = R.string.settings, onClick = onSettings)
            }
        }
    ) {
        when {
            !uiState.loaded -> Message(title = stringResource(R.string.checking_for_updates))
            updates.isEmpty() && installs.isEmpty() && notice != null -> Message(
                title = stringResource(R.string.check_failed),
                message = notice,
                action = {
                    Button(onClick = onRefresh, modifier = Modifier.focusRequester(focus)) {
                        Text(text = stringResource(R.string.try_again))
                    }
                }
            )
            updates.isEmpty() && installs.isEmpty() -> Message(
                title = stringResource(R.string.all_up_to_date),
                message = stringResource(R.string.all_up_to_date_note)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focus)
                    .focusRestorer(),
                contentPadding = TvContentPadding,
                verticalArrangement = Arrangement.spacedBy(TvLineSpacing)
            ) {
                if (notice != null) {
                    item(key = NOTICE_KEY, contentType = NOTICE_KEY) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = TvOverscanHorizontal)
                        )
                    }
                }
                items(installs, key = { it.key }, contentType = { INSTALL_TYPE }) { (_, install) ->
                    TvInstall(install = install)
                }
                items(updates, key = { (app, _, source) -> "${app.packageName}:$source" }, contentType = { UPDATE_TYPE }) { (app, update, source) ->
                    TvUpdate(
                        app = app,
                        update = update,
                        source = source,
                        install = uiState.playInstalls[app.packageName],
                        onApkMirror = onApkMirror,
                        onPlay = onPlay
                    )
                }
            }
        }
    }
}

@Composable
private fun TvInstall(install: InstallState) {
    ListItem(
        selected = false,
        onClick = {},
        headlineContent = { Text(text = install.appName) },
        supportingContent = { Text(text = stringResource(R.string.self_update_installing)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvListPadding)
    )
}

@Composable
private fun TvUpdate(
    app: InstalledApp,
    update: AppUpdateInfo,
    source: Source,
    install: PlayInstall?,
    onApkMirror: (String) -> Unit,
    onPlay: (AppUpdateInfo) -> Unit
) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { ListItemDefaults.IconSize.roundToPx() }
    val icon by produceState(AppIconCache.peek(app.packageName), app.packageName) {
        if (value == null) value = withContext(Dispatchers.IO) { AppIconCache.load(context, app.packageName, iconSizePx) }
    }
    val versionFormat = stringResource(R.string.version_number)
    val percent = remember { NumberFormat.getPercentInstance() }
    val busy = install != null
    ListItem(
        selected = false,
        onClick = {
            when (source) {
                Source.Play -> if (!busy) onPlay(update)
                Source.ApkMirror -> update.apkMirrorUrl?.let(onApkMirror)
            }
        },
        enabled = source == Source.Play || !busy,
        headlineContent = { Text(text = update.appName) },
        supportingContent = {
            Column {
                Text(
                    text = if (app.versionName.isEmpty()) {
                        stringResource(R.string.version_current_code, app.versionCode)
                    } else {
                        stringResource(R.string.version_current, displayVersion(app.versionName, versionFormat), app.versionCode)
                    }
                )
                Text(text = stringResource(R.string.version_latest, displayVersion(update.newVersionName, versionFormat), update.newVersionCode))
            }
        },
        leadingContent = {
            icon?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(ListItemDefaults.IconSize),
                    contentScale = ContentScale.Fit
                )
            }
        },
        trailingContent = {
            val progress = install?.progress
            if (source == Source.Play && busy) {
                Text(
                    text = progress?.let(percent::format) ?: stringResource(R.string.self_update_installing),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_FIGURES)
                )
            } else {
                Icon(
                    painter = painterResource(if (source == Source.Play) R.drawable.ic_source_play else R.drawable.ic_source_apkmirror),
                    contentDescription = stringResource(if (source == Source.Play) R.string.source_play else R.string.source_apkmirror)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvListPadding)
    )
}

@Composable
private fun TvSettings(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    includeDisabledApps: Boolean,
    onIncludeDisabledAppsChange: (Boolean) -> Unit,
    selfUpdate: SelfUpdate,
    onUpdateApp: () -> Unit
) {
    val sections = remember(selfUpdate == SelfUpdate.Idle) {
        SettingsSection.entries.filter { it != SettingsSection.AppUpdate || selfUpdate != SelfUpdate.Idle }
    }
    var chosen by rememberSaveable { mutableStateOf(SettingsSection.Theme) }
    val section = chosen.takeIf { it in sections } ?: sections.first()
    val sidebar = rememberInitialFocus()
    var optionsFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = optionsFocused,
        onBackCompleted = { sidebar.requestFocus() }
    )

    TvScreen(title = stringResource(R.string.settings)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TvListPadding),
            horizontalArrangement = Arrangement.spacedBy(TvGutter)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(SIDEBAR_FRACTION)
                    .fillMaxHeight()
                    .focusRequester(sidebar)
                    .focusRestorer(),
                contentPadding = TvContentPadding,
                verticalArrangement = Arrangement.spacedBy(TvLineSpacing)
            ) {
                items(sections, key = { it.name }) { item ->
                    ListItem(
                        selected = item == section,
                        onClick = {
                            when (item) {
                                SettingsSection.AppUpdate -> if (selfUpdate == SelfUpdate.Available || selfUpdate is SelfUpdate.Failed) onUpdateApp()
                                SettingsSection.Theme -> focusManager.moveFocus(FocusDirection.Right)
                                SettingsSection.DisabledApps -> onIncludeDisabledAppsChange(!includeDisabledApps)
                            }
                        },
                        headlineContent = { Text(text = stringResource(item.titleRes)) },
                        supportingContent = {
                            Text(
                                text = when (item) {
                                    SettingsSection.AppUpdate -> selfUpdate.text()
                                    SettingsSection.Theme -> stringResource(themeMode.labelRes)
                                    SettingsSection.DisabledApps -> stringResource(R.string.disabled_apps_description)
                                }
                            )
                        },
                        trailingContent = if (item == SettingsSection.DisabledApps) {
                            { Switch(checked = includeDisabledApps, onCheckedChange = null) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (item == SettingsSection.DisabledApps) {
                                    Modifier.semantics {
                                        role = Role.Switch
                                        toggleableState = ToggleableState(includeDisabledApps)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .onFocusChanged { if (it.isFocused) chosen = item }
                    )
                }
            }
            if (section == SettingsSection.Theme) {
                OptionList(
                    options = ThemeMode.entries,
                    current = themeMode,
                    onSelect = onThemeChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onFocusChanged { optionsFocused = it.hasFocus }
                ) { stringResource(it.labelRes) }
            }
        }
    }
}

@Composable
private fun TvAction(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painter = painterResource(icon), contentDescription = stringResource(label))
    }
}

@Composable
private fun <T> OptionList(
    options: List<T>,
    current: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String
) {
    val currentFocus = remember { FocusRequester() }
    Column(
        modifier = modifier
            .focusRestorer(currentFocus)
            .verticalScroll(rememberScrollState())
            .padding(TvContentPadding),
        verticalArrangement = Arrangement.spacedBy(TvLineSpacing)
    ) {
        options.forEach { option ->
            val chosen = option == current
            ListItem(
                selected = chosen,
                onClick = { onSelect(option) },
                headlineContent = { Text(text = label(option)) },
                trailingContent = if (chosen) {
                    { Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (chosen) Modifier.focusRequester(currentFocus) else Modifier)
            )
        }
    }
}

@Composable
private fun TvScreen(title: String, action: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = TvOverscanVertical)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvOverscanHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun Message(title: String, message: String? = null, action: (@Composable () -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TvOverscanHorizontal)
            .padding(TvContentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TvGutter)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            action?.invoke()
        }
    }
}

@Composable
private fun rememberInitialFocus(ready: Boolean = true): FocusRequester {
    val focus = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    LaunchedEffect(ready) {
        if (ready && inputModeManager.inputMode == InputMode.Keyboard) focus.requestFocus()
    }
    return focus
}

@get:StringRes
private val SettingsSection.titleRes: Int
    get() = when (this) {
        SettingsSection.AppUpdate -> R.string.self_update_available
        SettingsSection.Theme -> R.string.theme
        SettingsSection.DisabledApps -> R.string.disabled_apps
    }

@Composable
private fun SelfUpdate.text(): String = when (this) {
    is SelfUpdate.Downloading -> stringResource(R.string.self_update_downloading, (progress * 100).roundToInt())
    SelfUpdate.Installing -> stringResource(R.string.self_update_installing)
    is SelfUpdate.Failed -> stringResource(R.string.self_update_failed).withCode(code, LocalResources.current)
    else -> stringResource(R.string.self_update_install)
}
