# آزمایش استقلال از Base44

پروژه کوچک آزمایشی با رابط کاربری فارسی و راست‌چین (RTL) برای بررسی انتقال
کدها از Base44 به یک محیط مستقل و آماده‌سازی اتصال به Supabase.

## امکانات

- صفحه ورود با ایمیل و رمز عبور و دکمهٔ «ورود آزمایشی» (احراز هویت محلیِ آزمایشی در مرورگر؛ سمت سرور نیست)
- صفحه اصلی با عنوان «پنل آزمایشی»
- نمایش نام کاربر واردشده
- سه دکمه: «نمایش اطلاعات»، «افزودن رکورد آزمایشی»، «خروج»
- نمایش فهرست رکوردهای آزمایشی
- طراحی ساده، مرتب و سازگار با گوشی موبایل
- پنل سازنده (مسیر `/builder`) برای بررسی ساختار، وابستگی‌ها و وضعیت ذخیره‌سازی
- خروجی ZIP قابل دانلود و گزارش PDF فارسی (مسیر `/report`)

## دستورالعمل اجرای برنامه

```bash
# نصب وابستگی‌ها
npm install

# اجرای محیط توسعه
npm run dev

# ساخت نسخه نهایی
npm run build

# پیش‌نمایش نسخه نهایی
npm run preview
```

> این نسخه مستقل از Base44 است (بدون `@base44/*`). دستورات بالا دستورات استاندارد Vite هستند.
> احراز هویت فعلی (`src/api/authClient.js`) محلی و آزمایشی است و برای داده واقعی مناسب نیست.

## ساختار پروژه و جداسازی لایه‌ها

| لایه | مسیر | توضیح |
|------|------|-------|
| رابط کاربری (UI) | `src/pages/*.jsx`, `src/components/*` | فقط نمایش و تعامل کاربر |
| منطق برنامه | `src/pages/Home.jsx` (کنترل‌کننده رویدادها) | هماهنگی بین UI و لایه داده |
| ارتباط با داده | `src/lib/dataService.js` | تنها نقطه دسترسی به داده؛ امضای ثابت، پیاده‌سازی قابل تعویض |
| احراز هویت | `src/lib/AuthContext.jsx`, `src/components/ProtectedRoute.jsx` | دارایی Base44 (در ادامه) |

### لایه داده (`dataService.js`)

پیاده‌سازی فعلی از `localStorage` مرورگر استفاده می‌کند و فقط داده آزمایشی
نگه می‌دارد. برای اتصال به Supabase کافی است **بدنه** توابع `listRecords`،
`addRecord` و `clearRecords` را با فراخوانی‌های `supabase` client جایگزین کنید؛
امضای توابع ثابت می‌ماند و صفحه‌ها تغییری نیاز ندارند.

## وابستگی‌های Base44 (مستندسازی؛ در این نسخه جایگزین شده‌اند)

این موارد برای اجرای مستقل باید جایگزین شوند:

1. **احراز هویت** — `base44.auth.*`
   - `loginViaEmailPassword`, `loginWithProvider`, `me`, `logout`,
     `redirectToLogin` در `src/lib/AuthContext.jsx` و `src/pages/Login.jsx`.
   - جایگزین پیشنهادی برای محیط مستقل: **Supabase Auth**.
   - فایل‌های `Login/Register/ForgotPassword/ResetPassword` و `AuthContext`/
     `ProtectedRoute` از دارایی‌های Base44 هستند.

2. **SDK کلاینت** — `@/api/base44Client` و پکیج `@base44/sdk`
   - نمونه اولیه‌سازی شده‌ی SDK که `base44.auth` و (در صورت نیاز) `base44.entities`
     را فراهم می‌کند.

3. **ساخت و میزبانی** — `@base44/vite-plugin`
   - افزونه Vite مخصوص Base44. برای ساخت مستقل می‌توان آن را حذف و از Vite
     استاندارد استفاده کرد.

4. **پکیج‌های قابل‌حمل (غیر Base44)** — React, Tailwind CSS, shadcn/ui,
   lucide-react, react-router-dom, Vite. این‌ها بدون تغییر در محیط مستقل هم
   کار می‌کنند.

> در این آزمایش از ساخت توابع اختصاصی Base44 (backend functions) و از
> Entityهای Base44 استفاده نشده است؛ داده‌ها کاملاً از لایه جداگانه‌ی
> `dataService.js` خوانده می‌شوند تا وابستگی به حداقل برسد.

## اتصال به Supabase (طرح پیشنهادی)

> بدون تأیید شما هیچ تغییری در دیتابیس ایجاد نمی‌شود. طرح زیر فقط پیشنهاد است.

