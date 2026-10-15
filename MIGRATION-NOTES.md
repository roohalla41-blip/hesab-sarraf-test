# یادداشت جداسازی از Base44 (نسخهٔ کپی)

تغییرات نسبت به نسخهٔ اصلی:
- حذف `@base44/sdk` و `@base44/vite-plugin` از package.json و vite.config.js (alias «@» به‌صورت دستی اضافه شد).
- حذف `src/api/base44Client.js`، `src/lib/app-params.js`، `OAuthConsent.jsx`، `GoogleIcon.jsx` و پوشهٔ `base44/`.
- افزودن `src/api/authClient.js` (احراز هویت محلی آزمایشی با localStorage، بدون سرور) و بازنویسی `AuthContext.jsx`.
- Login: ورود با ایمیل/رمز + دکمهٔ «ورود آزمایشی». Register: بدون کد تأیید ایمیل. Forgot/Reset: در این حالت پشتیبانی نمی‌شود.
- index.html: حذف favicon و manifest مربوط به Base44.

بعد از دریافت: `npm install` (package-lock.json را به‌روز می‌کند) سپس `npm run dev`.
توجه: authClient امن نیست و فقط برای آزمایش است.

## دور دوم
- حذف هوک‌های ویرایشگر Base44 از کامپوننت‌های تصویر (`data-base44-*`, رویداد `base44:image-replace`, میزبان `media.base44.com`).
- پاک‌سازی دستی `package-lock.json` (حذف `@base44/*` و وابستگی‌های یتیم‌شده)؛ با npm تأیید نشده. اگر `npm ci` خطا داد، `npm install` اجرا کنید.
- به‌روزرسانی متن README، پنل سازنده و گزارش PDF با وضعیت جدید.
- Service Worker و manifest در پروژه وجود نداشتند.
- هیچ داده یا جدولی حذف/تغییر نشد (داده‌های localStorage دست‌نخورده‌اند؛ پایگاه‌دادهٔ سمت سرور در پروژه نیست).

## دور سوم (بررسی و گزارش)
- تحلیل ایستا: هیچ import یا فراخوانی اجرایی به Base44 باقی نمانده؛ ارجاع‌های باقی‌مانده فقط متن/برچسب مستندات (README، پنل سازنده، گزارش) و فایل‌های `AGENTS.md`/`CLAUDE.md` و یک خط در `.gitignore` هستند.
- افزودن هشدار «حالت آزمایشی/ناامن» در صفحه‌های ورود و ثبت‌نام؛ اصلاح متن README دربارهٔ وضعیت استقلال.
- نیازمندی اجرا: Node ‎^20.19 یا ≥22.12 (الزام Vite 8 و plugin-react 6).
- اجرای `npm install` / `npm run dev` / `npm run build` آزمایش نشده است (دسترسی npm در محیط بررسی مسدود بود).
