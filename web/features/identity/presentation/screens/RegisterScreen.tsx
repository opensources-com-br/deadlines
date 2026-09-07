"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type RegisterScreenProps = {
  initialEmail?: string;
  nextPath?: string;
};

export function RegisterScreen({ initialEmail, nextPath }: RegisterScreenProps) {
  const t = useTranslations("Register");
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const password = String(formData.get("password"));
    const passwordConfirmation = String(formData.get("passwordConfirmation"));

    if (password !== passwordConfirmation) {
      toast.error(t("mismatch"));
      return;
    }

    const email = String(formData.get("email"));
    setIsSubmitting(true);
    try {
      await identityApi.register({
        email,
        password,
        firstName: String(formData.get("firstName")),
        lastName: String(formData.get("lastName")),
      });
      toast.success(t("success"));
      const nextQuery = nextPath ? `&next=${encodeURIComponent(nextPath)}` : "";
      router.push(`/check-email?email=${encodeURIComponent(email)}${nextQuery}`);
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title={t("title")} description={t("description")}>
      <form onSubmit={handleSubmit}>
        <FieldGroup>
          <div className="grid gap-5 sm:grid-cols-2">
            <AuthTextField id="first-name" name="firstName" label={t("firstName")} autoComplete="given-name" placeholder={t("firstName")} required /><AuthTextField id="last-name" name="lastName" label={t("lastName")} autoComplete="family-name" placeholder={t("lastName")} required />
          </div>
          <AuthTextField id="email" name="email" label={t("email")} type="email" autoComplete="email" placeholder="you@example.com" defaultValue={initialEmail} readOnly={Boolean(initialEmail)} required />
          <AuthTextField
            id="password"
            name="password"
            label={t("password")}
            type="password"
            autoComplete="new-password"
            placeholder={t("passwordPlaceholder")} hint={t("passwordHelp")}
            required
          />
          <AuthTextField
            id="password-confirmation"
            name="passwordConfirmation"
            label={t("confirmPassword")}
            type="password"
            autoComplete="new-password"
            placeholder={t("confirmPlaceholder")}
            required
          />
          <Field>
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? t("submitting") : t("submit")}
            </Button>
          </Field>
          <FieldDescription className="text-center">
            {t("hasAccount")}{" "}
            <Link href={nextPath ? `/login?next=${encodeURIComponent(nextPath)}` : "/login"} className="font-medium text-foreground underline underline-offset-4">
              {t("signin")}
            </Link>
          </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