### جداول پیشنهادی

```sql
-- پروفایل کاربر
create table profiles (
  id uuid primary key references auth.users on delete cascade,
  full_name text,
  role text default 'user',
  created_at timestamptz default now()
);

-- رکوردهای آزمایشی
create table test_records (
  id uuid primary key default gen_random_uuid(),
  title text not null default 'رکورد آزمایشی',
  value int,
  created_by text,
  user_id uuid not null references auth.users on delete cascade,
  created_at timestamptz default now()
);
```

### سیاست‌های امنیتی (RLS)

```sql
alter table profiles enable row level security;
alter table test_records enable row level security;

-- هر کاربر فقط رکورد خودش را می‌بیند/ویرایش می‌کند
create policy "own profile" on profiles
  for all using (auth.uid() = id);

create policy "own records read" on test_records
  for select using (auth.uid() = user_id);
create policy "own records insert" on test_records
  for insert with check (auth.uid() = user_id);
create policy "own records update" on test_records
  for update using (auth.uid() = user_id);
create policy "own records delete" on test_records
  for delete using (auth.uid() = user_id);
```

### مراحل اتصال (پس از تأیید)

1. ایجاد پروژه و جداول در Supabase و اعمال سیاست‌های بالا.
2. نصب `@supabase/supabase-js` و ساخت کلاینت با متغیرهای محیطی
   (`VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`) — کلیدها داخل کد قرار
   نمی‌گیرند.
3. جایگزینی بدنه `dataService.js` با فراخوانی‌های Supabase.
4. جایگزینی `AuthContext`/`ProtectedRoute` با Supabase Auth.

## امنیت

- هشدار: در نسخهٔ آزمایشی فعلی، کاربران محلی با هش SHA-256 بدون salt در localStorage ذخیره می‌شوند؛ این امن نیست و فقط برای آزمایش است. با Supabase Auth جایگزین شود.
- کلیدهای محرمانه داخل کد قرار نمی‌گیرند (با متغیرهای محیطی مدیریت می‌شوند).
- دکمه‌های رابط کاربری جایگزین کنترل دسترسی سمت سرور نیستند؛ عملیات حساس
  باید توسط بک‌اند و مجوزهای دیتابیس بررسی شوند.
- داده‌های واقعی (حساب صراف، مشتریان، موجودی‌ها، تراکنش‌ها) استفاده نمی‌شوند.

## خروجی ZIP و انتقال به GitHub

- **ZIP**: کل پوشه پروژه را (به‌جز `node_modules`) فشرده کنید.
- **GitHub**: یک مخزن جدید بسازید، این پروژه را push کنید، (وابستگی‌های Base44 در این نسخه حذف شده‌اند)، سپس با Vite استاندارد build بگیرید.

## پنل سازنده و دریافت فایل‌ها

- **پنل سازنده**: پس از ورود به مسیر `/builder` (یا از لینک پایین پنل آزمایشی).
  در این پنل نام و نسخه پروژه، فهرست فایل‌ها، مسیرها، وابستگی‌ها، بخش‌های
  وابسته به Base44 و وضعیت ذخیره‌سازی محلی مرورگر نمایش داده می‌شود.
- **فایل ZIP**: دکمه «دانلود فایل ZIP» در پنل سازنده (یا آدرس
  `/base44-independence-experiment.zip`). شامل کل ساختار پروژه به‌جز `node_modules`
  و `dist` است و وابستگی‌های Base44 در آن پنهان نشده‌اند.
- **گزارش PDF**: دکمه «گزارش PDF» در پنل سازنده به مسیر `/report` می‌رود؛ آنجا
  با دکمه «دانلود PDF» یک گزارش فارسی خوانا تولید و دانلود کنید (خروجی تصویری
  است؛ چون jsPDF شکل‌دهی فارسی ندارد، گزارش با html2canvas رندر می‌شود).

## وضعیت استقلال

وابستگی اجرایی به Base44 (SDK، افزونهٔ Vite، فراخوانی‌های `base44.auth.*`) از این
نسخه حذف شده است. احراز هویت فعلی (`src/api/authClient.js`) **محلی و فقط
آزمایشی** است و برای استفادهٔ واقعی مناسب نیست؛ باید با بک‌اند مستقل
(مثلاً Supabase Auth) جایگزین شود. اجرای واقعی (`npm install` / `npm run dev` /
`npm run build`) در محیط تولید این نسخه آزمایش نشده است (دسترسی npm نبود).

نکته: فایل `/base44-independence-experiment.zip` که پنل سازنده به آن لینک می‌دهد
در پوشهٔ `public/` این نسخه وجود ندارد (لینک خراب است).
