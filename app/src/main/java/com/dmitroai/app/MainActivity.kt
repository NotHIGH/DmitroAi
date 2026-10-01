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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

private val Background = Color(0xFF111310)
private val Panel = Color(0xFF1B1E19)
private val PanelRaised = Color(0xFF242820)
private val Lime = Color(0xFFD8FF72)
private val TextPrimary = Color(0xFFF2F3ED)
private val TextMuted = Color(0xFF9A9E92)

private data class TrainingExample(val question: String, val answer: String)

private val trainingExamples = listOf(
    TrainingExample("привет", "Привет! Рад тебя видеть. Чем помочь?"),
    TrainingExample("здравствуй", "Здравствуй! О чём поговорим?"),
    TrainingExample("как дела", "У меня всё хорошо, спасибо! Готов помочь."),
    TrainingExample("как тебя зовут", "Я Dima-dian-0.0.3, твой локальный помощник."),
    TrainingExample("что ты умеешь", "Пока я знаю простые ответы и учусь находить похожие слова."),
    TrainingExample("что такое ии", "ИИ — это программы, которые находят закономерности и помогают решать задачи."),
    TrainingExample("что такое искусственный интеллект", "Искусственный интеллект — это программы, которые учатся на примерах и выполняют задачи."),
    TrainingExample("спасибо", "Пожалуйста! Обращайся, если понадобится помощь."),
    TrainingExample("пока", "До встречи! Хорошего дня."),
    TrainingExample("доброе утро", "Доброе утро! Пусть день начнётся хорошо."),
    TrainingExample("расскажи шутку", "Почему программист путает Хэллоуин и Рождество? Потому что OCT 31 = DEC 25."),
    TrainingExample("сколько будет два плюс два", "Два плюс два будет четыре."),
    TrainingExample("какая сегодня погода", "Для текущей погоды нужен специальный погодный источник; в Википедии нет прогноза на сегодня."),
    TrainingExample("помоги придумать идею", "Давай! Расскажи тему и для кого нужна идея."),
    TrainingExample("что ты такое", "Я Dima-dian-0.0.3 — небольшой чат-помощник, который учится на простых примерах."),
    TrainingExample("добрый день", "Добрый день! Что будем делать?"),
    TrainingExample("добрый вечер", "Добрый вечер! Рад продолжить разговор."),
    TrainingExample("хай", "Хай! Чем могу помочь?"),
    TrainingExample("ты искусственный интеллект", "Я простая локальная программа, которая ищет ответы среди примеров."),
    TrainingExample("что такое нейросеть", "Нейросеть — программа из связанных вычислительных узлов, которая учится находить закономерности в данных."),
    TrainingExample("как работает ии", "Программа сравнивает запрос с тем, чему научилась на примерах, и выбирает подходящий ответ."),
    TrainingExample("как работает компьютер", "Компьютер выполняет команды программ и обрабатывает данные с помощью процессора и памяти."),
    TrainingExample("что такое алгоритм", "Алгоритм — последовательность шагов для решения задачи."),
    TrainingExample("что такое программа", "Программа — набор команд, которые компьютер выполняет для решения задачи."),
    TrainingExample("что такое переменная", "Переменная — именованное место, где программа хранит значение."),
    TrainingExample("что такое kotlin", "Kotlin — современный язык программирования, который часто используют для Android-приложений."),
    TrainingExample("что такое андроид", "Android — операционная система для телефонов и других устройств."),
    TrainingExample("что такое интернет", "Интернет — всемирная сеть, которая соединяет устройства и позволяет обмениваться данными."),
    TrainingExample("что такое память телефона", "Память телефона хранит приложения, фотографии, сообщения и другие файлы."),
    TrainingExample("что такое приложение", "Приложение — программа, созданная для выполнения определённых задач на устройстве."),
    TrainingExample("сколько будет один плюс один", "Один плюс один будет два."),
    TrainingExample("сколько будет пять плюс три", "Пять плюс три будет восемь."),
    TrainingExample("сколько будет десять минус четыре", "Десять минус четыре будет шесть."),
    TrainingExample("сколько будет два умножить на три", "Два умножить на три будет шесть."),
    TrainingExample("сколько будет восемь разделить на два", "Восемь разделить на два будет четыре."),
    TrainingExample("сколько дней в неделе", "В неделе семь дней."),
    TrainingExample("сколько месяцев в году", "В году двенадцать месяцев."),
    TrainingExample("какого цвета небо", "Днём небо обычно кажется голубым."),
    TrainingExample("почему небо голубое", "Воздух сильнее рассеивает короткие синие волны солнечного света, поэтому небо кажется голубым."),
    TrainingExample("где живут люди", "Люди живут в разных странах, городах и сёлах по всему миру."),
    TrainingExample("что такое солнце", "Солнце — звезда, которая даёт Земле свет и тепло."),
    TrainingExample("что такое земля", "Земля — планета, на которой мы живём."),
    TrainingExample("что такое вода", "Вода — прозрачная жидкость, необходимая людям, животным и растениям."),
    TrainingExample("зачем спать", "Сон помогает организму отдыхать, восстанавливаться и сохранять силы."),
    TrainingExample("зачем учиться", "Учёба помогает узнавать новое, развивать навыки и находить решения."),
    TrainingExample("как начать программировать", "Выбери небольшой проект, изучи основы одного языка и пробуй писать код каждый день."),
    TrainingExample("как создать игру", "Начни с простой идеи: выбери движок, создай персонажа и добавляй механику по шагам."),
    TrainingExample("как придумать проект", "Выбери проблему, которая тебе интересна, и придумай маленькое приложение, способное её решить."),
    TrainingExample("дай совет", "Разбей задачу на небольшие шаги и начни с самого простого."),
    TrainingExample("мне грустно", "Жаль, что тебе грустно. Иногда помогает поговорить с близким человеком и рассказать, что случилось."),
    TrainingExample("я устал", "Сделай небольшой перерыв, выпей воды и отдохни."),
    TrainingExample("ты живой", "Нет, я программа в телефоне. Но я могу отвечать на сообщения."),
    TrainingExample("у тебя есть интернет", "Да, если переключатель интернета включён, я могу искать справочные статьи в Википедии."),
    TrainingExample("кто тебя создал", "Я Dima-dian-0.0.3 — проект персонального помощника Dima AI."),
    TrainingExample("какая твоя версия", "Моя версия — Dima-dian-0.0.3."),
    TrainingExample("что ты помнишь", "Я могу сохранять переписку и факты, которые ты попросишь запомнить."),
    TrainingExample("можешь запоминать", "Да. Напиши «запомни, что ...», и я сохраню эту заметку на устройстве."),
    TrainingExample("расскажи факт", "У осьминога три сердца."),
    TrainingExample("назови животное", "Например, дельфин — умное морское млекопитающее."),
    TrainingExample("назови число", "Семь."),
    TrainingExample("напиши привет", "Привет!"),
    TrainingExample("помоги с домашним заданием", "Пришли условие задания, и мы разберём его по шагам."),
    TrainingExample("помоги с математикой", "Пришли пример, и я постараюсь решить его вместе с тобой."),
    TrainingExample("что такое слово", "Слово — часть речи или языка, которая выражает значение."),
    TrainingExample("что такое книга", "Книга — произведение с текстом или изображениями, напечатанное или электронное."),
    TrainingExample("как дела у тебя", "Спасибо, у меня всё хорошо. Я готов помочь."),
    TrainingExample("расскажи о себе", "Я Dima-dian-0.0.3. Ищу знакомые слова в локальном словаре, а при необходимости могу обратиться к Википедии."),
    TrainingExample("что делать если скучно", "Можно прогуляться, почитать, порисовать или начать небольшой творческий проект."),
    TrainingExample("как тебя обучить", "Нажми кнопку «Обучить». Я обработаю примеры и сохраню их на устройстве."),
    TrainingExample("ты можешь ошибаться", "Да. Я небольшой прототип и иногда могу подобрать неподходящий ответ."),
    TrainingExample("до свидания", "До свидания! Возвращайся, когда захочешь поговорить."),
    TrainingExample("спокойной ночи", "Спокойной ночи и хорошего отдыха!"),
    TrainingExample("как тебя зовут", "Я Dima-dian-0.0.3, твой локальный помощник."),
    TrainingExample("расскажи интересный факт", "Мёд при правильном хранении может сохраняться очень долго."),
    TrainingExample("почему трава зелёная", "В листьях есть хлорофилл, который отражает зелёную часть света."),
    TrainingExample("что такое друг", "Друг — человек, которому доверяют и с которым приятно проводить время."),
    TrainingExample("как попросить тебя запомнить", "Напиши: «Запомни, что я люблю космос». Я сохраню эту заметку на устройстве.")
)

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
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply { addAll(model.loadConversation()) }
    }
    var draft by remember { mutableStateOf("") }
    var isTrained by remember { mutableStateOf(model.isTrained()) }
    var isAutoLearning by remember { mutableStateOf(model.isAutoLearning()) }
    var isInternetEnabled by remember { mutableStateOf(model.isInternetEnabled()) }
    var isAiConfigured by remember { mutableStateOf(ApiKeyVault.hasKey(context)) }
    var showAiSettings by remember { mutableStateOf(false) }
    var apiKeyDraft by remember { mutableStateOf("") }
    var dictionaryWordCount by remember { mutableIntStateOf(0) }
    var trainingProgress by remember { mutableIntStateOf(-1) }
    var learnedExampleCount by remember { mutableIntStateOf(model.exampleCount()) }
    var isThinking by remember { mutableStateOf(false) }
    var onlineStatus by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    if (showAiSettings) {
        AiSettingsDialog(
            apiKey = apiKeyDraft,
            isConfigured = isAiConfigured,
            onApiKeyChange = { apiKeyDraft = it },
            onSave = {
                ApiKeyVault.save(context, apiKeyDraft)
                isAiConfigured = ApiKeyVault.hasKey(context)
                apiKeyDraft = ""
                showAiSettings = false
            },
            onRemove = {
                ApiKeyVault.save(context, "")
                isAiConfigured = false
                apiKeyDraft = ""
                showAiSettings = false
            },
            onDismiss = {
                apiKeyDraft = ""
                showAiSettings = false
            }
        )
    }

    LaunchedEffect(dictionary) {
        dictionaryWordCount = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("russian_synonyms.json").use(dictionary::load)
                dictionary.wordCount
            }.getOrDefault(0)
        }
    }

    fun trainOrToggleLearning() {
        if (trainingProgress >= 0) return
        if (isTrained) {
            isAutoLearning = !isAutoLearning
            model.setAutoLearning(isAutoLearning)
            return
        }
        scope.launch {
            trainingProgress = 0
            kotlinx.coroutines.yield()
            model.train()
            isTrained = true
            learnedExampleCount = model.exampleCount()
            trainingProgress = 100
            delay(700)
            trainingProgress = -1
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
            val answer = model.reply(
                prompt = cleanText,
                conversation = messages.dropLast(1).toList(),
                apiKey = ApiKeyVault.read(context),
                chatHistory = chatHistory,
                onOnlineRequest = { onlineStatus = it }
            )
            messages.add(ChatMessage(answer, fromAssistant = true))
            model.saveConversation(messages)
            learnedExampleCount = model.exampleCount()
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
                isTrained = isTrained,
                isAutoLearning = isAutoLearning,
                isInternetEnabled = isInternetEnabled,
                isAiConfigured = isAiConfigured,
                dictionaryWordCount = dictionaryWordCount,
                trainingProgress = trainingProgress,
                onTrain = ::trainOrToggleLearning,
                onManageAi = { showAiSettings = true },
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
                        isTrained = isTrained,
                        isAutoLearning = isAutoLearning,
                        dictionaryWordCount = dictionaryWordCount,
                        trainingProgress = trainingProgress,
                        learnedExampleCount = learnedExampleCount,
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
    isTrained: Boolean,
    isAutoLearning: Boolean,
    isInternetEnabled: Boolean,
    isAiConfigured: Boolean,
    dictionaryWordCount: Int,
    trainingProgress: Int,
    onTrain: () -> Unit,
    onManageAi: () -> Unit,
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
            onClick = onTrain,
            enabled = trainingProgress < 0,
            color = when {
                trainingProgress >= 0 -> PanelRaised
                isTrained && !isAutoLearning -> PanelRaised
                else -> Lime
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = if (trainingProgress >= 0 || (isTrained && !isAutoLearning)) TextMuted else Background,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when {
                        trainingProgress >= 0 -> "Готовлю..."
                        !isTrained -> "Обучить"
                        isAutoLearning -> "Авто: ВКЛ"
                        else -> "Авто: ВЫКЛ"
                    },
                    color = if (trainingProgress >= 0 || (isTrained && !isAutoLearning)) TextMuted else Background,
                    fontSize = 12.sp,
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
        IconButton(onClick = onManageAi, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Key,
                contentDescription = if (isAiConfigured) "AI-ключ сохранён" else "Настроить AI-модель",
                tint = if (isAiConfigured) Lime else TextMuted,
                modifier = Modifier.size(19.dp)
            )
        }
        IconButton(onClick = onNewChat, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Новий чат", tint = TextPrimary)
        }
    }
}

