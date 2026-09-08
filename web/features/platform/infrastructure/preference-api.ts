import type { UpdateUserPreference, UserPreference } from "@/features/platform/domain/user-preference";
import { apiClient } from "@/lib/api-client";

export async function updatePreferences(input: UpdateUserPreference): Promise<UserPreference> {
  return apiClient.patch<UserPreference>("/api/preferences", input);
}
