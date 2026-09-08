import type { PlanList } from "@/features/plans/domain/plan";
import { apiClient } from "@/lib/api-client";

export async function listPlans(): Promise<PlanList> {
  return apiClient.get<PlanList>("/api/plans");
}
