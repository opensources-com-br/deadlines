import Link from "next/link";

import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";

export function RegisterScreen() {
  return (
    <AuthShell title="Create your account" description="Start organizing the work that matters.">
      <form className="grid gap-5">
        <div className="grid gap-5 sm:grid-cols-2">
          <Input id="first-name" label="First name" autoComplete="given-name" placeholder="First name" required />
          <Input id="last-name" label="Last name" autoComplete="family-name" placeholder="Last name" required />
        </div>
        <Input id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <Input
          id="password"
          label="Password"
          type="password"
          autoComplete="new-password"
          placeholder="Create a password"
          hint="Use at least 12 characters."
          required
        />
        <Input
          id="password-confirmation"
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          placeholder="Repeat your password"
          required
        />
        <Button className="mt-1 w-full">Create account</Button>
      </form>
      <p className="mt-6 text-sm text-zinc-600">
        Already have an account?{" "}
        <Link href="/login" className="font-medium text-zinc-950 underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
