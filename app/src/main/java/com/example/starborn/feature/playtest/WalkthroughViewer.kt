package com.example.starborn.feature.playtest

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private data class GuideJump(val section: Int, val block: Int, val label: String)

private fun guideText(raw: String): AnnotatedString = buildAnnotatedString {
    val text = raw.removePrefix("- ").replace("[ ]", "Unchecked:")
    val markup = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^]]+)\\]\\([^)]+\\)")
    var offset = 0
    markup.findAll(text).forEach { match ->
        append(text.substring(offset, match.range.first))
        when {
            match.groupValues[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
            match.groupValues[2].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(match.groupValues[2]) }
            else -> append(match.groupValues[3])
        }
        offset = match.range.last + 1
    }
    append(text.substring(offset))
}

@Composable
fun WalkthroughViewer(
    worldId: String?, roomId: String?, roomTitle: String?, questTitle: String?, questId: String?,
    isTestSession: Boolean, onReturnToGame: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context, isTestSession) {
        context.getSharedPreferences("walkthrough-${if (isTestSession) "playtest" else "campaign"}", android.content.Context.MODE_PRIVATE)
    }
    var world by rememberSaveable { mutableIntStateOf(if (worldId == "world_2") 2 else 1) }
    var guide by remember { mutableStateOf<WalkthroughGuide?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(world) {
        guide = null; error = null
        runCatching { withContext(Dispatchers.IO) { loadWalkthrough(context, world) } }
            .onSuccess { guide = it }.onFailure { error = "The bundled guide could not be opened. Close and try again." }
    }
    Dialog(onDismissRequest = onReturnToGame, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = FieldMenuDesign.shell, contentColor = FieldMenuDesign.text) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Playtest walkthrough", style = MaterialTheme.typography.titleLarge)
                        Text(roomTitle ?: "Exploration", color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onReturnToGame) { Text("Back to game", color = FieldMenuDesign.cyan) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (number in 1..2) FilterChip(selected = world == number, onClick = { world = number }, label = { Text("World $number") })
                }
                if (worldId !in listOf("world_1", "world_2")) Text("Guides currently cover Worlds 1 and 2.", modifier = Modifier.padding(horizontal = 16.dp), color = FieldMenuDesign.textMuted)
                HorizontalDivider(color = FieldMenuDesign.border)
                val loaded = guide
                if (loaded != null) key(world, loaded.revision) {
                    GuideReader(loaded, prefs, roomId, roomTitle, questTitle, questId, onReturnToGame)
                } else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (error == null) CircularProgressIndicator() else Text(error!!, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.GuideReader(
    guide: WalkthroughGuide, prefs: android.content.SharedPreferences,
    roomId: String?, roomTitle: String?, questTitle: String?, questId: String?, onReturn: () -> Unit
) {
    val context = LocalContext.current
    val prefix = "world-${guide.world}-${guide.revision}"
    val scope = rememberCoroutineScope()
    var section by remember { mutableIntStateOf(prefs.getInt("$prefix-section", 0).coerceIn(0, guide.sections.lastIndex)) }
    var checked by remember { mutableStateOf(prefs.getStringSet("$prefix-checked", emptySet())!!.toSet()) }
    val list = rememberLazyListState(prefs.getInt("$prefix-item", 0).coerceIn(0, guide.sections[section].blocks.lastIndex), prefs.getInt("$prefix-offset", 0).coerceAtLeast(0))
    var picker by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var noteOpen by remember { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var notesOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    val notesKey = "world-${guide.world}-notes"
    var notes by remember { mutableStateOf(prefs.getString(notesKey, "[]") ?: "[]") }
    var message by remember { mutableStateOf<String?>(null) }
    fun jump(targetSection: Int, block: Int = 0) {
        section = targetSection
        prefs.edit().putInt("$prefix-section", targetSection).putInt("$prefix-item", block).putInt("$prefix-offset", 0).apply()
        scope.launch { list.scrollToItem(block) }
        picker = null; query = ""
    }
    LaunchedEffect(list, section) {
        snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }.distinctUntilChanged().collect { (index, offset) ->
            prefs.edit().putInt("$prefix-section", section).putInt("$prefix-item", index).putInt("$prefix-offset", offset).apply()
        }
    }
    BackHandler { onReturn() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { picker = "Contents" }, modifier = Modifier.weight(1f)) { Text("Contents") }
        TextButton(onClick = { picker = "This room" }, modifier = Modifier.weight(1f)) { Text("This room") }
        TextButton(onClick = { picker = "Tracked quest" }, modifier = Modifier.weight(1f)) { Text("Quest") }
        TextButton(onClick = { picker = "Search" }, modifier = Modifier.weight(1f)) { Text("Search") }
    }
    Text(guide.sections[section].title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    Text("${checked.size} of ${guide.steps} steps checked • Position saved on this device", style = MaterialTheme.typography.bodySmall,
        color = FieldMenuDesign.textMuted, modifier = Modifier.padding(horizontal = 16.dp))
    message?.let { Text(it, color = FieldMenuDesign.cyan, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
    LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(guide.sections[section].blocks, key = { _, block -> block.id }) { _, block ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                if (block.step != null) Checkbox(checked = block.id in checked, onCheckedChange = { value ->
                    checked = if (value) checked + block.id else checked - block.id
                    prefs.edit().putStringSet("$prefix-checked", checked).apply()
                }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
                SelectionContainer(Modifier.weight(1f)) {
                    Text(guideText(block.text), style = when (block.heading) {
                        3 -> MaterialTheme.typography.titleLarge
                        4 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.bodyLarge
                    }, color = if (block.heading > 0) FieldMenuDesign.cyan else FieldMenuDesign.text)
                }
            }
        }
    }
    HorizontalDivider(color = FieldMenuDesign.border)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { jump(section - 1) }, enabled = section > 0, modifier = Modifier.weight(1f)) { Text("Previous") }
        TextButton(onClick = { noteOpen = true }, modifier = Modifier.weight(1f)) { Text("Add note") }
        TextButton(onClick = { notesOpen = true }, modifier = Modifier.weight(1f)) { Text("Notes") }
        TextButton(onClick = { jump(section + 1) }, enabled = section < guide.sections.lastIndex, modifier = Modifier.weight(1f)) { Text("Next") }
    }
    picker?.let { mode ->
        val matches = remember(mode, query, guide, roomId, roomTitle, questId, questTitle) {
            guide.sections.flatMapIndexed { si, s ->
                if (mode == "Contents" || (mode == "Tracked quest" && si in guide.questSections(questId))) listOf(GuideJump(si, 0, s.title))
                else if (mode == "Tracked quest") emptyList()
                else s.blocks.mapIndexedNotNull { bi, b ->
                    val roomMatch = (!roomId.isNullOrBlank() && b.text.contains(roomId)) ||
                        (!roomTitle.isNullOrBlank() && b.text.contains("**$roomTitle**"))
                    val match = if (mode == "This room") roomMatch else query.length >= 2 && (b.text.contains(query, true) || s.title.contains(query, true))
                    if (match) GuideJump(si, bi, s.title + "\n" + guideText(b.text).text.take(220)) else null
                }
            }
        }
        AlertDialog(onDismissRequest = { picker = null }, title = { Text(mode) }, text = {
            Column {
                if (mode == "Search") OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Room, quest, item or phrase") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (mode == "This room") Text("${roomTitle ?: "No current room"}\n${questTitle ?: "No tracked quest"}\nChoose your current visit or the room reference.", style = MaterialTheme.typography.bodyMedium)
                if (mode == "Tracked quest") Text(questTitle ?: "No tracked quest. Select one in your journal first.", style = MaterialTheme.typography.bodyMedium)
                if (mode == "Search" && query.isBlank() && !questTitle.isNullOrBlank()) TextButton(onClick = { query = questTitle }) { Text("Find tracked quest") }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    if (matches.isEmpty()) item { Text(when (mode) {
                        "Search" -> if (query.length < 2) "Enter at least two characters." else "No results. Try a room name, item or shorter phrase."
                        "Tracked quest" -> "No route for this tracked quest in the selected guide. Try the other world or Search."
                        else -> "No matching room in this world guide. Try the other world or Search."
                    }, modifier = Modifier.padding(vertical = 16.dp)) }
                    itemsIndexed(matches) { _, match -> TextButton(onClick = { jump(match.section, match.block) }, modifier = Modifier.fillMaxWidth()) {
                        Text(match.label, modifier = Modifier.fillMaxWidth())
                    } }
                }
            }
        }, confirmButton = { TextButton(onClick = { picker = null }) { Text("Close") } }, dismissButton = {
            if (mode == "Contents") TextButton(onClick = { picker = null; resetOpen = true }) { Text("Reset checklist") }
        })
    }
    if (resetOpen) AlertDialog(onDismissRequest = { resetOpen = false }, title = { Text("Start a fresh checklist?") },
        text = { Text("Clear World ${guide.world}'s walkthrough checkmarks and return to the beginning. Your notes and game saves stay intact.") },
        confirmButton = { TextButton(onClick = {
            checked = emptySet(); prefs.edit().remove("$prefix-checked").apply(); jump(0); resetOpen = false
        }) { Text("Reset checklist") } }, dismissButton = { TextButton(onClick = { resetOpen = false }) { Text("Cancel") } })
    if (noteOpen) AlertDialog(onDismissRequest = { noteOpen = false }, title = { Text("Playtest note") }, text = {
        Column {
            Text("${roomTitle ?: "Unknown room"}\n${questTitle ?: "No tracked quest"}", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("What happened?") }, minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(enabled = note.isNotBlank(), onClick = {
        val array = JSONArray(notes)
        array.put(JSONObject().put("time", System.currentTimeMillis()).put("room", roomId ?: "").put("roomTitle", roomTitle ?: "")
            .put("quest", questId ?: "").put("questTitle", questTitle ?: "").put("section", guide.sections[section].title)
            .put("version", com.example.starborn.BuildConfig.VERSION_NAME).put("text", note.trim()))
        notes = array.toString(); prefs.edit().putString(notesKey, notes).apply(); note = ""; noteOpen = false; message = "Note saved on this device."
    }) { Text("Save note") } }, dismissButton = { TextButton(onClick = { noteOpen = false }) { Text("Cancel") } })
    if (notesOpen) {
        val array = remember(notes) { JSONArray(notes) }
        val report = remember(notes) {
            (0 until array.length()).joinToString("\n\n") { index ->
                val n = array.getJSONObject(index)
                "${DateFormat.getDateTimeInstance().format(Date(n.getLong("time")))} | ${n.optString("version")}\n${n.optString("roomTitle")} (${n.optString("room")})\n${n.optString("questTitle")} (${n.optString("quest")})\n${n.optString("section")}\n${n.getString("text") }"
            }
        }
        AlertDialog(onDismissRequest = { notesOpen = false }, title = { Text("World ${guide.world} notes (${array.length()})") }, text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) { item { SelectionContainer { Text(report.ifBlank { "No notes yet. Add one while the issue is fresh." }) } } }
        }, confirmButton = { TextButton(enabled = report.isNotBlank(), onClick = {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Starborn World ${guide.world} playtest notes\n\n$report") }, "Share playtest notes"))
        }) { Text("Share") } }, dismissButton = { TextButton(onClick = { notesOpen = false }) { Text("Close") } })
    }
}
