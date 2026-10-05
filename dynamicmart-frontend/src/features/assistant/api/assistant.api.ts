import { apiClient } from "@/lib/api/client";

export type AssistantCitation = {
  source: string;
  title: string;
  excerpt: string;
  score: number;
};

export type AssistantResponse = {
  reply: string;
  intent: string;
  query: string;
  products: unknown[];
  citations: AssistantCitation[];
};

export const assistantApi = {
  ask: (message: string) =>
    apiClient.post<AssistantResponse>("/api/v1/assistant/chat", {
      tenant: "dynamicmart",
      message,
      products: [],
    }),
};

