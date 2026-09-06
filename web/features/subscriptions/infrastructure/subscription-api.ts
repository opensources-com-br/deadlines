import type { OrganizationSubscription } from "@/features/subscriptions/domain/subscription";

export async function getCurrentSubscription(): Promise<OrganizationSubscription> {
  const response = await fetch("/api/subscriptions/current");
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.error?.message ?? "Unable to load your subscription.");
  }
  return data as OrganizationSubscription;
}
