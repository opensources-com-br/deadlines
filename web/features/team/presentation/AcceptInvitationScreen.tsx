"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button, buttonVariants } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import type { InvitationPreview } from "@/features/team/domain/team";
import { teamApi } from "@/features/team/infrastructure/team-api";

type AcceptInvitationScreenProps = {
  token?: string;
  authenticated: boolean;
};

export function AcceptInvitationScreen({ token, authenticated }: AcceptInvitationScreenProps) {
  const t = useTranslations("Invitation");
  const router = useRouter();
  const [preview, setPreview] = useState<InvitationPreview>();
  const [error, setError] = useState<string | undefined>(token ? undefined : t("invalid"));
  const [acceptanceError, setAcceptanceError] = useState<string>();
  const [isAccepting, setIsAccepting] = useState(false);
  const hasStartedAcceptance = useRef(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    fetch("/api/invitations/remember", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    }).catch(() => undefined);
    teamApi.previewInvitation(token)
      .then(setPreview)
      .catch((reason) => setError(reason instanceof Error ? reason.message : t("loadError")));
  }, [t, token]);

  const acceptInvitation = useCallback(async () => {
    if (!token) return;
    setIsAccepting(true);
    setAcceptanceError(undefined);
    try {
      await teamApi.acceptInvitation(token);
      toast.success(t("accepted"));
      router.replace("/app");
      router.refresh();
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : t("acceptError");
      setAcceptanceError(message);
      toast.error(message);
      hasStartedAcceptance.current = false;
    } finally {
      setIsAccepting(false);
    }
  }, [router, t, token]);

  useEffect(() => {
    if (!authenticated || preview?.status !== "pending" || acceptanceError || hasStartedAcceptance.current) return;
    hasStartedAcceptance.current = true;
    void acceptInvitation();
  }, [acceptInvitation, acceptanceError, authenticated, preview?.status]);

  if (error) {
    return (
      <AuthShell title={t("unavailable")} description={error}>
        <Link href="/" className={buttonVariants({ variant: "outline" }) + " w-full"}>{t("home")}</Link>
      </AuthShell>
    );
  }

  if (!preview) {
    return <AuthShell title={t("loading")} description={t("checking")}>&nbsp;</AuthShell>;
  }

  const nextPath = `/invitations/accept?token=${encodeURIComponent(token ?? "")}`;
  const canAccept = preview.status === "pending";

  return (
    <AuthShell
      title={t("join", { organization: preview.organizationName })}
      description={t("joinDescription", { role: preview.roleName })}
    >
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        {t("sentTo", { email: preview.email })}{!canAccept ? ` ${t("status", { status: t(preview.status === "accepted" ? "acceptedStatus" : preview.status as "pending" | "expired" | "revoked") })}` : null}
      </div>
      {canAccept && authenticated ? (
        acceptanceError ? (
          <div className="mt-5 space-y-3">
            <p className="text-sm text-destructive">{acceptanceError}</p>
            <Button className="w-full" type="button" onClick={() => void acceptInvitation()} disabled={isAccepting}>
              {t("retry")}
            </Button>
          </div>
        ) : (
          <p className="mt-5 text-center text-sm text-muted-foreground">{isAccepting ? t("joining") : t("preparing")}</p>
        )
      ) : canAccept ? (
        <div className="mt-5 space-y-3">
          <Link
            href={`/register?email=${encodeURIComponent(preview.email)}&next=${encodeURIComponent(nextPath)}`}
            className={buttonVariants() + " w-full"}
          >
            {t("create")}
          </Link>
          <p className="text-center text-sm text-muted-foreground">
            {t("hasAccount")}{" "}
            <Link href={`/login?next=${encodeURIComponent(nextPath)}`} className="font-medium text-foreground underline underline-offset-4">
              {t("signin")}
            </Link>
          </p>
        </div>
      ) : null}
    </AuthShell>
  );
}
