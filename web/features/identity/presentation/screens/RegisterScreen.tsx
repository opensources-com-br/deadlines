"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type RegisterScreenProps = {
  initialEmail?: string;
  nextPath?: string;
};

export function RegisterScreen({ initialEmail, nextPath }: RegisterScreenProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const password = String(formData.get("password"));
    const passwordConfirmation = String(formData.get("passwordConfirmation"));

    if (password !== passwordConfirmation) {
      toast.error("Passwords do not match.");
      return;
    }

    const email = String(formData.get("email"));
    setIsSubmitting(true);
    try {
      await identityApi.register({
        email,
        password,
        firstName: String(formData.get("firstName")),
        lastName: String(formData.get("lastName")),
      });
      toast.success("Confirmation email sent.");
      const nextQuery = nextPath ? `&next=${encodeURIComponent(nextPath)}` : "";
      router.push(`/check-email?email=${encodeURIComponent(email)}${nextQuery}`);
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Create your account" description="Start organizing the work that matters.">
      <form onSubmit={handleSubmit}>
        <FieldGroup>
          <div className="grid gap-5 sm:grid-cols-2">
            <AuthTextField id="first-name" name="firstName" label="First name" autoComplete="given-name" placeholder="First name" required />
            <AuthTextField id="last-name" name="lastName" label="Last name" autoComplete="family-name" placeholder="Last name" required />
          </div>
          <AuthTextField id="email" name="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" defaultValue={initialEmail} readOnly={Boolean(initialEmail)} required />
          <AuthTextField
            id="password"
            name="password"
            label="Password"
            type="password"
            autoComplete="new-password"
            placeholder="Create a password"
            hint="Use at least 12 characters."
            required
          />
          <AuthTextField
            id="password-confirmation"
            name="passwordConfirmation"
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            placeholder="Repeat your password"
            required
          />
          <Field>
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Creating account..." : "Create account"}
            </Button>
          </Field>
          <FieldDescription className="text-center">
            Already have an account?{" "}
            <Link href={nextPath ? `/login?next=${encodeURIComponent(nextPath)}` : "/login"} className="font-medium text-foreground underline underline-offset-4">
              Sign in
            </Link>
          </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
