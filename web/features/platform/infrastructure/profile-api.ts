import type { UpdateUserProfileInput, UserProfile } from "@/features/platform/domain/user-profile";

type ErrorPayload = {
  error?: {
    code?: string;
    message?: string;
  };
};

export class AccountActionError extends Error {
  constructor(public readonly code: string | undefined, message: string) {
    super(message);
  }
}

export async function updateUserProfile(input: UpdateUserProfileInput): Promise<UserProfile> {
  const response = await fetch("/api/profile", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });

  const data = (await response.json().catch(() => ({}))) as UserProfile & ErrorPayload;
  if (!response.ok) {
    throw new Error(data.error?.message ?? "Unable to update your profile.");
  }

  return data;
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  const response = await fetch("/api/profile/password", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword, newPassword }),
  });

  if (!response.ok) {
    const data = (await response.json().catch(() => ({}))) as ErrorPayload;
    throw new Error(data.error?.message ?? "Unable to change your password.");
  }
}

async function accountAction(path: string, method: "POST" | "DELETE", password: string): Promise<void> {
  const response = await fetch(path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ password }),
  });
  if (!response.ok) {
    const data = (await response.json().catch(() => ({}))) as ErrorPayload;
    throw new AccountActionError(data.error?.code, data.error?.message ?? "Unable to update your account.");
  }
}

export async function deactivateAccount(password: string): Promise<void> {
  return accountAction("/api/profile/deactivate", "POST", password);
}

export async function deleteAccount(password: string): Promise<void> {
  return accountAction("/api/profile", "DELETE", password);
}
