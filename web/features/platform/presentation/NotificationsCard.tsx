"use client";

import { BellRing, Mail } from "lucide-react";
import { useTranslations } from "next-intl";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export function NotificationsCard() {
  const t = useTranslations("Notifications");
  const preferences = [
    { title: t("security"), description: t("securityDescription"), icon: BellRing },
    { title: t("organization"), description: t("organizationDescription"), icon: Mail },
    { title: t("deadlines"), description: t("deadlinesDescription"), icon: BellRing },
  ];
  return <Card><CardHeader><CardTitle>{t("title")}</CardTitle><CardDescription>{t("description")}</CardDescription></CardHeader><CardContent className="space-y-3">{preferences.map(({ title, description, icon: Icon }) => <div key={title} className="flex items-center gap-3 rounded-lg border p-4"><div className="rounded-md bg-muted p-2"><Icon className="size-4" /></div><div className="min-w-0 flex-1"><p className="text-sm font-medium">{title}</p><p className="mt-1 text-xs text-muted-foreground">{description}</p></div><span className="rounded-full border px-2.5 py-1 text-xs text-muted-foreground">{t("comingSoon")}</span></div>)}</CardContent></Card>;
}
