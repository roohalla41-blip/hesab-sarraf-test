const allowedModes = new Set(["general", "writing", "learning", "coding", "research"]);
const maxMessages = 30;
const maxMessageChars = 12_000;
const maxTotalChars = 48_000;
const providerTimeoutMs = 14_000;

type ChatMessage = { role: "user" | "assistant"; content: string };
type ProviderMessage = { role: "system" | "user" | "assistant"; content: string };
type ProviderName = "zai" | "cloudflare" | "openai";

type ProviderConfig = {
  name: ProviderName;
  endpoint: string;
  apiKey: string;
  model: string;
};

type ProviderReply = {
  answer: string;
  model: string;
  usage: { input_tokens: number | null; output_tokens: number | null } | null;
};

function json(data: unknown, status = 200, origin: string | null = null) {
  const headers = new Headers({ "Content-Type": "application/json", "Cache-Control": "no-store" });
  if (origin) {
    headers.set("Access-Control-Allow-Origin", origin);
    headers.set("Vary", "Origin");
  }
  headers.set("Access-Control-Allow-Headers", "authorization, apikey, x-client-info, content-type, x-request-id");
  headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  return new Response(JSON.stringify(data), { status, headers });
}

function fail(code: string, message: string, status: number, requestId: string, origin: string | null) {
  return json({ error: { code, message }, request_id: requestId }, status, origin);
}

function allowedOrigin(req: Request): string | null | undefined {
  const origin = req.headers.get("Origin");
  if (!origin) return null; // Native mobile clients may not send Origin.
  const allowlist = (Deno.env.get("ATLAS_ALLOWED_ORIGINS") ?? "")
    .split(",").map(v => v.trim()).filter(Boolean);
  return allowlist.includes(origin) ? origin : undefined;
}

function httpsUrl(raw: string): string | null {
  try {
    const url = new URL(raw);
    return url.protocol === "https:" ? url.toString() : null;
  } catch {
    return null;
  }
}

function buildProviderConfigs(): ProviderConfig[] {
  const configs = new Map<ProviderName, ProviderConfig>();

  const zaiKey = Deno.env.get("ZAI_API_KEY");
  if (zaiKey) {
    const endpoint = httpsUrl(
      Deno.env.get("ZAI_API_URL") ?? "https://api.z.ai/api/paas/v4/chat/completions"
    );
    if (endpoint) {
      configs.set("zai", {
        name: "zai",
        endpoint,
        apiKey: zaiKey,
        model: Deno.env.get("ZAI_MODEL") ?? "glm-5.3",
      });
    }
  }

  const cloudflareToken = Deno.env.get("CLOUDFLARE_API_TOKEN") ?? Deno.env.get("ATLAS_CLOUDFLARE");
  const cloudflareAccountId = Deno.env.get("CLOUDFLARE_ACCOUNT_ID");
  const cloudflareModel = Deno.env.get("CLOUDFLARE_MODEL");
  if (cloudflareToken && cloudflareAccountId && cloudflareModel) {
    configs.set("cloudflare", {
      name: "cloudflare",
      endpoint: `https://api.cloudflare.com/client/v4/accounts/${encodeURIComponent(cloudflareAccountId)}/ai/v1/chat/completions`,
      apiKey: cloudflareToken,
      model: cloudflareModel,
    });
  }

  const genericKey = Deno.env.get("AI_API_KEY");
  const genericModel = Deno.env.get("AI_MODEL");
  const genericBaseUrl = Deno.env.get("AI_API_URL") ?? "https://api.openai.com/v1";
  if (genericKey && genericModel) {
    const base = httpsUrl(genericBaseUrl.replace(/\/$/, "") + "/");
    if (base) {
      configs.set("openai", {
        name: "openai",
        endpoint: new URL("chat/completions", base).toString(),
        apiKey: genericKey,
        model: genericModel,
      });
    }
  }

  const order = (Deno.env.get("AI_PROVIDER_ORDER") ?? "zai,cloudflare,openai")
    .split(",")
    .map(value => value.trim().toLowerCase())
    .filter((value): value is ProviderName =>
      value === "zai" || value === "cloudflare" || value === "openai"
    );

  return order.map(name => configs.get(name)).filter((value): value is ProviderConfig => Boolean(value));
}

