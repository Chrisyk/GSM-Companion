package com.example.gsmcompanion

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextHookerScreen(
    navController: NavController,
    viewModel: TextHookerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val config by APIConfigs.getConfigs(context).collectAsState(
        initial = Configs()
    )
    val websocketAddress = APIConfigs.getURL(config, PortName.GSM, "ws")

    val statusMessage = when (uiState.connectionStatus) {
        ConnectionStatus.Connected -> "Connected"
        ConnectionStatus.Connecting -> "Connecting"
        ConnectionStatus.Disconnected -> "Disconnected"
        ConnectionStatus.Error -> uiState.textHookerErrorMessage ?: "Error"
    }

    val statusText = "$websocketAddress: $statusMessage"
    DisposableEffect(websocketAddress) {
        viewModel.connect(websocketAddress)
        onDispose {
            viewModel.disconnect()
        }
    }
    LaunchedEffect(uiState.ankiFieldsMessage) {
        if (uiState.ankiFieldsMessage != null) {
            Toast.makeText(context, uiState.ankiFieldsMessage, Toast.LENGTH_SHORT).show()
            viewModel.consumeAnkiError()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(statusText)
                }
            )
        },
        bottomBar = {
            BottomBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)

        ) {
            MessageList(uiState.sentences,
                viewModel::onTextPointSelected,
                uiState.selectedSentenceIndex,
                uiState.offset,
                uiState.originalTextLength)
        }

        if (uiState.termEntriesResponse != null || uiState.termEntriesErrorMessage != null) {
            DefinitionBottomSheet(
                response = uiState.termEntriesResponse,
                errorMessage = uiState.termEntriesErrorMessage,
                addingTerm = uiState.addingTerm,
                duplicateTermsId = uiState.duplicateTermsIds,
                onDismiss = viewModel::clearTermEntries,
                onAddCard = viewModel::onClickCardToAnki,
            )
        }
    }
}

@Composable
fun MessageList(sentences: List<String>,
                onTextPointSelected: (Int?, String, Int) -> Unit,
                selectedSentenceIndex: Int?,
                offset: Int?,
                originalTextLength: Int?
                ) {
    val listState = rememberLazyListState()

    LaunchedEffect(sentences.size) {
        if (sentences.isNotEmpty()) {
            listState.animateScrollToItem(sentences.size - 1)
        }
    }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
        itemsIndexed(sentences) { index, sentence ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp, top = 4.dp, bottom = 8.dp)
            ) {
                if (selectedSentenceIndex == index && offset != null && originalTextLength != null) {
                    SentenceText(text = highlightedSentence(sentence, offset, originalTextLength))
                } else {
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    SentenceText(
                        text = AnnotatedString(sentence),
                        modifier = Modifier.pointerInput(sentence) {
                            detectTapGestures { pos ->
                                layoutResult?.getOffsetForPosition(pos)?.let {
                                    onTextPointSelected(index, sentence, it)
                                }
                            }
                        },
                        onTextLayout = { layoutResult = it }
                    )
                }
            }
        }
    }
}


@Composable
private fun SentenceText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 22.sp,
            lineHeight = 34.sp
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp)
            .then(modifier),
        onTextLayout = onTextLayout
    )
}

@Composable
private fun highlightedSentence(sentence: String, start: Int, length: Int): AnnotatedString {
    val highlight = MaterialTheme.colorScheme.primaryContainer
    return remember(sentence, start, length, highlight) {
        buildAnnotatedString {
            append(sentence)
            val s = start.coerceIn(0, sentence.length)
            val e = (start + length).coerceIn(s, sentence.length)
            if (e > s) {
                addStyle(SpanStyle(background = highlight), s, e)
            }
        }
    }
}

