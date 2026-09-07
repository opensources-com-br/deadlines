"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button, buttonVariants } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";

type VerifyEmailScreenProps = {
  token?: string;
  hasInvitation: boolean;
};

export function VerifyEmailScreen({ token, hasInvitation }: VerifyEmailScreenProps) {
  const t = useTranslations("VerifyEmail");
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleVerification() {
    if (!token) {
      return;
    }

    setIsSubmitting(true);
    try {
      await identityApi.verifyEmail(token);
      toast.success(hasInvitation ? t("invitationSuccess") : t("success"));
      router.push(hasInvitation ? "/invitations/continue" : "/login");
    } catch (error) {
      toast.error(identityErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthShell title={t("invalidTitle")} description={t("invalidDescription")}>
        <Link
          href="/check-email"
          className={buttonVariants({ size: "lg" })}
        >
          {t("newLink")}
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title={t("title")} description={hasInvitation ? t("invitationDescription") : t("standardDescription")}>
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        {hasInvitation ? t("invitationHelp") : t("standardHelp")}
      </div>
      <Button className="mt-5 w-full" type="button" onClick={handleVerification} disabled={isSubmitting}>
        {isSubmitting ? t("submitting") : t("submit")}
      </Button>
    </AuthShell>
  );
}
