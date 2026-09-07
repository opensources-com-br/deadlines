"use client";

import { type FormEvent, type ReactNode, useState } from "react";
import { AlertTriangle, PauseCircle, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogMedia,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  AccountActionError,
  deactivateAccount,
  deleteAccount,
} from "@/features/platform/infrastructure/profile-api";

type Action = "deactivate" | "delete";

export function AccountDangerZone() {
  const t = useTranslations("Account");
  return (
    <Card className="border-destructive/30">
      <CardHeader>
        <CardTitle>{t("dangerZone")}</CardTitle>
        <CardDescription>{t("dangerZoneDescription")}</CardDescription>
      </CardHeader>
      <CardContent className="divide-y rounded-lg border border-destructive/20 p-0">
        <ActionRow
          icon={<PauseCircle />}
          title={t("deactivateAccount")}
          description={t("deactivateDescription")}
          action="deactivate"
          buttonLabel={t("deactivate")}
        />
        <ActionRow
          icon={<Trash2 />}
          title={t("deleteAccount")}
          description={t("deleteDescription")}
          action="delete"
          buttonLabel={t("deletePermanently")}
        />
      </CardContent>
    </Card>
  );
}

function ActionRow({ icon, title, description, action, buttonLabel }: {
  icon: ReactNode;
  title: string;
  description: string;
  action: Action;
  buttonLabel: string;
}) {
  const t = useTranslations("Account");
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function close(nextOpen: boolean) {
    if (isSubmitting) return;
    setOpen(nextOpen);
    if (!nextOpen) setPassword("");
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      if (action === "deactivate") await deactivateAccount(password);
      else await deleteAccount(password);
      window.location.replace("/login");
    } catch (error) {
      const message = error instanceof AccountActionError && error.code === "ACCOUNT_OWNER_CONFLICT"
        ? t("ownerConflict")
        : error instanceof AccountActionError && error.code === "INVALID_CURRENT_PASSWORD"
          ? t("invalidAccountPassword")
          : error instanceof Error ? error.message : t("accountActionError");
      toast.error(message);
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex gap-3">
        <div className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-md bg-destructive/10 text-destructive [&>svg]:size-4">{icon}</div>
        <div><p className="font-medium">{title}</p><p className="mt-1 text-sm text-muted-foreground">{description}</p></div>
      </div>
      <AlertDialog open={open} onOpenChange={close}>
        <AlertDialogTrigger render={<Button variant="outline" className="shrink-0 text-destructive hover:text-destructive" />}>
          {buttonLabel}
        </AlertDialogTrigger>
        <AlertDialogContent>
          <form onSubmit={submit}>
            <AlertDialogHeader>
              <AlertDialogMedia className="bg-destructive/10 text-destructive"><AlertTriangle /></AlertDialogMedia>
              <AlertDialogTitle>{action === "deactivate" ? t("confirmDeactivate") : t("confirmDelete")}</AlertDialogTitle>
              <AlertDialogDescription>{action === "deactivate" ? t("confirmDeactivateDescription") : t("confirmDeleteDescription")}</AlertDialogDescription>
            </AlertDialogHeader>
            <Field className="my-4">
              <FieldLabel htmlFor={`${action}-password`}>{t("accountPassword")}</FieldLabel>
              <Input id={`${action}-password`} type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required autoFocus />
            </Field>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={isSubmitting}>{t("cancel")}</AlertDialogCancel>
              <AlertDialogAction type="submit" variant="destructive" disabled={isSubmitting || !password}>
                {isSubmitting ? t("processing") : buttonLabel}
              </AlertDialogAction>
            </AlertDialogFooter>
          </form>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
