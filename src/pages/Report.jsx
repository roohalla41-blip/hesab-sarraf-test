import React, { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import ReportDocument from "@/pages/ReportDocument";
import { downloadReportPdf } from "@/lib/generatePdf";
import { ArrowRight, Download, Printer, Loader2 } from "lucide-react";

export default function Report() {
  const docRef = useRef(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState("");

  const handleDownload = async () => {
    if (!docRef.current) return;
    setError("");
    setGenerating(true);
    try {
      await downloadReportPdf(docRef.current);
    } catch (e) {
      setError("خطا در تولید PDF: " + (e?.message || "نامشخص"));
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="min-h-screen bg-background" dir="rtl">
      <div className="max-w-3xl mx-auto px-4 py-8">
        {/* نوار ابزار */}
        <div className="no-print flex items-center justify-between mb-6 sticky top-0 bg-background/80 backdrop-blur py-2 z-10">
          <Button asChild variant="outline" size="sm">
            <Link to="/builder">
              <ArrowRight className="w-4 h-4 ml-1" />
              بازگشت به پنل سازنده
            </Link>
          </Button>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.print()}
              disabled={generating}
            >
              <Printer className="w-4 h-4 ml-1" />
              چاپ
            </Button>
            <Button size="sm" onClick={handleDownload} disabled={generating}>
              {generating ? (
                <Loader2 className="w-4 h-4 ml-1 animate-spin" />
              ) : (
                <Download className="w-4 h-4 ml-1" />
              )}
              دانلود PDF
            </Button>
          </div>
        </div>

        {error && (
          <div className="no-print mb-4 p-3 rounded-lg bg-destructive/10 text-destructive text-sm">
            {error}
          </div>
        )}

        {/* سند گزارش */}
        <div className="shadow-lg rounded-lg overflow-hidden bg-white">
          <div ref={docRef}>
            <ReportDocument />
          </div>
        </div>

        <p className="no-print text-center text-xs text-muted-foreground mt-4">
          برای دریافت PDF روی «دانلود PDF» بزنید. خروجی تصویری و خوانا است.
        </p>
      </div>
    </div>
  );
}