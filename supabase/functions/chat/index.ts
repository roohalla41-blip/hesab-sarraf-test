const allowedModes = new Set(["general", "writing", "learning", "coding", "research"]);
const maxMessages = 30;
const maxMessageChars = 12_000;
const maxTotalChars = 48_000;
const requestTimeoutMs = 45_000;

type ChatMessage = { role: "user" | "assistant"; content: string };

function json(data: unknown, status = 200, origin: string | null = null) {
  const headers = new Headers({ "Content-Type": "application/json", "Cache-Control": "no-store" });
  if (origin) { headers.set("Access-Control-Allow-Origin", origin); headers.set("Vary", "Origin"); }
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
  const allowlist = (Deno.env.get("ATLAS_ALLOWED_ORIGINS") ?? "").split(",").map(v => v.trim()).filter(Boolean);
  return allowlist.includes(origin) ? origin : undefined;
}

Deno.serve(async (req: Request) => {
  const requestId = req.headers.get("x-request-id")?.slice(0, 100) || crypto.randomUUID();
  const origin = allowedOrigin(req);
  if (origin === undefined) return fail("ORIGIN_NOT_ALLOWED", "This application origin is not allowed.", 403, requestId, null);
  if (req.method === "OPTIONS") return json({ ok: true }, 200, origin);
  if (req.method !== "POST") return fail("METHOD_NOT_ALLOWED", "Use POST for chat requests.", 405, requestId, origin);

  // Keep Supabase verify_jwt enabled for this function.
  const apiKey = Deno.env.get("AI_API_KEY");
  const model = Deno.env.get("AI_MODEL");
  const rawApiUrl = Deno.env.get("AI_API_URL") ?? "https://api.openai.com/v1";
  if (!apiKey || !model) return fail("AI_NOT_CONFIGURED", "The AI service is not configured yet.", 503, requestId, origin);

  let apiUrl: URL;
  try {
    apiUrl = new URL(rawApiUrl);
    if (apiUrl.protocol !== "https:") throw new Error("HTTPS required");
  } catch {
    return fail("SERVER_CONFIGURATION_ERROR", "The AI service configuration is invalid.", 503, requestId, origin);
  }

  let body: { conversation_id?: string | null; mode?: string; messages?: ChatMessage[] };
  try { body = await req.json(); }
  catch { return fail("INVALID_JSON", "The request body must be valid JSON.", 400, requestId, origin); }

  const modes = new Set(["general", "writing", "learning", "coding", "research"]);
  const mode = body.mode ?? "general";
  const messages = body.messages;
  if (!modes.has(mode)) return fail("INVALID_MODE", "The selected assistant mode is not supported.", 400, requestId, origin);
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
    if (total > maxTotalChars) return fail("CONTEXT_TOO_LARGE", "The conversation context is too large.", 413, requestId, origin);
    clean.push({ role: item.role, content: item.content.trim() });
  }
  if (clean[clean.length - 1]?.role !== "user") {
    return fail("INVALID_TURN", "The final message must come from the user.", 400, requestId, origin);
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMs);
  try {
    const endpoint = new URL("chat/completions", rawApiUrl.replace(/\/$/, "") + "/");
    const upstream = await fetch(endpoint, {
      method: "POST",
      signal: controller.signal,
      headers: { "Authorization": `Bearer ${apiKey}`, "Content-Type": "application/json" },
      body: JSON.stringify({ model, messages: clean, temperature: 0.7 }),
    });
    if (upstream.status === 401 || upstream.status === 403) {
      return fail("AI_PROVIDER_AUTH_ERROR", "The AI service is temporarily unavailable.", 502, requestId, origin);
    }
    if (upstream.status === 429) return fail("AI_PROVIDER_BUSY", "The AI service is busy. Try again shortly.", 503, requestId, origin);
    if (!upstream.ok) return fail("AI_PROVIDER_ERROR", "The AI service could not complete this request.", 502, requestId, origin);

    const result = await upstream.json();
    const reply = result?.choices?.[0]?.message?.content;
    if (typeof reply !== "string" || !reply.trim()) {
      return fail("EMPTY_AI_RESPONSE", "The AI service returned an empty response.", 502, requestId, origin);
    }
    return json({
      request_id: requestId,
      conversation_id: typeof body.conversation_id === "string" ? body.conversation_id : null,
      assistant_message: { role: "assistant", content: reply.trim() },
      model,
      usage: result?.usage ? {
        input_tokens: Number.isFinite(result.usage.prompt_tokens) ? result.usage.prompt_tokens : null,
        output_tokens: Number.isFinite(result.usage.completion_tokens) ? result.usage.completion_tokens : null,
      } : null,
    }, 200, origin);
  } catch (error) {
    const timedOut = error instanceof Error && error.name === "AbortError";
    return fail(timedOut ? "AI_TIMEOUT" : "AI_NETWORK_ERROR",
      timedOut ? "The AI request took too long. Try again." : "The AI service could not be reached.",
      502, requestId, origin);
  } finally {
    clearTimeout(timeout);
  }
});
