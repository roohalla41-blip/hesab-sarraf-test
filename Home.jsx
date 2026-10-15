import React, { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/lib/AuthContext";
import { dataService } from "@/lib/dataService";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Info, Plus, LogOut, User, Database, Trash2, Loader2 } from "lucide-react";
import { Link } from "react-router-dom";

export default function Home() {
  const { user, logout } = useAuth();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [showInfo, setShowInfo] = useState(false);

  const loadRecords = useCallback(async () => {
    setLoading(true);
    try {
      const list = await dataService.listRecords();
      setRecords(list);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRecords();
  }, [loadRecords]);

  const handleAdd = async () => {
    setAdding(true);
    try {
      await dataService.addRecord({
        created_by: user?.full_name || user?.email || "کاربر آزمایشی",
      });
      await loadRecords();
    } finally {
      setAdding(false);
    }
  };

  const handleClear = async () => {
    await dataService.clearRecords();
    await loadRecords();
  };

  const displayName = user?.full_name || user?.email || "کاربر آزمایشی";

  return (
    <div className="min-h-screen bg-background" dir="rtl">
      <div className="max-w-2xl mx-auto px-4 py-8">
        {/* سربرگ */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-foreground">پنل آزمایشی</h1>
            <p className="text-sm text-muted-foreground mt-1 flex items-center gap-1.5">
              <User className="w-4 h-4" />
              {displayName}
            </p>
          </div>
          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
            <User className="w-5 h-5 text-primary" />
          </div>
        </div>

        {/* دکمه‌ها */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8">
          <Button
            variant="outline"
            className="h-14 justify-start"
            onClick={() => setShowInfo((s) => !s)}
          >
            <Info className="w-5 h-5 ml-2" />
            نمایش اطلاعات
          </Button>
          <Button
            className="h-14 justify-start"
            onClick={handleAdd}
            disabled={adding}
          >
            {adding ? (
              <Loader2 className="w-5 h-5 ml-2 animate-spin" />
            ) : (
              <Plus className="w-5 h-5 ml-2" />
            )}
            افزودن رکورد آزمایشی
          </Button>
          <Button
            variant="destructive"
            className="h-14 justify-start"
            onClick={() => logout()}
          >
            <LogOut className="w-5 h-5 ml-2" />
            خروج
          </Button>
        </div>

        {/* پنل اطلاعات کاربر */}
        {showInfo && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-base">اطلاعات کاربر</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">نام:</span>
                <span className="font-medium">{user?.full_name || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">ایمیل:</span>
                <span className="font-medium">{user?.email || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">نقش:</span>
                <span className="font-medium">{user?.role || "—"}</span>
              </div>
              <div className="text-muted-foreground pt-3 border-t border-border text-xs">
                این داده‌ها از نشست احراز هویت خوانده شده‌اند و در مرورگر ذخیره
                نمی‌شوند.
              </div>
            </CardContent>
          </Card>
        )}

        {/* فهرست رکوردها */}
        <Card>
          <CardHeader className="flex-row items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <Database className="w-4 h-4" />
              فهرست رکوردهای آزمایشی
            </CardTitle>
            {records.length > 0 && (
              <Button variant="ghost" size="sm" onClick={handleClear}>
                <Trash2 className="w-4 h-4 ml-1" />
                پاک کردن
              </Button>
            )}
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex justify-center py-8">
                <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
              </div>
            ) : records.length === 0 ? (
              <p className="text-center text-sm text-muted-foreground py-8">
                هنوز رکوردی ثبت نشده است.
              </p>
            ) : (
              <ul className="divide-y divide-border">
                {records.map((r) => (
                  <li
                    key={r.id}
                    className="py-3 flex items-center justify-between"
                  >
                    <div>
                      <div className="font-medium text-sm">{r.title}</div>
                      <div className="text-xs text-muted-foreground mt-0.5">
                        {r.created_by} •{" "}
                        {new Date(r.created_at).toLocaleString("fa-IR")}
                      </div>
                    </div>
                    <span className="text-sm font-mono text-primary">
                      {r.value}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <div className="text-center text-xs text-muted-foreground mt-8 space-y-2">
          <p>نسخه آزمایشی — داده‌ها فقط در همین مرورگر ذخیره می‌شوند.</p>
          <Link to="/builder" className="text-primary hover:underline">
            ورود به پنل سازنده
          </Link>
        </div>
      </div>
    </div>
  );
}