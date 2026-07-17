package com.example.gsmcompanion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefinitionBottomSheet(
    response: TermEntriesResponse?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAddCard: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage
            )
        }
        val dictionaryEntries = response?.dictionaryEntries ?: return@ModalBottomSheet

        if (dictionaryEntries.isEmpty()) {
            Text(
                text = "No Definitions Found"
            )
        } else {
            LazyColumn() {
                items(dictionaryEntries) { dictionaryEntry ->
                    DictionaryEntryCard(dictionaryEntry, onAddCard)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun DictionaryEntryCard(
    dictionaryEntry: DictionaryEntry,
    onAddCard: (String) -> Unit
) {
    Column() {
        // Entry expression
        dictionaryEntry.headwords.firstOrNull()?.let { hw ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = hw.term

                    )
                }
                Button(
                    onClick = { onAddCard(hw.term) },
                ) { Text("Add to Anki") }
            }

            hw.reading?.let {
                Text(
                    text = it
                )
            }
        }
        dictionaryEntry.definitions.forEach { definition ->
            val source = definition.dictionaryAlias?.takeIf { it.isNotBlank() } ?: definition.dictionary
            if (!source.isNullOrBlank()) {
                Text(
                    text = source
                )
            }
            definition.entries.forEach { entry ->
                when (entry.type) {
                    "structured-content" -> StructuredContent(entry.content)
                    "text" -> plainText(entry.content).takeIf { it.isNotEmpty() }?.let { Text(it) }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun StructuredContent(node: JsonElement?) {
    when(node) {
        is JsonPrimitive -> node.contentOrNull?.takeIf { it.isNotEmpty() }?.let { Text(it) }
        is JsonArray ->
            if (node.any { isBlockNode(it) }) {
                Column() {
                    node.forEach { StructuredContent(it) }
                }
            } else {
                FlowRow() { node.forEach { StructuredContent(it) }}
            }
        is JsonObject -> RenderElement(node)
        else -> {}
    }
}

@Composable
private fun RenderElement(node: JsonObject) {
    val tag = node["tag"]?.jsonPrimitive?.contentOrNull
    val child = node["content"]
    when (tag) {
        "br" -> Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
        "rt" -> {}
        "img" -> {}
        "ruby" -> RubyContent(child)
        "a" -> LinkText(node)
        "ul", "ol" -> Column() {
            StructuredContent(child)
        }
        "li" -> Column() {
            Text("\t")
            StructuredContent(child)
        }
        "div" -> Column() { StructuredContent(child) }
        "span" -> SpanContent(node)
        else -> StructuredContent(child)
    }
}

@Composable
private fun SpanContent(obj: JsonObject) {
    val child = obj["content"]
    val cssClass = obj["data"]?.jsonObject?.get("class")?.jsonPrimitive?.contentOrNull
    if (cssClass == "tag") {
        Surface() {
            Text(
                text = plainText(child),
            )
        }
    } else {
        StructuredContent(child)
    }
}

@Composable
private fun LinkText(obj: JsonObject) {
    val href = obj["href"]?.jsonPrimitive?.contentOrNull
    val uriHandler = LocalUriHandler.current

    Text(
        text = plainText(obj["content"]),
        modifier = if (href != null) Modifier.clickable {uriHandler.openUri(href)} else Modifier
    )

}

@Composable
private fun RubyContent(obj: JsonElement?) {
    val base = StringBuilder()
    val reading = StringBuilder()

    fun walk(node: JsonElement?) {
        when (node) {
            is JsonPrimitive -> base.append(node.contentOrNull ?: "")
            is JsonArray -> node.forEach { walk(it) }
            is JsonObject -> {
                if (node["tag"]?.jsonPrimitive?.contentOrNull == "rt") {
                    reading.append(node["content"]?.jsonPrimitive?.contentOrNull)
                } else {
                    walk(node["content"])
                }

            }
            else -> {}
        }
    }
    walk(obj)
    Column() {
        Text (
            text = reading.toString()
        )
        Text(
            text = base.toString()
        )
    }
}

private fun isBlockNode(node: JsonElement): Boolean {
    val tag = (node as? JsonObject)?.get("tag")?.jsonPrimitive?.contentOrNull
    return tag in setOf("div", "ul", "ol", "li", "table", "tr", "thead", "tbody", "details", "br")
}

private fun plainText(node: JsonElement?): String = when(node) {
        is JsonPrimitive -> node.contentOrNull ?: ""
        is JsonArray -> node.joinToString("") { plainText(it) }
        is JsonObject -> plainText(node["content"])
        else -> ""
}