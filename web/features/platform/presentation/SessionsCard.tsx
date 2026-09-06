"use client";

import { MonitorSmartphone } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { UserSession } from "@/features/platform/domain/session";
import { revokeAllSessions, revokeSession } from "@/features/platform/infrastructure/session-api";

type SessionsCardProps = {
  initialSessions: UserSession[];
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function sessionName(userAgent: string | null) {
  if (!userAgent) return "Unknown browser";
  if (userAgent.includes("Firefox")) return "Firefox";
  if (userAgent.includes("Edg/")) return "Microsoft Edge";
  if (userAgent.includes("Chrome")) return "Google Chrome";
  if (userAgent.includes("Safari")) return "Safari";
  return "Browser session";
}

export function SessionsCard({ initialSessions }: SessionsCardProps) {
  const router = useRouter();
  const [sessions, setSessions] = useState(initialSessions);
  const [revokingId, setRevokingId] = useState<string>();
  const [isRevokingAll, setIsRevokingAll] = useState(false);

  async function handleRevoke(sessionId: string) {
    setRevokingId(sessionId);
    try {
      await revokeSession(sessionId);
      setSessions((current) => current.filter((session) => session.id !== sessionId));
      toast.success("The session has been revoked.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to revoke this session.");
    } finally {
      setRevokingId(undefined);
    }
  }

  async function handleRevokeAll() {
    setIsRevokingAll(true);
    try {
      await revokeAllSessions();
      toast.success("You have been signed out from all devices.");
      router.replace("/login");
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to sign out from all devices.");
      setIsRevokingAll(false);
    }
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>Active sessions</CardTitle>
          <CardDescription className="mt-1">Review the devices currently signed in to your account.</CardDescription>
        </div>
        <Button variant="outline" type="button" onClick={handleRevokeAll} disabled={isRevokingAll || sessions.length === 0}>
          {isRevokingAll ? "Signing out..." : "Sign out all"}
        </Button>
      </CardHeader>
      <CardContent>
        {sessions.length === 0 ? (
          <p className="text-sm text-muted-foreground">No active sessions were found. Sign in again to create one.</p>
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
                          <span className="rounded-full border px-2 py-0.5 text-xs text-muted-foreground">Current</span>
                        ) : null}
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {session.ipAddress ?? "Unknown IP"} · Signed in {formatDate(session.createdAt)}
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
                      {revokingId === session.id ? "Revoking..." : "Revoke"}
                    </Button>
                  ) : null}
                </div>
              </div>
            ))}
            <p className="text-xs leading-5 text-muted-foreground">
              Revoked sessions cannot renew their access. An access token already issued remains valid until it expires.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
