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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hooshmasnooi.app.domain.model.ChatMessage
import com.hooshmasnooi.app.core.AtlasText
import com.hooshmasnooi.app.core.AtlasConfig
import com.hooshmasnooi.app.data.remote.AccessTokenProvider
import com.hooshmasnooi.app.data.remote.AiFailure
import com.hooshmasnooi.app.data.remote.AiGatewayResult
import com.hooshmasnooi.app.data.remote.SupabaseChatGateway
import com.hooshmasnooi.app.core.LocalePolicy
import com.hooshmasnooi.app.data.local.ChatStorage
import com.hooshmasnooi.app.data.local.StoredConversation

private fun isGmailCheckRequest(text: String): Boolean {
    val normalized = text.lowercase()
    val mentionsGmail = listOf("gmail", "جیمیل", "ایمیل", "نامه", "صندوق ورودی").any { normalized.contains(it) }
    val asksToCheck = listOf("بررسی", "چک", "بخوان", "بخون", "خلاصه", "جدید", "دریافت", "ببین", "نگاه", "وارسی", "check", "read", "review").any { normalized.contains(it) }
    return mentionsGmail && asksToCheck
}

private const val MAX_VISIBLE_MESSAGES = 300

private data class AtlasPage(val id: String, val title: String, val symbol: String)

private val atlasPages = listOf(
    AtlasPage("chat", AtlasText.of("گفت‌وگو", "Chat"), "✦"),
    AtlasPage("history", AtlasText.of("تاریخچه", "History"), "◷"),
    AtlasPage("explore", AtlasText.of("کاوش دستیارها", "Explore assistants"), "◇"),
    AtlasPage("tools", AtlasText.of("ابزارها", "Tools"), "⌘"),
    AtlasPage("integrations", AtlasText.of("اتصال برنامه‌ها", "Connected apps"), "⇄"),
    AtlasPage("profile", AtlasText.of("حساب کاربری", "Account"), "◎"),
    AtlasPage("plans", AtlasText.of("طرح‌های اشتراک", "Plans"), "♧"),
    AtlasPage("settings", AtlasText.of("تنظیمات", "Settings"), "⚙"),
    AtlasPage("privacy", AtlasText.of("حریم خصوصی و امنیت", "Privacy & security"), "▣"),
    AtlasPage("help", AtlasText.of("راهنما و پشتیبانی", "Help & support"), "?")
)

class MainActivity : ComponentActivity() {
    private lateinit var gmailAuthorizationLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val gmailScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var gmailStatus by mutableStateOf(AtlasText.of("حساب Gmail هنوز متصل نشده است.", "Gmail is not connected yet."))
        private set
    var gmailMessages by mutableStateOf<List<String>>(emptyList())
        private set
    var gmailLoading by mutableStateOf(false)
        private set
    var gmailConnected by mutableStateOf(false)
        private set
    private var gmailAccessToken: String? = null
    private var pendingGmailAuthorizationAction: (() -> Unit)? = null
    private var pendingGmailChatResult: ((String) -> Unit)? = null

