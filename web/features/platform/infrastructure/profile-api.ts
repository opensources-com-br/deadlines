import type { UpdateUserProfileInput, UserProfile } from "@/features/platform/domain/user-profile";
import { apiClient, ApiClientError } from "@/lib/api-client";

export class AccountActionError extends Error {
  constructor(public readonly code: string | undefined, message: string) {
    super(message);
  }
}

export async function updateUserProfile(input: UpdateUserProfileInput): Promise<UserProfile> {
  return apiClient.patch<UserProfile>("/api/profile", input);
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await apiClient.patch("/api/profile/password", { currentPassword, newPassword });
}

async function accountAction(path: string, method: "POST" | "DELETE", password: string): Promise<void> {
  try {
    if (method === "POST") await apiClient.post(path, { password });
    else await apiClient.delete(path, { body: JSON.stringify({ password }), headers: { "Content-Type": "application/json" } });
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw new AccountActionError(error.code, error.message);
    }
    throw error;
  }
}

export async function deactivateAccount(password: string): Promise<void> {
  return accountAction("/api/profile/deactivate", "POST", password);
}

export async function deleteAccount(password: string): Promise<void> {
  return accountAction("/api/profile", "DELETE", password);
}
