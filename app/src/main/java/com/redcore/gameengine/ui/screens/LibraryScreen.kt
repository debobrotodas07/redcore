package com.redcore.gameengine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.redcore.gameengine.data.model.GameEntity
import com.redcore.gameengine.navigation.Routes
import com.redcore.gameengine.ui.components.AppIcon
import com.redcore.gameengine.ui.components.ChipRow
import com.redcore.gameengine.ui.components.EmptyState
import com.redcore.gameengine.ui.components.LoadingState
import com.redcore.gameengine.ui.components.ScreenColumn
import com.redcore.gameengine.ui.components.ScreenTopBar
import com.redcore.gameengine.ui.components.SectionHeader
import com.redcore.gameengine.ui.theme.TextMuted
import com.redcore.gameengine.ui.theme.TextPrimary
import com.redcore.gameengine.ui.theme.TextSecondary
import com.redcore.gameengine.ui.vm.LibrarySort
import com.redcore.gameengine.ui.vm.LibraryViewModel
import com.redcore.gameengine.util.Format

@Composable
fun LibraryScreen(nav: NavHostController) {
    val vm: LibraryViewModel = viewModel()
    val state by vm.state.collectAsState()
    val addable by vm.addable.collectAsState()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ScreenTopBar(
            title = "Game library",
            subtitle = "${state.games.size} games • ${state.games.count { it.favorite }} favourites"
        ) {
            IconButton(onClick = { vm.refresh() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Rescan", tint = TextSecondary)
            }
            IconButton(onClick = {
                vm.loadAddable()
                showAdd = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add game", tint = accent)
            }
        }

        SearchField(
            value = state.query,
            onValueChange = vm::setQuery,
            placeholder = "Search games or packages"
        )

        Spacer(Modifier.height(10.dp))
        ChipRow(
            options = LibrarySort.entries.map { it.label },
            selectedIndex = LibrarySort.entries.indexOf(state.sort),
            onSelect = { vm.setSort(LibrarySort.entries[it]) },
            accent = accent
        )

        Spacer(Modifier.height(12.dp))

        // Lazy grid: only visible cards are composed, so a 200-game library scrolls
        // at the same cost as a 10-game one. The header above stays pinned.
        when {
            state.loading && state.games.isEmpty() -> LoadingState("Scanning installed games…")
            state.games.isEmpty() -> EmptyState(
                icon = Icons.Rounded.SportsEsports,
                title = if (state.query.isBlank()) "No games in the library" else "Nothing matches \"${state.query}\"",
                message = if (state.query.isBlank())
                    "REDCORE detects games Android marks as GAME. You can add any launchable app manually."
                else "Try a shorter search or clear the field.",
                actionLabel = if (state.query.isBlank()) "Add a game" else "Clear search",
                onAction = {
                    if (state.query.isBlank()) {
                        vm.loadAddable(); showAdd = true
                    } else vm.setQuery("")
                }
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.games, key = { it.packageName }) { game ->
                    GameCard(
                        game = game,
                        modifier = Modifier.fillMaxWidth(),
                        onOpen = { nav.navigate(Routes.game(game.packageName)) },
                        onLaunch = { vm.launch(context, game) },
                        onFavorite = { vm.toggleFavorite(game) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddGameDialog(
            apps = addable,
            onDismiss = { showAdd = false },
            onAdd = { pkg -> vm.add(pkg) },
            onOpenGames = { nav.navigate(Routes.LIBRARY) }
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val accent = MaterialTheme.colorScheme.primary
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0x0FFFFFFF),
            unfocusedContainerColor = Color(0x08FFFFFF),
            focusedIndicatorColor = accent.copy(alpha = 0.6f),
            unfocusedIndicatorColor = Color(0x14FFFFFF),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = accent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GameCard(
    game: GameEntity,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onLaunch: () -> Unit,
    onFavorite: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0x12FFFFFF), Color(0x07FFFFFF))))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onOpen() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AppIcon(
                packageName = game.packageName,
                customUri = game.customIconUri,
                size = 46,
                shape = RoundedCornerShape(13.dp)
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite, modifier = Modifier.size(30.dp)) {
                Icon(
                    if (game.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favourite",
                    tint = if (game.favorite) Color(0xFFFFC24B) else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            game.label,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            Format.ago(game.lastPlayedAt),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (game.totalPlaySeconds > 0) Format.duration(game.totalPlaySeconds) + " played" else "No sessions yet",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .clickable { onLaunch() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("PLAY", style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun AddGameDialog(
    apps: List<com.redcore.gameengine.ui.vm.AddableApp>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onOpenGames: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var filter by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121218),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column {
                Text("Add a game", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text(
                    "Every launchable app is listed. Android's GAME flag is shown so you can tell them apart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        text = {
            Column {
                SearchField(value = filter, onValueChange = { filter = it }, placeholder = "Filter apps")
                Spacer(Modifier.height(10.dp))
                val filtered = apps.filter {
                    filter.isBlank() || it.label.contains(filter, ignoreCase = true) ||
                        it.packageName.contains(filter, ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    Text(
                        "No launchable apps found. If the list is empty, Android's package visibility rules blocked the query — reinstall the build and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !app.alreadyAdded) {
                                        onAdd(app.packageName)
                                    }
                                    .padding(vertical = 7.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    packageName = app.packageName,
                                    customUri = null,
                                    size = 36,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                if (app.alreadyAdded) {
                                    Text("ADDED", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                } else if (app.isGame) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(accent.copy(alpha = 0.16f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("GAME", style = MaterialTheme.typography.labelSmall, color = accent)
                                    }
                                } else {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "DONE",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier
                    .clickable { onOpenGames(); onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    )
}
