import { jsPDF } from "jspdf";
import html2canvas from "html2canvas";

/**
 * تولید فایل PDF از یک عنصر HTML با استفاده از html2canvas + jsPDF.
 * چون jsPDF از شکل‌دهی خودکار متن فارسی/عربی پشتیبانی نمی‌کند، ابتدا عنصر
 * توسط مرورگر (با شکل‌دهی و RTL صحیح) رندر و سپس به تصویر تبدیل می‌شود؛
 * نتیجه یک PDF خوانا (تصویری) است.
 */
export async function downloadReportPdf(
  element,
  fileName = "گزارش-آزمایش-استقلال-Base44.pdf"
) {
  // اطمینان از بارگذاری فونت قبل از رندر
  if (document.fonts && document.fonts.ready) {
    try {
      await document.fonts.ready;
    } catch {
      /* ignore */
    }
  }

  const canvas = await html2canvas(element, {
    scale: 2,
    backgroundColor: "#ffffff",
    useCORS: true,
    logging: false,
  });

  const imgData = canvas.toDataURL("image/jpeg", 0.92);
  const pdf = new jsPDF("p", "mm", "a4");
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const imgWidth = pageWidth;
  const imgHeight = (canvas.height * imgWidth) / canvas.width;

  let heightLeft = imgHeight;
  let position = 0;
  pdf.addImage(imgData, "JPEG", 0, position, imgWidth, imgHeight);
  heightLeft -= pageHeight;
  while (heightLeft > 0) {
    position -= pageHeight;
    pdf.addPage();
    pdf.addImage(imgData, "JPEG", 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;
  }
  pdf.save(fileName);
}