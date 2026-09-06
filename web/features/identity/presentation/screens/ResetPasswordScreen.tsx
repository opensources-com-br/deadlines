import Link from "next/link";

import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { Button, buttonClassName } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";

type ResetPasswordScreenProps = {
  hasToken: boolean;
};

export function ResetPasswordScreen({ hasToken }: ResetPasswordScreenProps) {
  if (!hasToken) {
    return (
      <AuthShell title="This reset link is invalid" description="Request a new password reset link and try again.">
        <Link href="/forgot-password" className={buttonClassName}>
          Request a new link
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Choose a new password" description="Use a strong password you haven’t used before.">
      <form className="grid gap-5">
        <Input
          id="password"
          label="New password"
          type="password"
          autoComplete="new-password"
          placeholder="Create a new password"
          hint="Use at least 12 characters."
          required
        />
        <Input
          id="password-confirmation"
          label="Confirm new password"
          type="password"
          autoComplete="new-password"
          placeholder="Repeat your new password"
          required
        />
        <Button className="mt-1 w-full">Reset password</Button>
      </form>
      <p className="mt-6 text-sm text-zinc-600">
        <Link href="/login" className="font-medium text-zinc-950 underline underline-offset-4">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
