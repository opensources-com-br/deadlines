"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { Button, buttonVariants } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type ResetPasswordScreenProps = {
  token?: string;
};

export function ResetPasswordScreen({ token }: ResetPasswordScreenProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }

    const formData = new FormData(event.currentTarget);
    const password = String(formData.get("password"));
    if (password !== String(formData.get("passwordConfirmation"))) {
      toast.error("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await identityApi.resetPassword(token, password);
      toast.success("Your password has been reset. You can now sign in.");
      router.push("/login");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthShell title="This reset link is invalid" description="Request a new password reset link and try again.">
        <Link href="/forgot-password" className={buttonVariants({ size: "lg" })}>
          Request a new link
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Choose a new password" description="Use a strong password you haven’t used before.">
      <form onSubmit={handleSubmit}>
        <FieldGroup>
          <AuthTextField
            id="password"
            name="password"
            label="New password"
            type="password"
            autoComplete="new-password"
            placeholder="Create a new password"
            hint="Use at least 12 characters."
            required
          />
          <AuthTextField
            id="password-confirmation"
            name="passwordConfirmation"
            label="Confirm new password"
            type="password"
            autoComplete="new-password"
            placeholder="Repeat your new password"
            required
          />
          <Field>
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Resetting password..." : "Reset password"}
            </Button>
          </Field>
          <FieldDescription className="text-center">
            <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
              Back to sign in
            </Link>
          </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
