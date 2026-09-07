import type { UpdateUserPreference, UserPreference } from "@/features/platform/domain/user-preference";

export async function updatePreferences(input: UpdateUserPreference): Promise<UserPreference> {
  const response = await fetch("/api/preferences", { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input) });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.error?.message ?? "Unable to update preferences.");
  return data as UserPreference;
}
