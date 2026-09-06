"use client";

import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Field, FieldLabel } from "@/components/ui/field";
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

export function AuditsCard({ members }: { members: OrganizationMember[] }) {
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
        throw new Error("The end date must be on or after the start date.");
      }
      const params = new URLSearchParams({ offset: String(offset), limit: "10" });
      for (const [key, value] of Object.entries(nextFilters)) {
        if (value) params.set(key, key === "from" || key === "to" ? new Date(value).toISOString() : value.trim());
      }
      const response = await fetch(`/api/audits?${params}`, { cache: "no-store" });
      if (!response.ok) {
        throw new Error(response.status === 403 ? "Only the organization owner can view this history."
          : response.status === 401 ? "Your session has expired. Please log in again."
          : response.status === 422 ? "Check the filters: IDs must be valid UUIDs."
          : "Unable to load history. Please try again.");
      }
      setPage(await response.json() as AuditPage);
      setAppliedFilters(nextFilters);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to load history.");
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
        <CardTitle>Organization history</CardTitle>
        <CardDescription>Review who changed your organization, team, and access settings.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        {!opened ? <Button variant="outline" onClick={() => void load(0)}>View history</Button> : <>
          <form onSubmit={submit} className="space-y-4">
            <fieldset disabled={isLoading} className="grid min-w-0 gap-4 sm:grid-cols-2">
              <legend className="sr-only">Filter organization history</legend>
              <Field className="sm:col-span-2">
                <FieldLabel htmlFor="audit-action">Action</FieldLabel>
                <select id="audit-action" className="h-9 w-full rounded-md border bg-background px-3 text-sm" value={filters.action}
                  onChange={(event) => setFilters({ ...filters, action: event.target.value })}>
                  <option value="">All actions</option>
                  {Object.entries(actions).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </Field>
              {([ ["actorId", "Actor ID", "text"], ["resourceId", "Resource ID", "text"],
                ["from", "From (local time)", "datetime-local"], ["to", "To (local time)", "datetime-local"]] as const).map(([key, label, type]) => (
                <Field key={key}>
                  <FieldLabel htmlFor={`audit-${key}`}>{label}</FieldLabel>
                  <Input id={`audit-${key}`} type={type} value={filters[key]}
                    onChange={(event) => setFilters({ ...filters, [key]: event.target.value })} />
                </Field>
              ))}
            </fieldset>
            <div className="flex flex-wrap gap-2">
              <Button type="submit" disabled={isLoading}>Apply filters</Button>
              <Button type="button" variant="outline" disabled={isLoading} onClick={() => { setFilters(emptyFilters); void load(0, emptyFilters); }}>Clear</Button>
              <Button type="button" variant="outline" disabled={isLoading} onClick={() => void load(0)}>Refresh</Button>
            </div>
          </form>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <div aria-live="polite" aria-busy={isLoading} className="space-y-4">
            {isLoading ? <p className="text-sm text-muted-foreground">Loading history…</p> : !error && page && <>
              {page.data.length === 0 ? <p className="text-sm text-muted-foreground">No events found for these filters. History starts when auditing was enabled.</p> :
                <ol className="space-y-4">
                  {page.data.map((event) => {
                    const actor = members.find((member) => member.userId === event.actorId);
                    return <li key={event.id} className="min-w-0 rounded-lg border p-4 text-sm">
                      <p className="font-medium">{actions[event.action] ?? event.action}</p>
                      <time dateTime={event.occurredAt} className="text-muted-foreground">{new Date(event.occurredAt).toLocaleString()}</time>
                      <p className="mt-2 break-all">By {actor ? `${actor.firstName} ${actor.lastName}` : event.actorId ?? "System / maintenance"}</p>
                      <details className="mt-3">
                        <summary className="cursor-pointer text-muted-foreground">Event details</summary>
                        <dl className="mt-2 space-y-2 break-all">
                          <div><dt className="text-muted-foreground">Resource</dt><dd>{event.resource} · {event.resourceId}</dd></div>
                          <div><dt className="text-muted-foreground">Actor ID</dt><dd>{event.actorId ?? "System / maintenance"}</dd></div>
                          <div><dt className="text-muted-foreground">Event ID</dt><dd>{event.id}</dd></div>
                          {Object.entries(event.metadata).map(([key, value]) => <div key={key}><dt className="text-muted-foreground">{key}</dt><dd>{String(value)}</dd></div>)}
                        </dl>
                      </details>
                    </li>;
                  })}
                </ol>}
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Button variant="outline" disabled={page.offset === 0} onClick={() => void load(Math.max(0, page.offset - page.limit))}>Previous</Button>
                <span className="text-sm text-muted-foreground">Page {Math.floor(page.offset / page.limit) + 1}</span>
                <Button variant="outline" disabled={!page.hasMore} onClick={() => void load(page.offset + page.limit)}>Next</Button>
              </div>
            </>}
          </div>
        </>}
      </CardContent>
    </Card>
  );
}
