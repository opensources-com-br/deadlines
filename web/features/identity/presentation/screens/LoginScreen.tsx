import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup, FieldSeparator } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

export function LoginScreen() {
  return (
    <AuthShell title="Login to your account" description="Enter your email below to login to your account.">
      <form>
        <FieldGroup>
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
        <Field>
          <Button className="w-full" type="button">
            Login
          </Button>
        </Field>
        <FieldSeparator>Or continue with</FieldSeparator>
        <Field>
          <Button className="w-full" variant="outline" type="button">
            Continue with Google
          </Button>
        </Field>
        <FieldDescription className="text-center">
          Don&apos;t have an account?{" "}
          <Link href="/register" className="font-medium text-foreground underline underline-offset-4">
            Sign up
          </Link>
        </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
