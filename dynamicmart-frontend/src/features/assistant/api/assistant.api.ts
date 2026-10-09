import { apiClient } from "@/lib/api/client";
import { getApiBaseUrl, getApiFallbackUrl } from "@/lib/api/config";

export type AssistantCitation = {
  source: string;
  title: string;
  excerpt: string;
  score: number;
};

export type AssistantProduct = {
  id?: string | null;
  slug?: string | null;
  name: string;
  detail?: string | null;
  url: string;
  image_url?: string | null;
  price?: number | null;
  compare_at_price?: number | null;
  discount_percentage?: number | null;
  stock_total: number;
  in_stock: boolean;
};

export type AssistantResponse = {
  reply: string;
  intent: string;
  query: string;
  products: AssistantProduct[];
  citations: AssistantCitation[];
};

export type AssistantHistoryTurn = {
  role: "user" | "assistant";
  content: string;
};

type StreamEvent =
  | { type: "token" | "replace"; content: string }
  | { type: "result" | "done"; data: AssistantResponse };

function streamUrls(): string[] {
  const baseUrl = getApiBaseUrl();
  const fallbackUrl = getApiFallbackUrl(baseUrl);
  const directChatUrl = process.env.NEXT_PUBLIC_AI_ASSISTANT_URL
    ?? "http://127.0.0.1:8001/api/v1/assistant/chat";
  return [
    `${baseUrl}/api/v1/assistant/chat/stream`,
    ...(fallbackUrl ? [`${fallbackUrl}/api/v1/assistant/chat/stream`] : []),
    directChatUrl.replace(/\/chat\/?$/, "/chat/stream"),
  ].filter((url, index, urls) => urls.indexOf(url) === index);
}

async function openStream(payload: object): Promise<Response> {
  let lastError: unknown;
  for (const url of streamUrls()) {
    try {
      const response = await fetch(url, {
        method: "POST",
        // Chatbot là endpoint public; không gửi cookie để direct fallback dùng CORS wildcard an toàn.
        credentials: "omit",
        headers: {
          Accept: "application/x-ndjson",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });
      if (response.ok && response.body) return response;
      lastError = new Error(`AI assistant stream error ${response.status}`);
    } catch (error) {
      lastError = error;
    }
  }
  throw lastError ?? new Error("Không thể kết nối luồng AI assistant.");
}

export const assistantApi = {
  ask: async (message: string, history: AssistantHistoryTurn[] = []): Promise<AssistantResponse> => {
    const payload = {
      tenant: "dynamicmart",
      message,
      products: [],
      history: history.slice(-10),
    };

    try {
      return await apiClient.post<AssistantResponse>("/api/v1/assistant/chat", payload);
    } catch {
      // Tự động thử kết nối trực tiếp vào dịch vụ AI Assistant nếu API Gateway gặp lỗi chuyển tiếp
      const directUrl = process.env.NEXT_PUBLIC_AI_ASSISTANT_URL ?? "http://127.0.0.1:8001/api/v1/assistant/chat";
      const res = await fetch(directUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        throw new Error(`AI assistant error ${res.status}`);
      }
      return (await res.json()) as AssistantResponse;
    }
  },

  askStream: async (
    message: string,
    history: AssistantHistoryTurn[],
    onText: (content: string, replace: boolean) => void,
  ): Promise<AssistantResponse> => {
    const payload = { tenant: "dynamicmart", message, products: [], history: history.slice(-10) };
    const response = await openStream(payload);
    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let result: AssistantResponse | undefined;

    const consume = (line: string) => {
      if (!line.trim()) return;
      const event = JSON.parse(line) as StreamEvent;
      if (event.type === "token") onText(event.content, false);
      if (event.type === "replace") onText(event.content, true);
      if (event.type === "result" || event.type === "done") result = event.data;
    };

    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value, { stream: !done });
      const lines = buffer.split("\n");
      buffer = lines.pop() ?? "";
      lines.forEach(consume);
      if (done) break;
    }
    consume(buffer);
    if (!result) throw new Error("Luồng AI assistant kết thúc không hợp lệ.");
    return result;
  },
};

