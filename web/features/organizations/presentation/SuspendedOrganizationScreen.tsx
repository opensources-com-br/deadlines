"use client";

import { useState } from "react";
import { Building2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import type { Organization } from "@/features/organizations/domain/organization";
import { organizationApi } from "@/features/organizations/infrastructure/organization-api";

export function SuspendedOrganizationScreen({ organization }: { organization: Organization }) {
  const t = useTranslations("Organization");
  const [isReactivating, setIsReactivating] = useState(false);
  const isOwner = organization.role === "owner";

  async function reactivate() {
    setIsReactivating(true);
    try {
      await organizationApi.reactivate();
      window.location.replace("/app/organization");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("reactivateError"));
      setIsReactivating(false);
    }
  }

  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/20 p-6">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <div className="mb-3 flex size-10 items-center justify-center rounded-lg border bg-background">
            <Building2 className="size-5" />
          </div>
          <CardTitle>{t("suspendedTitle")}</CardTitle>
          <CardDescription>{t("suspendedPageDescription", { name: organization.name })}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          <p className="text-sm text-muted-foreground">{t(isOwner ? "ownerCanReactivate" : "memberWait")}</p>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => window.location.replace("/api/auth/logout")}>{t("logout")}</Button>
            {isOwner ? <Button onClick={reactivate} disabled={isReactivating}>{isReactivating ? t("reactivating") : t("reactivate")}</Button> : null}
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
