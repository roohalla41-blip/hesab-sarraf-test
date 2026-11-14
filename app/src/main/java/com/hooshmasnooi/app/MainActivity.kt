package com.hooshmasnooi.app

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.data.local.ChatStorage

private fun isGmailCheckRequest(text: String): Boolean {
    val normalized = text.lowercase()
    val mentionsGmail = listOf("gmail", "جیمیل", "ایمیل", "نامه", "صندوق ورودی").any { normalized.contains(it) }
    val asksToCheck = listOf("بررسی", "چک", "بخوان", "بخون", "خلاصه", "جدید", "دریافت", "ببین", "نگاه", "وارسی", "check", "read", "review").any { normalized.contains(it) }
    return mentionsGmail && asksToCheck
}

private data class AtlasPage(val id: String, val title: String, val symbol: String)

private val atlasPages = listOf(
    AtlasPage("chat", "گفت‌وگو", "✦"),
    AtlasPage("history", "تاریخچه", "◷"),
    AtlasPage("explore", "کاوش دستیارها", "◇"),
    AtlasPage("tools", "ابزارها", "⌘"),
    AtlasPage("integrations", "اتصال برنامه‌ها", "⇄"),
    AtlasPage("profile", "حساب کاربری", "◎"),
    AtlasPage("plans", "طرح‌های اشتراک", "♧"),
    AtlasPage("settings", "تنظیمات", "⚙"),
    AtlasPage("privacy", "حریم خصوصی و امنیت", "▣"),
    AtlasPage("help", "راهنما و پشتیبانی", "?")
)

class MainActivity : ComponentActivity() {
    private lateinit var gmailAuthorizationLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val gmailScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var gmailStatus by mutableStateOf("حساب Gmail هنوز متصل نشده است.")
        private set
    var gmailMessages by mutableStateOf<List<String>>(emptyList())
        private set
    var gmailLoading by mutableStateOf(false)
        private set
    private var gmailAccessToken: String? = null

