"use client";

import { CalendarIcon } from "lucide-react";
import { useState, type FormEvent } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Field, FieldLabel } from "@/components/ui/field";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import type { AuditPage } from "@/features/audits/domain/audit";
import type { OrganizationMember } from "@/features/team/domain/team";

const actions: Record<string, string> = {
  "organization.updated": "Organization updated",
  "member.role_updated": "Member role changed",
  "member.removed": "Member removed",
  "invitation.created": "Invitation created",
  "invitation.resent": "Invitation renewed",
  "invitation.revoked": "Invitation revoked",
  "invitation.accepted": "Invitation accepted",
  "role.created": "Role created",
  "role.updated": "Role updated",
  "role.deleted": "Role deleted",
  "permission.created": "Permission created",
  "permission.updated": "Permission updated",
  "permission.deleted": "Permission deleted",
  "role.permission_added": "Permission added to role",
  "role.permission_removed": "Permission removed from role",
};

type Filters = { action: string; actorId: string; resourceId: string; from: string; to: string };
const emptyFilters: Filters = { action: "", actorId: "", resourceId: "", from: "", to: "" };

function localDateValue(date: Date, endOfDay: boolean) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}T${endOfDay ? "23:59:59" : "00:00:00"}`;
}

function AuditDatePicker({ label, value, endOfDay, disabled, placeholder, clearLabel, onChange }: {
  label: string;
  value: string;
  endOfDay: boolean;
  disabled: boolean;
  placeholder: string;
  clearLabel: string;
  onChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const selected = value ? new Date(value) : undefined;

  return (
    <Field>
      <FieldLabel>{label}</FieldLabel>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          disabled={disabled}
          render={<Button type="button" variant="outline" className="w-full justify-start font-normal" />}
        >
          <CalendarIcon className="text-muted-foreground" aria-hidden="true" />
          {selected
            ? new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(selected)
            : <span className="text-muted-foreground">{placeholder}</span>}
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto p-0">
          <Calendar
            mode="single"
            selected={selected}
            onSelect={(date) => {
              if (!date) return;
              onChange(localDateValue(date, endOfDay));
              setOpen(false);
            }}
          />
          {value ? (
            <div className="border-t p-2">
              <Button type="button" variant="ghost" size="sm" className="w-full" onClick={() => { onChange(""); setOpen(false); }}>
                {clearLabel}
              </Button>
            </div>
          ) : null}
        </PopoverContent>
      </Popover>
    </Field>
  );
}

export function AuditsCard({ members }: { members: OrganizationMember[] }) {
  const t = useTranslations("Security");
  const locale = useLocale();
  const [page, setPage] = useState<AuditPage | null>(null);
  const [filters, setFilters] = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [opened, setOpened] = useState(false);

  async function load(offset: number, nextFilters = appliedFilters) {
    setOpened(true);
    setIsLoading(true);
    setError("");
    try {
      if (nextFilters.from && nextFilters.to && nextFilters.from > nextFilters.to) {
        throw new Error(t("dateOrderError"));
      }
      const params = new URLSearchParams({ offset: String(offset), limit: "10" });
      for (const [key, value] of Object.entries(nextFilters)) {
        if (value) params.set(key, key === "from" || key === "to" ? new Date(value).toISOString() : value.trim());
      }
      const response = await fetch(`/api/audits?${params}`, { cache: "no-store" });
      if (!response.ok) {
        throw new Error(response.status === 403 ? t("ownerOnly")
          : response.status === 401 ? t("sessionExpired")
          : response.status === 422 ? t("invalidFilters")
          : t("historyError"));
      }
      setPage(await response.json() as AuditPage);
      setAppliedFilters(nextFilters);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : t("historyError"));
    } finally {
      setIsLoading(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void load(0, filters);
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("history")}</CardTitle><CardDescription>{t("historyDescription")}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        {!opened ? <Button variant="outline" onClick={() => void load(0)}>{t("viewHistory")}</Button> : <>
          <form onSubmit={submit} className="space-y-4">
            <fieldset disabled={isLoading} className="grid min-w-0 gap-4 sm:grid-cols-2">
              <legend className="sr-only">{t("filterLegend")}</legend>
              <Field className="sm:col-span-2">
                <FieldLabel>{t("action")}</FieldLabel>
                <Select
                  value={filters.action || "all"}
                  onValueChange={(value) => setFilters({ ...filters, action: value === "all" || value === null ? "" : value })}
                >
                  <SelectTrigger className="w-full" aria-label={t("action")}>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent align="start">
                    <SelectItem value="all">{t("allActions")}</SelectItem>
                    {Object.keys(actions).map((value) => <SelectItem key={value} value={value}>{t(`actions.${value}`)}</SelectItem>)}
                  </SelectContent>
                </Select>
              </Field>
              {([ ["actorId", t("actorId"), "text"], ["resourceId", t("resourceId"), "text"]] as const).map(([key, label, type]) => (
                <Field key={key}>
                  <FieldLabel htmlFor={`audit-${key}`}>{label}</FieldLabel>
                  <Input id={`audit-${key}`} type={type} value={filters[key]}
                    onChange={(event) => setFilters({ ...filters, [key]: event.target.value })} />
                </Field>
              ))}
              <AuditDatePicker label={t("from")} placeholder={t("selectDate")} clearLabel={t("clearDate")} value={filters.from} endOfDay={false} disabled={isLoading}
                onChange={(from) => setFilters({ ...filters, from })} />
              <AuditDatePicker label={t("to")} placeholder={t("selectDate")} clearLabel={t("clearDate")} value={filters.to} endOfDay disabled={isLoading}
                onChange={(to) => setFilters({ ...filters, to })} />
            </fieldset>
            <div className="flex flex-wrap gap-2">
              <Button type="submit" disabled={isLoading}>{t("apply")}</Button><Button type="button" variant="outline" disabled={isLoading} onClick={() => { setFilters(emptyFilters); void load(0, emptyFilters); }}>{t("clear")}</Button><Button type="button" variant="outline" disabled={isLoading} onClick={() => void load(0)}>{t("refresh")}</Button>
            </div>
          </form>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <div aria-live="polite" aria-busy={isLoading} className="space-y-4">
            {isLoading ? <p className="text-sm text-muted-foreground">{t("loadingHistory")}</p> : !error && page && <>
              {page.data.length === 0 ? <p className="text-sm text-muted-foreground">{t("noEvents")}</p> :
                <ol className="space-y-4">
                  {page.data.map((event) => {
                    const actor = members.find((member) => member.userId === event.actorId);
                    return <li key={event.id} className="min-w-0 rounded-lg border p-4 text-sm">
                      <p className="font-medium">{actions[event.action] ? t(`actions.${event.action}`) : event.action}</p>
                      <time dateTime={event.occurredAt} className="text-muted-foreground">{new Date(event.occurredAt).toLocaleString(locale)}</time>
                      <p className="mt-2 break-all">{t("by", { actor: actor ? `${actor.firstName} ${actor.lastName}` : event.actorId ?? t("systemMaintenance") })}</p>
                      <details className="mt-3">
                        <summary className="cursor-pointer text-muted-foreground">{t("eventDetails")}</summary>
                        <dl className="mt-2 space-y-2 break-all">
                          <div><dt className="text-muted-foreground">{t("resource")}</dt><dd>{event.resource} · {event.resourceId}</dd></div><div><dt className="text-muted-foreground">{t("actorId")}</dt><dd>{event.actorId ?? t("systemMaintenance")}</dd></div><div><dt className="text-muted-foreground">{t("eventId")}</dt><dd>{event.id}</dd></div>
                          {Object.entries(event.metadata).map(([key, value]) => <div key={key}><dt className="text-muted-foreground">{key}</dt><dd>{String(value)}</dd></div>)}
                        </dl>
                      </details>
                    </li>;
                  })}
                </ol>}
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Button variant="outline" disabled={page.offset === 0} onClick={() => void load(Math.max(0, page.offset - page.limit))}>{t("previous")}</Button><span className="text-sm text-muted-foreground">{t("page", { page: Math.floor(page.offset / page.limit) + 1 })}</span><Button variant="outline" disabled={!page.hasMore} onClick={() => void load(page.offset + page.limit)}>{t("next")}</Button>
              </div>
            </>}
          </div>
        </>}
      </CardContent>
    </Card>
  );
}
