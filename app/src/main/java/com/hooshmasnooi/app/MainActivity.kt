package com.hooshmasnooi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AssistantScreen() } }
    }
}

data class ChatMessage(val text: String, val fromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen() {
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("سلام! من دستیار هوشمند شما هستم. برای شروع یک پیام بنویسید.", false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("هوش مصنوعی")
                        Text("دستیار شخصی", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "وضعیت اتصال")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("پیام خود را بنویسید...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        messages.add(ChatMessage(text, true))
                        messages.add(
                            ChatMessage(
                                "زیرساخت سرور در حال آماده‌سازی است؛ اتصال واقعی پس از راه‌اندازی Supabase فعال می‌شود.",
                                false
                            )
                        )
                        input = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "ارسال")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Text(message.text, Modifier.padding(14.dp), textAlign = TextAlign.Start)
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("امنیت و وضعیت اتصال") },
            text = {
                Text(
                    "کلید هوش مصنوعی داخل برنامه وارد یا ذخیره نمی‌شود. " +
                        "کلید فقط در Secrets سرور Supabase نگهداری خواهد شد. " +
                        "اتصال واقعی هنوز راه‌اندازی نشده است."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("متوجه شدم") }
            }
        )
    }
}
