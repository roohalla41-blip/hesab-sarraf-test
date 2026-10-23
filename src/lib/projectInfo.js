/**
 * فراداده‌ی پروژه آزمایشی — منبع واحد داده‌ها برای پنل سازنده و گزارش PDF.
 * این داده‌ها به‌صورت دستی نگه‌داری می‌شوند تا صفحه‌ها بدون خواندن فایل‌سیستم
 * کار کنند. در صورت تغییر ساختار پروژه، این فایل را به‌روز کنید.
 */

export const projectInfo = {
  name: "آزمایش استقلال از Base44",
  version: "0.1.0",
  goal:
    "بررسی امکان اجرای مستقل کدهای تولیدشده در Base44 خارج از پلتفرم و آماده‌سازی " +
    "برای اتصال آینده به Supabase. در صورت موفقیت، همین روش برای پروژه اصلی " +
    "«حساب صراف» به‌کار گرفته خواهد شد.",
  zipFile: "/base44-independence-experiment.zip",
  routes: [
    { path: "/", desc: "پنل آزمایشی (محافظت‌شده)", independent: true },
    { path: "/login", desc: "صفحه ورود", independent: true },
    { path: "/register", desc: "ساخت حساب", independent: true },
    { path: "/forgot-password", desc: "فراموشی رمز عبور", independent: true },
    { path: "/reset-password", desc: "بازنشانی رمز عبور", independent: true },
    { path: "/builder", desc: "پنل سازنده", independent: true },
    { path: "/report", desc: "گزارش PDF", independent: true },
  ],
  fileTree: [
    { name: "index.html", kind: "file" },
    { name: "package.json", kind: "file" },
    { name: "package-lock.json", kind: "file" },
    { name: "vite.config.js", kind: "file" },
    { name: "tailwind.config.js", kind: "file" },
    { name: "postcss.config.js", kind: "file" },
    { name: "jsconfig.json", kind: "file" },
    { name: "eslint.config.js", kind: "file" },
    { name: "components.json", kind: "file" },
    { name: ".gitignore", kind: "file" },
    { name: ".npmrc", kind: "file" },
    { name: "README.md", kind: "file" },
    {
      name: "public/",
      kind: "dir",
      children: [{ name: "base44-independence-experiment.zip", kind: "file" }],
    },
    {
      name: "src/",
      kind: "dir",
      children: [
        { name: "App.jsx", kind: "file" },
        { name: "main.jsx", kind: "file" },
        { name: "index.css", kind: "file" },
        { name: "api/authClient.js", kind: "file" },
        {
          name: "components/",
          kind: "dir",
          children: [
            { name: "AuthLayout.jsx", kind: "file" },
            { name: "ProtectedRoute.jsx", kind: "file" },
            { name: "ScrollToTop.jsx", kind: "file" },
            { name: "UserNotRegisteredError.jsx", kind: "file" },
            { name: "ReportDocument.jsx", kind: "file" },
            { name: "ui/ (shadcn)", kind: "dir" },
          ],
        },
        {
          name: "lib/",
          kind: "dir",
          children: [
            { name: "AuthContext.jsx", kind: "file" },
            { name: "authReturnTo.js", kind: "file" },
            { name: "dataService.js", kind: "file" },
            { name: "generatePdf.js", kind: "file" },
            { name: "projectInfo.js", kind: "file" },
            { name: "query-client.js", kind: "file" },
            { name: "utils.js", kind: "file" },
            { name: "PageNotFound.jsx", kind: "file" },
          ],
        },
        {
          name: "pages/",
          kind: "dir",
          children: [
            { name: "Home.jsx", kind: "file" },
            { name: "Login.jsx", kind: "file" },
            { name: "Register.jsx", kind: "file" },
            { name: "ForgotPassword.jsx", kind: "file" },
            { name: "ResetPassword.jsx", kind: "file" },
            { name: "BuilderPanel.jsx", kind: "file" },
            { name: "Report.jsx", kind: "file" },
          ],
        },
        { name: "hooks/", kind: "dir" },
        { name: "utils/index.ts", kind: "file" },
      ],
    },
  ],
  dependencies: {
    runtime: [
      "react ^18.2",
      "react-dom ^18.2",
      "react-router-dom ^6.26",
      "lucide-react ^0.475",
      "jspdf ^4.2 (تولید PDF)",
      "html2canvas ^1.4 (رندر گزارش به PDF)",
    ],
    build: [
      "vite ^8.2",
      "@vitejs/plugin-react ^6.0",
      "tailwindcss ^3.4",
      "postcss ^8.5",
      "autoprefixer ^10.4",
    ],
    ui: ["shadcn/ui (مبتنی بر Radix UI)", "tailwind css", "clsx", "tailwind-merge"],
    base44: [],
  },
  base44Dependencies: [
    {
      item: "احراز هویت (base44.auth.*)",
      files: "AuthContext.jsx, Login.jsx, Register.jsx, ForgotPassword.jsx, ResetPassword.jsx",
      replaceWith: "authClient.js محلی (آزمایشی، localStorage) — جایگزین نهایی: Supabase Auth",
      severity: "برطرف شد",
      direct: true,
    },
    {
      item: "SDK کلاینت (@base44/sdk)",
      files: "src/api/base44Client.js (حذف شد)",
      replaceWith: "حذف شد؛ بدون جایگزین (SDK دیگری لازم نیست)",
      severity: "برطرف شد",
      direct: true,
    },
    {
      item: "افزونه ساخت (@base44/vite-plugin)",
      files: "vite.config.js",
      replaceWith: "Vite استاندارد + alias دستی «@»",
      severity: "برطرف شد",
      direct: true,
    },
    {
      item: "فراداده و هوک‌های ویرایشگر Base44",
      files: "base44/, ui/responsive-image, ui/use-responsive-image, index.html",
      replaceWith: "حذف شد",
      severity: "برطرف شد",
      direct: false,
    },
  ],
  independentParts: [
    "احراز هویت محلی آزمایشی (authClient.js + localStorage) — جایگزین Base44 Auth؛ فقط برای آزمایش",
    "رابط کاربری (React + Tailwind + shadcn/ui) — کاملاً مستقل",
    "لایه داده (dataService.js با localStorage) — بدون اتصال به Base44",
    "منطق برنامه (Home.jsx) — بدون فراخوانی Base44",
    "پنل سازنده و گزارش PDF — فقط رابط کاربری و کتابخانه‌های استاندارد",
  ],
  features: [
    { name: "صفحه ورود", works: true, storage: "—", note: "احراز هویت محلی آزمایشی (authClient)" },
    { name: "پنل آزمایشی + نام کاربر", works: true, storage: "—", note: "مستقل" },
    { name: "نمایش اطلاعات کاربر", works: true, storage: "—", note: "خوانده‌شده از نشست" },
    { name: "افزودن رکورد آزمایشی", works: true, storage: "localStorage (test_records)", note: "مستقل" },
    { name: "نمایش فهرست رکوردها", works: true, storage: "localStorage", note: "مستقل" },
    { name: "خروج", works: true, storage: "—", note: "احراز هویت محلی آزمایشی (authClient)" },
    { name: "پنل سازنده", works: true, storage: "—", note: "مستقل" },
    { name: "گزارش PDF", works: true, storage: "—", note: "مستقل (html2canvas + jsPDF)" },
  ],
  remainingWork: [
    "جایگزینی authClient محلی با Supabase Auth (فقط پس از تأیید)",
    "افزودن فونت Vazirmatn به‌صورت محلی برای اجرای آفلاین (فعلاً از Google Fonts بارگذاری می‌شود)",
    "PDF تولیدشده تصویری است (متن قابل انتخاب نیست)؛ در صورت نیاز به PDF متنی، باید فونت فارسی در jsPDF تعبیه شود",
    "برای اتصال آینده به Supabase: ایجاد جداول و سیاست‌های RLS طبق README (فقط پس از تأیید)",
  ],
};