    fun connectGmail() {
        gmailStatus = "در حال درخواست اجازه از Google..."
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/gmail.readonly")))
            .build()
        Identity.getAuthorizationClient(this).authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    gmailAuthorizationLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build())
                } else {
                    finishGmailAuthorization(result)
                }
            }
            .addOnFailureListener { error ->
                gmailStatus = "اتصال Gmail ناموفق بود: ${error.localizedMessage ?: "خطای نامشخص"}"
            }
    }

    private fun finishGmailAuthorization(result: AuthorizationResult) {
        val token = result.accessToken
        if (token.isNullOrBlank()) {
            gmailStatus = "مجوز Gmail دریافت نشد."
            return
        }
        gmailAccessToken = token
        loadRecentGmail()
    }

    fun loadRecentGmail() {
        val token = gmailAccessToken
        if (token.isNullOrBlank()) {
            gmailStatus = "ابتدا حساب Gmail را متصل کن."
            return
        }
        gmailLoading = true
        gmailStatus = "در حال دریافت ایمیل‌های اخیر..."
        gmailScope.launch {
            try {
                gmailMessages = GmailApi.latestInbox(token)
                gmailStatus = if (gmailMessages.isEmpty()) "اتصال موفق است؛ ایمیلی در صندوق ورودی پیدا نشد."
                    else "اتصال موفق است؛ ${gmailMessages.size} ایمیل اخیر دریافت شد."
            } catch (error: Exception) {
                gmailStatus = "دریافت ایمیل ناموفق بود: ${error.localizedMessage ?: "خطای شبکه یا مجوز"}"
            } finally {
                gmailLoading = false
            }
        }
    }

    fun readGmailForChat(onResult: (String) -> Unit) {
        val token = gmailAccessToken
        if (token.isNullOrBlank()) {
            onResult("برای بررسی ایمیل‌ها، ابتدا از منوی «اتصال برنامه‌ها» حساب Gmail را متصل کن.")
            return
        }
        gmailLoading = true
        gmailStatus = "در حال بررسی Gmail از داخل گفت‌وگو..."
        gmailScope.launch {
            try {
                val inbox = GmailApi.latestInbox(token)
                gmailMessages = inbox
                gmailStatus = if (inbox.isEmpty()) {
                    "اتصال موفق است؛ ایمیلی در صندوق ورودی پیدا نشد."
                } else {
                    "اتصال موفق است؛ ${inbox.size} ایمیل دریافت شد."
                }
                val answer = if (inbox.isEmpty()) {
                    "صندوق ورودی Gmail بررسی شد؛ ایمیل تازه‌ای برای نمایش پیدا نکردم."
                } else {
                    buildString {
                        append("صندوق ورودی Gmail بررسی شد. ")
                        append(inbox.size)
                        append(" ایمیل اخیر دریافت کردم:\n\n")
                        inbox.forEachIndexed { index, item ->
                            append(index + 1)
                            append(") ")
                            append(item)
                            append("\n\n")
                        }
                        append("این بررسی فقط خواندنی بود؛ هیچ ایمیلی ارسال یا حذف نشد.")
                    }
                }
                onResult(answer)
            } catch (error: Exception) {
                val detail = error.localizedMessage ?: "خطای شبکه یا مجوز Google"
                gmailStatus = "بررسی Gmail ناموفق بود: $detail"
                onResult("نتوانستم ایمیل‌ها را بخوانم: $detail\nاگر مجوز منقضی شده یا لغو شده است، از بخش اتصال برنامه‌ها دوباره Gmail را متصل کن.")
            } finally {
                gmailLoading = false
            }
        }
    }

    fun disconnectGmail() {
        gmailAccessToken = null
        gmailMessages = emptyList()
        gmailStatus = "توکن موقت از حافظه برنامه پاک شد. برای لغو کامل مجوز، دسترسی ATLAS AI را از تنظیمات حساب Google حذف کن."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gmailAuthorizationLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val authorization = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data)
                finishGmailAuthorization(authorization)
            } catch (error: Exception) {
                gmailStatus = "مجوز Gmail تکمیل نشد: ${error.localizedMessage ?: "لغو یا خطای Google"}"
            }
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF8B83FF),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFF292A55),
                    onPrimaryContainer = Color(0xFFE9E7FF),
                    secondary = Color(0xFF55D6C7),
                    secondaryContainer = Color(0xFF173C42),
                    background = Color(0xFF091321),
                    surface = Color(0xFF101D30),
                    surfaceVariant = Color(0xFF192A40),
                    onSurface = Color(0xFFEAF1FC),
                    onSurfaceVariant = Color(0xFF9AAEC5)
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AtlasApp(
                    gmailStatus = gmailStatus,
                    gmailMessages = gmailMessages,
                    gmailLoading = gmailLoading,
                    onConnectGmail = { connectGmail() },
                    onRefreshGmail = { loadRecentGmail() },
                    onDisconnectGmail = { disconnectGmail() },
                    onReadGmailForChat = { onResult -> readGmailForChat(onResult) }
                )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasApp(
    gmailStatus: String,
    gmailMessages: List<String>,
    gmailLoading: Boolean,
    onConnectGmail: () -> Unit,
    onRefreshGmail: () -> Unit,
    onDisconnectGmail: () -> Unit,
    onReadGmailForChat: ((String) -> Unit) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    var currentPage by remember { mutableStateOf("chat") }
    var input by remember { mutableStateOf("") }
    var showStatus by remember { mutableStateOf(false) }
    var darkPreview by remember { mutableStateOf(false) }
    var languagePreview by remember { mutableStateOf("فارسی") }
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(ChatStorage.load(appContext)) } }
    var nextId by remember { mutableStateOf((messages.maxOfOrNull { it.id } ?: 0L) + 1L) }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun sendMessage(rawText: String) {
        val cleanText = rawText.trim()
        if (cleanText.isNotEmpty()) {
            messages.add(ChatMessage(nextId++, cleanText, true))
            ChatStorage.save(appContext, messages)
            input = ""
            if (isGmailCheckRequest(cleanText)) {
                val pendingId = nextId++
                messages.add(ChatMessage(pendingId, "در حال بررسی صندوق ورودی Gmail...", false))
                ChatStorage.save(appContext, messages)
                onReadGmailForChat { answer ->
                    val pendingIndex = messages.indexOfFirst { it.id == pendingId }
                    if (pendingIndex >= 0) {
                        messages[pendingIndex] = ChatMessage(pendingId, answer, false)
                    } else {
                        messages.add(ChatMessage(nextId++, answer, false))
                    }
                    ChatStorage.save(appContext, messages)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(16.dp)) {
                    Text("ATLAS AI", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text("هوشمندی بدون مرز", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    atlasPages.forEach { page ->
                        NavigationDrawerItem(
                            label = { Text(page.title) },
                            icon = { Text(page.symbol, fontSize = 18.sp) },
                            selected = currentPage == page.id,
                            onClick = {
                                currentPage = page.id
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "گفت‌وگوی ذخیره‌شده در فضای برنامه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("ATLAS AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                atlasPages.firstOrNull { it.id == currentPage }?.title ?: "اطلس",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "منو")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            messages.clear()
                            ChatStorage.clear(appContext)
                            input = ""
                            currentPage = "chat"
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "گفت‌وگوی جدید")
                        }
                        TextButton(onClick = { showStatus = true }) { Text("وضعیت") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                if (currentPage == "chat") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("پیام خود را بنویسید...") },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { sendMessage(input) })
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { sendMessage(input) },
                                enabled = input.isNotBlank(),
                                modifier = Modifier.size(52.dp).background(
                                    if (input.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "ارسال پیام",
                                    tint = if (input.isNotBlank()) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "برای بررسی ایمیل، Gmail را از بخش «اتصال برنامه‌ها» وصل کن؛ سپس در همین گفت‌وگو بنویس «ایمیل‌های جدیدم را بررسی کن».",
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        ) { padding ->
            when (currentPage) {
                "chat" -> ChatPage(messages, listState, Modifier.fillMaxSize().padding(padding)) {
                    sendMessage(it)
                }
                "history" -> HistoryPage(messages, Modifier.fillMaxSize().padding(padding))
                "explore" -> FeaturePage(
                    "کاوش دستیارها",
                    "حالت‌های کاری تخصصی برای نیازهای مختلف.",
                    listOf(
                        "نویسنده و ویراستار" to "نگارش، بازنویسی و خلاصه‌سازی",
                        "مربی یادگیری" to "آموزش مرحله‌ای و تمرین",
                        "همراه برنامه‌نویسی" to "توضیح کد و عیب‌یابی",
                        "پژوهشگر" to "تحلیل موضوع و طراحی تحقیق",
                        "دستیار کسب‌وکار" to "برنامه‌ریزی و تحلیل فرایند"
                    ),
                    Modifier.fillMaxSize().padding(padding)
                )
                "tools" -> FeaturePage(
                    "ابزارهای اطلس",
                    "ابزارها پس از پیاده‌سازی و اتصال امن فعال می‌شوند.",
                    listOf(
                        "تحلیل فایل و سند" to "دریافت و خلاصه‌سازی اسناد",
                        "درک تصویر" to "پرسش درباره تصاویر",
                        "جست‌وجوی وب" to "تحقیق همراه منابع",
                        "ورودی و خروجی صوت" to "گفتار به متن و خواندن پاسخ",
                        "محیط کدنویسی" to "توضیح و بررسی کد"
                    ),
                    Modifier.fillMaxSize().padding(padding)
                )
                "integrations" -> IntegrationsPage(
                    gmailStatus, gmailMessages, gmailLoading,
                    onConnectGmail, onRefreshGmail, onDisconnectGmail,
                    Modifier.fillMaxSize().padding(padding)
                )
                "profile" -> ProfilePage(Modifier.fillMaxSize().padding(padding))
                "plans" -> PlansPage(Modifier.fillMaxSize().padding(padding))
                "settings" -> SettingsPage(
                    darkPreview,
                    { darkPreview = !darkPreview },
                    languagePreview,
                    { languagePreview = it },
                    Modifier.fillMaxSize().padding(padding)
                )
                "privacy" -> FeaturePage(
                    "حریم خصوصی و امنیت",
                    "اصول لازم پیش از اتصال سرویس‌ها.",
                    listOf(
                        "کلیدهای محرمانه" to "فقط در Secret Manager سمت سرور نگهداری می‌شوند.",
                        "حداقل‌سازی داده" to "فقط داده ضروری جمع‌آوری می‌شود.",
                        "کنترل کاربر" to "مدیریت و حذف داده‌ها در طراحی لحاظ می‌شود.",
                        "ارتباط امن" to "احراز هویت و محدودیت درخواست در سمت سرور."
                    ),
                    Modifier.fillMaxSize().padding(padding)
                )
                else -> HelpPage(Modifier.fillMaxSize().padding(padding))
            }
        }
    }

    if (showStatus) {
        AlertDialog(
            onDismissRequest = { showStatus = false },
            title = { Text("وضعیت ATLAS AI") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("رابط اصلی Android: در حال توسعه")
                    Text("زبان پیش‌نمایش: " + languagePreview)
                    Text("هوش مصنوعی: هنوز متصل نشده")
                    Text("دیتابیس و حساب آنلاین: هنوز متصل نشده")
                    Text("کلید محرمانه داخل گوشی یا GitHub قرار نمی‌گیرد")
                    Text("گفت‌وگو: ذخیره محلی فعال؛ اتصال هوش مصنوعی و آزمون نهایی باقی است.")
                }
            },
            confirmButton = { TextButton(onClick = { showStatus = false }) { Text("بستن") } }
        )
    }
}

@Composable
private fun ChatPage(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier,
    onPromptClick: (String) -> Unit
) {
    if (messages.isEmpty()) {
        WelcomeContent(modifier, onPromptClick)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
        }
    }
}

@Composable
private fun WelcomeContent(modifier: Modifier = Modifier, onPromptClick: (String) -> Unit) {
    Column(
        modifier = modifier
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF0D1A2B), Color(0xFF091321), Color(0xFF101A31))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = RoundedCornerShape(27.dp),
            color = Color(0xFF292A55)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "نشان اطلس",
                    tint = Color(0xFFB9B2FF),
                    modifier = Modifier.size(43.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("مرکز فرماندهی اطلس", fontSize = 12.sp, color = Color(0xFF70DCCE), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Text("سلام، من اطلس هستم", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Color(0xFFF2F6FF))
        Spacer(Modifier.height(8.dp))
        Text(
            "یک فضای هوشمند برای گفتگو، خلق محتوا، یادگیری و ساختن ایده‌ها.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFA5B6CC)
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFF173B42),
            modifier = Modifier.padding(bottom = 22.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(Color(0xFFFFC66D), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("نسخه ساختاری • اتصال سرویس‌ها در حال آماده‌سازی",
                    color = Color(0xFFB8E9E2), fontSize = 10.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.ChatBubbleOutline, "چت هوشمند", "گفتگو و پاسخ‌گویی", Color(0xFF8F86FF), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Image, "ساخت تصویر", "خلق تصویر با هوش مصنوعی", Color(0xFF62B8FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.Movie, "ساخت ویدیو", "تبدیل ایده به ویدیو", Color(0xFFFFB86B), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Mic, "دستیار صوتی", "گفتار و شنیدن پاسخ", Color(0xFF5ED9C8), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.Code, "برنامه‌نویسی", "کدنویسی و عیب‌یابی", Color(0xFF79A9FF), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Description, "تحلیل اسناد", "فایل و محتوای متنی", Color(0xFFE2A7FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.TravelExplore, "تحقیق وب", "جستجو و بررسی منابع", Color(0xFF62D5B2), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Psychology, "تحلیل تخصصی", "یادگیری و حل مسئله", Color(0xFFFFD078), Modifier.weight(1f))
        }

        Spacer(Modifier.height(23.dp))
        Text("شروع سریع", modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFEAF1FC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        listOf(
            Triple("✎", "نوشتن و ترجمه", "متن حرفه‌ای بنویس یا ترجمه کن"),
            Triple("⌘", "برنامه‌نویسی", "کد را بررسی و خطاها را پیدا کن"),
            Triple("◇", "یادگیری و تحقیق", "یک موضوع را مرحله‌به‌مرحله بررسی کن")
        ).forEach { option ->
            SuggestionCard(option.first, option.second, option.third) {
                onPromptClick("می‌خواهم در بخش " + option.second + " کمک بگیرم.")
            }
            Spacer(Modifier.height(9.dp))
        }
        Text("قابلیت‌های نمایشی تا زمان اتصال و آزمایش سرویس مربوطه فعال نیستند.",
            color = Color(0xFF8195AE), fontSize = 10.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
    }
}

@Composable
private fun AtlasToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14243A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(11.dp), color = tint.copy(alpha = 0.14f)) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("آماده‌سازی", fontSize = 8.sp, color = Color(0xFF91A5BF))
            }
            Column {
                Text(title, color = Color(0xFFEAF1FC), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(subtitle, color = Color(0xFF91A5BF), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SuggestionCard(icon: String, title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 20.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
private fun IntegrationsPage(
    gmailStatus: String,
    gmailMessages: List<String>,
    gmailLoading: Boolean,
    onConnectGmail: () -> Unit,
    onRefreshGmail: () -> Unit,
    onDisconnectGmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("اتصال برنامه‌ها", "اتصال واقعی Gmail با مجوز فقط‌خواندنی در این نسخه آماده آزمایش است.")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gmail", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text(if (gmailMessages.isNotEmpty()) "داده دریافت شد" else "اتصال آزمایشی",
                        color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("اطلس می‌تواند ۸ ایمیل اخیر صندوق ورودی را بخواند و موضوع، فرستنده و خلاصه اولیه آن‌ها را نمایش دهد.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(gmailStatus, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onConnectGmail, modifier = Modifier.fillMaxWidth()) {
                    Text("اتصال حساب Gmail با Google")
                }
                Button(onClick = onRefreshGmail, enabled = !gmailLoading && gmailMessages.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (gmailLoading) "در حال دریافت..." else "دریافت دوباره ایمیل‌ها")
                }
                TextButton(onClick = onDisconnectGmail, modifier = Modifier.fillMaxWidth()) {
                    Text("پاک‌کردن اتصال از این نشست")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("ایمیل‌های اخیر", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (gmailMessages.isEmpty()) {
            EmptyCard("هنوز ایمیلی دریافت نشده", "پس از اتصال Google و تأیید مجوز، ایمیل‌های اخیر اینجا نمایش داده می‌شوند.")
        } else {
            gmailMessages.forEachIndexed { index, item ->
                InfoCard("ایمیل " + (index + 1), item)
                Spacer(Modifier.height(8.dp))
            }
        }
        InfoCard("مجوز و حریم خصوصی", "فقط gmail.readonly درخواست می‌شود. اطلس نمی‌تواند ایمیل ارسال یا حذف کند. توکن دسترسی فقط در حافظه همین اجرای برنامه است و در GitHub یا دیتابیس ذخیره نمی‌شود. برای لغو کامل، مجوز ATLAS AI را از حساب Google حذف کن.")
        InfoCard("سایر سرویس‌ها", "GitHub، Google Drive، Supabase و Calendar در مرحله بعدی اتصال واقعی می‌شوند.")
    }
}

@Composable
private fun HistoryPage(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("تاریخچه گفت‌وگوها", "پیام‌های این دستگاه از فضای خصوصی برنامه خوانده می‌شوند.")
        if (messages.isEmpty()) {
            EmptyCard("هنوز گفت‌وگویی وجود ندارد", "با شروع یک گفت‌وگو، پیام‌های این نشست اینجا دیده می‌شوند.")
        } else {
            messages.forEach { message ->
                InfoCard("پیام " + message.id, message.text)
                Spacer(Modifier.height(8.dp))
            }
        }
        Text("ذخیره محلی فعال است؛ همگام‌سازی ابری هنوز فعال نیست.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FeaturePage(
    title: String,
    description: String,
    features: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(title, description)
        features.forEach { feature ->
            InfoCard(feature.first, feature.second)
            Spacer(Modifier.height(10.dp))
        }
        Text("این بخش در مرحله ساختار است و قابلیت عملیاتی آن هنوز فعال نشده است.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProfilePage(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("حساب کاربری", "نمایش ساختار حساب؛ ثبت‌نام آنلاین هنوز پیاده‌سازی نشده است.")
        InfoCard("کاربر مهمان", "بدون ورود • حساب ابری ایجاد نشده")
        InfoCard("اطلاعات پروفایل", "نام نمایشی، تصویر و مشخصات حساب در مرحله بعد")
        InfoCard("ورود و امنیت", "مدیریت نشست و احراز هویت پس از انتخاب سرویس امن")
        InfoCard("داده‌های من", "دریافت و حذف داده‌ها پس از طراحی ذخیره‌سازی")
    }
}

@Composable
private fun PlansPage(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("طرح‌های اشتراک", "طرح‌ها نمایشی هستند؛ قیمت و پرداخت هنوز تعریف نشده است.")
        listOf(
            "رایگان" to "گفت‌وگوی پایه و سهمیه محدود پس از اتصال",
            "پلاس" to "سهمیه بیشتر و مدل‌های پیشرفته‌تر",
            "پرو" to "سقف مصرف بالاتر و ابزارهای تخصصی"
        ).forEach { plan ->
            InfoCard(plan.first, plan.second + " • هنوز فعال نیست")
            Spacer(Modifier.height(10.dp))
        }
        Text("هیچ پرداختی انجام نمی‌شود و درگاه پرداخت متصل نیست.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingsPage(
    darkPreview: Boolean,
    onThemeToggle: () -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("تنظیمات", "گزینه‌های پایه برای آماده‌سازی تجربه کاربری.")
        InfoCard("ظاهر برنامه", if (darkPreview) "حالت تاریک انتخاب شده (پیش‌نمایش)" else "حالت روشن انتخاب شده (پیش‌نمایش)")
        Button(onClick = onThemeToggle, modifier = Modifier.fillMaxWidth()) { Text("تغییر حالت نمایشی") }
        Spacer(Modifier.height(12.dp))
        InfoCard("زبان رابط", "زبان فعلی: " + language)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onLanguageChange("فارسی") }) { Text("فارسی") }
            Button(onClick = { onLanguageChange("English (planned)") }) { Text("English") }
            Button(onClick = { onLanguageChange("دری (planned)") }) { Text("دری") }
        }
        Spacer(Modifier.height(12.dp))
        InfoCard("دسترس‌پذیری", "خوانایی، کنتراست و پشتیبانی صفحه‌خوان در مرحله تکمیل")
        InfoCard("اعلان‌ها", "تنظیم اعلان‌ها پس از ایجاد حساب و سرویس آنلاین")
    }
}

@Composable
private fun HelpPage(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("راهنما و پشتیبانی", "راهنمای استفاده از اطلس AI.")
        InfoCard("آیا اطلس پاسخ هوش مصنوعی می‌دهد؟", "خیر؛ موتور هوش مصنوعی هنوز متصل نشده است.")
        InfoCard("آیا برای دیدن ساختار کلید لازم است؟", "خیر؛ طراحی و نمایش صفحات بدون کلید انجام می‌شود.")
        InfoCard("آیا داده‌ها ذخیره می‌شوند؟", "پیام‌های گفت‌وگو به‌صورت محلی و خصوصی روی همین دستگاه ذخیره می‌شوند.")
        InfoCard("پشتیبانی", "راه ارتباطی رسمی در زمان آماده‌سازی انتشار اضافه خواهد شد.")
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("◷", fontSize = 28.sp)
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = if (message.fromUser) 5.dp else 18.dp,
                bottomStart = if (message.fromUser) 18.dp else 5.dp
            ),
            color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(message.text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
