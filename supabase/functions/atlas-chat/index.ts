import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const allowedOrigin = "https://roohalla41-blip.github.io";
const baseCorsHeaders = {
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-request-id",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};

const windows = new Map<string, number[]>();
const globalWindow: number[] = [];
const MINUTE = 60_000;
const HOUR = 60 * MINUTE;

function reply(status: number, data: Record<string, unknown>, origin: string | null) {
  const headers: Record<string, string> = {
    ...baseCorsHeaders,
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
  };
  if (origin === allowedOrigin) headers["Access-Control-Allow-Origin"] = allowedOrigin;
  return new Response(JSON.stringify(data), { status, headers });
}

function limited(id: string): boolean {
  const now = Date.now();
  for (const [key, values] of windows) {
    const fresh = values.filter((t) => now - t < HOUR);
    if (fresh.length) windows.set(key, fresh);
    else windows.delete(key);
  }
  const all = globalWindow.filter((t) => now - t < MINUTE);
  globalWindow.splice(0, globalWindow.length, ...all);
  if (globalWindow.length >= 200) return true;
  const own = (windows.get(id) || []).filter((t) => now - t < HOUR);
  if (own.filter((t) => now - t < MINUTE).length >= 20 || own.length >= 200) return true;
  own.push(now);
  windows.set(id, own);
  globalWindow.push(now);
  return false;
}

type Provider = { name: string; endpoint: string; apiKey: string; model: string };

function completionEndpoint(base: string): string {
  const clean = base.trim().replace(/\/+$/, "");
  return /\/chat\/completions$/i.test(clean) ? clean : clean + "/chat/completions";
}

function configuredProviders(): Provider[] {
  const providers: Provider[] = [];
  const genericKey = Deno.env.get("AI_API_KEY");
  const genericUrl = Deno.env.get("AI_API_URL");
  const genericModel = Deno.env.get("AI_MODEL");
  if (genericKey && genericUrl && genericModel) {
    providers.push({
      name: "configured",
      endpoint: completionEndpoint(genericUrl),
      apiKey: genericKey,
      model: genericModel,
    });
  }

  const groqKey = Deno.env.get("GROQ_API_KEY");
  if (groqKey) {
    providers.push({
      name: "groq",
      endpoint: completionEndpoint(Deno.env.get("GROQ_API_URL") || "https://api.groq.com/openai/v1"),
      apiKey: groqKey,
      model: Deno.env.get("GROQ_MODEL") || "openai/gpt-oss-20b",
    });
  }

  const zaiKey = Deno.env.get("ZAI_API_KEY");
  const zaiModel = Deno.env.get("ZAI_MODEL");
  if (zaiKey && zaiModel) {
    providers.push({
      name: "zai",
      endpoint: Deno.env.get("ZAI_API_URL") || "https://api.z.ai/api/paas/v4/chat/completions",
      apiKey: zaiKey,
      model: zaiModel,
    });
  }

  const cloudflareToken = Deno.env.get("CLOUDFLARE_API_TOKEN") || Deno.env.get("ATLAS_CLOUDFLARE");
  const cloudflareAccount = Deno.env.get("CLOUDFLARE_ACCOUNT_ID");
  const cloudflareModel = Deno.env.get("CLOUDFLARE_MODEL");
  if (cloudflareToken && cloudflareAccount && cloudflareModel) {
    providers.push({
      name: "cloudflare",
      endpoint: "https://api.cloudflare.com/client/v4/accounts/" +
        encodeURIComponent(cloudflareAccount) + "/ai/v1/chat/completions",
      apiKey: cloudflareToken,
      model: cloudflareModel,
    });
  }
  return providers;
}

async function askProvider(provider: Provider, messages: Array<{ role: string; content: string }>) {
  const upstream = await fetch(provider.endpoint, {
    method: "POST",
    headers: {
      "Authorization": "Bearer " + provider.apiKey,
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    body: JSON.stringify({
      model: provider.model,
      messages,
      temperature: 0.4,
      max_tokens: 900,
    }),
    signal: AbortSignal.timeout(30_000),
  });
  if (!upstream.ok) return null;
  const data = await upstream.json();
  const content = data?.choices?.[0]?.message?.content;
  const answer = typeof content === "string" ? content.trim() :
    Array.isArray(content) ? content.map((part: { text?: unknown }) =>
      typeof part?.text === "string" ? part.text : "").join("").trim() : "";
  return answer || null;
}

Deno.serve(async (req: Request) => {
  const origin = req.headers.get("origin");
  if (origin && origin !== allowedOrigin) {
    return reply(403, { error: "مبدأ درخواست مجاز نیست." }, origin);
  }
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: origin === allowedOrigin
        ? { ...baseCorsHeaders, "Access-Control-Allow-Origin": allowedOrigin }
        : baseCorsHeaders,
    });
  }
  if (req.method !== "POST") {
    return reply(405, { error: "روش درخواست پشتیبانی نمی‌شود." }, origin);
  }

  const ip = (req.headers.get("cf-connecting-ip") || req.headers.get("x-real-ip") ||
    req.headers.get("x-forwarded-for") || "unknown").split(",")[0].trim().slice(0, 100);
  if (limited(ip)) {
    return reply(429, { error: "تعداد درخواست‌ها در مدت کوتاه زیاد شده است؛ کمی بعد دوباره تلاش کن." }, origin);
  }

  let body: { message?: unknown; history?: unknown; mode?: unknown };
  try {
    body = await req.json();
  } catch {
    return reply(400, { error: "درخواست نامعتبر است." }, origin);
  }
  const message = typeof body.message === "string" ? body.message.trim() : "";
  if (!message) return reply(400, { error: "پیام خالی است." }, origin);
  if (message.length > 3_000) return reply(413, { error: "پیام بیش از حد طولانی است." }, origin);

  const providers = configuredProviders();
  if (!providers.length) {
    return reply(503, { error: "هیچ ارائه‌دهنده هوش مصنوعی در تنظیمات امن سرور پیکربندی نشده است." }, origin);
  }

  const history: Array<{ role: "user" | "assistant"; content: string }> = [];
  if (Array.isArray(body.history)) {
    for (const item of body.history.slice(-8)) {
      if (!item || typeof item !== "object") continue;
      const role = item.role;
      const content = item.content;
      if ((role === "user" || role === "assistant") &&
        typeof content === "string" && content.trim()) {
        history.push({ role, content: content.trim().slice(0, 1_000) });
      }
    }
  }
  const mode = typeof body.mode === "string" ? body.mode.slice(0, 40) : "عمومی";
  const system = "تو دستیار هوشمند ATLAS AI (اطلس) هستی. به زبان پیام کاربر پاسخ بده؛ فارسی و دری را روشن و طبیعی بنویس. به آخرین پیام مستقیماً پاسخ بده و پیوستگی تاریخچه را حفظ کن. دقیق، مفید و صادق باش؛ اگر چیزی را نمی‌دانی صریح بگو. ادعای دسترسی به فایل‌ها، حساب‌ها یا اطلاعات خصوصی کاربر نکن. کلیدها و اطلاعات محرمانه را درخواست نکن. حالت گفتگو: " + mode + ".";

  const messages = [
    { role: "system", content: system },
    ...history,
    { role: "user", content: message },
  ];

  for (const provider of providers) {
    try {
      const answer = await askProvider(provider, messages);
      if (answer) return reply(200, { reply: answer, provider: provider.name, model: provider.model }, origin);
    } catch {
      // Continue to the next configured provider without exposing provider errors or secrets.
    }
  }
  return reply(502, { error: "هیچ‌یک از سرویس‌های هوش مصنوعی در حال حاضر پاسخ‌گو نیستند. کمی بعد دوباره تلاش کن." }, origin);
});