@Composable
private fun AiSettingsDialog(
    apiKey: String,
    isConfigured: Boolean,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подключить AI-модель") },
        text = {
            Column {
                Text("Нужен личный ключ Pollinations для генерации связных ответов.")
                Spacer(Modifier.height(10.dp))
                Text("Получить ключ: enter.pollinations.ai/keys", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("Личный API-ключ") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Последние сообщения чата будут отправляться сервису Pollinations. " +
                        "Возможны ограничения или списание кредитов. Не отправляй секретные данные.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                if (isConfigured) {
                    Spacer(Modifier.height(8.dp))
                    Text("Сохранённый ключ зашифрован Android Keystore.", fontSize = 12.sp, color = Lime)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = apiKey.isNotBlank()) {
                Text("Сохранить ключ")
            }
        },
        dismissButton = {
            Row {
                if (isConfigured) TextButton(onClick = onRemove) { Text("Удалить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

@Composable
private fun Welcome(
    isTrained: Boolean,
    isAutoLearning: Boolean,
    dictionaryWordCount: Int,
    trainingProgress: Int,
    learnedExampleCount: Int,
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
            when {
                trainingProgress >= 0 -> "Сохраняю базовые ответы..."
                isTrained -> "Словарь: $dictionaryWordCount слов · автообучение ${if (isAutoLearning) "включено" else "выключено"}"
                else -> "Базовые ответы загрузятся менее чем за секунду"
            },
            color = TextMuted,
            fontSize = 11.sp
        )
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
    fun isTrained(): Boolean =
        preferences.getBoolean("is_trained", false) || loadExamples().isNotEmpty()

    fun isAutoLearning(): Boolean = preferences.getBoolean("auto_learning", false)

    fun isInternetEnabled(): Boolean = preferences.getBoolean("internet_enabled", true)

    fun setAutoLearning(enabled: Boolean) {
        preferences.edit().putBoolean("auto_learning", enabled).apply()
    }

    fun setInternetEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("internet_enabled", enabled).apply()
    }

    fun exampleCount(): Int =
        (trainingExamples + loadExamples()).distinctBy { normalize(it.question) }.size

    fun train() {
        val merged = (loadExamples() + trainingExamples)
            .distinctBy { normalize(it.question) }
        saveExamples(merged)
        preferences.edit().putBoolean("is_trained", true).apply()
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
        conversation: List<ChatMessage>,
        apiKey: String?,
        chatHistory: List<AiMessage>,
        onOnlineRequest: (String) -> Unit
    ): String {
        rememberFact(prompt)?.let { return it }

        if (isMemoryQuestion(prompt)) {
            val facts = preferences.getStringSet("memories", emptySet()).orEmpty()
            return if (facts.isEmpty()) {
                "Пока у меня нет сохранённых заметок. Напиши «Запомни, что ...», и я сохраню это на устройстве."
            } else {
                "Вот что я запомнил: ${facts.joinToString("; ")}."
            }
        }

        if (isInternetEnabled() && !apiKey.isNullOrBlank()) {
            val dictionaryContext = dictionary.search(prompt)
                .take(4)
                .joinToString("; ") { match ->
                    "${match.word.name}: ${(match.word.synonyms + match.word.similarWords).take(8).joinToString(", ")}"
                }
            onOnlineRequest("AI формує відповідь…")
            val generatedAnswer = AiChatClient.generate(apiKey, chatHistory, dictionaryContext)
            if (generatedAnswer != null) {
                if (isAutoLearning()) saveLearnedExample(TrainingExample(prompt, generatedAnswer))
                return generatedAnswer
            }
        }

        if (!isTrained()) {
            return "Сначала нажми «Обучить» вверху. Базовые ответы загрузятся менее чем за секунду."
        }

        val queryWords = words(prompt)
        if (queryWords.isEmpty()) return "Напиши немного подробнее, и я попробую найти подходящий ответ."

        val previousQuestion = conversation.lastOrNull { !it.fromAssistant }?.text.orEmpty()
        val searchText = if (queryWords.size <= 3 && previousQuestion.isNotBlank()) {
            "$previousQuestion $prompt"
        } else {
            prompt
        }
        val searchWords = words(searchText)
        val examples = (trainingExamples + loadExamples())
            .distinctBy { normalize(it.question) }
        var bestAnswer: String? = null
        var bestScore = 0.0
        for (example in examples) {
            val exampleWords = words(example.question)
            val overlap = searchWords.intersect(exampleWords).size
            val score = overlap / sqrt((searchWords.size * exampleWords.size).toDouble())
            if (score > bestScore) {
                bestScore = score
                bestAnswer = example.answer
            }
        }

        if (bestScore >= 0.25) {
            val answer = bestAnswer.orEmpty()
            if (isAutoLearning()) saveLearnedExample(TrainingExample(prompt, answer))
            return answer
        }

        val dictionaryMatches = dictionary.search(prompt)
        val synonymQuestion = Regex(
            "(?iu)(синоним|как пишется|проверь слово|исправь опечатку)"
        ).containsMatchIn(prompt)
        if (dictionaryMatches.isNotEmpty() && (synonymQuestion || queryWords.size == 1)) {
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
            if (isAutoLearning()) {
                dictionaryMatches.firstOrNull()?.let { match ->
                    saveLearnedExample(TrainingExample(prompt, "Близкие слова: ${match.word.synonyms.take(12).joinToString(", ")}"))
                }
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

    private fun loadExamples(): List<TrainingExample> {
        val saved = preferences.getString("examples", null) ?: return emptyList()
        return runCatching {
            val examples = JSONArray(saved)
            List(examples.length()) { index ->
                val example = examples.getJSONObject(index)
                TrainingExample(example.getString("question"), example.getString("answer"))
            }
        }.getOrDefault(emptyList())
    }

    private fun saveExamples(training: List<TrainingExample>) {
        val json = JSONArray()
        training.forEach { example ->
            json.put(
                JSONObject()
                    .put("question", example.question)
                    .put("answer", example.answer)
            )
        }
        preferences.edit().putString("examples", json.toString()).apply()
    }

    private fun saveLearnedExample(example: TrainingExample) {
        val examples = (loadExamples().filterNot { normalize(it.question) == normalize(example.question) } + example)
            .takeLast(500)
        saveExamples(examples)
    }

    private fun normalize(text: String): String = text.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun words(text: String): Set<String> {
        val commonWords = setOf("что", "как", "это", "мне", "меня", "тебя", "твой", "для", "или", "про")
        return Regex("[a-zа-яё0-9]+", RegexOption.IGNORE_CASE)
            .findAll(text.lowercase())
            .map { it.value }
            .filter { it.length > 1 && it !in commonWords }
            .toSet()
    }
}