import { apiClient } from "@/lib/api-client";

export async function revokeSession(sessionId: string): Promise<void> {
  await apiClient.delete(`/api/sessions/${sessionId}`);
}

export async function revokeAllSessions(): Promise<void> {
  await apiClient.post("/api/sessions/revoke-all");
}
