const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

type ChatMessage = {
  role: "system" | "user" | "assistant";
  content: string;
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  // Supabase gateway verifies the user's JWT because verify_jwt = true.
  const apiKey = Deno.env.get("AI_API_KEY");
  const apiUrl = (Deno.env.get("AI_API_URL") ?? "https://api.openai.com/v1").replace(/\/$/, "");
  const model = Deno.env.get("AI_MODEL");

  if (!apiKey || !model) {
    return json({ error: "AI service is not configured yet." }, 503);
  }

  let body: { messages?: ChatMessage[] };
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON body." }, 400);
  }

  const messages = body.messages;
  if (!Array.isArray(messages) || messages.length === 0 || messages.length > 30) {
    return json({ error: "Send between 1 and 30 messages." }, 400);
  }

  const validRoles = new Set(["system", "user", "assistant"]);
  const cleanMessages: ChatMessage[] = [];
  for (const item of messages) {
    if (
      !item ||
      !validRoles.has(item.role) ||
      typeof item.content !== "string" ||
      item.content.trim().length === 0 ||
      item.content.length > 12000
    ) {
      return json({ error: "A message is invalid or too long." }, 400);
    }
    cleanMessages.push({ role: item.role, content: item.content.trim() });
  }

  try {
    const upstream = await fetch(`${apiUrl}/chat/completions`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        messages: cleanMessages,
        temperature: 0.7,
      }),
    });

    if (!upstream.ok) {
      // Do not return upstream response bodies; they may contain provider details.
      return json({ error: "AI provider request failed.", providerStatus: upstream.status }, 502);
    }

    const result = await upstream.json();
    const reply = result?.choices?.[0]?.message?.content;
    if (typeof reply !== "string" || !reply.trim()) {
      return json({ error: "AI provider returned an empty response." }, 502);
    }

    return json({ reply: reply.trim() });
  } catch {
    return json({ error: "Could not reach the AI provider." }, 502);
  }
});
