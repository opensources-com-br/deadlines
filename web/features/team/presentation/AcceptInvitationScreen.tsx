"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "sonner";

import { Button, buttonVariants } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import type { InvitationPreview } from "@/features/team/domain/team";
import { teamApi } from "@/features/team/infrastructure/team-api";

type AcceptInvitationScreenProps = {
  token?: string;
  authenticated: boolean;
};

export function AcceptInvitationScreen({ token, authenticated }: AcceptInvitationScreenProps) {
  const router = useRouter();
  const [preview, setPreview] = useState<InvitationPreview>();
  const [error, setError] = useState<string | undefined>(token ? undefined : "This invitation link is invalid.");
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
      .catch((reason) => setError(reason instanceof Error ? reason.message : "Unable to load this invitation."));
  }, [token]);

  const acceptInvitation = useCallback(async () => {
    if (!token) return;
    setIsAccepting(true);
    setAcceptanceError(undefined);
    try {
      await teamApi.acceptInvitation(token);
      toast.success("Invitation accepted. Welcome to the organization.");
      router.replace("/app");
      router.refresh();
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : "Unable to join this organization.";
      setAcceptanceError(message);
      toast.error(message);
      hasStartedAcceptance.current = false;
    } finally {
      setIsAccepting(false);
    }
  }, [router, token]);

  useEffect(() => {
    if (!authenticated || preview?.status !== "pending" || acceptanceError || hasStartedAcceptance.current) return;
    hasStartedAcceptance.current = true;
    void acceptInvitation();
  }, [acceptInvitation, acceptanceError, authenticated, preview?.status]);

  if (error) {
    return (
      <AuthShell title="Invitation unavailable" description={error}>
        <Link href="/" className={buttonVariants({ variant: "outline" }) + " w-full"}>Return home</Link>
      </AuthShell>
    );
  }

  if (!preview) {
    return <AuthShell title="Loading invitation" description="Checking your invitation details...">&nbsp;</AuthShell>;
  }

  const nextPath = `/invitations/accept?token=${encodeURIComponent(token ?? "")}`;
  const canAccept = preview.status === "pending";

  return (
    <AuthShell
      title={`Join ${preview.organizationName}`}
      description={`Create an account to join as ${preview.roleName}.`}
    >
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        This invitation was sent to <span className="font-medium text-foreground">{preview.email}</span>.
        {!canAccept ? ` Its current status is ${preview.status}.` : null}
      </div>
      {canAccept && authenticated ? (
        acceptanceError ? (
          <div className="mt-5 space-y-3">
            <p className="text-sm text-destructive">{acceptanceError}</p>
            <Button className="w-full" type="button" onClick={() => void acceptInvitation()} disabled={isAccepting}>
              Try again
            </Button>
          </div>
        ) : (
          <p className="mt-5 text-center text-sm text-muted-foreground">{isAccepting ? "Joining organization..." : "Preparing your organization..."}</p>
        )
      ) : canAccept ? (
        <div className="mt-5 space-y-3">
          <Link
            href={`/register?email=${encodeURIComponent(preview.email)}&next=${encodeURIComponent(nextPath)}`}
            className={buttonVariants() + " w-full"}
          >
            Create invited account
          </Link>
          <p className="text-center text-sm text-muted-foreground">
            Already have an account?{" "}
            <Link href={`/login?next=${encodeURIComponent(nextPath)}`} className="font-medium text-foreground underline underline-offset-4">
              Sign in
            </Link>
          </p>
        </div>
      ) : null}
    </AuthShell>
  );
}
