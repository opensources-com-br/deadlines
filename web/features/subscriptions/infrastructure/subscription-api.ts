import type { OrganizationSubscription } from "@/features/subscriptions/domain/subscription";
import { apiClient } from "@/lib/api-client";

export async function getCurrentSubscription(): Promise<OrganizationSubscription> {
  return apiClient.get<OrganizationSubscription>("/api/subscriptions/current");
}
