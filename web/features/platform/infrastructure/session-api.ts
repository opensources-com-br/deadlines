type ErrorPayload = {
  error?: {
    message?: string;
  };
};

async function assertSuccess(response: Response, fallbackMessage: string) {
  if (response.ok) return;

  const data = (await response.json().catch(() => ({}))) as ErrorPayload;
  throw new Error(data.error?.message ?? fallbackMessage);
}

export async function revokeSession(sessionId: string): Promise<void> {
  const response = await fetch(`/api/sessions/${sessionId}`, { method: "DELETE" });
  await assertSuccess(response, "Unable to revoke this session.");
}

export async function revokeAllSessions(): Promise<void> {
  const response = await fetch("/api/sessions/revoke-all", { method: "POST" });
  await assertSuccess(response, "Unable to sign out from all devices.");
}
