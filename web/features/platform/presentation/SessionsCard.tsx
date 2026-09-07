"use client";

import { MonitorSmartphone } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { useLocale, useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { UserSession } from "@/features/platform/domain/session";
import { revokeAllSessions, revokeSession } from "@/features/platform/infrastructure/session-api";
import { useUserPreferences } from "@/features/platform/presentation/UserPreferenceProvider";

type SessionsCardProps = {
  initialSessions: UserSession[];
};

function parseDate(value: string | undefined) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date;
}

function formatDate(value: string | undefined, locale: string, timezone: string, unknownDate: string) {
  const date = parseDate(value);
  if (!date) return unknownDate;

  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: timezone,
  }).format(date);
}

export function SessionsCard({ initialSessions }: SessionsCardProps) {
  const t = useTranslations("Security");
  const locale = useLocale();
  const { preferences } = useUserPreferences();
  const router = useRouter();
  const [sessions, setSessions] = useState(initialSessions);
  const [revokingId, setRevokingId] = useState<string>();
  const [isRevokingAll, setIsRevokingAll] = useState(false);
  const [renderedAt] = useState(() => Date.now());
  function sessionName(userAgent: string | null) {
    if (!userAgent) return t("unknownBrowser");
    if (userAgent.includes("Firefox")) return "Firefox";
    if (userAgent.includes("Edg/")) return "Microsoft Edge";
    if (userAgent.includes("Chrome")) return "Google Chrome";
    if (userAgent.includes("Safari")) return "Safari";
    return t("browserSession");
  }
  function formatLastActive(value: string | undefined, fallbackValue: string, isCurrent: boolean) {
    if (isCurrent) return t("activeNow");
    const lastActive = parseDate(value) ?? parseDate(fallbackValue);
    if (!lastActive) return t("unknownActivity");
    const elapsedMinutes = Math.max(0, Math.floor((renderedAt - lastActive.getTime()) / 60_000));
    if (elapsedMinutes < 1) return t("activeMinute");
    if (elapsedMinutes < 60) return t("activeMinutes", { count: elapsedMinutes });
    const elapsedHours = Math.floor(elapsedMinutes / 60);
    if (elapsedHours < 24) return t("activeHours", { count: elapsedHours });
    return t("lastActive", { date: formatDate(lastActive.toISOString(), locale, preferences.timezone, t("unknownDate")) });
  }

  async function handleRevoke(sessionId: string) {
    setRevokingId(sessionId);
    try {
      await revokeSession(sessionId);
      setSessions((current) => current.filter((session) => session.id !== sessionId));
      toast.success(t("revoked"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("revokeError"));
    } finally {
      setRevokingId(undefined);
    }
  }

  async function handleRevokeAll() {
    setIsRevokingAll(true);
    try {
      await revokeAllSessions();
      toast.success(t("signedOutAll"));
      router.replace("/login");
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("signOutError"));
      setIsRevokingAll(false);
    }
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>{t("sessions")}</CardTitle><CardDescription className="mt-1">{t("sessionsDescription")}</CardDescription>
        </div>
        <Button variant="outline" type="button" onClick={handleRevokeAll} disabled={isRevokingAll || sessions.length === 0}>
          {isRevokingAll ? t("signingOut") : t("signOutAll")}
        </Button>
      </CardHeader>
      <CardContent>
        {sessions.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("noSessions")}</p>
        ) : (
          <div className="space-y-5">
            {sessions.map((session, index) => (
              <div key={session.id}>
                {index > 0 ? <Separator className="mb-5" /> : null}
                <div className="flex items-start justify-between gap-4">
                  <div className="flex min-w-0 gap-3">
                    <div className="mt-0.5 rounded-md border p-2 text-muted-foreground">
                      <MonitorSmartphone className="size-4" aria-hidden="true" />
                    </div>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-medium">{sessionName(session.userAgent)}</p>
                        {session.isCurrent ? (
                          <span className="rounded-full border px-2 py-0.5 text-xs text-muted-foreground">{t("current")}</span>
                        ) : null}
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {session.ipAddress ?? t("unknownIp")} · {formatLastActive(session.lastSeenAt, session.createdAt, session.isCurrent)}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {t("signedInExpires", { signedIn: formatDate(session.createdAt, locale, preferences.timezone, t("unknownDate")), expires: formatDate(session.expiresAt, locale, preferences.timezone, t("unknownDate")) })}
                      </p>
                    </div>
                  </div>
                  {!session.isCurrent ? (
                    <Button
                      variant="ghost"
                      size="sm"
                      type="button"
                      onClick={() => handleRevoke(session.id)}
                      disabled={revokingId === session.id || isRevokingAll}
                    >
                      {revokingId === session.id ? t("revoking") : t("revoke")}
                    </Button>
                  ) : null}
                </div>
              </div>
            ))}
            <p className="text-xs leading-5 text-muted-foreground">
              {t("tokenNotice")}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
