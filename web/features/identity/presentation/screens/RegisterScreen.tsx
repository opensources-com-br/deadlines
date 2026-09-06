import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

export function RegisterScreen() {
  return (
    <AuthShell title="Create your account" description="Start organizing the work that matters.">
      <form>
        <FieldGroup>
        <div className="grid gap-5 sm:grid-cols-2">
          <AuthTextField id="first-name" label="First name" autoComplete="given-name" placeholder="First name" required />
          <AuthTextField id="last-name" label="Last name" autoComplete="family-name" placeholder="Last name" required />
        </div>
        <AuthTextField id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <AuthTextField
          id="password"
          label="Password"
          type="password"
          autoComplete="new-password"
          placeholder="Create a password"
          hint="Use at least 12 characters."
          required
        />
        <AuthTextField
          id="password-confirmation"
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          placeholder="Repeat your password"
          required
        />
        <Field>
          <Button className="w-full" type="button">Create account</Button>
        </Field>
        <FieldDescription className="text-center">
          Already have an account?{" "}
          <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
            Sign in
          </Link>
        </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
