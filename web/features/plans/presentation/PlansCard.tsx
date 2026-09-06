"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { Plan } from "@/features/plans/domain/plan";
import { listPlans } from "@/features/plans/infrastructure/plan-api";

export function PlansCard() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [error, setError] = useState<string>();
  useEffect(() => { void listPlans().then((result) => setPlans(result.data)).catch((reason) => setError(reason.message)); }, []);
  return <Card><CardHeader><CardTitle>Plans</CardTitle><CardDescription>Plan selection and billing will be available soon.</CardDescription></CardHeader><CardContent>{error ? <p className="text-sm text-destructive">{error}</p> : plans.length === 0 ? <p className="text-sm text-muted-foreground">Loading plans...</p> : <div className="space-y-4">{plans.map((plan) => <div key={plan.id} className="rounded-lg border p-4"><p className="font-medium">{plan.name}</p><p className="text-sm text-muted-foreground">{plan.monthlyPriceCents === 0 ? "Free" : `$${(plan.monthlyPriceCents / 100).toFixed(2)} / month`}</p><p className="mt-2 text-xs text-muted-foreground">{plan.limits.map((limit) => `${limit.value === -1 ? "Unlimited" : limit.value} ${limit.resource}`).join(" · ")}</p></div>)}</div>}<Button className="mt-5" variant="outline" disabled>Manage plan soon</Button></CardContent></Card>;
}
