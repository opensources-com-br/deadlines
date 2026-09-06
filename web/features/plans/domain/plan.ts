export type PlanLimit = { resource: string; value: number };
export type Plan = { id: string; key: string; name: string; description: string | null; monthlyPriceCents: number; currency: string; limits: PlanLimit[] };
export type PlanList = { data: Plan[] };
