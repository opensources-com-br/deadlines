"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { OrganizationSubscription } from "@/features/subscriptions/domain/subscription";
import { getCurrentSubscription } from "@/features/subscriptions/infrastructure/subscription-api";
import { useLocalizedFormatters } from "@/features/platform/presentation/useLocalizedFormatters";

export function SubscriptionCard() {
  const t = useTranslations("Subscription");
  const { formatCurrency } = useLocalizedFormatters();
  const [subscription, setSubscription] = useState<OrganizationSubscription>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    void getCurrentSubscription().then(setSubscription).catch((reason) => setError(reason.message));
  }, []);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("title")}</CardTitle>
        <CardDescription>{t("description")}</CardDescription>
      </CardHeader>
      <CardContent>
        {error ? <p className="text-sm text-destructive">{error}</p> : null}
        {!error && !subscription ? <p className="text-sm text-muted-foreground">{t("loading")}</p> : null}
        {subscription ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-4">
              <p className="font-medium">{subscription.plan.name}</p>
              <p className="text-sm text-muted-foreground">{t(subscription.status)}</p>
            </div>
            <p className="text-sm text-muted-foreground">
              {subscription.plan.monthlyPriceCents === 0
                ? t("free")
                : t("perMonth", {
                    price: formatCurrency(subscription.plan.monthlyPriceCents / 100),
                  })}
            </p>
            <p className="text-xs text-muted-foreground">
              {t("limits", {
                limits: subscription.plan.limits
                  .map(
                    (limit) =>
                      `${limit.value === -1 ? t("unlimited") : limit.value} ${t(
                        limit.resource as "members" | "projects" | "deadlines",
                      )}`,
                  )
                  .join(" · "),
              })}
            </p>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
