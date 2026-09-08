"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

import { Button, buttonVariants } from "@/components/ui/button";
import { identityApi, identityErrorMessage } from "@/features/identity/infrastructure/identity-api";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";

export function ConfirmEmailChangeScreen({ token }: { token?: string }) {
  const t = useTranslations("ConfirmEmailChange");
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  async function confirm() {
    if (!token) return;
    setIsSubmitting(true);
    try { await identityApi.confirmEmailChange(token); toast.success(t("success")); router.push("/app/account"); }
    catch (error) { toast.error(identityErrorMessage(error)); }
    finally { setIsSubmitting(false); }
  }
  if (!token) return <AuthShell title={t("invalidTitle")} description={t("invalidDescription")}><Link href="/login" className={buttonVariants({ size: "lg" })}>{t("login")}</Link></AuthShell>;
  return <AuthShell title={t("title")} description={t("description")}><Button className="mt-5 w-full" type="button" onClick={confirm} disabled={isSubmitting}>{isSubmitting ? t("submitting") : t("submit")}</Button></AuthShell>;
}
