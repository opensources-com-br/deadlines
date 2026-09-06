import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

export function ForgotPasswordScreen() {
  return (
    <AuthShell title="Reset your password" description="Enter your email and we’ll send a reset link if an account exists.">
      <form>
        <FieldGroup>
        <AuthTextField id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <Field>
          <Button className="w-full" type="button">Send reset link</Button>
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
