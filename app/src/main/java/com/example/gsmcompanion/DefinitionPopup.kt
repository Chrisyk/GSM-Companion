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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                text = errorMessage,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
        val dictionaryEntries = response?.dictionaryEntries ?: return@ModalBottomSheet

        if (dictionaryEntries.isEmpty()) {
            Text(
                text = "No Definitions Found",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        } else {
            LazyColumn (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                items(dictionaryEntries) { dictionaryEntry ->
                    DictionaryEntryCard(dictionaryEntry, onAddCard)
                    HorizontalDivider( modifier = Modifier.padding(vertical = 8.dp))
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
    Column (
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
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
                        text = hw.term,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Button(
                    onClick = { onAddCard(hw.term) },
                ) { Text("Add to Anki") }
            }

            hw.reading?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        dictionaryEntry.definitions.forEach { definition ->
            val source =
                definition.dictionaryAlias?.takeIf { it.isNotBlank() } ?: definition.dictionary
            if (!source.isNullOrBlank()) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =  Modifier.padding(top = 12.dp, bottom = 4.dp)
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
    when (node) {
        is JsonPrimitive -> node.contentOrNull?.takeIf { it.isNotEmpty() }?.let { Text(it) }
        is JsonArray ->
            if (node.any { isBlockNode(it) }) {
                Column {
                    node.forEach { StructuredContent(it) }
                }
            } else {
                FlowRow { node.forEach { StructuredContent(it) } }
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
        "br" -> Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(4.dp))
        "rt" -> {}
        "img" -> {}
        "ruby" -> RubyContent(child)
        "a" -> LinkText(node)
        "ul", "ol" -> Column (modifier = Modifier.fillMaxWidth().padding(start = 4.dp))
        {
            StructuredContent(child)
        }
        "li" -> Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
        {
            Text("•  ")
            Column(modifier = Modifier.weight(1f)) {
                StructuredContent(child)
            }
        }

        "div" -> Column(modifier = Modifier.fillMaxWidth()) { StructuredContent(child) }
        "span" -> SpanContent(node)
        else -> StructuredContent(child)
    }
}

@Composable
private fun SpanContent(obj: JsonObject) {
    val child = obj["content"]
    val cssClass = obj["data"]?.jsonObject?.get("class")?.jsonPrimitive?.contentOrNull
    if (cssClass == "tag") {
        Surface (
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(end = 4.dp, top = 2.dp, bottom = 2.dp)
        ){
            Text(
                text = plainText(child),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = if (href != null) Modifier.clickable { uriHandler.openUri(href) } else Modifier
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
    Column {
        Text(
            text = reading.toString(),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = base.toString(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun isBlockNode(node: JsonElement): Boolean {
    val tag = (node as? JsonObject)?.get("tag")?.jsonPrimitive?.contentOrNull
    return tag in setOf("div", "ul", "ol", "li", "table", "tr", "thead", "tbody", "details", "br")
}

private fun plainText(node: JsonElement?): String = when (node) {
    is JsonPrimitive -> node.contentOrNull ?: ""
    is JsonArray -> node.joinToString("") { plainText(it) }
    is JsonObject -> plainText(node["content"])
    else -> ""
}