"use client";

import { useEffect, useState } from "react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { OrganizationSubscription } from "@/features/subscriptions/domain/subscription";
import { getCurrentSubscription } from "@/features/subscriptions/infrastructure/subscription-api";

export function SubscriptionCard() {
  const [subscription, setSubscription] = useState<OrganizationSubscription>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    void getCurrentSubscription().then(setSubscription).catch((reason) => setError(reason.message));
  }, []);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Subscription</CardTitle>
        <CardDescription>Your organization&apos;s current plan.</CardDescription>
      </CardHeader>
      <CardContent>
        {error ? <p className="text-sm text-destructive">{error}</p> : null}
        {!error && !subscription ? <p className="text-sm text-muted-foreground">Loading subscription...</p> : null}
        {subscription ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-4">
              <p className="font-medium">{subscription.plan.name}</p>
              <p className="text-sm capitalize text-muted-foreground">{subscription.status}</p>
            </div>
            <p className="text-sm text-muted-foreground">
              {subscription.plan.monthlyPriceCents === 0
                ? "Free"
                : `$${(subscription.plan.monthlyPriceCents / 100).toFixed(2)} / month`}
            </p>
            <p className="text-xs text-muted-foreground">
              Catalog limits, not enforced yet: {subscription.plan.limits
                .map((limit) => `${limit.value === -1 ? "Unlimited" : limit.value} ${limit.resource}`)
                .join(" · ")}
            </p>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
