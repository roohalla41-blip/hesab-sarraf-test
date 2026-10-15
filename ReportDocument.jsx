import React from "react";
import { projectInfo } from "@/lib/projectInfo";

// رنگ‌ها به‌صورت هگز استفاده شده‌اند تا html2canvas در parsing رنگ‌ها مشکلی نداشته باشد.
const C = {
  bg: "#ffffff",
  text: "#1f2937",
  heading: "#111827",
  muted: "#6b7280",
  accent: "#2563eb",
  border: "#e5e7eb",
  light: "#f9fafb",
  danger: "#b91c1c",
  ok: "#047857",
  warn: "#b45309",
};

const font = { fontFamily: "'Vazirmatn', sans-serif", direction: "rtl" };

const th = {
  border: `1px solid ${C.border}`,
  padding: "6px 8px",
  textAlign: "right",
  fontWeight: 700,
  color: C.heading,
};
const td = { border: `1px solid ${C.border}`, padding: "6px 8px", textAlign: "right" };
const codeBlock = {
  background: C.light,
  border: `1px solid ${C.border}`,
  borderRadius: 6,
  padding: "10px 12px",
  fontSize: 11.5,
  direction: "ltr",
  textAlign: "left",
  margin: "6px 0",
};

function Section({ n, title, children }) {
  return (
    <div style={{ marginTop: 22, borderTop: `1px solid ${C.border}`, paddingTop: 14 }}>
      <h2 style={{ fontSize: 15, fontWeight: 700, color: C.heading, margin: "0 0 10px" }}>
        {n}. {title}
      </h2>
      <div style={{ fontSize: 12.5, lineHeight: 1.9, color: C.text }}>{children}</div>
    </div>
  );
}

function Bullet({ children }) {
  return (
    <div style={{ display: "flex", gap: 8, margin: "4px 0" }}>
      <span style={{ color: C.accent }}>•</span>
      <span>{children}</span>
    </div>
  );
}

function Step({ children }) {
  return (
    <div style={{ display: "flex", gap: 8, margin: "4px 0" }}>
      <span
        style={{
          flexShrink: 0,
          width: 18,
          height: 18,
          borderRadius: "50%",
          background: C.accent,
          color: "#fff",
          fontSize: 10,
          fontWeight: 700,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginTop: 1,
        }}
      >
        ▸
      </span>
      <span>{children}</span>
    </div>
  );
}

function Tree({ nodes, depth = 0 }) {
  return (
    <div>
      {nodes.map((n, i) => (
        <div key={i} style={{ paddingRight: depth * 14 }}>
          <div style={{ fontFamily: "monospace", fontSize: 11.5, color: n.base44 ? C.danger : C.text }}>
            {n.kind === "dir" ? "📁 " : n.base44 ? "🔒 " : "📄 "}
            {n.name}
            {n.base44 ? "  (وابسته به Base44)" : ""}
          </div>
          {n.children && <Tree nodes={n.children} depth={depth + 1} />}
        </div>
      ))}
    </div>
  );
}

