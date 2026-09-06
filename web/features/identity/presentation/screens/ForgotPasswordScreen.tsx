import Link from "next/link";

import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";

export function ForgotPasswordScreen() {
  return (
    <AuthShell title="Reset your password" description="Enter your email and we’ll send a reset link if an account exists.">
      <form className="grid gap-5">
        <Input id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <Button className="mt-1 w-full">Send reset link</Button>
      </form>
      <p className="mt-6 text-sm text-zinc-600">
        Remembered your password?{" "}
        <Link href="/login" className="font-medium text-zinc-950 underline underline-offset-4">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
