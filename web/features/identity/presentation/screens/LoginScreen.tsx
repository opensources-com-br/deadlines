"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type LoginScreenProps = {
  nextPath?: string;
};

export function LoginScreen({ nextPath }: LoginScreenProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    setIsSubmitting(true);
    try {
      await identityApi.login({
        email: String(formData.get("email")),
        password: String(formData.get("password")),
      });
      toast.success("Login successful.");
      router.push(nextPath ?? "/app");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title="Login to your account" description="Enter your email below to login to your account.">
      <form onSubmit={handleSubmit}>
        <FieldGroup>
          <AuthTextField
            id="email"
            name="email"
            label="Email"
            type="email"
            autoComplete="email"
            placeholder="m@example.com"
            required
          />
          <AuthTextField
            id="password"
            name="password"
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
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Signing in..." : "Login"}
            </Button>
            <FieldDescription className="text-center">
              Don&apos;t have an account?{" "}
              <Link
                href={nextPath ? `/register?next=${encodeURIComponent(nextPath)}` : "/register"}
                className="underline-offset-4 hover:underline"
              >
                Sign up
              </Link>
            </FieldDescription>
          </Field>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
