import Link from "next/link";

import { Button } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

export function ForgotPasswordScreen() {
  return (
    <AuthShell title="Reset your password" description="Enter your email and we’ll send a reset link if an account exists.">
      <form className="grid gap-5">
        <AuthTextField id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <Button className="mt-1 w-full" size="lg">Send reset link</Button>
      </form>
      <p className="mt-6 text-sm text-muted-foreground">
        Remembered your password?{" "}
        <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
