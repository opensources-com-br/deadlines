import type { PlanList } from "@/features/plans/domain/plan";

export async function listPlans(): Promise<PlanList> {
  const response = await fetch("/api/plans");
  if (!response.ok) throw new Error("Unable to load plans.");
  return response.json() as Promise<PlanList>;
}
