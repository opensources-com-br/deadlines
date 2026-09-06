import Link from "next/link";

import { Button, buttonVariants } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

type ResetPasswordScreenProps = {
  hasToken: boolean;
};

export function ResetPasswordScreen({ hasToken }: ResetPasswordScreenProps) {
  if (!hasToken) {
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
      <form>
        <FieldGroup>
        <AuthTextField
          id="password"
          label="New password"
          type="password"
          autoComplete="new-password"
          placeholder="Create a new password"
          hint="Use at least 12 characters."
          required
        />
        <AuthTextField
          id="password-confirmation"
          label="Confirm new password"
          type="password"
          autoComplete="new-password"
          placeholder="Repeat your new password"
          required
        />
        <Field>
          <Button className="w-full" type="button">Reset password</Button>
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
