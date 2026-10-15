import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { projectInfo } from "@/lib/projectInfo";
import { dataService } from "@/lib/dataService";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  FileArchive,
  FileText,
  Folder,
  Route,
  Package,
  Lock,
  CheckCircle2,
  HardDrive,
  ArrowRight,
  Loader2,
} from "lucide-react";

function Tree({ nodes, depth = 0 }) {
  return (
    <div>
      {nodes.map((n, i) => (
        <div key={i} style={{ paddingRight: depth * 14 }}>
          <div
            className={`flex items-center gap-1.5 py-0.5 text-xs font-mono ${
              n.base44 ? "text-destructive" : "text-foreground"
            }`}
          >
            <span>{n.kind === "dir" ? "📁" : n.base44 ? "🔒" : "📄"}</span>
            <span>{n.name}</span>
            {n.base44 && <span className="text-[10px] text-muted-foreground">(Base44)</span>}
          </div>
          {n.children && <Tree nodes={n.children} depth={depth + 1} />}
        </div>
      ))}
    </div>
  );
}

export default function BuilderPanel() {
  const [storageStatus, setStorageStatus] = useState({ checking: true });
  const [recordCount, setRecordCount] = useState(0);

  useEffect(() => {
    const available =
      typeof Storage !== "undefined" && !!window.localStorage;
    (async () => {
      let count = 0;
      if (available) {
        try {
          const list = await dataService.listRecords();
          count = list.length;
        } catch {
          /* ignore */
        }
      }
      setStorageStatus({ available });
      setRecordCount(count);
    })();
  }, []);

  return (
    <div className="min-h-screen bg-background" dir="rtl">
      <div className="max-w-3xl mx-auto px-4 py-8">
        {/* سربرگ */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-foreground">پنل سازنده</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {projectInfo.name} — نسخه {projectInfo.version}
            </p>
          </div>
          <Button variant="outline" size="sm" asChild>
            <Link to="/">
              بازگشت به پنل آزمایشی
              <ArrowRight className="w-4 h-4 mr-1" />
            </Link>
          </Button>
        </div>

        {/* دانلودها */}
        <Card className="mb-6 border-primary/30">
          <CardHeader>
            <CardTitle className="text-base">دریافت خروجی‌ها</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <a href={projectInfo.zipFile} download>
              <Button className="w-full h-14 justify-start">
                <FileArchive className="w-5 h-5 ml-2" />
                <div className="text-right">
                  <div>دانلود فایل ZIP</div>
                  <div className="text-[11px] font-normal opacity-80">
                    ساختار کامل پروژه
                  </div>
                </div>
              </Button>
            </a>
            <Button asChild variant="outline" className="w-full h-14 justify-start">
              <Link to="/report">
                <FileText className="w-5 h-5 ml-2" />
                <div className="text-right">
                  <div>گزارش PDF</div>
                  <div className="text-[11px] font-normal opacity-70">
                    تولید و دانلود گزارش
                  </div>
                </div>
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* مسیرها */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Route className="w-4 h-4" />
              ساختار برنامه و مسیرهای اصلی
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-1.5 text-sm">
              {projectInfo.routes.map((r) => (
                <li key={r.path} className="flex items-center justify-between">
                  <span className="font-mono text-xs bg-muted px-2 py-0.5 rounded">
                    {r.path}
                  </span>
                  <span className="text-muted-foreground text-xs flex items-center gap-2">
                    {r.desc}
                    <span
                      className={`text-[10px] px-1.5 py-0.5 rounded ${
                        r.independent
                          ? "bg-emerald-100 text-emerald-700"
                          : "bg-amber-100 text-amber-700"
                      }`}
                    >
                      {r.independent ? "مستقل" : "Base44"}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        {/* فایل‌ها */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Folder className="w-4 h-4" />
              فهرست فایل‌ها و پوشه‌ها
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Tree nodes={projectInfo.fileTree} />
            <p className="text-[11px] text-muted-foreground mt-3">
              🔒 = فایل وابسته به Base44 (در این نسخه فایلی با این علامت باقی نمانده است)
            </p>
          </CardContent>
        </Card>

        {/* وابستگی‌ها */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Package className="w-4 h-4" />
              وابستگی‌های نرم‌افزاری
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <DepGroup title="زمان اجرا" items={projectInfo.dependencies.runtime} />
            <DepGroup title="ساخت" items={projectInfo.dependencies.build} />
            <DepGroup title="رابط کاربری" items={projectInfo.dependencies.ui} />
            <DepGroup
              title="Base44 (برای حذف/جایگزینی)"
              items={projectInfo.dependencies.base44}
              danger
            />
          </CardContent>
        </Card>

        {/* وابستگی‌های Base44 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Lock className="w-4 h-4" />
              بخش‌های وابسته به Base44
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {projectInfo.base44Dependencies.map((d) => (
              <div
                key={d.item}
                className="border border-border rounded-lg p-3 text-sm"
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium">{d.item}</span>
                  <span
                    className={`text-[10px] px-1.5 py-0.5 rounded ${
                      d.severity === "زیاد"
                        ? "bg-red-100 text-red-700"
                        : d.severity === "متوسط"
                        ? "bg-amber-100 text-amber-700"
                        : "bg-slate-100 text-slate-600"
                    }`}
                  >
                    شدت: {d.severity}
                  </span>
                </div>
                <div className="text-xs text-muted-foreground">
                  فایل‌ها: {d.files}
                </div>
                <div className="text-xs text-emerald-700 mt-1">
                  جایگزین: {d.replaceWith}
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* وضعیت ذخیره‌سازی محلی */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <HardDrive className="w-4 h-4" />
              وضعیت ذخیره‌سازی محلی مرورگر
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {storageStatus.checking ? (
              <div className="flex items-center gap-2 text-muted-foreground">
                <Loader2 className="w-4 h-4 animate-spin" />
                در حال بررسی…
              </div>
            ) : (
              <>
                <Row
                  label="localStorage"
                  value={
                    storageStatus.available ? (
                      <span className="text-emerald-600 flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4" /> در دسترس
                      </span>
                    ) : (
                      <span className="text-destructive">ناموجود</span>
                    )
                  }
                />
                <Row
                  label="کلید داده آزمایشی"
                  value={<span className="font-mono text-xs">test_records</span>}
                />
                <Row
                  label="تعداد رکوردهای آزمایشی"
                  value={<span className="font-mono">{recordCount}</span>}
                />
                <p className="text-xs text-muted-foreground pt-2 border-t border-border">
                  داده‌ها فقط در همین مرورگر و فقط برای آزمایش نگه‌داری می‌شوند؛
                  هیچ اتصال به Supabase یا دیتابیس خارجی وجود ندارد.
                </p>
              </>
            )}
          </CardContent>
        </Card>

        {/* امکانات */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4" />
              وضعیت امکانات آزمایشی
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="divide-y divide-border">
              {projectInfo.features.map((f) => (
                <li
                  key={f.name}
                  className="py-2 flex items-center justify-between text-sm"
                >
                  <div>
                    <div className="font-medium">{f.name}</div>
                    <div className="text-xs text-muted-foreground">{f.note}</div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground">
                      {f.storage}
                    </span>
                    <span
                      className={`text-xs px-2 py-0.5 rounded ${
                        f.works
                          ? "bg-emerald-100 text-emerald-700"
                          : "bg-red-100 text-red-700"
                      }`}
                    >
                      {f.works ? "فعال" : "ناموفق"}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function DepGroup({ title, items, danger }) {
  return (
    <div>
      <div
        className={`text-xs font-medium mb-1 ${danger ? "text-destructive" : "text-muted-foreground"}`}
      >
        {title}
      </div>
      <div className="flex flex-wrap gap-1.5">
        {items.map((d) => (
          <span
            key={d}
            className={`text-xs px-2 py-1 rounded font-mono ${
              danger ? "bg-red-50 text-red-700 border border-red-200" : "bg-muted"
            }`}
          >
            {d}
          </span>
        ))}
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span>{value}</span>
    </div>
  );
}