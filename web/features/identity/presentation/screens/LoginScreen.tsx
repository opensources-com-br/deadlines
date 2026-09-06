import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";

export function LoginScreen() {
  return (
    <AuthShell title="Login to your account" description="Enter your email below to login to your account.">
      <form>
        <FieldGroup>
          <AuthTextField
            id="email"
            label="Email"
            type="email"
            autoComplete="email"
            placeholder="m@example.com"
            required
          />
          <AuthTextField
            id="password"
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            action={
              <Link href="/forgot-password" className="text-sm underline-offset-4 hover:underline">
                Forgot your password?
              </Link>
            }
          />
          <Field>
            <Button className="w-full" type="button">
              Login
            </Button>
            <FieldDescription className="text-center">
              Don&apos;t have an account?{" "}
              <Link href="/register" className="underline-offset-4 hover:underline">
                Sign up
              </Link>
            </FieldDescription>
          </Field>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
