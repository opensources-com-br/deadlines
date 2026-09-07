"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button, buttonVariants } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { AuthTextField } from "@/features/identity/presentation/components/AuthTextField";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type ResetPasswordScreenProps = {
  token?: string;
};

export function ResetPasswordScreen({ token }: ResetPasswordScreenProps) {
  const t = useTranslations("ResetPassword");
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }

    const formData = new FormData(event.currentTarget);
    const password = String(formData.get("password"));
    if (password !== String(formData.get("passwordConfirmation"))) {
      toast.error(t("mismatch"));
      return;
    }

    setIsSubmitting(true);
    try {
      await identityApi.resetPassword(token, password);
      toast.success(t("success"));
      router.push("/login");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthShell title={t("invalidTitle")} description={t("invalidDescription")}>
        <Link href="/forgot-password" className={buttonVariants({ size: "lg" })}>
          {t("request")}
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title={t("title")} description={t("description")}>
      <form onSubmit={handleSubmit}>
        <FieldGroup>
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
            label={t("confirm")}
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
            <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
              {t("signin")}
            </Link>
          </FieldDescription>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
