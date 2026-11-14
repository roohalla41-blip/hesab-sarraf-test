package com.hooshmasnooi.app

import android.os.Bundle
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
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hooshmasnooi.app.domain.model.ChatMessage
import kotlinx.coroutines.launch

private data class AtlasPage(val id: String, val title: String, val symbol: String)

private val atlasPages = listOf(
    AtlasPage("chat", "گفت‌وگو", "✦"),
    AtlasPage("history", "تاریخچه", "◷"),
    AtlasPage("explore", "کاوش دستیارها", "◇"),
    AtlasPage("tools", "ابزارها", "⌘"),
    AtlasPage("profile", "حساب کاربری", "◎"),
    AtlasPage("plans", "طرح‌های اشتراک", "♧"),
    AtlasPage("settings", "تنظیمات", "⚙"),
    AtlasPage("privacy", "حریم خصوصی و امنیت", "▣"),
    AtlasPage("help", "راهنما و پشتیبانی", "?")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF173B63),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFDCEBFA),
                    onPrimaryContainer = Color(0xFF102C49),
                    secondary = Color(0xFF168C91),
                    secondaryContainer = Color(0xFFD6F1EF),
                    background = Color(0xFFF7F9FC),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFEAF0F6),
                    onSurface = Color(0xFF172536),
                    onSurfaceVariant = Color(0xFF526273)
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AtlasApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasApp() {
    var currentPage by remember { mutableStateOf("chat") }
    var input by remember { mutableStateOf("") }
    var showStatus by remember { mutableStateOf(false) }
    var darkPreview by remember { mutableStateOf(false) }
    var languagePreview by remember { mutableStateOf("فارسی") }
    var nextId by remember { mutableStateOf(1L) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun sendMessage(rawText: String) {
        val cleanText = rawText.trim()
        if (cleanText.isNotEmpty()) {
            messages.add(ChatMessage(nextId++, cleanText, true))
            input = ""
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
                        "نسخه ساختاری • بدون اتصال هوش مصنوعی",
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
                            "پیش‌نمایش ساختاری؛ هوش مصنوعی هنوز متصل نیست و پاسخی تولید نمی‌شود.",
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
                    Text("ساختار صفحات: در حال تکمیل")
                    Text("زبان پیش‌نمایش: " + languagePreview)
                    Text("هوش مصنوعی: هنوز متصل نشده")
                    Text("دیتابیس و حساب آنلاین: هنوز متصل نشده")
                    Text("کلید محرمانه داخل گوشی یا GitHub قرار نمی‌گیرد")
                    Text("هیچ APK یا Build در این مرحله اجرا نشده است.")
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
            item {
                Text(
                    "پیام شما نمایش داده شد؛ چون موتور هوش مصنوعی متصل نیست، پاسخی ساخته نشده است.",
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(modifier: Modifier = Modifier, onPromptClick: (String) -> Unit) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✦", fontSize = 38.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("سلام، من اطلس هستم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "دستیار هوشمند شما برای نوشتن، یادگیری، تحقیق و برنامه‌نویسی.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))
        listOf(
            Triple("✎", "نوشتن و ترجمه", "کمک برای نگارش و ترجمه"),
            Triple("⌘", "برنامه‌نویسی", "توضیح کد و پیدا کردن خطا"),
            Triple("◇", "یادگیری و تحقیق", "توضیح موضوعات و خلاصه‌سازی"),
            Triple("✧", "ایده‌پردازی", "تبدیل ایده‌ها به برنامه")
        ).forEach { option ->
            SuggestionCard(option.first, option.second, option.third) {
                onPromptClick("می‌خواهم در بخش " + option.second + " کمک بگیرم.")
            }
            Spacer(Modifier.height(10.dp))
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
private fun HistoryPage(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageTitle("تاریخچه گفت‌وگوها", "پیام‌های این نشست در حافظه موقت نمایش داده می‌شوند.")
        if (messages.isEmpty()) {
            EmptyCard("هنوز گفت‌وگویی وجود ندارد", "با شروع یک گفت‌وگو، پیام‌های این نشست اینجا دیده می‌شوند.")
        } else {
            messages.forEach { message ->
                InfoCard("پیام " + message.id, message.text)
                Spacer(Modifier.height(8.dp))
            }
        }
        Text("ذخیره دائمی و همگام‌سازی ابری هنوز فعال نیست.",
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
        PageTitle("راهنما و پشتیبانی", "پاسخ به پرسش‌های پایه درباره نسخه ساختاری اطلس.")
        InfoCard("آیا اطلس پاسخ هوش مصنوعی می‌دهد؟", "خیر؛ موتور هوش مصنوعی هنوز متصل نشده است.")
        InfoCard("آیا برای دیدن ساختار کلید لازم است؟", "خیر؛ طراحی و نمایش صفحات بدون کلید انجام می‌شود.")
        InfoCard("آیا داده‌ها ذخیره می‌شوند؟", "خیر؛ این نسخه فقط ساختار نمایشی است.")
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
