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
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.OutlinedButton
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
    private var pendingGmailAuthorizationAction: (() -> Unit)? = null
    private var pendingGmailChatResult: ((String) -> Unit)? = null

    fun connectGmail(afterAuthorization: (() -> Unit)? = null) {
        pendingGmailAuthorizationAction = afterAuthorization ?: { loadRecentGmail() }
        gmailStatus = "در حال درخواست اجازه از Google..."
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/gmail.readonly")))
            .build()
        Identity.getAuthorizationClient(this).authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    gmailStatus = "حساب انتخاب شد؛ در انتظار تأیید مجوز Gmail از Google..."
                    gmailAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                    )
                } else {
                    try {
                        finishGmailAuthorization(result)
                        runPendingGmailAuthorizationAction()
                    } catch (error: Exception) {
                        failGmailAuthorization(error)
                    }
                }
            }
            .addOnFailureListener { error ->
                failGmailAuthorization(error)
            }
    }

    private fun finishGmailAuthorization(result: AuthorizationResult) {
        val token = result.accessToken
        if (token.isNullOrBlank()) {
            throw IllegalStateException(
                "Google نتیجه داد، اما توکن دسترسی Gmail برنگرداند. تنظیم OAuth Android و مجوز gmail.readonly را بررسی کن."
            )
        }
        gmailAccessToken = token
        gmailStatus = "مجوز Gmail با موفقیت دریافت شد."
    }

    private fun runPendingGmailAuthorizationAction() {
        val action = pendingGmailAuthorizationAction
        pendingGmailAuthorizationAction = null
        action?.invoke()
    }

    private fun failGmailAuthorization(error: Exception) {
        pendingGmailAuthorizationAction = null
        val detail = error.localizedMessage ?: error.message ?: error.javaClass.simpleName
        gmailStatus = "اتصال Gmail ناموفق بود: $detail"
        pendingGmailChatResult?.invoke(gmailStatus)
        pendingGmailChatResult = null
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
            pendingGmailChatResult = onResult
            gmailStatus = "در حال بازیابی مجوز Gmail برای بررسی ایمیل‌ها..."
            connectGmail {
                val callback = pendingGmailChatResult
                pendingGmailChatResult = null
                if (callback != null) readGmailForChat(callback)
            }
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
                if (result.resultCode != RESULT_OK) {
                    throw IllegalStateException("پنجره مجوز Google بسته شد یا تأیید نشد (resultCode=${result.resultCode}).")
                }
                val data = result.data
                    ?: throw IllegalStateException("Google نتیجه‌ای به برنامه برنگرداند (Intent خالی بود).")
                val authorization = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
                finishGmailAuthorization(authorization)
                runPendingGmailAuthorizationAction()
            } catch (error: Exception) {
                failGmailAuthorization(error)
            }
        }
        setContent {
            val preferences = getSharedPreferences("atlas_preferences", MODE_PRIVATE)
            var darkThemeEnabled by remember {
                mutableStateOf(preferences.getBoolean("dark_theme", true))
            }
            val appColors = if (darkThemeEnabled) {
                darkColorScheme(
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
            } else {
                lightColorScheme(
                    primary = Color(0xFF5148B8),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE7E4FF),
                    onPrimaryContainer = Color(0xFF201B65),
                    secondary = Color(0xFF087F78),
                    secondaryContainer = Color(0xFFD2F4EF),
                    background = Color(0xFFF5F7FC),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE9EDF5),
                    onSurface = Color(0xFF172033),
                    onSurfaceVariant = Color(0xFF59677D)
                )
            }
            MaterialTheme(colorScheme = appColors) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AtlasApp(
                        gmailStatus = gmailStatus,
                        gmailMessages = gmailMessages,
                        gmailLoading = gmailLoading,
                        onConnectGmail = { connectGmail() },
                        onRefreshGmail = { loadRecentGmail() },
                        onDisconnectGmail = { disconnectGmail() },
                        onReadGmailForChat = { onResult -> readGmailForChat(onResult) },
                        darkThemeEnabled = darkThemeEnabled,
                        onThemeToggle = {
                            darkThemeEnabled = !darkThemeEnabled
                            preferences.edit().putBoolean("dark_theme", darkThemeEnabled).apply()
                        }
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
    onReadGmailForChat: ((String) -> Unit) -> Unit,
    darkThemeEnabled: Boolean,
    onThemeToggle: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    var currentPage by remember { mutableStateOf("chat") }
    var input by remember { mutableStateOf("") }
    var showStatus by remember { mutableStateOf(false) }
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
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF172B43)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(46.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    color = Color(0xFF294B68)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✦", color = Color(0xFF8B83FF), fontSize = 25.sp)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("ATLAS AI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Text("هوشمندی بدون مرز", color = Color(0xFFB8C8DC), fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(15.dp))
                            Text("دستیار هوشمند چندسرویس", color = Color(0xFFEAF1FC), fontWeight = FontWeight.SemiBold)
                            Text("نسخه اولیه • در حال توسعه", color = Color(0xFF9FB4CC), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    atlasPages.forEach { page ->
                        when (page.id) {
                            "chat" -> Text("فضای کاری", modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                            "integrations" -> Text("اتصال و حساب", modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                            "settings" -> Text("تنظیمات و پشتیبانی", modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        }
                        NavigationDrawerItem(
                            label = { Text(page.title, fontWeight = if (currentPage == page.id) FontWeight.SemiBold else FontWeight.Normal) },
                            icon = { Text(page.symbol, fontSize = 18.sp) },
                            selected = currentPage == page.id,
                            onClick = {
                                currentPage = page.id
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF253B59),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                selectedIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Column(Modifier.padding(13.dp)) {
                            Text("وضعیت نسخه", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("نسخه 0.1.0 • امکانات در حال تکمیل هستند.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
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
                    darkThemeEnabled,
                    onThemeToggle,
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
                    Text("زبان رابط فعلی: فارسی")
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
                    listOf(Color(0xFF0A1424), Color(0xFF0D1B2E), Color(0xFF101D30))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF242B55)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "ATLAS AI",
                    tint = Color(0xFFC4BEFF), modifier = Modifier.size(38.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("ATLAS AI  /  PERSONAL INTELLIGENCE", fontSize = 9.sp,
            color = Color(0xFF70DCCE), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("سلام، من اطلس هستم", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = Color(0xFFF2F6FF))
        Spacer(Modifier.height(7.dp))
        Text("یک فضای یکپارچه برای گفتگو، تحقیق و انجام کارهای هوشمند.",
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA9B9CE))
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF172B3C)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Color(0xFFFFC66D), CircleShape))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("نسخه آزمایشی", color = Color(0xFFEAF1FC), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("برخی ابزارها هنوز به سرویس واقعی متصل نیستند",
                        color = Color(0xFF91A5BF), fontSize = 10.sp)
                }
                Text("۰.۱", color = Color(0xFFB8E9E2), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("فضای کاری اطلس", color = Color(0xFFEAF1FC),
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text("ابزارها", color = Color(0xFF8FA4BE), fontSize = 11.sp)
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.ChatBubbleOutline, "گفت‌وگوی هوشمند", "تعامل و پاسخ‌گویی", Color(0xFF9B91FF), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Image, "هوش تصویری", "تحلیل و تولید تصویر", Color(0xFF62B8FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.Movie, "رسانه و ویدیو", "ابزارهای چندرسانه‌ای", Color(0xFFFFB86B), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Mic, "دستیار صوتی", "گفتار و شنیدن پاسخ", Color(0xFF5ED9C8), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.Code, "برنامه‌نویسی", "کدنویسی و عیب‌یابی", Color(0xFF79A9FF), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Description, "تحلیل اسناد", "کار با فایل و متن", Color(0xFFE2A7FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.TravelExplore, "پژوهش وب", "جست‌وجوی منابع", Color(0xFF62D5B2), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Psychology, "تحلیل تخصصی", "حل مسئله و یادگیری", Color(0xFFFFD078), Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text("برای شروع انتخاب کنید", modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFEAF1FC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        listOf(
            Triple("✎", "نگارش و ترجمه", "نوشتن، بازنویسی و ترجمه متن"),
            Triple("⌘", "برنامه‌نویسی", "بررسی کد و یافتن خطاها"),
            Triple("◇", "یادگیری و پژوهش", "بررسی مرحله‌ای یک موضوع")
        ).forEach { option ->
            SuggestionCard(option.first, option.second, option.third) {
                onPromptClick("در زمینه " + option.second + " به من کمک کن.")
            }
            Spacer(Modifier.height(9.dp))
        }
        Text("قابلیت‌های نمایشی تا زمان اتصال و آزمون سرویس مربوطه فعال نیستند.",
            color = Color(0xFF8195AE), fontSize = 10.sp,
            modifier = Modifier.padding(top = 7.dp, bottom = 12.dp))
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
        PageTitle("مرکز اتصال سرویس‌ها", "مدیریت یکپارچه دسترسی‌ها و ارتباط اطلس با برنامه‌های دیگر.")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF172B3D)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF294B59), modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF83E2D2), modifier = Modifier.size(25.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("اتصال‌های حساب", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("دسترسی‌ها فقط با اجازه شما فعال می‌شوند.", color = Color(0xFFB5C6D8), fontSize = 11.sp)
                }
                Text("۰۱ فعال", color = Color(0xFF83E2D2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("سرویس‌های متصل", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(9.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFE8E5), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", color = Color(0xFFB33A2B), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Google Gmail", fontWeight = FontWeight.Bold)
                        Text("خواندن ایمیل‌ها • مجوز فقط‌خواندنی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = if (gmailMessages.isNotEmpty()) Color(0xFFDDF5E9) else Color(0xFFFFF0D8)) {
                        Text(if (gmailMessages.isNotEmpty()) "دریافت موفق" else "نیازمند اتصال",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = if (gmailMessages.isNotEmpty()) Color(0xFF176B45) else Color(0xFF8A5B14),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("اطلس می‌تواند ۸ ایمیل اخیر صندوق ورودی را دریافت کند و فرستنده، موضوع و خلاصه اولیه را نمایش دهد.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(gmailStatus, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onConnectGmail, modifier = Modifier.fillMaxWidth()) {
                    Text("اتصال امن حساب Google")
                }
                OutlinedButton(onClick = onRefreshGmail, enabled = !gmailLoading && gmailMessages.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (gmailLoading) "در حال دریافت..." else "همگام‌سازی دوباره")
                }
                TextButton(onClick = onDisconnectGmail, modifier = Modifier.fillMaxWidth()) {
                    Text("قطع دسترسی در این نشست")
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("فعالیت اخیر Gmail", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(9.dp))
        if (gmailMessages.isEmpty()) {
            EmptyCard("هنوز ایمیلی دریافت نشده", "پس از اتصال Google و تأیید مجوز، ایمیل‌های اخیر اینجا نمایش داده می‌شوند.")
        } else {
            gmailMessages.forEachIndexed { index, item ->
                InfoCard("ایمیل " + (index + 1), item)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("سرویس‌های قابل توسعه", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(9.dp))
        listOf(
            "GitHub" to "مخزن‌ها و ابزارهای توسعه • اتصال در مرحله بعد",
            "Google Drive" to "فایل‌ها و اسناد • اتصال در مرحله بعد",
            "Google Calendar" to "رویدادها و برنامه‌ریزی • اتصال در مرحله بعد",
            "Supabase" to "زیرساخت داده و حساب • هنوز متصل نشده"
        ).forEach { item ->
            InfoCard(item.first, item.second)
            Spacer(Modifier.height(8.dp))
        }
        InfoCard("حریم خصوصی Gmail", "فقط مجوز gmail.readonly درخواست می‌شود. اطلس نمی‌تواند ایمیل ارسال یا حذف کند. توکن دسترسی فقط در حافظه همین اجرای برنامه است و در GitHub یا دیتابیس ذخیره نمی‌شود. برای لغو کامل، مجوز ATLAS AI را از حساب Google حذف کن.")
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
        PageTitle("حساب کاربری", "مدیریت هویت، نشست و اطلاعات شخصی در یک مکان.")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF172B43)
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF294B68), modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("م", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("حساب مهمان", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("ذخیره‌سازی گفتگوها روی همین دستگاه", color = Color(0xFFB8C8DC), fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("ورود آنلاین هنوز فعال نشده", color = Color(0xFF83E2D2), fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("مدیریت حساب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard("اطلاعات نمایه", "نام نمایشی و تصویر حساب پس از راه‌اندازی حساب امن قابل تنظیم خواهد بود.")
        Spacer(Modifier.height(8.dp))
        InfoCard("امنیت و نشست‌ها", "ورود، خروج و مدیریت نشست‌ها هنوز پیاده‌سازی نشده‌اند.")
        Spacer(Modifier.height(8.dp))
        InfoCard("داده‌ها و حریم خصوصی", "گفتگوهای فعلی در فضای خصوصی برنامه ذخیره می‌شوند؛ ابزار خروجی‌گرفتن و حذف کامل داده‌ها در دست توسعه است.")
        Spacer(Modifier.height(8.dp))
        InfoCard("همگام‌سازی ابری", "فعلاً غیرفعال است؛ اطلاعات دستگاه به سرور ارسال نمی‌شود.")
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
    darkThemeEnabled: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("تنظیمات", "کنترل تجربه ظاهری و ترجیحات برنامه.")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ظاهر برنامه", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (darkThemeEnabled) "حالت تاریک فعال است" else "حالت روشن فعال است",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text(if (darkThemeEnabled) "تاریک" else "روشن", color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onThemeToggle, modifier = Modifier.fillMaxWidth()) {
            Text(if (darkThemeEnabled) "فعال‌کردن حالت روشن" else "فعال‌کردن حالت تاریک")
        }
        Spacer(Modifier.height(18.dp))
        Text("زبان و منطقه", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard("زبان رابط", "فارسی در نسخه فعلی فعال است. ترجمه کامل انگلیسی و دری پیش از انتشار اضافه و آزمایش می‌شود.")
        Spacer(Modifier.height(12.dp))
        Text("تجربه و دسترس‌پذیری", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard("دسترس‌پذیری", "بهینه‌سازی صفحه‌خوان، اندازه متن و کنتراست در مرحله کنترل کیفیت قرار دارد.")
        Spacer(Modifier.height(8.dp))
        InfoCard("اعلان‌ها", "اعلان‌های حساب و همگام‌سازی پس از راه‌اندازی سرویس حساب فعال می‌شوند.")
        Spacer(Modifier.height(8.dp))
        InfoCard("ذخیره‌سازی", "گفتگوها فعلاً فقط در فضای خصوصی همین دستگاه ذخیره می‌شوند.")
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
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = if (message.fromUser) Alignment.Start else Alignment.End
        ) {
            Text(
                if (message.fromUser) "شما" else "ATLAS AI",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomEnd = if (message.fromUser) 5.dp else 18.dp,
                    bottomStart = if (message.fromUser) 18.dp else 5.dp
                ),
                color = if (message.fromUser) Color(0xFF29345B)
                    else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    message.text,
                    modifier = Modifier.padding(15.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
