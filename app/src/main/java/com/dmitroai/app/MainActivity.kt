package com.dmitroai.app

import android.os.Bundle
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Background = Color(0xFF111310)
private val Panel = Color(0xFF1B1E19)
private val PanelRaised = Color(0xFF242820)
private val Lime = Color(0xFFD8FF72)
private val TextPrimary = Color(0xFFF2F3ED)
private val TextMuted = Color(0xFF9A9E92)

private const val MAX_SAVED_MESSAGES = 200

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(17, 19, 16)
        window.navigationBarColor = android.graphics.Color.rgb(17, 19, 16)
        setContent {
            MaterialTheme {
                ChatScreen()
            }
        }
    }
}

private data class ChatMessage(val text: String, val fromAssistant: Boolean)

@Composable
private fun ChatScreen() {
    val context = LocalContext.current
    val dictionary = remember { RussianDictionary() }
    val model = remember {
        LocalLearningModel(context.getSharedPreferences("dima_model", 0), dictionary)
    }
    val modelStore = remember { QwenModelStore(context) }
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply { addAll(model.loadConversation()) }
    }
    var draft by remember { mutableStateOf("") }
    var isInternetEnabled by remember { mutableStateOf(model.isInternetEnabled()) }
    var hasLocalModel by remember { mutableStateOf(modelStore.isDownloaded()) }
    var isLocalModelLoaded by remember { mutableStateOf(false) }
    var modelDownloadProgress by remember { mutableIntStateOf(-1) }
    var modelStatus by remember { mutableStateOf("") }
    var dictionaryWordCount by remember { mutableIntStateOf(0) }
    var isThinking by remember { mutableStateOf(false) }
    var onlineStatus by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(dictionary) {
        dictionaryWordCount = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("russian_synonyms.json").use(dictionary::load)
                dictionary.wordCount
            }.getOrDefault(0)
        }
    }

    fun installOrToggleLocalModel() {
        if (modelDownloadProgress >= 0) return
        if (isLocalModelLoaded) {
            scope.launch {
                withContext(Dispatchers.IO) { QwenLocalModel.release() }
                isLocalModelLoaded = false
                modelStatus = "Модель выгружена из памяти"
            }
            return
        }
        scope.launch {
            modelDownloadProgress = 0
            modelStatus = if (hasLocalModel) "Загружаю Qwen в память телефона…" else "Скачиваю модель 1,28 ГБ…"
            try {
                val modelFile = modelStore.download { modelDownloadProgress = it }
                hasLocalModel = true
                modelStatus = "Загружаю модель в память телефона…"
                if (!QwenLocalModel.load(modelFile.absolutePath)) {
                    throw IllegalStateException("Не удалось загрузить Qwen. Проверь свободную оперативную память телефона.")
                }
                isLocalModelLoaded = true
                modelStatus = "Qwen готова и работает офлайн"
            } catch (error: Exception) {
                modelStatus = error.message ?: "Не удалось запустить локальную модель."
            } finally {
                modelDownloadProgress = -1
            }
        }
    }

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty() || isThinking) return
        messages.add(ChatMessage(cleanText, fromAssistant = false))
        model.saveConversation(messages)
        draft = ""
        focusManager.clearFocus()
        isThinking = true
        onlineStatus = ""
        scope.launch {
            delay(900)
            val chatHistory = messages.takeLast(12).map { message ->
                AiMessage(
                    role = if (message.fromAssistant) "assistant" else "user",
                    content = message.text
                )
            }
            val pageUrl = if (isInternetEnabled) WebPageReader.findUrl(cleanText) else null
            var pageText: String? = null
            if (pageUrl != null) {
                onlineStatus = "Читаю страницу…"
                val page = WebPageReader.read(pageUrl)
                pageText = page?.text
                if (page == null) modelStatus = "Не удалось прочитать страницу. Поддерживаются открытые HTTPS HTML-сайты."
            }

            val localAnswer = if (isLocalModelLoaded) {
                onlineStatus = "Qwen думает локально…"
                val prompt = LocalPromptBuilder.build(chatHistory, pageUrl, pageText)
                QwenLocalModel.generate(prompt)
            } else {
                null
            }
            val answer = model.memoryAnswer(cleanText)
                ?: localAnswer?.takeUnless {
                    it.startsWith("Не удалось") || it.startsWith("Слишком длинный")
                }
                ?: if (pageText != null) {
                    "Страницу загрузил, но для её пересказа сначала скачай локальную Qwen-модель."
                } else {
                    model.reply(
                        prompt = cleanText,
                        onOnlineRequest = { onlineStatus = it }
                    )
                }
            messages.add(ChatMessage(answer, fromAssistant = true))
            model.saveConversation(messages)
            onlineStatus = ""
            isThinking = false
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(if (isThinking) messages.size else messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            Header(
                isInternetEnabled = isInternetEnabled,
                hasLocalModel = hasLocalModel,
                isLocalModelLoaded = isLocalModelLoaded,
                modelDownloadProgress = modelDownloadProgress,
                dictionaryWordCount = dictionaryWordCount,
                onModelClick = ::installOrToggleLocalModel,
                onToggleInternet = {
                    isInternetEnabled = !isInternetEnabled
                    model.setInternetEnabled(isInternetEnabled)
                },
                onNewChat = {
                    messages.clear()
                    model.saveConversation(messages)
                    draft = ""
                }
            )
        },
        bottomBar = {
            Composer(
                value = draft,
                onValueChange = { draft = it },
                onSend = { sendMessage(draft) },
                focusManager = focusManager
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Welcome(
                        dictionaryWordCount = dictionaryWordCount,
                        hasLocalModel = hasLocalModel,
                        isLocalModelLoaded = isLocalModelLoaded,
                        modelDownloadProgress = modelDownloadProgress,
                        modelStatus = modelStatus,
                        onModelClick = ::installOrToggleLocalModel,
                        onSuggestion = ::sendMessage
                    )
                }
            }
            items(messages) { message ->
                MessageBubble(message)
            }
            if (isThinking) {
                item { ThinkingBubble(onlineStatus) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Header(
    isInternetEnabled: Boolean,
    hasLocalModel: Boolean,
    isLocalModelLoaded: Boolean,
    modelDownloadProgress: Int,
    dictionaryWordCount: Int,
    onModelClick: () -> Unit,
    onToggleInternet: () -> Unit,
    onNewChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Background)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("DIMA AI", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Dima-dian-0.0.3", color = TextMuted, fontSize = 12.sp)
            Text(
                if (dictionaryWordCount > 0) "$dictionaryWordCount слов" else "Загружаю словарь...",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Surface(
            onClick = onModelClick,
            enabled = modelDownloadProgress < 0,
            color = if (isLocalModelLoaded) PanelRaised else Lime,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isLocalModelLoaded) Lime else Background,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when {
                        modelDownloadProgress >= 0 -> "$modelDownloadProgress%"
                        isLocalModelLoaded -> "Qwen ВКЛ"
                        hasLocalModel -> "Запустить"
                        else -> "Qwen 1,28 ГБ"
                    },
                    color = if (isLocalModelLoaded) TextPrimary else Background,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onToggleInternet, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Public,
                contentDescription = if (isInternetEnabled) "Интернет включён" else "Интернет выключен",
                tint = if (isInternetEnabled) Lime else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onNewChat, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Новий чат", tint = TextPrimary)
        }
    }
}

@Composable
private fun Welcome(
    dictionaryWordCount: Int,
    hasLocalModel: Boolean,
    isLocalModelLoaded: Boolean,
    modelDownloadProgress: Int,
    modelStatus: String,
    onModelClick: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 46.dp, bottom = 16.dp)) {
        Text("ТВОЯ AI-МАСТЕРСКАЯ", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(
            "Привет, Дима.\nС чего начнём?",
            color = TextPrimary,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Я Dima-dian-0.0.3, твой персональный AI-помощник.",
            color = TextMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        Text("ПОПРОБУЙ СПРОСИТЬ", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Suggestion("Объясни, что такое искусственный интеллект", onClick = onSuggestion)
        Spacer(Modifier.height(8.dp))
        Suggestion("Помоги придумать идею для проекта", onClick = onSuggestion)
        Spacer(Modifier.height(18.dp))
        Text(
            modelStatus.ifBlank {
                if (isLocalModelLoaded) "Qwen уже работает офлайн · словарь: $dictionaryWordCount слов"
                else "Готовая модель Qwen3 · словарь: $dictionaryWordCount слов"
            },
            color = TextMuted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = onModelClick,
            enabled = modelDownloadProgress < 0,
            color = if (isLocalModelLoaded) PanelRaised else Lime,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                Text(
                    when {
                        modelDownloadProgress >= 0 -> "Скачиваю модель: $modelDownloadProgress%"
                        isLocalModelLoaded -> "Выгрузить Qwen из памяти"
                        hasLocalModel -> "Запустить уже скачанную Qwen"
                        else -> "Скачать готовую Qwen3 · 1,28 ГБ"
                    },
                    color = if (isLocalModelLoaded) TextPrimary else Background,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (!hasLocalModel && !isLocalModelLoaded && modelDownloadProgress < 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Однократно. Для запуска нужно около 2,5 ГБ свободной RAM.",
                        color = Background.copy(alpha = 0.72f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Suggestion(text: String, onClick: (String) -> Unit) {
    Surface(
        onClick = { onClick(text) },
        color = Panel,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = 13.sp)
            Text("↗", color = Lime, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromAssistant) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (message.fromAssistant) Panel else PanelRaised,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = if (message.fromAssistant) 18.dp else 5.dp,
                bottomStart = if (message.fromAssistant) 5.dp else 18.dp
            ),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                if (message.fromAssistant) {
                    Text("DIMA", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                }
                Text(message.text, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun ThinkingBubble(onlineStatus: String) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp),
        modifier = Modifier.fillMaxWidth(0.86f)
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
            Text("DIMA", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                onlineStatus.ifBlank { "Думаю над ответом…" },
                color = TextPrimary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (onlineStatus.contains("Википедии")) {
                    "Ищу справочную информацию"
                } else if (onlineStatus.isNotBlank()) {
                    "Учитываю контекст переписки"
                } else {
                    "Сверяю вопрос со словарём и памятью"
                },
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    focusManager: FocusManager
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Panel)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Lime),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 5,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) Text("Напиши сообщение...", color = TextMuted, fontSize = 14.sp)
                        innerTextField()
                    }
                }
            )
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank()) Lime else PanelRaised)
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Надіслати",
                    tint = if (value.isNotBlank()) Background else TextMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

private class LocalLearningModel(
    private val preferences: SharedPreferences,
    private val dictionary: RussianDictionary
) {
    fun memoryAnswer(prompt: String): String? {
        rememberFact(prompt)?.let { return it }
        if (!isMemoryQuestion(prompt)) return null
        val facts = preferences.getStringSet("memories", emptySet()).orEmpty()
        return if (facts.isEmpty()) {
            "Пока у меня нет сохранённых заметок. Напиши «Запомни, что ...», и я сохраню это на устройстве."
        } else {
            "Вот что я запомнил: ${facts.joinToString("; ")}."
        }
    }

    fun isInternetEnabled(): Boolean = preferences.getBoolean("internet_enabled", true)

    fun setInternetEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("internet_enabled", enabled).apply()
    }

    fun loadConversation(): List<ChatMessage> {
        val saved = preferences.getString("conversation", null) ?: return emptyList()
        return runCatching {
            val messages = JSONArray(saved)
            List(messages.length()) { index ->
                val message = messages.getJSONObject(index)
                ChatMessage(
                    text = message.getString("text"),
                    fromAssistant = message.getBoolean("assistant")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveConversation(messages: List<ChatMessage>) {
        val saved = JSONArray()
        messages.takeLast(MAX_SAVED_MESSAGES).forEach { message ->
            saved.put(
                JSONObject()
                    .put("text", message.text)
                    .put("assistant", message.fromAssistant)
            )
        }
        preferences.edit().putString("conversation", saved.toString()).apply()
    }

    suspend fun reply(
        prompt: String,
        onOnlineRequest: (String) -> Unit
    ): String {
        memoryAnswer(prompt)?.let { return it }

        val dictionaryMatches = dictionary.search(prompt)
        val synonymQuestion = Regex(
            "(?iu)(синоним|как пишется|проверь слово|исправь опечатку)"
        ).containsMatchIn(prompt)
        val singleWord = prompt.trim().matches(Regex("[\\p{L}-]+"))
        if (dictionaryMatches.isNotEmpty() && (synonymQuestion || singleWord)) {
            val suggestions = dictionaryMatches.take(3).joinToString("\n") { match ->
                val correction = if (match.editDistance > 0) {
                    "Возможно, ты имел в виду «${match.correctedToken}». "
                } else {
                    ""
                }
                val variants = (match.word.synonyms + match.word.similarWords)
                    .distinctBy { it.lowercase() }
                    .take(12)
                val synonymText = if (variants.isEmpty()) {
                    "Варианты не указаны."
                } else {
                    "Варианты: ${variants.joinToString(", ")}."
                }
                "$correction${match.word.name}: $synonymText"
            }
            return suggestions
        }

        if (isInternetEnabled()) {
            onOnlineRequest("Шукаю у Вікіпедії…")
            val onlineAnswer = withContext(Dispatchers.IO) { InternetSearch.search(prompt) }
            if (onlineAnswer != null) return onlineAnswer
        }

        if (dictionaryMatches.isNotEmpty()) {
            val match = dictionaryMatches.first()
            val correction = if (match.editDistance > 0) {
                "Возможно, ты имел в виду «${match.correctedToken}». "
            } else {
                ""
            }
            val variants = (match.word.synonyms + match.word.similarWords)
                .distinctBy { it.lowercase() }
                .take(12)
            return "$correction${match.word.name}: ${variants.joinToString(", ").ifBlank { "варианты в словаре не указаны" }}."
        }
        return "Не нашёл это в локальном словаре${if (isInternetEnabled()) " или Википедии" else ""}. Попробуй переформулировать запрос."
    }

    private fun rememberFact(prompt: String): String? {
        val explicitFact = Regex("""(?iu)^(?:запомни|запиши)(?:\s+мне)?\s*,?\s*(?:что\s+)?(.+)$""")
            .find(prompt.trim())
            ?.groupValues
            ?.getOrNull(1)
        val name = Regex("""(?iu)\bменя зовут\s+([\p{L}-]+)""")
            .find(prompt)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { "Пользователя зовут $it" }
        val like = Regex("""(?iu)\bя люблю\s+(.+)""")
            .find(prompt)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { "Пользователь любит $it" }
        val fact = explicitFact ?: name ?: like ?: return null
        val memories = preferences.getStringSet("memories", emptySet()).orEmpty().toMutableSet()
        memories.add(fact.trim().trimEnd('.', '!'))
        preferences.edit().putStringSet("memories", memories).apply()
        return "Хорошо, запомнил: ${fact.trim().trimEnd('.', '!')}."
    }

    private fun isMemoryQuestion(prompt: String): Boolean =
        Regex("""(?iu)(что ты помнишь|что ты запомнил|что ты знаешь обо мне|что я просил запомнить)""")
            .containsMatchIn(prompt)

}