function modeInstruction(mode: string): string {
  const common = "You are ATLAS AI, a multilingual assistant. Reply in the language used by the user. Be accurate, clear, and honest about uncertainty. Never claim to have used tools or accessed data unless the request actually provides that capability.";
  const instructions: Record<string, string> = {
    general: "Answer the user's question directly and helpfully.",
    writing: "Act as a writing and editing assistant. Preserve the user's intent and requested language and tone.",
    learning: "Teach step by step, define unfamiliar terms, and adapt explanations to the learner.",
    coding: "Act as a careful programming assistant. Explain assumptions and provide safe, maintainable code.",
    research: "Analyze the topic carefully, distinguish established facts from uncertainty, and do not claim live web access.",
  };
  return common + " " + (instructions[mode] ?? instructions.general);
}

async function callProvider(
  provider: ProviderConfig,
  messages: ProviderMessage[],
  requestId: string,
): Promise<ProviderReply | null> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), providerTimeoutMs);
  try {
    const upstream = await fetch(provider.endpoint, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Authorization": `Bearer ${provider.apiKey}`,
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Request-ID": requestId,
      },
      body: JSON.stringify({
        model: provider.model,
        messages,
        temperature: 0.7,
        stream: false,
        max_tokens: 2048,
      }),
    });

    if (!upstream.ok) return null;
    const result = await upstream.json();
    const answer = result?.choices?.[0]?.message?.content;
    if (typeof answer !== "string" || !answer.trim()) return null;

    const promptTokens = result?.usage?.prompt_tokens;
    const completionTokens = result?.usage?.completion_tokens;
    return {
      answer: answer.trim(),
      model: typeof result?.model === "string" ? result.model : provider.model,
      usage: {
        input_tokens: Number.isFinite(promptTokens) ? promptTokens : null,
        output_tokens: Number.isFinite(completionTokens) ? completionTokens : null,
      },
    };
  } catch {
    return null;
  } finally {
    clearTimeout(timeout);
  }
}

Deno.serve(async (req: Request) => {
  const requestId = req.headers.get("x-request-id")?.slice(0, 100) || crypto.randomUUID();
  const origin = allowedOrigin(req);
  if (origin === undefined) {
    return fail("ORIGIN_NOT_ALLOWED", "This application origin is not allowed.", 403, requestId, null);
  }
  if (req.method === "OPTIONS") return json({ ok: true }, 200, origin);
  if (req.method !== "POST") {
    return fail("METHOD_NOT_ALLOWED", "Use POST for chat requests.", 405, requestId, origin);
  }

  // Keep Supabase verify_jwt enabled for this function.
  const providers = buildProviderConfigs();
  if (providers.length === 0) {
    return fail("AI_NOT_CONFIGURED", "No AI provider is configured yet.", 503, requestId, origin);
  }

  let body: { conversation_id?: string | null; mode?: string; messages?: ChatMessage[] };
  try {
    body = await req.json();
  } catch {
    return fail("INVALID_JSON", "The request body must be valid JSON.", 400, requestId, origin);
  }

  const mode = body.mode ?? "general";
  const messages = body.messages;
  if (!allowedModes.has(mode)) {
    return fail("INVALID_MODE", "The selected assistant mode is not supported.", 400, requestId, origin);
  }
  if (!Array.isArray(messages) || messages.length < 1 || messages.length > maxMessages) {
    return fail("INVALID_MESSAGES", "Send between 1 and 30 messages.", 400, requestId, origin);
  }

  const clean: ChatMessage[] = [];
  let total = 0;
  for (const item of messages) {
    if (!item || (item.role !== "user" && item.role !== "assistant") ||
      typeof item.content !== "string" || !item.content.trim() || item.content.length > maxMessageChars) {
      return fail("INVALID_MESSAGE", "A message is invalid or too long.", 400, requestId, origin);
    }
    total += item.content.length;
    if (total > maxTotalChars) {
      return fail("CONTEXT_TOO_LARGE", "The conversation context is too large.", 413, requestId, origin);
    }
    clean.push({ role: item.role, content: item.content.trim() });
  }
  if (clean[clean.length - 1]?.role !== "user") {
    return fail("INVALID_TURN", "The final message must come from the user.", 400, requestId, origin);
  }

  const providerMessages: ProviderMessage[] = [
    { role: "system", content: modeInstruction(mode) },
    ...clean,
  ];

  for (const provider of providers) {
    const reply = await callProvider(provider, providerMessages, requestId);
    if (!reply) continue;
    return json({
      request_id: requestId,
      conversation_id: typeof body.conversation_id === "string" ? body.conversation_id : null,
      assistant_message: { role: "assistant", content: reply.answer },
      provider: provider.name,
      model: reply.model,
      usage: reply.usage,
    }, 200, origin);
  }

  return fail(
    "AI_PROVIDERS_UNAVAILABLE",
    "All configured AI providers are temporarily unavailable. Please try again.",
    503,
    requestId,
    origin,
  );
});
