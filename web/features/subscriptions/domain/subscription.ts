import type { Plan } from "@/features/plans/domain/plan";

export type OrganizationSubscription = {
  id: string;
  organizationId: string;
  status: "active" | "canceled";
  startedAt: string;
  endedAt: string | null;
  plan: Plan;
};
