"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type ForgotPasswordScreenProps = {
  initialEmail?: string;
};

export function ForgotPasswordScreen({ initialEmail }: ForgotPasswordScreenProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    setIsSubmitting(true);
    try {
      await identityApi.requestPasswordReset(String(formData.get("email")));
      toast.success("If an account exists, a password reset link has been sent.");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Reset your password" description="Enter your email and we’ll send a reset link if an account exists.">
      <form onSubmit={handleSubmit}>
        <FieldGroup>
          <AuthTextField
            id="email"
            name="email"
            label="Email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            defaultValue={initialEmail}
            required
          />
          <Field>
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Sending reset link..." : "Send reset link"}
            </Button>
          </Field>
          <FieldDescription className="text-center">
            Remembered your password?{" "}
            <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
              Back to sign in
            </Link>
          </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