    fun connectGmail(afterAuthorization: (() -> Unit)? = null) {
        pendingGmailAuthorizationAction = afterAuthorization ?: { loadRecentGmail() }
        gmailStatus = AtlasText.of("در حال درخواست اجازه از Google...", "Requesting permission from Google...")
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/gmail.readonly")))
            .build()
        Identity.getAuthorizationClient(this).authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    gmailStatus = AtlasText.of("حساب انتخاب شد؛ در انتظار تأیید مجوز Gmail از Google...", "Account selected; waiting for Gmail permission approval...")
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
                AtlasText.of("Google نتیجه داد، اما توکن دسترسی Gmail برنگرداند. تنظیم OAuth Android و مجوز gmail.readonly را بررسی کن.", "Google returned no Gmail access token. Check the Android OAuth client and gmail.readonly permission.")
            )
        }
        gmailAccessToken = token
        gmailConnected = true
        gmailStatus = AtlasText.of("مجوز Gmail با موفقیت دریافت شد.", "Gmail permission granted successfully.")
    }

    private fun runPendingGmailAuthorizationAction() {
        val action = pendingGmailAuthorizationAction
        pendingGmailAuthorizationAction = null
        action?.invoke()
    }

    private fun failGmailAuthorization(error: Exception) {
        pendingGmailAuthorizationAction = null
        gmailConnected = false
        gmailAccessToken = null
        val detail = error.localizedMessage ?: error.message ?: error.javaClass.simpleName
        gmailStatus = AtlasText.of("اتصال Gmail ناموفق بود: $detail", "Gmail connection failed: $detail")
        pendingGmailChatResult?.invoke(gmailStatus)
        pendingGmailChatResult = null
    }

    fun loadRecentGmail() {
        val token = gmailAccessToken
        if (token.isNullOrBlank()) {
            gmailStatus = AtlasText.of("ابتدا حساب Gmail را متصل کن.", "Connect a Gmail account first.")
            return
        }
        gmailLoading = true
        gmailStatus = AtlasText.of("در حال دریافت ایمیل‌های اخیر...", "Fetching recent emails...")
        gmailScope.launch {
            try {
                gmailMessages = GmailApi.latestInbox(token)
                gmailStatus = if (gmailMessages.isEmpty()) AtlasText.of("اتصال موفق است؛ ایمیلی در صندوق ورودی پیدا نشد.", "Connected successfully; no inbox messages were found.")
                    else AtlasText.of("اتصال موفق است؛ ${gmailMessages.size} ایمیل اخیر دریافت شد.", "Connected successfully; retrieved ${gmailMessages.size} recent emails.")
            } catch (error: Exception) {
                gmailStatus = AtlasText.of("دریافت ایمیل ناموفق بود: ${error.localizedMessage ?: "خطای شبکه یا مجوز"}", "Could not retrieve emails: ${error.localizedMessage ?: "network or permission error"}")
            } finally {
                gmailLoading = false
            }
        }
    }

    fun readGmailForChat(onResult: (String) -> Unit) {
        val token = gmailAccessToken
        if (token.isNullOrBlank()) {
            pendingGmailChatResult = onResult
            gmailStatus = AtlasText.of("در حال بازیابی مجوز Gmail برای بررسی ایمیل‌ها...", "Restoring Gmail permission to check messages...")
            connectGmail {
                val callback = pendingGmailChatResult
                pendingGmailChatResult = null
                if (callback != null) readGmailForChat(callback)
            }
            return
        }
        gmailLoading = true
        gmailStatus = AtlasText.of("در حال بررسی Gmail از داخل گفت‌وگو...", "Checking Gmail from chat...")
        gmailScope.launch {
            try {
                val inbox = GmailApi.latestInbox(token)
                gmailMessages = inbox
                gmailStatus = if (inbox.isEmpty()) {
                    AtlasText.of("اتصال موفق است؛ ایمیلی در صندوق ورودی پیدا نشد.", "Connected successfully; no inbox messages were found.")
                } else {
                    AtlasText.of("اتصال موفق است؛ ${inbox.size} ایمیل دریافت شد.", "Connected successfully; retrieved ${inbox.size} emails.")
                }
                val answer = if (inbox.isEmpty()) {
                    AtlasText.of("صندوق ورودی Gmail بررسی شد؛ ایمیل تازه‌ای برای نمایش پیدا نکردم.", "Gmail inbox checked; there are no recent messages to show.")
                } else {
                    buildString {
                        append(AtlasText.of("صندوق ورودی Gmail بررسی شد. ", "Gmail inbox checked. "))
                        append(inbox.size)
                        append(AtlasText.of(" ایمیل اخیر دریافت کردم:\n\n", " recent emails were retrieved:\n\n"))
                        inbox.forEachIndexed { index, item ->
                            append(index + 1)
                            append(") ")
                            append(item)
                            append("\n\n")
                        }
                        append(AtlasText.of("این بررسی فقط خواندنی بود؛ هیچ ایمیلی ارسال یا حذف نشد.", "This check was read-only; no emails were sent or deleted."))
                    }
                }
                onResult(answer)
            } catch (error: Exception) {
                val detail = error.localizedMessage ?: AtlasText.of("خطای شبکه یا مجوز Google", "Network or Google permission error")
                gmailStatus = AtlasText.of("بررسی Gmail ناموفق بود: $detail", "Gmail check failed: $detail")
                onResult(AtlasText.of("نتوانستم ایمیل‌ها را بخوانم: $detail\nاگر مجوز منقضی شده یا لغو شده است، از بخش اتصال برنامه‌ها دوباره Gmail را متصل کن.", "I couldn't read your emails: $detail\nIf permission expired or was revoked, reconnect Gmail from Connected apps."))
            } finally {
                gmailLoading = false
            }
        }
    }

    fun disconnectGmail() {
        gmailAccessToken = null
        gmailConnected = false
        gmailMessages = emptyList()
        gmailStatus = AtlasText.of("توکن موقت از حافظه برنامه پاک شد. برای لغو کامل مجوز، دسترسی ATLAS AI را از تنظیمات حساب Google حذف کن.", "The temporary token was cleared from memory. To revoke access completely, remove ATLAS AI from your Google Account security settings.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gmailAuthorizationLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                if (result.resultCode != RESULT_OK) {
                    throw IllegalStateException(AtlasText.of("پنجره مجوز Google بسته شد یا تأیید نشد (resultCode=${result.resultCode}).", "The Google permission window was closed or not approved (resultCode=${result.resultCode})."))
                }
                val data = result.data
                    ?: throw IllegalStateException(AtlasText.of("Google نتیجه‌ای به برنامه برنگرداند (Intent خالی بود).", "Google returned no result to the app (empty intent)."))
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
                CompositionLocalProvider(LocalLayoutDirection provides if (LocalePolicy.isRightToLeft(java.util.Locale.getDefault().toLanguageTag())) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    AtlasApp(
                        gmailStatus = gmailStatus,
                        gmailMessages = gmailMessages,
                        gmailLoading = gmailLoading,
                        gmailConnected = gmailConnected,
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
    gmailConnected: Boolean,
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
    val initialConversation = remember { ChatStorage.ensureActive(appContext) }
    val conversations = remember {
        mutableStateListOf<StoredConversation>().apply { addAll(ChatStorage.loadAll(appContext)) }
    }
    var activeConversationId by remember { mutableStateOf(initialConversation.id) }
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply { addAll(initialConversation.messages) }
    }
    var nextId by remember { mutableStateOf((messages.maxOfOrNull { it.id } ?: 0L) + 1L) }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val aiGateway = remember {
        SupabaseChatGateway(
            supabaseUrl = AtlasConfig.SUPABASE_URL,
            anonKey = AtlasConfig.SUPABASE_PUBLISHABLE_KEY,
            accessTokenProvider = AccessTokenProvider { null }
        )
    }

    fun refreshConversations() {
        conversations.clear()
        conversations.addAll(ChatStorage.loadAll(appContext))
    }

    fun persistCurrentConversation() {
        // Keep the visible conversation and on-device storage under the same FIFO cap.
        while (messages.size > MAX_VISIBLE_MESSAGES) {
            messages.removeAt(0)
        }
        ChatStorage.save(appContext, messages, activeConversationId)
        refreshConversations()
    }

    fun sendMessage(rawText: String) {
        val cleanText = rawText.trim()
        if (cleanText.isNotEmpty()) {
            messages.add(ChatMessage(nextId++, cleanText, true))
            persistCurrentConversation()
            input = ""
            if (isGmailCheckRequest(cleanText)) {
                val pendingId = nextId++
                messages.add(ChatMessage(pendingId, AtlasText.of("در حال بررسی صندوق ورودی Gmail...", "Checking the Gmail inbox..."), false))
                persistCurrentConversation()
                onReadGmailForChat { answer ->
                    val pendingIndex = messages.indexOfFirst { it.id == pendingId }
                    if (pendingIndex >= 0) {
                        messages[pendingIndex] = ChatMessage(pendingId, answer, false)
                    } else {
                        messages.add(ChatMessage(nextId++, answer, false))
                    }
                    persistCurrentConversation()
                }
            } else {
                val requestMessages = messages.toList()
                val requestConversationId = activeConversationId
                val pendingId = nextId++
                messages.add(ChatMessage(pendingId, AtlasText.of("در حال دریافت پاسخ از اطلس...", "ATLAS is generating a response..."), false))
                persistCurrentConversation()
                scope.launch {
                    val result = aiGateway.complete(requestMessages, requestConversationId)
                    val answer = when (result) {
                        is AiGatewayResult.Success -> result.answer
                        is AiGatewayResult.Failure -> when (result.reason) {
                            AiFailure.NOT_CONFIGURED -> AtlasText.of("سرویس هوش مصنوعی روی سرور تنظیم نشده است. لطفاً بعداً دوباره تلاش کن.", "The AI service is not configured on the server yet. Please try again later.")
                            AiFailure.UNAUTHORIZED -> AtlasText.of("دسترسی به سرویس اطلس مجاز نیست. وضعیت اتصال را بررسی کن.", "Access to the ATLAS service was denied. Please check the connection.")
                            AiFailure.RATE_LIMITED -> AtlasText.of("درخواست‌ها زیاد شده است؛ کمی بعد دوباره تلاش کن.", "Too many requests. Please try again shortly.")
                            AiFailure.NETWORK -> AtlasText.of("اتصال اینترنت برقرار نشد. اتصال خود را بررسی و دوباره تلاش کن.", "The network connection failed. Check your connection and try again.")
                            AiFailure.SERVER -> AtlasText.of("سرویس هوش مصنوعی موقتاً پاسخ نمی‌دهد. کمی بعد دوباره تلاش کن.", "The AI service is temporarily unavailable. Please try again shortly.")
                            AiFailure.INVALID_RESPONSE -> AtlasText.of("پاسخ دریافتی معتبر نبود. دوباره تلاش کن.", "The response was invalid. Please try again.")
                            AiFailure.UNKNOWN -> AtlasText.of("در دریافت پاسخ خطایی رخ داد. دوباره تلاش کن.", "An error occurred while receiving the response. Please try again.")
                        }
                    }
                    if (activeConversationId == requestConversationId) {
                        val pendingIndex = messages.indexOfFirst { it.id == pendingId }
                        if (pendingIndex >= 0) {
                            messages[pendingIndex] = ChatMessage(pendingId, answer, false)
                        }
                        persistCurrentConversation()
                    }
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
                                    Text(AtlasText.of("هوشمندی بدون مرز", "Intelligence without borders"), color = Color(0xFFB8C8DC), fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(15.dp))
                            Text(AtlasText.of("دستیار هوشمند چندسرویس", "Multi-service AI assistant"), color = Color(0xFFEAF1FC), fontWeight = FontWeight.SemiBold)
                            Text(AtlasText.of("فضای هوشمند اطلس", "Your intelligent workspace"), color = Color(0xFF9FB4CC), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    atlasPages.forEach { page ->
                        when (page.id) {
                            "chat" -> Text(AtlasText.of("فضای کاری", "Workspace"), modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                            "integrations" -> Text(AtlasText.of("اتصال و حساب", "Connections & account"), modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                            "settings" -> Text(AtlasText.of("تنظیمات و پشتیبانی", "Settings & support"), modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 3.dp),
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
                            Text(AtlasText.of("وضعیت نسخه", "Build status"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(AtlasText.of("مسیر گفت‌وگوی آنلاین متصل است • ابزارهای دیگر نیازمند سرویس اختصاصی هستند.", "Online chat route is connected • other tools require dedicated services."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
                                atlasPages.firstOrNull { it.id == currentPage }?.title ?: AtlasText.of("اطلس", "Atlas"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = AtlasText.of("منو", "Menu"))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val created = ChatStorage.createConversation(appContext)
                            activeConversationId = created.id
                            messages.clear()
                            nextId = 1L
                            refreshConversations()
                            input = ""
                            currentPage = "chat"
                        }) {
                            Icon(Icons.Default.Add, contentDescription = AtlasText.of("گفت‌وگوی جدید", "New chat"))
                        }
                        TextButton(onClick = { showStatus = true }) { Text(AtlasText.of("وضعیت", "Status")) }
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
                                placeholder = { Text(AtlasText.of("پیام خود را بنویسید...", "Write a message...")) },
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
                                    contentDescription = AtlasText.of("ارسال پیام", "Send message"),
                                    tint = if (input.isNotBlank()) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            AtlasText.of("درخواست‌های گفت‌وگو از طریق سرویس آنلاین اطلس ارسال می‌شوند.", "Chat requests are sent through the ATLAS online service."),
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
                "history" -> HistoryPage(
                    conversations,
                    activeConversationId,
                    Modifier.fillMaxSize().padding(padding),
                    onOpenConversation = { selected ->
                        ChatStorage.setActiveConversation(appContext, selected.id)
                        activeConversationId = selected.id
                        messages.clear()
                        messages.addAll(selected.messages)
                        nextId = (messages.maxOfOrNull { it.id } ?: 0L) + 1L
                        currentPage = "chat"
                    },
                    onDeleteConversation = { selected ->
                        ChatStorage.deleteConversation(appContext, selected.id)
                        if (activeConversationId == selected.id) {
                            val replacement = ChatStorage.ensureActive(appContext)
                            activeConversationId = replacement.id
                            messages.clear()
                            messages.addAll(replacement.messages)
                            nextId = (messages.maxOfOrNull { it.id } ?: 0L) + 1L
                        }
                        refreshConversations()
                    },
                    onRenameConversation = { selected, title ->
                        ChatStorage.renameConversation(appContext, selected.id, title)
                        refreshConversations()
                    }
                )
                "explore" -> FeaturePage(
                    AtlasText.of("کاوش دستیارها", "Explore assistants"),
                    AtlasText.of("حالت‌های کاری تخصصی برای نیازهای مختلف.", "Specialized work modes for different needs."),
                    listOf(
                        AtlasText.of("نویسنده و ویراستار", "Writer & editor") to AtlasText.of("نگارش، بازنویسی و خلاصه‌سازی", "Writing, rewriting, and summarizing"),
                        AtlasText.of("مربی یادگیری", "Learning coach") to AtlasText.of("آموزش مرحله‌ای و تمرین", "Step-by-step learning and practice"),
                        AtlasText.of("همراه برنامه‌نویسی", "Coding assistant") to AtlasText.of("توضیح کد و عیب‌یابی", "Code explanation and debugging"),
                        AtlasText.of("پژوهشگر", "Researcher") to AtlasText.of("تحلیل موضوع و طراحی تحقیق", "Topic analysis and research design"),
                        AtlasText.of("دستیار کسب‌وکار", "Business assistant") to AtlasText.of("برنامه‌ریزی و تحلیل فرایند", "Planning and process analysis")
                    ),
                    Modifier.fillMaxSize().padding(padding)
                )
                "tools" -> FeaturePage(
                    AtlasText.of("ابزارهای اطلس", "ATLAS tools"),
                    AtlasText.of("ابزارها پس از پیاده‌سازی و اتصال امن فعال می‌شوند.", "Tools will be enabled after implementation and secure connection."),
                    listOf(
                        AtlasText.of("تحلیل فایل و سند", "File & document analysis") to AtlasText.of("دریافت و خلاصه‌سازی اسناد", "Document extraction and summarization"),
                        AtlasText.of("درک تصویر", "Image understanding") to AtlasText.of("پرسش درباره تصاویر", "Ask questions about images"),
                        AtlasText.of("جست‌وجوی وب", "Web search") to AtlasText.of("تحقیق همراه منابع", "Research with sources"),
                        AtlasText.of("ورودی و خروجی صوت", "Voice input & output") to AtlasText.of("گفتار به متن و خواندن پاسخ", "Speech-to-text and spoken responses"),
                        AtlasText.of("محیط کدنویسی", "Coding workspace") to AtlasText.of("توضیح و بررسی کد", "Explain and review code")
                    ),
                    Modifier.fillMaxSize().padding(padding)
                )
                "integrations" -> IntegrationsPage(
                    gmailStatus, gmailMessages, gmailLoading, gmailConnected,
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
                    AtlasText.of("حریم خصوصی و امنیت", "Privacy & security"),
                    AtlasText.of("اصول لازم پیش از اتصال سرویس‌ها.", "Principles to follow before connecting services."),
                    listOf(
                        AtlasText.of("کلیدهای محرمانه", "Secret keys") to AtlasText.of("فقط در Secret Manager سمت سرور نگهداری می‌شوند.", "Stored only in the server-side secret manager."),
                        AtlasText.of("حداقل‌سازی داده", "Data minimization") to AtlasText.of("فقط داده ضروری جمع‌آوری می‌شود.", "Only necessary data is collected."),
                        AtlasText.of("کنترل کاربر", "User control") to AtlasText.of("مدیریت و حذف داده‌ها در طراحی لحاظ می‌شود.", "Data management and deletion are part of the design."),
                        AtlasText.of("ارتباط امن", "Secure communication") to AtlasText.of("احراز هویت و محدودیت درخواست در سمت سرور.", "Server-side authentication and request limits.")
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
            title = { Text(AtlasText.of("وضعیت ATLAS AI", "ATLAS AI status")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(AtlasText.of("رابط اصلی Android: فعال", "Android interface: active"))
                    Text(AtlasText.of("زبان رابط فعلی: فارسی یا دری", "Current interface language: English"))
                    Text(AtlasText.of("مسیر گفت‌وگو به تابع آنلاین اطلس متصل است؛ پاسخ مدل هنوز تأیید نشده", "Chat is routed to the ATLAS function; a successful model response is not yet verified"))
                    Text(AtlasText.of("دیتابیس و حساب آنلاین: هنوز متصل نشده", "Cloud database and account: not connected"))
                    Text(AtlasText.of("کلید محرمانه داخل گوشی یا GitHub قرار نمی‌گیرد", "Secrets are never placed in the app or GitHub"))
                    Text(AtlasText.of("گفت‌وگو: ذخیره محلی فعال است و درخواست‌ها به تابع آنلاین اطلس ارسال می‌شوند.", "Chat: local storage is active and requests are sent to the ATLAS online function."))
                }
            },
            confirmButton = { TextButton(onClick = { showStatus = false }) { Text(AtlasText.of("بستن", "Close")) } }
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
        Text(AtlasText.of("سلام، من اطلس هستم", "Hello, I'm Atlas"), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = Color(0xFFF2F6FF))
        Spacer(Modifier.height(7.dp))
        Text(AtlasText.of("یک فضای یکپارچه برای گفتگو، تحقیق و انجام کارهای هوشمند.", "One workspace for conversation, research, and intelligent tasks."),
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA9B9CE))
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF172B3C)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Color(0xFFFFC66D), CircleShape))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(AtlasText.of("اتصال سرویس", "Service connection"), color = Color(0xFFEAF1FC), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(AtlasText.of("برخی ابزارها هنوز به سرویس واقعی متصل نیستند", "Some tools are not connected to live services yet"),
                        color = Color(0xFF91A5BF), fontSize = 10.sp)
                }
                Text("AI", color = Color(0xFFB8E9E2), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(AtlasText.of("فضای کاری اطلس", "ATLAS workspace"), color = Color(0xFFEAF1FC),
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text(AtlasText.of("ابزارها", "Tools"), color = Color(0xFF8FA4BE), fontSize = 11.sp)
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.ChatBubbleOutline, AtlasText.of("گفت‌وگوی هوشمند", "AI chat"), AtlasText.of("تعامل و پاسخ‌گویی", "Conversation and answers"), Color(0xFF9B91FF), Modifier.weight(1f), state = AtlasText.of("فعال", "Active"))
            AtlasToolCard(Icons.Filled.Image, AtlasText.of("هوش تصویری", "Image intelligence"), AtlasText.of("تحلیل و تولید تصویر", "Image analysis and generation"), Color(0xFF62B8FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.Movie, AtlasText.of("رسانه و ویدیو", "Media & video"), AtlasText.of("ابزارهای چندرسانه‌ای", "Multimedia tools"), Color(0xFFFFB86B), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Mic, AtlasText.of("دستیار صوتی", "Voice assistant"), AtlasText.of("گفتار و شنیدن پاسخ", "Speak and listen to answers"), Color(0xFF5ED9C8), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.Code, AtlasText.of("برنامه‌نویسی", "Programming"), AtlasText.of("کدنویسی و عیب‌یابی", "Coding and debugging"), Color(0xFF79A9FF), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Description, AtlasText.of("تحلیل اسناد", "Document analysis"), AtlasText.of("کار با فایل و متن", "Work with files and text"), Color(0xFFE2A7FF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AtlasToolCard(Icons.Filled.TravelExplore, AtlasText.of("پژوهش وب", "Web research"), AtlasText.of("جست‌وجوی منابع", "Search for sources"), Color(0xFF62D5B2), Modifier.weight(1f))
            AtlasToolCard(Icons.Filled.Psychology, AtlasText.of("تحلیل تخصصی", "Specialized analysis"), AtlasText.of("حل مسئله و یادگیری", "Problem solving and learning"), Color(0xFFFFD078), Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text(AtlasText.of("برای شروع انتخاب کنید", "Choose a starting point"), modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFEAF1FC), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        listOf(
            Triple("✎", AtlasText.of("نگارش و ترجمه", "Writing and translation"), AtlasText.of("نوشتن، بازنویسی و ترجمه متن", "Write, rewrite, and translate text")),
            Triple("⌘", AtlasText.of("برنامه‌نویسی", "Programming"), AtlasText.of("بررسی کد و یافتن خطاها", "Review code and find bugs")),
            Triple("◇", AtlasText.of("یادگیری و پژوهش", "Learning and research"), AtlasText.of("بررسی مرحله‌ای یک موضوع", "Explore a topic step by step"))
        ).forEach { option ->
            SuggestionCard(option.first, option.second, option.third) {
                onPromptClick(AtlasText.of("در زمینه " + option.second + " به من کمک کن.", "Help me with " + option.second + "."))
            }
            Spacer(Modifier.height(9.dp))
        }
        Text(AtlasText.of("قابلیت‌هایی که به سرویس اختصاصی نیاز دارند، پس از اتصال در دسترس قرار می‌گیرند.", "Features that require dedicated services are still in development."),
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
    modifier: Modifier = Modifier,
    state: String = AtlasText.of("در نقشه راه", "Planned")
) {
    Card(
        modifier = modifier.height(120.dp),
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
                Text(state, fontSize = 9.sp, color = if (state == AtlasText.of("فعال", "Active")) Color(0xFF70DCCE) else Color(0xFF91A5BF))
            }
            Column {
                Text(title, color = Color(0xFFEAF1FC), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = Color(0xFF91A5BF), fontSize = 10.sp, lineHeight = 15.sp)
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
    gmailConnected: Boolean,
    onConnectGmail: () -> Unit,
    onRefreshGmail: () -> Unit,
    onDisconnectGmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(AtlasText.of("مرکز اتصال سرویس‌ها", "Service connection center"), AtlasText.of("مدیریت یکپارچه دسترسی‌ها و ارتباط اطلس با برنامه‌های دیگر.", "Manage ATLAS permissions and connections to other apps."))
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
                    Text(AtlasText.of("اتصال‌های حساب", "Account connections"), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(AtlasText.of("دسترسی‌ها فقط با اجازه شما فعال می‌شوند.", "Access is enabled only with your permission."), color = Color(0xFFB5C6D8), fontSize = 11.sp)
                }
                Text(if (gmailConnected) AtlasText.of("۰۱ فعال", "01 active") else AtlasText.of("۰۰ فعال", "00 active"),
                    color = if (gmailConnected) Color(0xFF83E2D2) else Color(0xFFB5C6D8),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8E5FF), modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                                tint = Color(0xFF5148B8), modifier = Modifier.size(23.dp))
                        }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(AtlasText.of("موتور هوش مصنوعی اطلس", "ATLAS AI engine"), fontWeight = FontWeight.Bold)
                        Text(AtlasText.of("مسیر درخواست به بک‌اند اطلس متصل است", "Requests are routed to the ATLAS backend"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Text(AtlasText.of("تابع سرور فعال", "Server function active"), color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Text("Z.AI  •  Cloudflare Workers AI  •  OpenAI-compatible",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    AtlasText.of(
                        "درخواست‌های گفت‌وگو به تابع امن atlas-chat ارسال می‌شوند. پاسخ نهایی به تنظیمات معتبر ارائه‌دهنده هوش مصنوعی در سمت سرور وابسته است.",
                        "Chat requests are routed to the deployed ATLAS endpoint. A successful model response still depends on valid server-side provider settings."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(AtlasText.of("سرویس‌های متصل", "Connected services"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        Text(AtlasText.of("خواندن ایمیل‌ها • مجوز فقط‌خواندنی", "Read emails • read-only permission"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = if (gmailConnected) Color(0xFFDDF5E9) else Color(0xFFFFF0D8)) {
                        Text(if (gmailConnected) AtlasText.of("متصل", "Connected") else AtlasText.of("نیازمند اتصال", "Not connected"),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = if (gmailConnected) Color(0xFF176B45) else Color(0xFF8A5B14),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(AtlasText.of("اطلس می‌تواند ۸ ایمیل اخیر صندوق ورودی را دریافت کند و فرستنده، موضوع و خلاصه اولیه را نمایش دهد.", "ATLAS can retrieve up to 8 recent inbox messages and show the sender, subject, and preview."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(gmailStatus, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onConnectGmail, modifier = Modifier.fillMaxWidth()) {
                    Text(if (gmailConnected) AtlasText.of("اتصال دوباره / تغییر حساب Google", "Reconnect / change Google account") else AtlasText.of("اتصال امن حساب Google", "Securely connect Google account"))
                }
                OutlinedButton(onClick = onRefreshGmail, enabled = !gmailLoading && gmailConnected,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (gmailLoading) AtlasText.of("در حال دریافت...", "Loading...") else AtlasText.of("همگام‌سازی دوباره", "Refresh"))
                }
                TextButton(onClick = onDisconnectGmail, modifier = Modifier.fillMaxWidth()) {
                    Text(AtlasText.of("قطع دسترسی در این نشست", "Disconnect this session"))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(AtlasText.of("فعالیت اخیر Gmail", "Recent Gmail activity"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(9.dp))
        if (gmailMessages.isEmpty()) {
            EmptyCard(AtlasText.of("هنوز ایمیلی دریافت نشده", "No emails retrieved yet"), AtlasText.of("پس از اتصال Google و تأیید مجوز، ایمیل‌های اخیر اینجا نمایش داده می‌شوند.", "Recent emails will appear here after Google is connected and permission is granted."))
        } else {
            gmailMessages.forEachIndexed { index, item ->
                InfoCard(AtlasText.of("ایمیل ", "Email ") + (index + 1), item)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(AtlasText.of("سرویس‌های قابل توسعه", "Services planned for integration"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(9.dp))
        listOf(
            "GitHub" to AtlasText.of("مخزن‌ها و ابزارهای توسعه • اتصال در مرحله بعد", "Repositories and developer tools • planned"),
            "Google Drive" to AtlasText.of("فایل‌ها و اسناد • اتصال در مرحله بعد", "Files and documents • planned"),
            "Google Calendar" to AtlasText.of("رویدادها و برنامه‌ریزی • اتصال در مرحله بعد", "Events and scheduling • planned"),
            "Supabase" to AtlasText.of("زیرساخت داده و حساب • هنوز متصل نشده", "Account database • not configured")
        ).forEach { item ->
            InfoCard(item.first, item.second)
            Spacer(Modifier.height(8.dp))
        }
        InfoCard(AtlasText.of("حریم خصوصی Gmail", "Gmail privacy"), AtlasText.of("فقط مجوز gmail.readonly درخواست می‌شود. اطلس نمی‌تواند ایمیل ارسال یا حذف کند. توکن دسترسی فقط در حافظه همین اجرای برنامه است و در GitHub یا دیتابیس ذخیره نمی‌شود. برای لغو کامل، مجوز ATLAS AI را از حساب Google حذف کن.", "Only the gmail.readonly permission is requested. ATLAS cannot send or delete emails. The access token stays in this app session's memory and is not stored in GitHub or a database. To revoke access fully, remove ATLAS AI from your Google Account."))
    }
}

@Composable
private fun HistoryPage(
    conversations: List<StoredConversation>,
    activeConversationId: String,
    modifier: Modifier = Modifier,
    onOpenConversation: (StoredConversation) -> Unit,
    onDeleteConversation: (StoredConversation) -> Unit,
    onRenameConversation: (StoredConversation, String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<StoredConversation?>(null) }
    var pendingRename by remember { mutableStateOf<StoredConversation?>(null) }
    var renameText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val visibleConversations = conversations.filter { conversation ->
        conversation.messageCount > 0 && (
            searchQuery.isBlank() ||
                conversation.title.contains(searchQuery, ignoreCase = true) ||
                conversation.messages.lastOrNull()?.text?.contains(searchQuery, ignoreCase = true) == true
        )
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(AtlasText.of("تاریخچه گفت‌وگوها", "Chat history"), AtlasText.of("مکالمه‌های ذخیره‌شده روی همین دستگاه را باز کن، ادامه بده یا حذف کن.", "Open, continue, rename, search, or delete conversations saved on this device."))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(AtlasText.of("جست‌وجوی گفت‌وگو", "Search conversations")) }
        )
        Spacer(Modifier.height(10.dp))
        if (visibleConversations.isEmpty()) {
            if (searchQuery.isBlank()) {
                EmptyCard(AtlasText.of("هنوز گفت‌وگویی وجود ندارد", "No conversations yet"), AtlasText.of("با ارسال نخستین پیام، مکالمه در این بخش ذخیره می‌شود.", "Your conversation will appear here after you send the first message."))
            } else {
                EmptyCard(AtlasText.of("نتیجه‌ای پیدا نشد", "No results found"), AtlasText.of("عبارت جست‌وجو را کوتاه‌تر کن یا واژه دیگری را امتحان کن.", "Try a shorter search phrase or different keyword."))
            }
        } else {
            visibleConversations.forEach { conversation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (conversation.id == activeConversationId)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                conversation.title,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                            if (conversation.id == activeConversationId) {
                                Text(AtlasText.of("فعال", "Active"), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            conversation.messages.lastOrNull()?.text?.take(100).orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(7.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                conversation.messageCount.toString() + AtlasText.of(" پیام", " messages"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                renameText = conversation.title
                                pendingRename = conversation
                            }) { Text(AtlasText.of("تغییر نام", "Rename")) }
                            TextButton(onClick = { pendingDelete = conversation }) { Text(AtlasText.of("حذف", "Delete")) }
                            TextButton(onClick = { onOpenConversation(conversation) }) {
                                Text(if (conversation.id == activeConversationId) AtlasText.of("ادامه گفت‌وگو", "Continue") else AtlasText.of("بازکردن", "Open"))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
        Text(
            AtlasText.of("تاریخچه فقط روی همین دستگاه ذخیره می‌شود؛ همگام‌سازی ابری فعال نیست.", "History is stored only on this device; cloud sync is not active."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
    pendingRename?.let { selected ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text(AtlasText.of("تغییر نام گفت‌وگو", "Rename conversation")) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(60) },
                    singleLine = true,
                    label = { Text(AtlasText.of("نام گفت‌وگو", "Conversation name")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameConversation(selected, renameText)
                    pendingRename = null
                }, enabled = renameText.isNotBlank()) { Text(AtlasText.of("ذخیره", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) { Text(AtlasText.of("انصراف", "Cancel")) }
            }
        )
    }
    pendingDelete?.let { selected ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(AtlasText.of("حذف گفت‌وگو؟", "Delete conversation?")) },
            text = { Text(AtlasText.of("«" + selected.title + "» و پیام‌های ذخیره‌شده آن از این دستگاه حذف می‌شوند.", "“" + selected.title + "” and its saved messages will be deleted from this device.")) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(selected)
                    pendingDelete = null
                }) { Text(AtlasText.of("حذف", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(AtlasText.of("انصراف", "Cancel")) }
            }
        )
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
        Text(AtlasText.of("قابلیت‌های این بخش به‌تدریج و پس از اتصال سرویس‌های مربوطه فعال می‌شوند.", "These capabilities become available when their dedicated services are connected."),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProfilePage(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(
            AtlasText.of("حساب کاربری", "Account"),
            AtlasText.of("وضعیت حساب و محل نگهداری اطلاعاتت را ببین.", "Review account status and where your information is stored.")
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF172B43)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF294B68), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(AtlasText.of("حساب مهمان", "Guest account"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(AtlasText.of("بدون ورود یا ثبت‌نام", "No sign-in or registration"), color = Color(0xFFB8C8DC), fontSize = 11.sp)
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF294B59)) {
                        Text(
                            AtlasText.of("محلی", "Local"),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color(0xFF83E2D2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text(
                    AtlasText.of("اطلاعات این نسخه فقط در فضای خصوصی برنامه روی همین دستگاه نگهداری می‌شود.", "This version keeps its information in the app's private storage on this device."),
                    color = Color(0xFFD5E1EF), style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(AtlasText.of("وضعیت سرویس حساب", "Account service status"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("ورود و ثبت‌نام", "Sign-in and registration"),
            AtlasText.of("هنوز راه‌اندازی نشده است؛ در حال حاضر حساب آنلاین یا همگام‌سازی بین دستگاه‌ها وجود ندارد.", "Not implemented yet; there is currently no online account or cross-device sync.")
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("اطلاعات نمایه", "Profile information"),
            AtlasText.of("نمایه مهمان فعال است؛ ورود و حساب آنلاین هنوز راه‌اندازی نشده‌اند.", "Guest profile is active; online sign-in and accounts are not set up yet.")
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("تاریخچه گفتگوها", "Conversation history"),
            AtlasText.of("گفتگوها روی همین دستگاه ذخیره می‌شوند و از بخش تاریخچه می‌توانی آن‌ها را مدیریت یا حذف کنی.", "Conversations are stored on this device and can be managed or deleted from History.")
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("اتصال Gmail", "Gmail connection"),
            AtlasText.of("مجوز Gmail فقط برای خواندن ایمیل‌ها درخواست می‌شود. توکن دسترسی در حافظه موقت برنامه نگهداری می‌شود.", "Gmail access is read-only. The access token is held in app memory.")
        )
    }
}

@Composable
private fun PlansPage(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(AtlasText.of("طرح‌های اشتراک", "Plans"), AtlasText.of("اشتراک و پرداخت آنلاین هنوز فعال نشده‌اند.", "Subscriptions and online payments are not enabled yet."))
        listOf(
            AtlasText.of("رایگان", "Free") to AtlasText.of("گفت‌وگوی پایه و سهمیه محدود پس از اتصال", "Basic chat and limited quota after connection"),
            AtlasText.of("پلاس", "Plus") to AtlasText.of("سهمیه بیشتر و مدل‌های پیشرفته‌تر", "Higher quota and advanced models"),
            AtlasText.of("پرو", "Pro") to AtlasText.of("سقف مصرف بالاتر و ابزارهای تخصصی", "Higher usage limits and specialized tools")
        ).forEach { plan ->
            InfoCard(plan.first, plan.second + AtlasText.of(" • نیازمند راه‌اندازی", " • Setup required"))
            Spacer(Modifier.height(10.dp))
        }
        Text(AtlasText.of("هیچ پرداختی انجام نمی‌شود و درگاه پرداخت متصل نیست.", "No payment is processed and no payment provider is connected."),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingsPage(
    darkThemeEnabled: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceLocale = java.util.Locale.getDefault()
    val deviceLanguage = deviceLocale.getDisplayName(deviceLocale).ifBlank { deviceLocale.toLanguageTag() }
    val layoutDirection = if (LocalePolicy.isRightToLeft(deviceLocale.toLanguageTag())) {
        AtlasText.of("راست‌به‌چپ", "Right-to-left")
    } else {
        AtlasText.of("چپ‌به‌راست", "Left-to-right")
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(
            AtlasText.of("تنظیمات", "Settings"),
            AtlasText.of("ظاهر و رفتار برنامه را مدیریت کن.", "Manage the app's appearance and behavior.")
        )
        Text(AtlasText.of("ظاهر", "Appearance"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (darkThemeEnabled) "☾" else "☀", fontSize = 21.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(AtlasText.of("حالت رنگی", "Color theme"), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (darkThemeEnabled) AtlasText.of("حالت تاریک فعال است", "Dark mode is active")
                        else AtlasText.of("حالت روشن فعال است", "Light mode is active"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                }
                TextButton(onClick = onThemeToggle) {
                    Text(if (darkThemeEnabled) AtlasText.of("روشن", "Light") else AtlasText.of("تاریک", "Dark"))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(AtlasText.of("زبان و منطقه", "Language & region"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("زبان دستگاه", "Device language"),
            AtlasText.of("زبان شناسایی‌شده: $deviceLanguage. زبان رابط به‌صورت خودکار از تنظیمات دستگاه پیروی می‌کند.", "Detected language: $deviceLanguage. The interface automatically follows your device language.")
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("جهت نوشتار", "Text direction"),
            AtlasText.of("چیدمان فعلی: $layoutDirection.", "Current layout: $layoutDirection.")
        )
        Spacer(Modifier.height(18.dp))
        Text(AtlasText.of("حریم خصوصی و داده‌ها", "Privacy & data"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("ذخیره‌سازی گفتگوها", "Conversation storage"),
            AtlasText.of("گفتگوها در حافظه خصوصی برنامه روی همین دستگاه ذخیره می‌شوند؛ همگام‌سازی ابری فعال نیست.", "Conversations are stored in the app's private storage on this device; cloud sync is not active.")
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("کلیدهای سرویس‌ها", "Service credentials"),
            AtlasText.of("کلیدهای هوش مصنوعی فقط در سمت سرور نگهداری می‌شوند؛ پاسخ مدل به تنظیمات امن ارائه‌دهندگان وابسته است.", "AI provider keys stay server-side; model responses depend on valid provider configuration.")
        )
        Spacer(Modifier.height(18.dp))
        Text(AtlasText.of("دسترس‌پذیری", "Accessibility"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("خوانایی و صفحه‌خوان", "Readability & screen readers"),
            AtlasText.of("بررسی کامل اندازه متن، کنتراست و TalkBack هنوز بخشی از آزمون نهایی انتشار است.", "Full text-size, contrast, and TalkBack review is still part of release testing.")
        )
        Spacer(Modifier.height(8.dp))
        InfoCard(
            AtlasText.of("اعلان‌ها", "Notifications"),
            AtlasText.of("اعلان‌ها تا زمان پیاده‌سازی حساب و همگام‌سازی ابری فعال نیستند.", "Notifications are not enabled until account and cloud-sync features are implemented.")
        )
    }
}

@Composable
private fun HelpPage(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle(AtlasText.of("راهنما و پشتیبانی", "Help & support"), AtlasText.of("راهنمای استفاده از اطلس AI.", "How to use ATLAS AI."))
        InfoCard(AtlasText.of("آیا اطلس پاسخ هوش مصنوعی می‌دهد؟", "Does ATLAS provide AI answers?"), AtlasText.of("بله؛ گفت‌وگوی عمومی درخواست‌ها را به تابع آنلاین اطلس می‌فرستد؛ پاسخ موفق به تنظیمات ارائه‌دهنده هوش مصنوعی وابسته است.", "Yes; general chat sends requests to the ATLAS online function. A successful answer depends on valid AI provider settings."))
        InfoCard(AtlasText.of("آیا برای دیدن ساختار کلید لازم است؟", "Do I need a key to view the app structure?"), AtlasText.of("خیر؛ طراحی و نمایش صفحات بدون کلید انجام می‌شود.", "No; the UI and screens work without API keys."))
        InfoCard(AtlasText.of("آیا داده‌ها ذخیره می‌شوند؟", "Is my data stored?"), AtlasText.of("پیام‌های گفت‌وگو به‌صورت محلی و خصوصی روی همین دستگاه ذخیره می‌شوند.", "Chat messages are stored locally in the private app storage on this device."))
        InfoCard(AtlasText.of("پشتیبانی", "Support"), AtlasText.of("راه ارتباطی پشتیبانی رسمی پیش از انتشار ثبت خواهد شد.", "Official support contact details must be added before release."))
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
    val clipboardManager = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.Start else Arrangement.End
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = if (message.fromUser) Alignment.Start else Alignment.End
        ) {
            Text(
                if (message.fromUser) AtlasText.of("شما", "You") else "ATLAS AI",
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
            TextButton(
                onClick = { clipboardManager.setText(AnnotatedString(message.text)) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(AtlasText.of("کپی متن", "Copy text"), fontSize = 10.sp)
            }
        }
    }
}