export default function ReportDocument() {
  return (
    <div
      id="report-document"
      style={{
        ...font,
        color: C.text,
        background: C.bg,
        padding: "28px 30px",
        width: 794,
        margin: "0 auto",
      }}
    >
      {/* سربرگ */}
      <div style={{ textAlign: "center", borderBottom: `2px solid ${C.accent}`, paddingBottom: 14, marginBottom: 8 }}>
        <div style={{ fontSize: 12, color: C.muted }}>گزارش پروژه</div>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: C.heading, margin: "4px 0" }}>
          {projectInfo.name}
        </h1>
        <div style={{ fontSize: 12, color: C.muted }}>
          نسخه {projectInfo.version} — تاریخ: ۱۴۰۵/۰۷/۲۲
        </div>
      </div>

      <Section n={1} title="معرفی پروژه و هدف آزمایش">
        <p>{projectInfo.goal}</p>
        <p style={{ marginTop: 8 }}>
          این پروژه صرفاً آزمایشی است و از داده‌های واقعی (حساب صراف، مشتریان،
          موجودی‌ها و تراکنش‌ها) استفاده نمی‌کند. هدف، شناسایی وابستگی‌های باقی‌مانده
          به Base44 و بررسی امکان اجرای مستقل کدها پیش از ورود به پروژه اصلی است.
        </p>
      </Section>

      <Section n={2} title="نقشه کلی ساختار برنامه">
        <p>برنامه یک اپلیکیشن تک‌صفحه‌ای (SPA) مبتنی بر React + Vite با مسیرهای زیر است:</p>
        <div style={{ marginTop: 6 }}>
          {projectInfo.routes.map((r) => (
            <Bullet key={r.path}>
              <b>{r.path}</b> — {r.desc}{" "}
              <span style={{ color: r.independent ? C.ok : C.warn, fontSize: 11 }}>
                ({r.independent ? "مستقل" : "وابسته به Base44"})
              </span>
            </Bullet>
          ))}
        </div>
        <p style={{ marginTop: 8 }}>
          لایه‌ها از هم جدا شده‌اند: رابط کاربری (pages/components)، منطق برنامه
          (کنترل‌کننده‌های رویداد) و ارتباط با داده (dataService.js).
        </p>
      </Section>

      <Section n={3} title="فهرست فایل‌ها و پوشه‌های مهم">
        <div
          style={{
            background: C.light,
            border: `1px solid ${C.border}`,
            borderRadius: 8,
            padding: 12,
            marginTop: 6,
          }}
        >
          <Tree nodes={projectInfo.fileTree} />
        </div>
        <p style={{ marginTop: 8, fontSize: 11, color: C.muted }}>
          🔒 = فایل وابسته به Base44 که برای اجرای مستقل باید جایگزین شود.
        </p>
      </Section>

      <Section n={4} title="فناوری‌ها و کتابخانه‌های استفاده‌شده">
        <div style={{ fontWeight: 700, color: C.heading, marginBottom: 4 }}>زمان اجرا:</div>
        {projectInfo.dependencies.runtime.map((d) => (
          <Bullet key={d}>{d}</Bullet>
        ))}
        <div style={{ fontWeight: 700, color: C.heading, margin: "8px 0 4px" }}>ساخت:</div>
        {projectInfo.dependencies.build.map((d) => (
          <Bullet key={d}>{d}</Bullet>
        ))}
        <div style={{ fontWeight: 700, color: C.heading, margin: "8px 0 4px" }}>رابط کاربری:</div>
        {projectInfo.dependencies.ui.map((d) => (
          <Bullet key={d}>{d}</Bullet>
        ))}
      </Section>

      <Section n={5} title="وابستگی‌های مستقیم و غیرمستقیم به Base44">
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 11.5, marginTop: 6 }}>
          <thead>
            <tr style={{ background: C.light }}>
              <th style={th}>بخش</th>
              <th style={th}>فایل‌ها</th>
              <th style={th}>جایگزین</th>
              <th style={th}>شدت</th>
            </tr>
          </thead>
          <tbody>
            {projectInfo.base44Dependencies.map((d) => (
              <tr key={d.item}>
                <td style={td}>{d.item}{d.direct ? "" : " (غیرمستقیم)"}</td>
                <td style={td}>{d.files}</td>
                <td style={td}>{d.replaceWith}</td>
                <td style={{ ...td, color: d.severity === "زیاد" ? C.danger : d.severity === "متوسط" ? C.warn : C.ok }}>
                  {d.severity}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div style={{ fontWeight: 700, color: C.heading, margin: "12px 0 4px" }}>
          بخش‌های مستقل (بدون Base44):
        </div>
        {projectInfo.independentParts.map((p) => (
          <Bullet key={p}>{p}</Bullet>
        ))}
      </Section>

      <Section n={6} title="روش اجرای پروژه در محیطی خارج از Base44">
        <div style={{ fontWeight: 700, color: C.heading, marginBottom: 4 }}>بدون جایگزینی (برای بررسی کد):</div>
        <pre style={codeBlock}>{`npm install
npm run dev`}</pre>
        <div style={{ fontWeight: 700, color: C.heading, margin: "10px 0 4px" }}>برای اجرای کاملاً مستقل:</div>
        <Step>فایل ZIP را استخراج کنید.</Step>
        <Step>در این نسخه وابستگی‌های Base44 حذف شده‌اند و احراز هویت محلی آزمایشی (authClient.js) جایگزین است.</Step>
        <Step>برای محیط واقعی، بدنه authClient.js را با Supabase Auth عوض کنید (فقط پس از تأیید).</Step>
        <Step>npm install و سپس npm run dev را اجرا کنید.</Step>
      </Section>

      <Section n={7} title="مراحل لازم برای انتقال به GitHub">
        <Step>یک مخزن جدید در GitHub بسازید.</Step>
        <Step>{"در پوشه پروژه: git init و git add . و git commit -m initial"}</Step>
        <Step>{"remote را اضافه و push کنید: git remote add origin و git push -u origin main"}</Step>
        <Step>کلیدهای محرمانه را در متغیرهای محیطی (.env) قرار دهید و هرگز داخل کد commit نکنید.</Step>
        <Step>node_modules و dist در .gitignore هستند و وارد مخزن نمی‌شوند.</Step>
      </Section>

      <Section n={8} title="مشکلات احتمالی و کارهای باقی‌مانده">
        {projectInfo.remainingWork.map((w) => (
          <Bullet key={w}>{w}</Bullet>
        ))}
        <p style={{ marginTop: 8, color: C.danger, fontWeight: 700 }}>
          نتیجه: وابستگی اجرایی به Base44 حذف شده است. احراز هویت فعلی محلی و
          آزمایشی است (امن نیست) و برای استفاده واقعی باید با بک‌اند مستقل
          (مثلاً Supabase Auth) جایگزین شود.
        </p>
      </Section>

      <Section n={9} title="نتایج آزمایش عملکرد امکانات برنامه">
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 11.5, marginTop: 6 }}>
          <thead>
            <tr style={{ background: C.light }}>
              <th style={th}>امکان</th>
              <th style={th}>وضعیت</th>
              <th style={th}>ذخیره‌سازی</th>
              <th style={th}>یادداشت</th>
            </tr>
          </thead>
          <tbody>
            {projectInfo.features.map((f) => (
              <tr key={f.name}>
                <td style={td}>{f.name}</td>
                <td style={{ ...td, color: f.works ? C.ok : C.danger, fontWeight: 700 }}>
                  {f.works ? "فعال" : "ناموفق"}
                </td>
                <td style={td}>{f.storage}</td>
                <td style={td}>{f.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p style={{ marginTop: 10, fontSize: 11, color: C.muted, textAlign: "center" }}>
          پایان گزارش — تولیدشده توسط پنل سازنده پروژه آزمایشی
        </p>
      </Section>
    </div>
  );
}