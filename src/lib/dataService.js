/**
 * لایه ارتباط با داده (Data Layer)
 * ------------------------------------------------------------------
 * این ماژول تنها راه دسترسی صفحه‌ها به داده‌های آزمایشی است تا رابط کاربری،
 * منطق برنامه و ارتباط با دیتابیس از هم جدا بمانند.
 *
 * پیاده‌سازی فعلی: داده‌های آزمایشی در localStorage مرورگر نگه‌داری می‌شوند.
 * این فقط برای نسخه اول آزمایشی است و هیچ داده واقعی در آن قرار ندارد.
 *
 * اتصال به Supabase: کافی است بدنه این توابع را با فراخوانی‌های Supabase
 * client جایگزین کنید؛ امضای توابع (list/add/clear) ثابت می‌ماند تا صفحه‌ها
 * نیازی به تغییر نداشته باشند. طرح جداول و سیاست‌های پیشنهادی در README.md
 * آمده است.
 */

const STORAGE_KEY = "test_records";

function readAll() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function writeAll(records) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records));
}

export const dataService = {
  /** بازگرداندن همه رکوردهای آزمایشی (نزدیک‌ترین به جدیدترین). */
  async listRecords() {
    return readAll().sort(
      (a, b) => new Date(b.created_at) - new Date(a.created_at)
    );
  },

  /** افزودن یک رکورد آزمایشی جدید. */
  async addRecord(payload = {}) {
    const records = readAll();
    const record = {
      id: crypto.randomUUID(),
      title: payload.title || "رکورد آزمایشی",
      value: payload.value ?? Math.floor(Math.random() * 1000),
      created_by: payload.created_by || "کاربر آزمایشی",
      created_at: new Date().toISOString(),
    };
    records.push(record);
    writeAll(records);
    return record;
  },

  /** پاک کردن همه رکوردهای آزمایشی. */
  async clearRecords() {
    writeAll([]);
  },
};