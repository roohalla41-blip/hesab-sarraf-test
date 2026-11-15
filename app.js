const $ = (selector) => document.querySelector(selector);
const promptInput = $("#prompt");
const messages = $("#messages");
const welcome = $("#welcome");
const pageView = $("#pageView");
const composerWrap = $("#composerWrap");
const toast = $("#toast");
const sidebar = $("#sidebar");
const conversations = [];
let currentMessages = [];
let currentMode = "عمومی";
const browserLanguage = (navigator.languages?.[0] || navigator.language || "fa").toLowerCase();
const browserLanguageLabel = browserLanguage.startsWith("fa") ? "فارسی" : browserLanguage.startsWith("prs") ? "دری" : browserLanguage.startsWith("en") ? "English" : browserLanguage;
let darkMode = true;

function notify(message) {
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(window.atlasToastTimer);
  window.atlasToastTimer = setTimeout(() => toast.classList.remove("show"), 2600);
}
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"
  }[char]));
}
function saveCurrentConversation() {
  if (!currentMessages.length) return;
  const first = currentMessages[0].text;
  const existing = conversations.find(item => item.id === currentMessages[0].conversationId);
  if (existing) {
    existing.messages = [...currentMessages];
    existing.updated = new Date();
  } else {
    conversations.unshift({
      id: currentMessages[0].conversationId,
      title: first.slice(0, 52),
      messages: [...currentMessages],
      updated: new Date()
    });
  }
}
function startNewChat() {
  saveCurrentConversation();
  currentMessages = [];
  messages.replaceChildren();
  messages.hidden = true;
  welcome.hidden = false;
  pageView.hidden = true;
  composerWrap.hidden = false;
  promptInput.value = "";
  setActivePanel("chat");
  sidebar.classList.remove("open");
}
function renderMessage(text, role = "user") {
  const bubble = document.createElement("div");
  bubble.className = role === "assistant" ? "message assistant-message" : "message";
  bubble.textContent = text;
  messages.appendChild(bubble);
  messages.scrollTop = messages.scrollHeight;
}
async function sendMessage(value = promptInput.value) {
  const text = value.trim();
  if (!text) return;
  const id = currentMessages[0]?.conversationId || (Date.now() + Math.random()).toString(36);
  const history = currentMessages.slice(-8).map(item => ({
    role: item.role === "assistant" ? "assistant" : "user",
    content: item.text
  }));
  currentMessages.push({ id: currentMessages.length + 1, conversationId: id, text, mode: currentMode, role: "user" });
  saveCurrentConversation();
  welcome.hidden = true;
  pageView.hidden = true;
  messages.hidden = false;
  composerWrap.hidden = false;
  renderMessage(text, "user");
  promptInput.value = "";
  const sendButton = $("#send");
  sendButton.disabled = true;
  sendButton.textContent = "…";
  const pending = document.createElement("div");
  pending.className = "notice";
  pending.textContent = "اطلس در حال پاسخ‌گویی است…";
  messages.appendChild(pending);
  messages.scrollTop = messages.scrollHeight;
  try {
    const response = await fetch("https://wwlyvojtsrklcpqsloqz.supabase.co/functions/v1/atlas-chat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "apikey": "sb_publishable_TFLE7E_j9-1H4DWM-yQ8GQ_15kUInFo"
      },
      body: JSON.stringify({ message: text, history, mode: currentMode })
    });
    const data = await response.json().catch(() => ({}));
    pending.remove();
    if (!response.ok) throw new Error(data.error || "پاسخی از سرویس دریافت نشد.");
    const answer = typeof data.reply === "string" ? data.reply.trim() : "";
    if (!answer) throw new Error("پاسخ خالی از سرویس دریافت شد.");
    currentMessages.push({ id: currentMessages.length + 1, conversationId: id, text: answer, mode: currentMode, role: "assistant" });
    saveCurrentConversation();
    renderMessage(answer, "assistant");
  } catch (error) {
    pending.className = "notice error-notice";
    pending.textContent = error.message || "ارتباط برقرار نشد؛ دوباره تلاش کن.";
    messages.appendChild(pending);
  } finally {
    sendButton.disabled = false;
    sendButton.textContent = "↑";
    promptInput.focus();
  }
}
function setActivePanel(panel) {
  document.querySelectorAll(".nav-item").forEach(item => {
    item.classList.toggle("active", item.dataset.panel === panel);
  });
  const active = document.querySelector(`.nav-item[data-panel="${panel}"]`);
  $("#sectionTitle").textContent = panel === "chat" ? "دستیار اطلس" : (active?.textContent.trim() || "اطلس");
}
function featureCard(icon, title, description, state = "در نقشه راه") {
  return `<article class="feature-card"><div class="feature-icon">${icon}</div><div class="feature-copy"><b>${title}</b><p>${description}</p></div><span class="state-pill">${state}</span></article>`;
}
function renderPanel(panel) {
  const panels = {
    history: `
      <div class="page-heading"><div><div class="eyebrow">فضای کاری شما</div><h2>تاریخچه گفت‌وگوها</h2><p>مکالمه‌های همین نشست در این فهرست دیده می‌شوند.</p></div><button class="primary-btn" data-action="new-chat">＋ گفت‌وگوی تازه</button></div>
      <div class="panel-card"><div class="panel-toolbar"><b>گفت‌وگوهای اخیر</b><span class="muted">${conversations.length} مورد در این نشست</span></div>
      ${conversations.length ? conversations.map(item => `<button class="history-row" data-conversation="${escapeHtml(item.id)}"><span class="history-icon">◷</span><span class="history-title"><b>${escapeHtml(item.title)}</b><small>${item.messages.length} پیام • ${item.updated.toLocaleTimeString("fa-IR",{hour:"2-digit",minute:"2-digit"})}</small></span><span class="muted">›</span></button>`).join("") : `<div class="empty-state"><div class="empty-icon">◷</div><b>هنوز گفت‌وگویی ثبت نشده</b><p>بعد از نوشتن نخستین پیام، عنوان آن در این قسمت نمایش داده می‌شود.</p></div>`}
      </div><p class="disclaimer">تاریخچه فعلاً فقط تا زمانی که این صفحه باز است در حافظه موقت مرورگر نگهداری می‌شود؛ ذخیره دائمی یا ابری فعال نیست.</p>`,
    explore: `
      <div class="page-heading"><div><div class="eyebrow">Explore ATLAS</div><h2>کاوش دستیارها</h2><p>حالت‌های تخصصی برای کارهای گوناگون؛ انتخاب هر مورد فقط حالت نمایشی را تغییر می‌دهد.</p></div></div>
      <div class="feature-grid">
        ${featureCard("✍","نویسنده و ویراستار","نگارش، بازنویسی، خلاصه‌سازی و ایده‌پردازی","قالب آماده")}
        ${featureCard("◈","مربی یادگیری","آموزش مرحله‌ای، تمرین و توضیح مفاهیم","قالب آماده")}
        ${featureCard("⌘","همراه برنامه‌نویسی","توضیح کد، طراحی ساختار و عیب‌یابی","قالب آماده")}
        ${featureCard("⌕","پژوهشگر","تحلیل موضوع، طرح پرسش و ساختار تحقیق","قالب آماده")}
        ${featureCard("◎","دستیار کسب‌وکار","برنامه‌ریزی، ایده تجاری و تحلیل فرایند","در نقشه راه")}
        ${featureCard("✧","دستیار عمومی","گفت‌وگوی چندمنظوره و پاسخ‌گویی عمومی","قالب آماده")}
      </div><p class="disclaimer">اتصال گفت‌وگوی عمومی فعال است؛ دستیارهای تخصصی و ابزارهای این بخش هنوز کامل پیاده‌سازی نشده‌اند.</p>`,
    tools: `
      <div class="page-heading"><div><div class="eyebrow">ATLAS TOOLBOX</div><h2>ابزارهای اطلس</h2><p>جایگاه ابزارهای آینده در یک محیط یکپارچه.</p></div></div>
      <div class="feature-grid">
        ${featureCard("▧","تحلیل فایل و سند","انتخاب فایل، استخراج متن و خلاصه‌سازی امن","بعداً")}
        ${featureCard("▣","درک تصویر","ارسال تصویر و پرسش درباره محتوای آن","بعداً")}
        ${featureCard("⌕","جست‌وجوی وب","تحقیق با منبع و پیوند قابل بررسی","بعداً")}
        ${featureCard("⌘","محیط کدنویسی","قالب کد، توضیح و بررسی خطاها","بعداً")}
        ${featureCard("♫","ورودی و خروجی صوت","گفتار به متن و خواندن پاسخ","بعداً")}
        ${featureCard("⇧","اشتراک‌گذاری خروجی","کپی، خروجی گرفتن و اشتراک مکالمه","بعداً")}
      </div><p class="disclaimer">ابزارها تا زمان پیاده‌سازی، اتصال امن و آزمایش واقعی غیرفعال می‌مانند.</p>`,
    integrations: `
      <div class="page-heading"><div><div class="eyebrow">CONNECTED APPS</div><h2>مرکز اتصال برنامه‌ها</h2><p>وضعیت واقعی هر اتصال را ببین؛ موارد نمایشی به‌عنوان اتصال فعال معرفی نمی‌شوند.</p></div></div>
      <div class="security-banner"><span>⇄</span><div><b>اتصال فقط با اجازه کاربر</b><p>هر سرویس باید مجوز جداگانه و محدود به نیاز همان قابلیت دریافت کند.</p></div></div>
      <div class="feature-grid">
        ${featureCard("G","Google Gmail","در کد Android مسیر OAuth فقط‌خواندنی آماده شده؛ آزمایش روی گوشی واقعی هنوز انجام نشده است.","آزمایش لازم")}
        ${featureCard("⌘","GitHub","اتصال حساب کاربر، دسترسی مخزن و مدیریت مجوزها هنوز پیاده‌سازی نشده است.","غیرفعال")}
        ${featureCard("▣","Google Drive","انتخاب فایل، مجوز محدود و دریافت امن فایل هنوز پیاده‌سازی نشده است.","غیرفعال")}
        ${featureCard("◷","Google Calendar","خواندن رویدادها و مدیریت مجوز تقویم هنوز پیاده‌سازی نشده است.","غیرفعال")}
      </div>
      <p class="disclaimer">این صفحه فقط وضعیت توسعه را نشان می‌دهد. هیچ‌یک از این سرویس‌ها از داخل پیش‌نمایش وب متصل نمی‌شوند.</p>`,
    profile: `
      <div class="page-heading"><div><div class="eyebrow">ACCOUNT</div><h2>حساب کاربری</h2><p>نمای کلی حساب در نسخه ساختاری.</p></div></div>
      <div class="profile-card"><div class="profile-avatar">A</div><div><h3>کاربر مهمان</h3><p class="muted">بدون ورود • حساب آنلاین ایجاد نشده</p><span class="state-pill">حالت محلی نمایشی</span></div></div>
      <div class="feature-grid">
        ${featureCard("◎","اطلاعات پروفایل","نام نمایشی، تصویر و مشخصات حساب","بعداً")}
        ${featureCard("◉","ورود و امنیت","ورود امن و مدیریت نشست‌ها","نیازمند طراحی")}
        ${featureCard("⇩","داده‌های من","دریافت یا حذف داده‌های ذخیره‌شده","بعداً")}
      </div><p class="disclaimer">هیچ ثبت‌نام، رمز عبور، ایمیل یا حسابی در این پیش‌نمایش دریافت یا ذخیره نمی‌شود.</p>`,
    plans: `
      <div class="page-heading"><div><div class="eyebrow">PLANS</div><h2>طرح‌های اشتراک</h2><p>ساختار پیشنهادی پلن‌ها؛ قیمت‌گذاری و پرداخت هنوز تعریف نشده است.</p></div></div>
      <div class="plan-grid">
        <article class="plan-card"><span class="state-pill">شروع</span><h3>رایگان</h3><p>برای آشنایی با اطلس</p><ul><li>گفت‌وگوی پایه</li><li>سهمیه محدود پس از اتصال</li><li>ابزارهای پایه</li></ul><button class="secondary-btn" disabled>هنوز فعال نیست</button></article>
        <article class="plan-card featured-plan"><span class="state-pill">پیشنهادی</span><h3>پلاس</h3><p>برای استفاده روزمره و حرفه‌ای</p><ul><li>سهمیه بیشتر</li><li>مدل‌های پیشرفته‌تر</li><li>ابزارهای تخصصی</li></ul><button class="secondary-btn" disabled>هنوز فعال نیست</button></article>
        <article class="plan-card"><span class="state-pill">حرفه‌ای</span><h3>پرو</h3><p>برای کارهای سنگین و تخصصی</p><ul><li>سقف مصرف بالاتر</li><li>قابلیت‌های پیشرفته</li><li>اولویت پردازش</li></ul><button class="secondary-btn" disabled>هنوز فعال نیست</button></article>
      </div><p class="disclaimer">هیچ مبلغی دریافت نمی‌شود و هیچ درگاه پرداختی متصل نیست.</p>`,
    settings: `
      <div class="page-heading"><div><div class="eyebrow">PREFERENCES</div><h2>تنظیمات</h2><p>گزینه‌های نمایشی محیط اطلس.</p></div></div>
      <div class="settings-list">
        <div class="setting-row"><div><b>ظاهر برنامه</b><small>انتخاب پوسته روشن یا تاریک</small></div><button class="secondary-btn" data-action="theme">تغییر پوسته</button></div>
        <div class="setting-row"><div><b>زبان دستگاه</b><small>زبان شناسایی‌شده مرورگر: ${escapeHtml(browserLanguageLabel)} (${escapeHtml(browserLanguage)})</small></div><span class="state-pill">تشخیص خودکار</span></div>
        <div class="setting-row"><div><b>جهت نوشتار</b><small>این پیش‌نمایش فعلاً با چیدمان فارسی نمایش داده می‌شود.</small></div><span class="state-pill">راست‌به‌چپ</span></div>
        <div class="setting-row"><div><b>اندازه نوشته</b><small>اندازه استاندارد رابط کاربری</small></div><span class="state-pill">استاندارد</span></div>
        <div class="setting-row"><div><b>اعلان‌ها</b><small>تنظیم اعلان‌های برنامه پس از ساخت حساب</small></div><span class="state-pill">بعداً</span></div>
        <div class="setting-row"><div><b>دسترس‌پذیری</b><small>کنتراست، خوانایی و پشتیبانی صفحه‌خوان</small></div><span class="state-pill">در نقشه راه</span></div>
      </div>`,
    privacy: `
      <div class="page-heading"><div><div class="eyebrow">PRIVACY & SAFETY</div><h2>حریم خصوصی و امنیت</h2><p>اصول امنیتی که پیش از اتصال سرویس‌ها باید رعایت شوند.</p></div></div>
      <div class="security-banner"><span>◆</span><div><b>کلید هوش مصنوعی داخل برنامه قرار نمی‌گیرد</b><p>کلیدها فقط در محیط امن سمت سرور نگهداری خواهند شد.</p></div></div>
      <div class="feature-grid">
        ${featureCard("▣","حداقل‌سازی داده","فقط داده ضروری برای قابلیت موردنظر جمع‌آوری شود","اصل طراحی")}
        ${featureCard("⌑","کنترل کاربر","امکان حذف و مدیریت داده‌ها","در نقشه راه")}
        ${featureCard("◇","ارتباط امن","درخواست‌های محافظت‌شده و کنترل دسترسی","پیش از اتصال")}
        ${featureCard("⚑","شفافیت قابلیت‌ها","اعلام روشن خطاها و قابلیت‌های غیرفعال","اصل طراحی")}
      </div><p class="disclaimer">این بخش توضیح سیاست طراحی است، نه گواهی ممیزی امنیتی یا تضمین امنیت نسخه نهایی.</p>`,
    help: `
      <div class="page-heading"><div><div class="eyebrow">HELP CENTER</div><h2>راهنما و پشتیبانی</h2><p>راهنمای استفاده از بخش‌های اصلی برنامه.</p></div></div>
      <div class="faq-list">
        <details open><summary>آیا اطلس اکنون پاسخ هوش مصنوعی می‌دهد؟</summary><p>بله. گفت‌وگوی عمومی این پیش‌نمایش به مدل هوش مصنوعی از طریق تابع امن سمت سرور متصل است؛ ابزارهای دیگر هنوز فعال نیستند.</p></details>
        <details><summary>آیا برای دیدن ساختار به کلید نیاز دارم؟</summary><p>خیر. ساختار، صفحات، منوها و تنظیمات نمایشی بدون کلید قابل طراحی و بررسی هستند.</p></details>
        <details><summary>آیا اطلاعات این پیش‌نمایش ذخیره می‌شوند؟</summary><p>خیر. پیام‌ها فقط در حافظه موقت همین نشست نمایشی نگهداری می‌شوند.</p></details>
        <details><summary>چه زمانی قابلیت‌ها فعال می‌شوند؟</summary><p>پس از تکمیل رابط، بازبینی امنیتی، انتخاب سرویس و اجازه جداگانه برای اتصال.</p></details>
      </div><div class="support-card"><b>پشتیبانی اطلس</b><p>راه ارتباطی رسمی در زمان آماده‌سازی نسخه انتشار اضافه خواهد شد.</p><span class="state-pill">در حال طراحی</span></div>`
  };
  pageView.innerHTML = panels[panel] || "";
  pageView.hidden = false;
  welcome.hidden = true;
  messages.hidden = true;
  composerWrap.hidden = true;
  pageView.querySelectorAll("[data-action]").forEach(button => button.addEventListener("click", () => {
    if (button.dataset.action === "new-chat") startNewChat();
    if (button.dataset.action === "theme") toggleTheme();
  }));
  pageView.querySelectorAll("[data-conversation]").forEach(button => button.addEventListener("click", () => {
    const item = conversations.find(conversation => conversation.id === button.dataset.conversation);
    if (!item) return;
    currentMessages = [...item.messages];
    messages.replaceChildren();
    currentMessages.forEach(message => renderMessage(message.text, message.role || "user"));
    pageView.hidden = true;
    messages.hidden = false;
    composerWrap.hidden = false;
    setActivePanel("chat");
  }));
}
function toggleTheme() {
  darkMode = !darkMode;
  document.body.classList.toggle("light", !darkMode);
  notify(darkMode ? "پوسته تاریک فعال شد" : "پوسته روشن فعال شد");
}
function toggleLanguage() {
  notify("این پیش‌نمایش فعلاً فارسی است؛ ترجمه کامل چندزبانه هنوز در دست تکمیل است.");
}
document.querySelectorAll(".nav-item").forEach(button => button.addEventListener("click", () => {
  const panel = button.dataset.panel;
  setActivePanel(panel);
  if (panel === "chat") {
    pageView.hidden = true;
    welcome.hidden = currentMessages.length > 0;
    messages.hidden = currentMessages.length === 0;
    composerWrap.hidden = false;
  } else {
    renderPanel(panel);
  }
  sidebar.classList.remove("open");
}));
$("#newChat").addEventListener("click", startNewChat);
$("#send").addEventListener("click", () => sendMessage());
promptInput.addEventListener("keydown", event => {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    sendMessage();
  }
});
document.querySelectorAll("[data-prompt]").forEach(button => button.addEventListener("click", () => sendMessage(button.dataset.prompt)));
document.querySelectorAll(".mode-chip").forEach(button => button.addEventListener("click", () => {
  document.querySelectorAll(".mode-chip").forEach(item => item.classList.remove("selected"));
  button.classList.add("selected");
  currentMode = button.dataset.mode;
  notify("حالت انتخاب‌شده: " + currentMode);
}));
$("#menuToggle").addEventListener("click", () => sidebar.classList.toggle("open"));
$("#themeToggle").addEventListener("click", toggleTheme);
$("#languageToggle").textContent = "FA";
$("#languageToggle").setAttribute("aria-label", "زبان فعلی پیش‌نمایش: فارسی");
$("#languageToggle").addEventListener("click", toggleLanguage);
$("#attach").addEventListener("click", () => notify("پیوست فایل در نسخه ساختاری غیرفعال است؛ هنوز فایل بارگذاری نمی‌شود."));
