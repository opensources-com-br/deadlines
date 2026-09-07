"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

export function ForgotPasswordScreen() {
  const t = useTranslations("ForgotPassword");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    setIsSubmitting(true);
    try {
      await identityApi.requestPasswordReset(String(formData.get("email")));
      toast.success(t("sent"));
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
          <AuthTextField
            id="email"
            name="email"
            label={t("email")}
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            required
          />
          <Field>
            <Button className="w-full" type="submit" disabled={isSubmitting}>
              {isSubmitting ? t("sending") : t("submit")}
            </Button>
          </Field>
          <FieldDescription className="text-center">
            {t("remembered")}{" "}
            <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
              {t("signin")}
            </Link>
          </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
