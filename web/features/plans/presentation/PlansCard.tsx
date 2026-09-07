"use client";

import { useEffect, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { Plan } from "@/features/plans/domain/plan";
import { listPlans } from "@/features/plans/infrastructure/plan-api";
import type { OrganizationSubscription } from "@/features/subscriptions/domain/subscription";
import { getCurrentSubscription } from "@/features/subscriptions/infrastructure/subscription-api";

export function PlansCard() {
  const t = useTranslations("Plans");
  const locale = useLocale();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [subscription, setSubscription] = useState<OrganizationSubscription>();
  const [error, setError] = useState<string>();
  useEffect(() => {
    void Promise.all([listPlans(), getCurrentSubscription()])
      .then(([planList, currentSubscription]) => {
        setPlans(planList.data);
        setSubscription(currentSubscription);
      })
      .catch((reason: Error) => setError(reason.message));
  }, []);

  if (error) return <p className="text-sm text-destructive">{error}</p>;
  if (!subscription || plans.length === 0) return <p className="text-sm text-muted-foreground">{t("loading")}</p>;

  return <div className="space-y-6">
    <Card className="bg-muted/40">
      <CardHeader>
        <CardTitle>{t("currentPlan", { plan: subscription.plan.name })}</CardTitle>
        <CardDescription>{t("subscription", { status: t(subscription.status as "active" | "canceled") })}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid gap-3 sm:grid-cols-3">
          {subscription.plan.limits.map((limit) => <div key={limit.resource} className="rounded-lg border bg-background p-3">
            <p className="text-2xl font-semibold">{limit.value === -1 ? "∞" : limit.value}</p>
            <p className="mt-1 text-xs text-muted-foreground">{t(limit.resource as "members" | "projects" | "deadlines")}</p>
          </div>)}
        </div>
        <p className="mt-4 text-xs text-muted-foreground">{t("usageSoon")}</p>
      </CardContent>
    </Card>
    <Card>
      <CardHeader>
        <CardTitle>{t("available")}</CardTitle>
        <CardDescription>{t("availableDescription")}</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4 lg:grid-cols-3">
        {plans.map((plan) => {
          const current = plan.id === subscription.plan.id;
          return <div key={plan.id} className={`rounded-xl border p-4 ${current ? "border-foreground/30 bg-muted/50" : ""}`}>
            <div className="flex items-center justify-between gap-2">
              <p className="font-medium">{plan.name}</p>
              {current ? <span className="rounded-full bg-foreground px-2 py-0.5 text-xs text-background">{t("current")}</span> : null}
            </div>
            <p className="mt-2 text-sm text-muted-foreground">{plan.monthlyPriceCents === 0 ? t("free") : t("perMonth", { price: new Intl.NumberFormat(locale, { style: "currency", currency: "USD" }).format(plan.monthlyPriceCents / 100) })}</p>
            {plan.description ? <p className="mt-3 text-sm text-muted-foreground">{plan.description}</p> : null}
            <ul className="mt-4 space-y-2 text-sm text-muted-foreground">
              {plan.limits.map((limit) => <li key={limit.resource}>{limit.value === -1 ? t("unlimited") : limit.value} {t(limit.resource as "members" | "projects" | "deadlines")}</li>)}
            </ul>
            <Button className="mt-5 w-full" variant={current ? "secondary" : "outline"} disabled>{current ? t("currentButton") : t("comingSoon")}</Button>
          </div>;
        })}
      </CardContent>
    </Card>
  </div>;
}
