"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
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
  const [isAccepting, setIsAccepting] = useState(false);

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

  async function acceptInvitation() {
    if (!token) return;
    setIsAccepting(true);
    try {
      await teamApi.acceptInvitation(token);
      toast.success("Invitation accepted. Welcome to the organization.");
      router.replace("/app");
      router.refresh();
    } catch (reason) {
      toast.error(reason instanceof Error ? reason.message : "Unable to accept this invitation.");
    } finally {
      setIsAccepting(false);
    }
  }

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
      description={`You were invited as ${preview.roleName}.`}
    >
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        This invitation was sent to <span className="font-medium text-foreground">{preview.email}</span>.
        {!canAccept ? ` Its current status is ${preview.status}.` : null}
      </div>
      {canAccept && authenticated ? (
        <Button className="mt-5 w-full" type="button" onClick={acceptInvitation} disabled={isAccepting}>
          {isAccepting ? "Joining..." : "Accept invitation"}
        </Button>
      ) : canAccept ? (
        <div className="mt-5 space-y-3">
          <Link href={`/login?next=${encodeURIComponent(nextPath)}`} className={buttonVariants() + " w-full"}>Sign in to accept</Link>
          <Link
            href={`/register?email=${encodeURIComponent(preview.email)}&next=${encodeURIComponent(nextPath)}`}
            className={buttonVariants({ variant: "outline" }) + " w-full"}
          >
            Create invited account
          </Link>
        </div>
      ) : null}
    </AuthShell>
  );
}
