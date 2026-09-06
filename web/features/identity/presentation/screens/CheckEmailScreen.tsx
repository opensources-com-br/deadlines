"use client";

import Link from "next/link";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type CheckEmailScreenProps = {
  email?: string;
  nextPath?: string;
};

export function CheckEmailScreen({ email, nextPath }: CheckEmailScreenProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleResend() {
    if (!email) {
      toast.error("Your email is missing. Create your account again to request a new confirmation link.");
      return;
    }

    setIsSubmitting(true);
    try {
      await identityApi.resendVerification(email);
      toast.success("If an account exists, a confirmation email has been sent.");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Check your inbox" description="We sent a confirmation link to your email address.">
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        Your account will be ready once you confirm your email. {nextPath ? "Then sign in and we will add you to the organization automatically." : "The link expires after a limited time for your security."}
      </div>
      <Button className="mt-5 w-full" type="button" onClick={handleResend} disabled={isSubmitting}>
        {isSubmitting ? "Sending confirmation email..." : "Resend confirmation email"}
      </Button>
      <p className="mt-6 text-sm text-muted-foreground">
        Already confirmed your email?{" "}
        <Link href={nextPath ? `/login?next=${encodeURIComponent(nextPath)}` : "/login"} className="font-medium text-foreground underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
