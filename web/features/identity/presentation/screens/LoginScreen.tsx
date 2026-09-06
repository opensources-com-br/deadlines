import Link from "next/link";

import { Button } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

export function LoginScreen() {
  return (
    <AuthShell title="Welcome back" description="Enter your details to access your workspace.">
      <form className="grid gap-5">
        <AuthTextField id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <AuthTextField
          id="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          placeholder="Enter your password"
          required
          action={
            <Link href="/forgot-password" className="text-sm font-medium text-muted-foreground underline underline-offset-4">
              Forgot password?
            </Link>
          }
        />
        <Button className="mt-1 w-full" size="lg">
          Sign in
        </Button>
      </form>
      <p className="mt-6 text-sm text-muted-foreground">
        New to Deadlines?{" "}
        <Link href="/register" className="font-medium text-foreground underline underline-offset-4">
          Create an account
        </Link>
      </p>
    </AuthShell>
  );
}
