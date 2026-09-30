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
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    TrainingExample("какая сегодня погода", "Я пока не подключён к интернету и не знаю текущую погоду."),
    TrainingExample("помоги придумать идею", "Давай! Расскажи тему и для кого нужна идея."),
    TrainingExample("что ты такое", "Я Dima-dian-0.0.3 — небольшой чат-помощник, который учится на простых примерах.")
)

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
    val model = remember {
        LocalLearningModel(context.getSharedPreferences("dima_model", 0))
    }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var isTrained by remember { mutableStateOf(model.isTrained()) }
    var trainingProgress by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun startTraining() {
        if (trainingProgress >= 0) return
        scope.launch {
            trainingProgress = 0
            trainingExamples.forEachIndexed { index, _ ->
                delay(90)
                trainingProgress = (index + 1) * 100 / trainingExamples.size
            }
            model.train()
            isTrained = true
            trainingProgress = -1
        }
    }

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        messages.add(ChatMessage(cleanText, fromAssistant = false))
        messages.add(ChatMessage(model.reply(cleanText), fromAssistant = true))
        draft = ""
        focusManager.clearFocus()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            Header(
                isTrained = isTrained,
                trainingProgress = trainingProgress,
                onTrain = ::startTraining,
                onNewChat = {
                    messages.clear()
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
                        trainingProgress = trainingProgress,
                        onSuggestion = ::sendMessage
                    )
                }
            }
            items(messages) { message ->
                MessageBubble(message)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Header(
    isTrained: Boolean,
    trainingProgress: Int,
    onTrain: () -> Unit,
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
        }
        Surface(
            onClick = onTrain,
            enabled = trainingProgress < 0,
            color = if (trainingProgress >= 0) PanelRaised else Lime,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = if (trainingProgress >= 0) TextMuted else Background,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (trainingProgress >= 0) "${trainingProgress}%" else "Обучить",
                    color = if (trainingProgress >= 0) TextMuted else Background,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onNewChat, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Новий чат", tint = TextPrimary)
        }
    }
}

@Composable
private fun Welcome(
    isTrained: Boolean,
    trainingProgress: Int,
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
                trainingProgress >= 0 -> "Обучаю словарь: $trainingProgress%"
                isTrained -> "Выучено примеров: ${trainingExamples.size} · ответы сохраняются на устройстве"
                else -> "Нажми «Обучить» вверху, чтобы выучить простые ответы"
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

private class LocalLearningModel(private val preferences: SharedPreferences) {
    fun isTrained(): Boolean = !preferences.getString("examples", null).isNullOrBlank()

    fun train() {
        val examples = JSONArray()
        trainingExamples.forEach { example ->
            examples.put(
                JSONObject()
                    .put("question", example.question)
                    .put("answer", example.answer)
            )
        }
        preferences.edit().putString("examples", examples.toString()).apply()
    }

    fun reply(prompt: String): String {
        val storedExamples = preferences.getString("examples", null)
            ?: return "Сначала нажми «Обучить» вверху. Я выучу несколько простых ответов и сохраню их на устройстве."
        val queryWords = words(prompt)
        if (queryWords.isEmpty()) return "Напиши немного подробнее, и я попробую найти подходящий ответ."

        val examples = JSONArray(storedExamples)
        var bestAnswer: String? = null
        var bestScore = 0.0
        for (index in 0 until examples.length()) {
            val example = examples.getJSONObject(index)
            val exampleWords = words(example.getString("question"))
            val overlap = queryWords.intersect(exampleWords).size
            val score = overlap / sqrt((queryWords.size * exampleWords.size).toDouble())
            if (score > bestScore) {
                bestScore = score
                bestAnswer = example.getString("answer")
            }
        }

        return if (bestScore >= 0.25) {
            bestAnswer.orEmpty()
        } else {
            "Я пока знаю только простые примеры. Попробуй спросить о приветствии, ИИ, погоде или попросить шутку."
        }
    }

    private fun words(text: String): Set<String> =
        Regex("[a-zа-яё0-9]+", RegexOption.IGNORE_CASE)
            .findAll(text.lowercase())
            .map { it.value }
            .filter { it.length > 1 }
            .toSet()
}