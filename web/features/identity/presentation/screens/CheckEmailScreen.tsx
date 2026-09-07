"use client";

import Link from "next/link";
import { useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type CheckEmailScreenProps = {
  email?: string;
  nextPath?: string;
};

export function CheckEmailScreen({ email, nextPath }: CheckEmailScreenProps) {
  const t = useTranslations("CheckEmail");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleResend() {
    if (!email) {
      toast.error(t("missing"));
      return;
    }

    setIsSubmitting(true);
    try {
      await identityApi.resendVerification(email);
      toast.success(t("sent"));
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell title={t("title")} description={t("description")}>
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        {nextPath ? t("invitationHelp") : t("standardHelp")}
      </div>
      <Button className="mt-5 w-full" type="button" onClick={handleResend} disabled={isSubmitting}>
        {isSubmitting ? t("sending") : t("resend")}
      </Button>
      <p className="mt-6 text-sm text-muted-foreground">
        {t("confirmed")}{" "}
        <Link href={nextPath ? `/login?next=${encodeURIComponent(nextPath)}` : "/login"} className="font-medium text-foreground underline underline-offset-4">
          {t("signin")}
        </Link>
      </p>
    </AuthShell>
  );
}
