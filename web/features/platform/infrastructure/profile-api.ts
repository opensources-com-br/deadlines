import type { UpdateUserProfileInput, UserProfile } from "@/features/platform/domain/user-profile";

type ErrorPayload = {
  error?: {
    message?: string;
  };
};

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
