"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { Button, buttonVariants } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type VerifyEmailScreenProps = {
  token?: string;
  hasInvitation: boolean;
};

export function VerifyEmailScreen({ token, hasInvitation }: VerifyEmailScreenProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleVerification() {
    if (!token) {
      return;
    }

    setIsSubmitting(true);
    try {
      await identityApi.verifyEmail(token);
      toast.success(hasInvitation ? "Your email has been confirmed. Sign in to join the organization." : "Your email has been confirmed. You can now sign in.");
      router.push(hasInvitation ? "/invitations/continue" : "/login");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthShell title="This link is invalid" description="Request a new confirmation email and try again.">
        <Link
          href="/check-email"
          className={buttonVariants({ size: "lg" })}
        >
          Send a new link
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Confirm your email" description={hasInvitation ? "Confirm your email address to join the organization." : "Confirm your email address to activate your Deadlines account."}>
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        {hasInvitation ? "Once confirmed, sign in and we will add you to the organization automatically." : "Your confirmation link is ready. Once confirmed, you can sign in to your account."}
      </div>
      <Button className="mt-5 w-full" type="button" onClick={handleVerification} disabled={isSubmitting}>
        {isSubmitting ? "Confirming email..." : "Confirm email"}
      </Button>
    </AuthShell>
  );
}
